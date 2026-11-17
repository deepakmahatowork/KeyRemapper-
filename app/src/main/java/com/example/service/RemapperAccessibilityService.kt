package com.example.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.example.data.preferences.RemapperPreferences
import com.example.data.preferences.RemapperPreferences.Companion.GLOBAL_PACKAGE
import com.example.data.preferences.RemapperSettings
import com.example.data.repository.RemapperRepository
import com.example.model.ActionType
import com.example.model.ButtonGesture
import com.example.model.RemapRule
import com.example.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RemapperAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var repository: RemapperRepository
    private lateinit var constraintEvaluator: ConstraintEvaluator
    private lateinit var actionExecutor: ActionExecutor
    private lateinit var gestureEngine: GestureEngine

    @Volatile
    private var currentSettings = RemapperSettings()

    @Volatile
    private var cachedRules: List<RemapRule> = RemapperPreferences.defaultGlobalRules()

    @Volatile
    private var currentForegroundPackage = ""

    override fun onCreate() {
        super.onCreate()
        repository = RemapperRepository(
            context = applicationContext,
            preferences = RemapperPreferences(applicationContext),
        )
        constraintEvaluator = ConstraintEvaluator(applicationContext)
        actionExecutor = ActionExecutor(applicationContext).apply {
            registerTorchCallback()
        }
        ShizukuManager.init(applicationContext)

        gestureEngine = GestureEngine(
            scope = serviceScope,
            doublePressDelayMsProvider = { currentSettings.doublePressDelayMs.toLong() },
            longPressThresholdMsProvider = { currentSettings.longPressThresholdMs.toLong() },
            hasMultiTapRulesProvider = { hasActiveMultiTapRules() },
            onGestureRecognized = { gesture -> handleGesture(gesture) },
        )

        serviceScope.launch {
            repository.settingsFlow.collectLatest { settings ->
                currentSettings = settings
            }
        }
        serviceScope.launch {
            repository.allRulesFlow.collectLatest { rules ->
                cachedRules = rules
            }
        }
    }

    private fun hasActiveMultiTapRules(): Boolean {
        val fg = currentForegroundPackage
        return cachedRules.any { rule ->
            (rule.profilePackage == GLOBAL_PACKAGE || rule.profilePackage == fg) &&
                (rule.gesture == ButtonGesture.DOUBLE_PRESS || rule.gesture == ButtonGesture.TRIPLE_PRESS) &&
                rule.isEnabled &&
                rule.actionType != ActionType.NONE
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        RemapperServiceState.setAccessibilityRunning(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()?.trim().orEmpty()
            if (pkg.isNotEmpty() && pkg != "com.android.systemui") {
                currentForegroundPackage = pkg
                RemapperServiceState.setForegroundPackage(pkg)
            }
        }
    }

    override fun onInterrupt() {
        gestureEngine.reset()
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        RemapperServiceState.setAccessibilityRunning(false)
        gestureEngine.reset()
        return super.onUnbind(intent)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) return false

        if (RemapperServiceState.isCapturingKey.value &&
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0
        ) {
            RemapperServiceState.emitCapturedKey(
                CapturedKeyEvent(
                    keyCode = event.keyCode,
                    scanCode = event.scanCode,
                    sourceLabel = KeyEvent.keyCodeToString(event.keyCode),
                ),
            )
            return true
        }

        if (event.keyCode != currentSettings.targetKeyCode) {
            return false
        }

        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isInteractive = powerManager?.isInteractive ?: true
        if (!isInteractive && !currentSettings.workWhenScreenOff) {
            return false
        }

        when (event.action) {
            KeyEvent.ACTION_DOWN -> gestureEngine.onKeyDown(event.repeatCount)
            KeyEvent.ACTION_UP -> {
                val canceled = (event.flags and KeyEvent.FLAG_CANCELED) != 0
                gestureEngine.onKeyUp(canceled = canceled)
            }
        }

        return currentSettings.blockDefaultKey
    }

    private fun handleGesture(gesture: ButtonGesture) {
        val foreground = currentForegroundPackage
        val settings = currentSettings
        serviceScope.launch {
            val rule = repository.resolveEffectiveRule(foreground, gesture)
            if (rule == null) {
                RemapperServiceState.setLastTriggeredBanner("${gesture.shortName} · Unassigned")
                return@launch
            }

            if (!constraintEvaluator.isSatisfied(rule.constraint)) {
                RemapperServiceState.setLastTriggeredBanner(
                    "${gesture.shortName} skipped (${rule.constraint.shortName})",
                )
                return@launch
            }

            if (settings.hapticFeedbackEnabled) {
                triggerHaptic(isLongPress = gesture == ButtonGesture.LONG_PRESS)
            }

            val result = actionExecutor.execute(
                rule = rule,
                accessibilityService = this@RemapperAccessibilityService,
            )
            RemapperServiceState.setLastTriggeredBanner("${gesture.shortName} → ${result.message}")
        }
    }

    private fun triggerHaptic(isLongPress: Boolean) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val effectId = if (isLongPress) {
                    VibrationEffect.EFFECT_HEAVY_CLICK
                } else {
                    VibrationEffect.EFFECT_CLICK
                }
                vibrator.vibrate(VibrationEffect.createPredefined(effectId))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(if (isLongPress) 35L else 18L)
            }
        } catch (_: Throwable) {
        }
    }

    override fun onDestroy() {
        RemapperServiceState.setAccessibilityRunning(false)
        actionExecutor.unregisterTorchCallback()
        gestureEngine.reset()
        serviceScope.cancel()
        super.onDestroy()
    }
}
