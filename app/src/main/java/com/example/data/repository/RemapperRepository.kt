package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.example.data.preferences.RemapperPreferences
import com.example.data.preferences.RemapperPreferences.Companion.GLOBAL_PACKAGE
import com.example.data.preferences.RemapperSettings
import com.example.model.ActionType
import com.example.model.AppProfile
import com.example.model.ButtonGesture
import com.example.model.InstalledAppInfo
import com.example.model.RemapRule
import com.example.model.StateConstraint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class RemapperRepository(
    private val context: Context,
    val preferences: RemapperPreferences,
) {
    val settingsFlow: Flow<RemapperSettings> = preferences.settingsFlow
    val allRulesFlow: Flow<List<RemapRule>> = preferences.rulesFlow
    val globalRulesFlow: Flow<List<RemapRule>> = preferences.rulesFlow.map { rules ->
        rules.filter { it.profilePackage == GLOBAL_PACKAGE }
    }
    val appProfilesFlow: Flow<List<AppProfile>> = preferences.profilesFlow

    suspend fun resolveEffectiveRule(
        foregroundPackage: String,
        gesture: ButtonGesture,
    ): RemapRule? {
        val rules = preferences.rulesFlow.first()
        if (foregroundPackage.isNotBlank()) {
            val profile = preferences.profilesFlow.first().firstOrNull { it.packageName == foregroundPackage }
            if (profile != null && profile.isEnabled) {
                val appRule = rules.firstOrNull {
                    it.profilePackage == foregroundPackage &&
                        it.gesture == gesture &&
                        it.isEnabled &&
                        it.actionType != ActionType.NONE
                }
                if (appRule != null) return appRule
            }
        }
        return rules.firstOrNull {
            it.profilePackage == GLOBAL_PACKAGE &&
                it.gesture == gesture &&
                it.isEnabled &&
                it.actionType != ActionType.NONE
        }
    }

    suspend fun saveRule(
        profilePackage: String,
        profileLabel: String,
        gesture: ButtonGesture,
        actionType: ActionType,
        actionPayload: String = "",
        actionPayloadLabel: String = "",
        constraint: StateConstraint = StateConstraint.ALWAYS,
        isEnabled: Boolean = true,
    ) {
        val current = preferences.rulesFlow.first().toMutableList()
        val idx = current.indexOfFirst { it.profilePackage == profilePackage && it.gesture == gesture }
        val updated = RemapRule(
            profilePackage = profilePackage,
            profileLabel = profileLabel,
            gesture = gesture,
            actionType = actionType,
            actionPayload = actionPayload,
            actionPayloadLabel = actionPayloadLabel,
            constraint = constraint,
            isEnabled = isEnabled,
        )
        if (idx >= 0) current[idx] = updated else current.add(updated)
        preferences.saveRules(current)
    }

    suspend fun toggleRuleEnabled(
        profilePackage: String,
        profileLabel: String,
        gesture: ButtonGesture,
        enabled: Boolean,
    ) {
        val current = preferences.rulesFlow.first().toMutableList()
        val idx = current.indexOfFirst { it.profilePackage == profilePackage && it.gesture == gesture }
        if (idx >= 0) {
            current[idx] = current[idx].copy(isEnabled = enabled)
        } else {
            current.add(
                RemapRule(
                    profilePackage = profilePackage,
                    profileLabel = profileLabel,
                    gesture = gesture,
                    actionType = ActionType.NONE,
                    isEnabled = enabled,
                ),
            )
        }
        preferences.saveRules(current)
    }

    suspend fun createAppProfile(packageName: String, appName: String) {
        val profiles = preferences.profilesFlow.first().toMutableList()
        if (profiles.none { it.packageName == packageName }) {
            profiles.add(0, AppProfile(packageName = packageName, appName = appName, isEnabled = true))
            preferences.saveProfiles(profiles)
        }
    }

    suspend fun toggleAppProfile(packageName: String, enabled: Boolean) {
        val profiles = preferences.profilesFlow.first().map {
            if (it.packageName == packageName) it.copy(isEnabled = enabled) else it
        }
        preferences.saveProfiles(profiles)
    }

    suspend fun deleteAppProfile(packageName: String) {
        val profiles = preferences.profilesFlow.first().filterNot { it.packageName == packageName }
        val rules = preferences.rulesFlow.first().filterNot { it.profilePackage == packageName }
        preferences.saveProfiles(profiles)
        preferences.saveRules(rules)
    }

    suspend fun resetToDefaults() {
        preferences.resetAll()
    }

    suspend fun loadLaunchableApps(): List<InstalledAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .asSequence()
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                val label = info.loadLabel(pm)?.toString()?.trim().orEmpty().ifBlank { pkg }
                InstalledAppInfo(packageName = pkg, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }
}
