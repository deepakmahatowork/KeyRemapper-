package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ActionType
import com.example.model.AppProfile
import com.example.model.ButtonGesture
import com.example.model.RemapRule
import com.example.model.StateConstraint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.remapperDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "keyremap_prefs_v2",
)

data class RemapperSettings(
    val targetKeyCode: Int = 0,
    val targetKeyLabel: String = "Essential Key (Keycode 0)",
    val doublePressDelayMs: Int = 260,
    val longPressThresholdMs: Int = 500,
    val hapticFeedbackEnabled: Boolean = true,
    val blockDefaultKey: Boolean = true,
    val workWhenScreenOff: Boolean = true,
    val dynamicColorEnabled: Boolean = true,
)

class RemapperPreferences(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private object Keys {
        val TARGET_KEY_CODE = intPreferencesKey("target_key_code")
        val TARGET_KEY_LABEL = stringPreferencesKey("target_key_label")
        val DOUBLE_PRESS_DELAY_MS = intPreferencesKey("double_press_delay_ms")
        val LONG_PRESS_THRESHOLD_MS = intPreferencesKey("long_press_threshold_ms")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val BLOCK_DEFAULT_KEY = booleanPreferencesKey("block_default_key")
        val WORK_WHEN_SCREEN_OFF = booleanPreferencesKey("work_when_screen_off")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val RULES_JSON = stringPreferencesKey("rules_json")
        val PROFILES_JSON = stringPreferencesKey("profiles_json")
    }

    val settingsFlow: Flow<RemapperSettings> = context.remapperDataStore.data.map { prefs ->
        RemapperSettings(
            targetKeyCode = prefs[Keys.TARGET_KEY_CODE] ?: 0,
            targetKeyLabel = prefs[Keys.TARGET_KEY_LABEL] ?: "Essential Key (Keycode 0)",
            doublePressDelayMs = prefs[Keys.DOUBLE_PRESS_DELAY_MS] ?: 260,
            longPressThresholdMs = prefs[Keys.LONG_PRESS_THRESHOLD_MS] ?: 500,
            hapticFeedbackEnabled = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            blockDefaultKey = prefs[Keys.BLOCK_DEFAULT_KEY] ?: true,
            workWhenScreenOff = prefs[Keys.WORK_WHEN_SCREEN_OFF] ?: true,
            dynamicColorEnabled = prefs[Keys.DYNAMIC_COLOR] ?: true,
        )
    }

    val rulesFlow: Flow<List<RemapRule>> = context.remapperDataStore.data.map { prefs ->
        val raw = prefs[Keys.RULES_JSON]
        if (raw.isNullOrBlank()) {
            defaultGlobalRules()
        } else {
            runCatching { json.decodeFromString<List<RemapRule>>(raw) }
                .getOrElse { defaultGlobalRules() }
                .ifEmpty { defaultGlobalRules() }
        }
    }

    val profilesFlow: Flow<List<AppProfile>> = context.remapperDataStore.data.map { prefs ->
        val raw = prefs[Keys.PROFILES_JSON]
        if (raw.isNullOrBlank()) {
            emptyList()
        } else {
            runCatching { json.decodeFromString<List<AppProfile>>(raw) }.getOrDefault(emptyList())
        }
    }

    suspend fun saveRules(rules: List<RemapRule>) {
        context.remapperDataStore.edit {
            it[Keys.RULES_JSON] = json.encodeToString(rules)
        }
    }

    suspend fun saveProfiles(profiles: List<AppProfile>) {
        context.remapperDataStore.edit {
            it[Keys.PROFILES_JSON] = json.encodeToString(profiles)
        }
    }

    suspend fun setTargetKey(keyCode: Int, label: String) {
        context.remapperDataStore.edit {
            it[Keys.TARGET_KEY_CODE] = keyCode
            it[Keys.TARGET_KEY_LABEL] = label
        }
    }

    suspend fun setDoublePressDelay(delayMs: Int) {
        context.remapperDataStore.edit { it[Keys.DOUBLE_PRESS_DELAY_MS] = delayMs.coerceIn(150, 600) }
    }

    suspend fun setLongPressThreshold(thresholdMs: Int) {
        context.remapperDataStore.edit { it[Keys.LONG_PRESS_THRESHOLD_MS] = thresholdMs.coerceIn(300, 1200) }
    }

    suspend fun setHapticFeedback(enabled: Boolean) {
        context.remapperDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setBlockDefaultKey(enabled: Boolean) {
        context.remapperDataStore.edit { it[Keys.BLOCK_DEFAULT_KEY] = enabled }
    }

    suspend fun setWorkWhenScreenOff(enabled: Boolean) {
        context.remapperDataStore.edit { it[Keys.WORK_WHEN_SCREEN_OFF] = enabled }
    }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.remapperDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun resetAll() {
        context.remapperDataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.RULES_JSON] = json.encodeToString(defaultGlobalRules())
        }
    }

    companion object {
        const val GLOBAL_PACKAGE = "*"
        const val GLOBAL_LABEL = "Global"

        fun defaultGlobalRules(): List<RemapRule> = listOf(
            RemapRule(
                profilePackage = GLOBAL_PACKAGE,
                profileLabel = GLOBAL_LABEL,
                gesture = ButtonGesture.SINGLE_PRESS,
                actionType = ActionType.VOICE_ASSISTANT,
                constraint = StateConstraint.ALWAYS,
                isEnabled = true,
            ),
            RemapRule(
                profilePackage = GLOBAL_PACKAGE,
                profileLabel = GLOBAL_LABEL,
                gesture = ButtonGesture.DOUBLE_PRESS,
                actionType = ActionType.NONE,
                constraint = StateConstraint.ALWAYS,
                isEnabled = false,
            ),
            RemapRule(
                profilePackage = GLOBAL_PACKAGE,
                profileLabel = GLOBAL_LABEL,
                gesture = ButtonGesture.TRIPLE_PRESS,
                actionType = ActionType.NONE,
                constraint = StateConstraint.ALWAYS,
                isEnabled = false,
            ),
            RemapRule(
                profilePackage = GLOBAL_PACKAGE,
                profileLabel = GLOBAL_LABEL,
                gesture = ButtonGesture.LONG_PRESS,
                actionType = ActionType.TOGGLE_FLASHLIGHT,
                constraint = StateConstraint.ALWAYS,
                isEnabled = true,
            ),
        )
    }
}
