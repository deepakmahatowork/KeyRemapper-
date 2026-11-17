package com.example.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import com.example.model.ActionType
import com.example.model.RemapRule
import com.example.shizuku.ShizukuManager

data class ActionExecutionResult(
    val success: Boolean,
    val message: String,
)

class ActionExecutor(private val context: Context) {

    private var torchCameraId: String? = null
    private var torchEnabled: Boolean = false

    private val cameraManager: CameraManager? by lazy {
        context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (torchCameraId == null || torchCameraId == cameraId) {
                torchEnabled = enabled
                RemapperServiceState.setTorchOn(enabled)
            }
        }
    }

    fun registerTorchCallback() {
        try {
            cameraManager?.registerTorchCallback(torchCallback, null)
        } catch (_: Throwable) {
        }
    }

    fun unregisterTorchCallback() {
        try {
            cameraManager?.unregisterTorchCallback(torchCallback)
        } catch (_: Throwable) {
        }
    }

    suspend fun execute(
        rule: RemapRule,
        accessibilityService: AccessibilityService?,
    ): ActionExecutionResult {
        return when (rule.actionType) {
            ActionType.NONE -> ActionExecutionResult(true, "No action mapped")
            ActionType.VOICE_ASSISTANT -> launchGeminiOrAssistant(accessibilityService)
            ActionType.TOGGLE_FLASHLIGHT -> toggleFlashlight()
            ActionType.TAKE_SCREENSHOT -> performGlobal(
                accessibilityService,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
                } else {
                    -1
                },
                "Screenshot requires Android 9+ and Accessibility Service",
                "Screenshot captured",
            )
            ActionType.LOCK_SCREEN -> performGlobal(
                accessibilityService,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN
                } else {
                    -1
                },
                "Lock Screen requires Android 9+ and Accessibility Service",
                "Screen locked",
            )
            ActionType.OPEN_NOTIFICATIONS -> performGlobal(
                accessibilityService,
                AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS,
                "Enable Accessibility Service to expand notifications",
                "Notifications expanded",
            )
            ActionType.CYCLE_RINGER_MODE -> cycleRingerMode()
            ActionType.MEDIA_PLAY_PAUSE -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "Media Play/Pause")
            ActionType.MEDIA_NEXT -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT, "Next Track")
            ActionType.OPEN_CAMERA -> launchIntent(
                Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA),
                "Launched Camera",
            )
            ActionType.LAUNCH_APP -> launchPackage(rule.actionPayload, rule.actionPayloadLabel)
            ActionType.SIMULATE_KEYCODE -> simulateKeyCode(rule.actionPayload.toIntOrNull())
        }
    }

    /**
     * Triggers the exact Power-button long-press Digital Assistant overlay (Gemini mic overlay):
     * 1. Via Shizuku (`input keyevent --longpress KEYCODE_POWER`) when Shizuku is active and Power
     *    long-press is configured as Digital Assistant.
     * 2. Via [AssistTriggerActivity] (`Activity.showAssist(Bundle)`), which invokes Android's
     *    `VoiceInteractionManagerService` (`showSessionForActiveService`) to open the floating
     *    Gemini voice overlay without root or Shizuku.
     * 3. Fallback to `Intent.ACTION_ASSIST` / Gemini (`com.google.android.apps.bard`), never
     *    opening the Google Search app (`com.google.android.googlequicksearchbox`).
     */
    private suspend fun launchGeminiOrAssistant(accessibilityService: AccessibilityService?): ActionExecutionResult {
        val launchContext: Context = accessibilityService ?: context
        val pm = context.packageManager

        val powerLongPress = runCatching {
            Settings.Global.getInt(context.contentResolver, POWER_BUTTON_LONG_PRESS_KEY, -1)
        }.getOrDefault(-1)

        if (ShizukuManager.isReady() &&
            shouldUsePowerLongPressAssist(
                powerButtonLongPress = powerLongPress,
                manufacturer = Build.MANUFACTURER.orEmpty(),
                brand = Build.BRAND.orEmpty(),
            )
        ) {
            val powerAssistOutput = ShizukuManager.runCommand(
                "input keyevent --longpress ${KeyEvent.KEYCODE_POWER}",
            )
            if (!powerAssistOutput.startsWith("ERR")) {
                return ActionExecutionResult(true, "Invoked Gemini Voice Overlay (Power Assist)")
            }
        }

        // Launch zero-frame transparent AssistTriggerActivity which calls Activity.showAssist(args)
        // to summon the system's active VoiceInteractionSession (Gemini floating voice overlay).
        val triggerIntent = buildShowAssistTriggerIntent(context)
        if (runCatching { launchContext.startActivity(triggerIntent) }.isSuccess) {
            return ActionExecutionResult(true, "Invoked Gemini Digital Assistant Overlay")
        }

        val assistantSetting = runCatching {
            Settings.Secure.getString(context.contentResolver, "assistant")
                ?: Settings.Secure.getString(context.contentResolver, "voice_interaction_service")
        }.getOrNull().orEmpty()
        val assistantPkg = assistantSetting.substringBefore('/').trim()

        val candidates = buildAssistantIntentCandidates(assistantPackage = assistantPkg)
        for (candidate in candidates) {
            if (runCatching { launchContext.startActivity(candidate) }.isSuccess) {
                return ActionExecutionResult(true, "Invoked Gemini / Digital Assistant")
            }
        }

        for (pkg in listOf(
            GEMINI_APP_PACKAGE,
            "com.google.android.apps.googleassistant",
        )) {
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (runCatching { launchContext.startActivity(launch) }.isSuccess) {
                    return ActionExecutionResult(true, "Launched Gemini")
                }
            }
        }

        return ActionExecutionResult(
            false,
            "Gemini not found — set Gemini as your Default Digital Assistant in Setup",
        )
    }

    companion object {
        private const val POWER_BUTTON_LONG_PRESS_KEY = "power_button_long_press"
        private const val LONG_PRESS_POWER_VOICE_ASSIST = 4
        private const val LONG_PRESS_POWER_ASSISTANT = 5
        private const val GOOGLE_APP_PACKAGE = "com.google.android.googlequicksearchbox"
        internal const val GEMINI_APP_PACKAGE = "com.google.android.apps.bard"
        private const val EXTRA_INVOCATION_TYPE = "invocation_type"
        private const val EXTRA_SYSUI_INVOCATION_TYPE = "com.android.systemui.assist.invocation_type"
        private const val INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS = 6

        internal fun buildShowAssistTriggerIntent(context: Context): Intent {
            return Intent(context, com.example.ui.AssistTriggerActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_NO_ANIMATION or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS,
                )
            }
        }

        internal fun shouldUsePowerLongPressAssist(
            powerButtonLongPress: Int,
            manufacturer: String,
            brand: String,
        ): Boolean {
            if (powerButtonLongPress == LONG_PRESS_POWER_ASSISTANT ||
                powerButtonLongPress == LONG_PRESS_POWER_VOICE_ASSIST
            ) {
                return true
            }
            if (powerButtonLongPress == -1) {
                return manufacturer.equals("Nothing", ignoreCase = true) ||
                    brand.equals("Nothing", ignoreCase = true) ||
                    brand.equals("CMF", ignoreCase = true)
            }
            return false
        }

        internal fun buildAssistantIntentCandidates(
            assistantPackage: String,
        ): List<Intent> {
            val intents = mutableListOf<Intent>()

            fun newAssistIntent(pkg: String? = null): Intent {
                return Intent(Intent.ACTION_ASSIST).apply {
                    if (!pkg.isNullOrBlank()) {
                        setPackage(pkg)
                    }
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(EXTRA_INVOCATION_TYPE, INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS)
                    putExtra(EXTRA_SYSUI_INVOCATION_TYPE, INVOCATION_TYPE_POWER_BUTTON_LONG_PRESS)
                }
            }

            // Target Gemini explicitly first if installed or configured
            intents += newAssistIntent(GEMINI_APP_PACKAGE)
            if (assistantPackage.isNotEmpty() &&
                assistantPackage != GOOGLE_APP_PACKAGE &&
                assistantPackage != GEMINI_APP_PACKAGE
            ) {
                intents += newAssistIntent(assistantPackage)
            }
            intents += newAssistIntent()

            return intents
        }
    }

    private fun toggleFlashlight(): ActionExecutionResult {
        val cm = cameraManager ?: return ActionExecutionResult(false, "CameraManager unavailable")
        return try {
            val id = torchCameraId ?: cm.cameraIdList.firstOrNull { cameraId ->
                val chars = cm.getCameraCharacteristics(cameraId)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                val facing = chars.get(CameraCharacteristics.LENS_FACING)
                hasFlash && facing == CameraCharacteristics.LENS_FACING_BACK
            } ?: cm.cameraIdList.firstOrNull { cameraId ->
                cm.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionExecutionResult(false, "No camera flash hardware found")

            torchCameraId = id
            val nextState = !torchEnabled
            cm.setTorchMode(id, nextState)
            torchEnabled = nextState
            RemapperServiceState.setTorchOn(nextState)
            ActionExecutionResult(true, if (nextState) "Flashlight ON" else "Flashlight OFF")
        } catch (t: Throwable) {
            ActionExecutionResult(false, "Flashlight error: ${t.message}")
        }
    }

    private fun performGlobal(
        service: AccessibilityService?,
        globalAction: Int,
        missingServiceMessage: String,
        successMessage: String,
    ): ActionExecutionResult {
        if (service == null || globalAction < 0) {
            return ActionExecutionResult(false, missingServiceMessage)
        }
        val ok = service.performGlobalAction(globalAction)
        return if (ok) {
            ActionExecutionResult(true, successMessage)
        } else {
            ActionExecutionResult(false, "System rejected global action")
        }
    }

    private fun cycleRingerMode(): ActionExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult(false, "AudioManager unavailable")
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        val hasPolicyAccess = notificationManager?.isNotificationPolicyAccessGranted == true

        val next = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE ->
                if (hasPolicyAccess) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
            else -> AudioManager.RINGER_MODE_NORMAL
        }

        return try {
            audioManager.ringerMode = next
            val label = when (next) {
                AudioManager.RINGER_MODE_NORMAL -> "Ringer: Normal"
                AudioManager.RINGER_MODE_VIBRATE -> "Ringer: Vibrate"
                AudioManager.RINGER_MODE_SILENT -> "Ringer: Silent"
                else -> "Ringer updated"
            }
            ActionExecutionResult(true, label)
        } catch (_: Throwable) {
            ActionExecutionResult(false, "Grant DND Access in Setup for Silent mode")
        }
    }

    private fun dispatchMediaKey(keyCode: Int, label: String): ActionExecutionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return ActionExecutionResult(false, "AudioManager unavailable")
        val now = SystemClock.uptimeMillis()
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, keyCode, 0))
        audioManager.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, keyCode, 0))
        return ActionExecutionResult(true, label)
    }

    private fun launchIntent(intent: Intent, label: String): ActionExecutionResult {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionExecutionResult(true, label)
        } catch (t: Throwable) {
            ActionExecutionResult(false, "Unable to launch target: ${t.message}")
        }
    }

    private fun launchPackage(packageName: String, label: String): ActionExecutionResult {
        if (packageName.isBlank()) {
            return ActionExecutionResult(false, "No app selected")
        }
        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            ?: return ActionExecutionResult(false, "App not installed: $packageName")
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
        return ActionExecutionResult(true, "Launched ${label.ifBlank { packageName }}")
    }

    private suspend fun simulateKeyCode(keyCode: Int?): ActionExecutionResult {
        if (keyCode == null || keyCode < 0) {
            return ActionExecutionResult(false, "Invalid keycode")
        }
        if (!ShizukuManager.isReady()) {
            return ActionExecutionResult(false, "Connect Shizuku in Setup to inject keycodes")
        }
        val result = ShizukuManager.injectKeyEvent(keyCode)
        return if (result.isSuccess) {
            ActionExecutionResult(true, "Injected ${KeyEvent.keyCodeToString(keyCode)}")
        } else {
            ActionExecutionResult(false, result.exceptionOrNull()?.message ?: "Injection failed")
        }
    }
}
