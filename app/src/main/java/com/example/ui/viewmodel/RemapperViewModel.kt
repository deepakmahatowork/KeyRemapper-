package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.RemapperPreferences
import com.example.data.preferences.RemapperPreferences.Companion.GLOBAL_LABEL
import com.example.data.preferences.RemapperPreferences.Companion.GLOBAL_PACKAGE
import com.example.data.preferences.RemapperSettings
import com.example.data.repository.RemapperRepository
import com.example.model.ActionType
import com.example.model.AppProfile
import com.example.model.ButtonGesture
import com.example.model.InstalledAppInfo
import com.example.model.KeycodePresets
import com.example.model.RemapRule
import com.example.model.StateConstraint
import com.example.service.ActionExecutor
import com.example.service.CapturedKeyEvent
import com.example.service.ConstraintEvaluator
import com.example.service.RemapperAccessibilityService
import com.example.service.RemapperServiceState
import com.example.shizuku.ShizukuManager
import com.example.shizuku.ShizukuUiSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SystemBridgeHealth(
    val accessibilityEnabled: Boolean = false,
    val accessibilityConnected: Boolean = false,
    val dndPolicyGranted: Boolean = false,
    val isTorchOn: Boolean = false,
    val lastTriggeredBanner: String? = null,
)

data class EditingRuleTarget(
    val profilePackage: String,
    val profileLabel: String,
    val rule: RemapRule,
)

data class RemapperUiState(
    val settings: RemapperSettings = RemapperSettings(),
    val globalRules: List<RemapRule> = RemapperPreferences.defaultGlobalRules(),
    val appProfiles: List<AppProfile> = emptyList(),
    val allRules: List<RemapRule> = RemapperPreferences.defaultGlobalRules(),
    val shizuku: ShizukuUiSnapshot = ShizukuUiSnapshot(),
    val health: SystemBridgeHealth = SystemBridgeHealth(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val appsLoading: Boolean = false,
    val isCapturingKey: Boolean = false,
    val shizukuBusy: Boolean = false,
)

class RemapperViewModel(
    application: Application,
    private val repository: RemapperRepository,
) : AndroidViewModel(application) {

    private val actionExecutor = ActionExecutor(application.applicationContext).apply {
        registerTorchCallback()
    }
    private val constraintEvaluator = ConstraintEvaluator(application.applicationContext)

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _appsLoading = MutableStateFlow(false)
    private val _shizukuBusy = MutableStateFlow(false)
    private val _systemHealth = MutableStateFlow(SystemBridgeHealth())

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    val uiState: StateFlow<RemapperUiState> = combine(
        repository.settingsFlow,
        repository.globalRulesFlow,
        repository.appProfilesFlow,
        repository.allRulesFlow,
        ShizukuManager.snapshot,
        _systemHealth,
        _installedApps,
        _appsLoading,
        RemapperServiceState.isCapturingKey,
        _shizukuBusy,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        RemapperUiState(
            settings = values[0] as RemapperSettings,
            globalRules = (values[1] as List<RemapRule>).sortedBy { it.gesture.ordinal },
            appProfiles = values[2] as List<AppProfile>,
            allRules = values[3] as List<RemapRule>,
            shizuku = values[4] as ShizukuUiSnapshot,
            health = values[5] as SystemBridgeHealth,
            installedApps = values[6] as List<InstalledAppInfo>,
            appsLoading = values[7] as Boolean,
            isCapturingKey = values[8] as Boolean,
            shizukuBusy = values[9] as Boolean,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RemapperUiState(),
    )

    init {
        ShizukuManager.init(application.applicationContext)
        refreshSystemHealth()
        viewModelScope.launch {
            combine(
                RemapperServiceState.isAccessibilityRunning,
                RemapperServiceState.isTorchOn,
                RemapperServiceState.lastTriggeredBanner,
            ) { connected, torch, banner ->
                Triple(connected, torch, banner)
            }.collect { (connected, torch, banner) ->
                refreshSystemHealth(
                    connectedOverride = connected,
                    torchOverride = torch,
                    bannerOverride = banner,
                )
            }
        }
        viewModelScope.launch {
            RemapperServiceState.capturedKeys.collect { event ->
                if (RemapperServiceState.isCapturingKey.value) {
                    RemapperServiceState.setCapturingKey(false)
                    val label = KeycodePresets.labelForKeyCode(event.keyCode)
                    repository.preferences.setTargetKey(event.keyCode, label)
                    _snackbarMessages.tryEmit("Target key bound to $label")
                }
            }
        }
    }

    fun refreshSystemHealth(
        connectedOverride: Boolean = RemapperServiceState.isAccessibilityRunning.value,
        torchOverride: Boolean = RemapperServiceState.isTorchOn.value,
        bannerOverride: String? = RemapperServiceState.lastTriggeredBanner.value,
    ) {
        val ctx = getApplication<Application>().applicationContext
        val accessibilityEnabled = isAccessibilityServiceEnabledInSettings(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val dndGranted = nm?.isNotificationPolicyAccessGranted == true

        _systemHealth.value = SystemBridgeHealth(
            accessibilityEnabled = accessibilityEnabled,
            accessibilityConnected = connectedOverride,
            dndPolicyGranted = dndGranted,
            isTorchOn = torchOverride,
            lastTriggeredBanner = bannerOverride,
        )
        ShizukuManager.refreshStatus()
    }

    private fun isAccessibilityServiceEnabledInSettings(context: Context): Boolean {
        val enabled = Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0,
        )
        if (enabled != 1) return false
        val settingValue = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val expectedComponent = ComponentName(context, RemapperAccessibilityService::class.java).flattenToString()
        val shortComponent = ComponentName(context, RemapperAccessibilityService::class.java).flattenToShortString()
        return settingValue.split(':').any {
            it.equals(expectedComponent, ignoreCase = true) ||
                it.equals(shortComponent, ignoreCase = true)
        }
    }

    fun loadInstalledApps() {
        if (_appsLoading.value || _installedApps.value.isNotEmpty()) return
        viewModelScope.launch {
            _appsLoading.value = true
            _installedApps.value = repository.loadLaunchableApps()
            _appsLoading.value = false
        }
    }

    fun updateRule(
        profilePackage: String,
        profileLabel: String,
        gesture: ButtonGesture,
        actionType: ActionType,
        actionPayload: String,
        actionPayloadLabel: String,
        constraint: StateConstraint,
        isEnabled: Boolean,
    ) {
        viewModelScope.launch {
            repository.saveRule(
                profilePackage = profilePackage,
                profileLabel = profileLabel,
                gesture = gesture,
                actionType = actionType,
                actionPayload = actionPayload,
                actionPayloadLabel = actionPayloadLabel,
                constraint = constraint,
                isEnabled = isEnabled,
            )
            _snackbarMessages.tryEmit("${gesture.shortName} → ${actionType.displayName}")
        }
    }

    fun toggleRuleEnabled(
        profilePackage: String,
        profileLabel: String,
        gesture: ButtonGesture,
        enabled: Boolean,
    ) {
        viewModelScope.launch {
            repository.toggleRuleEnabled(profilePackage, profileLabel, gesture, enabled)
        }
    }

    fun addAppProfile(app: InstalledAppInfo) {
        viewModelScope.launch {
            repository.createAppProfile(app.packageName, app.label)
            _snackbarMessages.tryEmit("Added override for ${app.label}")
        }
    }

    fun toggleAppProfile(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleAppProfile(packageName, enabled)
        }
    }

    fun deleteAppProfile(profile: AppProfile) {
        viewModelScope.launch {
            repository.deleteAppProfile(profile.packageName)
            _snackbarMessages.tryEmit("Removed ${profile.appName}")
        }
    }

    fun setKeyCaptureMode(active: Boolean) {
        RemapperServiceState.setCapturingKey(active)
    }

    fun onActivityHardwareKeyDown(event: KeyEvent): Boolean {
        if (!RemapperServiceState.isCapturingKey.value) return false
        if (event.repeatCount > 0) return true
        val keyCode = event.keyCode
        val label = KeycodePresets.labelForKeyCode(keyCode)
        RemapperServiceState.setCapturingKey(false)
        viewModelScope.launch {
            repository.preferences.setTargetKey(keyCode, label)
            _snackbarMessages.tryEmit("Captured $label (code $keyCode)")
        }
        return true
    }

    fun setTargetKeyPreset(keyCode: Int) {
        val label = KeycodePresets.labelForKeyCode(keyCode)
        viewModelScope.launch {
            repository.preferences.setTargetKey(keyCode, label)
            _snackbarMessages.tryEmit("Target key: $label")
        }
    }

    fun updateDoublePressDelay(delayMs: Int) {
        viewModelScope.launch { repository.preferences.setDoublePressDelay(delayMs) }
    }

    fun updateLongPressThreshold(thresholdMs: Int) {
        viewModelScope.launch { repository.preferences.setLongPressThreshold(thresholdMs) }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setHapticFeedback(enabled) }
    }

    fun setBlockDefaultKey(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setBlockDefaultKey(enabled) }
    }

    fun setWorkWhenScreenOff(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setWorkWhenScreenOff(enabled) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { repository.preferences.setDynamicColorEnabled(enabled) }
    }

    fun simulateGestureTest(gesture: ButtonGesture) {
        viewModelScope.launch {
            val rule = repository.resolveEffectiveRule(GLOBAL_PACKAGE, gesture)
            if (rule == null || rule.actionType == ActionType.NONE) {
                _snackbarMessages.tryEmit("No action assigned to ${gesture.displayName}")
                return@launch
            }
            if (!constraintEvaluator.isSatisfied(rule.constraint)) {
                _snackbarMessages.tryEmit("Skipped: ${rule.constraint.displayName} not met")
                return@launch
            }
            val result = actionExecutor.execute(
                rule = rule,
                accessibilityService = null,
            )
            RemapperServiceState.setLastTriggeredBanner("${gesture.shortName} → ${result.message}")
            _snackbarMessages.tryEmit(result.message)
        }
    }

    fun requestShizukuPermission() {
        ShizukuManager.requestPermission()
    }

    fun disableNothingDefaultHandlers() {
        viewModelScope.launch {
            _shizukuBusy.value = true
            ShizukuManager.disableNothingEssentialPackages()
            _shizukuBusy.value = false
            _snackbarMessages.tryEmit("Nothing default Essential Space disabled")
        }
    }

    fun restoreNothingDefaultHandlers() {
        viewModelScope.launch {
            _shizukuBusy.value = true
            ShizukuManager.restoreNothingEssentialPackages()
            _shizukuBusy.value = false
            _snackbarMessages.tryEmit("Nothing default packages restored")
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            repository.resetToDefaults()
            _snackbarMessages.tryEmit("Reset to defaults (1× Click = Gemini)")
        }
    }

    fun openAccessibilitySettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun openDefaultAssistantSettings(context: Context) {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { runCatching { context.startActivity(fallback) } }
    }

    fun openAppDetailsSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openDndPolicySettings(context: Context) {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun openShizukuOrStore(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(ShizukuManager.SHIZUKU_PACKAGE)
        if (launch != null) {
            context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } else {
            runCatching {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }

    override fun onCleared() {
        actionExecutor.unregisterTorchCallback()
        super.onCleared()
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repo = RemapperRepository(
                context = application.applicationContext,
                preferences = RemapperPreferences(application.applicationContext),
            )
            return RemapperViewModel(application, repo) as T
        }
    }
}
