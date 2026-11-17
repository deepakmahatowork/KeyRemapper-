package com.example.model

import android.view.KeyEvent
import kotlinx.serialization.Serializable

@Serializable
enum class ButtonGesture(
    val displayName: String,
    val shortName: String,
    val description: String,
    val badgeText: String,
) {
    SINGLE_PRESS(
        displayName = "Single Press",
        shortName = "1× Click",
        description = "Instant trigger on one button press",
        badgeText = "1×",
    ),
    DOUBLE_PRESS(
        displayName = "Double Press",
        shortName = "2× Click",
        description = "Two rapid presses within the double-tap window",
        badgeText = "2×",
    ),
    TRIPLE_PRESS(
        displayName = "Triple Press",
        shortName = "3× Click",
        description = "Three rapid presses for extra shortcuts",
        badgeText = "3×",
    ),
    LONG_PRESS(
        displayName = "Long Press",
        shortName = "Hold",
        description = "Press and hold past the hold threshold",
        badgeText = "HOLD",
    ),
}

@Serializable
enum class ActionType(
    val displayName: String,
    val subtitle: String,
    val requiresPayload: Boolean = false,
) {
    VOICE_ASSISTANT(
        displayName = "Gemini / Assistant",
        subtitle = "Invoke Gemini overlay like long-pressing Power",
    ),
    TOGGLE_FLASHLIGHT(
        displayName = "Toggle Flashlight",
        subtitle = "Switch rear camera torch on or off",
    ),
    TAKE_SCREENSHOT(
        displayName = "Take Screenshot",
        subtitle = "Capture current screen (Android 9+)",
    ),
    CYCLE_RINGER_MODE(
        displayName = "Cycle Ringer Mode",
        subtitle = "Rotate Normal → Vibrate → Silent",
    ),
    MEDIA_PLAY_PAUSE(
        displayName = "Play / Pause Media",
        subtitle = "Toggle active media playback",
    ),
    MEDIA_NEXT(
        displayName = "Next Track",
        subtitle = "Skip to next track",
    ),
    OPEN_CAMERA(
        displayName = "Launch Camera",
        subtitle = "Open default camera app",
    ),
    LOCK_SCREEN(
        displayName = "Lock Screen",
        subtitle = "Turn off and lock display (Android 9+)",
    ),
    OPEN_NOTIFICATIONS(
        displayName = "Notifications Shade",
        subtitle = "Pull down notification panel",
    ),
    LAUNCH_APP(
        displayName = "Launch App",
        subtitle = "Open any installed application",
        requiresPayload = true,
    ),
    SIMULATE_KEYCODE(
        displayName = "Simulate Keycode",
        subtitle = "Inject Android KeyEvent via Shizuku",
        requiresPayload = true,
    ),
    NONE(
        displayName = "Unassigned",
        subtitle = "Disabled / no action",
    ),
}

@Serializable
enum class StateConstraint(
    val displayName: String,
    val shortName: String,
) {
    ALWAYS("Always Active", "Always"),
    SCREEN_ON("Screen On Only", "Screen On"),
    SCREEN_OFF("Screen Off Only", "Screen Off"),
    MEDIA_PLAYING("Media Playing", "Media"),
    WIFI_CONNECTED("Wi-Fi Connected", "Wi-Fi"),
    CHARGING("While Charging", "Charging"),
}

@Serializable
data class RemapRule(
    val profilePackage: String,
    val profileLabel: String,
    val gesture: ButtonGesture,
    val actionType: ActionType,
    val actionPayload: String = "",
    val actionPayloadLabel: String = "",
    val constraint: StateConstraint = StateConstraint.ALWAYS,
    val isEnabled: Boolean = true,
)

@Serializable
data class AppProfile(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = true,
)

data class InstalledAppInfo(
    val packageName: String,
    val label: String,
)

data class KeycodePreset(
    val keyCode: Int,
    val name: String,
)

object KeycodePresets {
    val common = listOf(
        KeycodePreset(0, "Essential Key (Keycode 0)"),
        KeycodePreset(KeyEvent.KEYCODE_ASSIST, "KEYCODE_ASSIST (219)"),
        KeycodePreset(KeyEvent.KEYCODE_CAMERA, "KEYCODE_CAMERA (27)"),
        KeycodePreset(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, "KEYCODE_MEDIA_PLAY_PAUSE (85)"),
        KeycodePreset(KeyEvent.KEYCODE_VOLUME_UP, "KEYCODE_VOLUME_UP (24)"),
        KeycodePreset(KeyEvent.KEYCODE_VOLUME_DOWN, "KEYCODE_VOLUME_DOWN (25)"),
    )

    fun labelForKeyCode(keyCode: Int): String {
        return common.firstOrNull { it.keyCode == keyCode }?.name
            ?: KeyEvent.keyCodeToString(keyCode)
    }
}
