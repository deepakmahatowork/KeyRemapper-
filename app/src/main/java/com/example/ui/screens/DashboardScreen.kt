package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ActionType
import com.example.model.AppProfile
import com.example.model.ButtonGesture
import com.example.model.RemapRule
import com.example.shizuku.ShizukuBridgeStatus
import com.example.ui.components.GestureRuleCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.viewmodel.RemapperUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: RemapperUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenSetupTab: () -> Unit,
    onStartKeyCapture: () -> Unit,
    onSimulateGesture: (ButtonGesture) -> Unit,
    onEditGlobalRule: (ButtonGesture, RemapRule?) -> Unit,
    onToggleGlobalRule: (ButtonGesture, Boolean) -> Unit,
    onOpenAddAppSheet: () -> Unit,
    onToggleAppProfile: (String, Boolean) -> Unit,
    onDeleteAppProfile: (AppProfile) -> Unit,
    onEditProfileRule: (AppProfile, ButtonGesture, RemapRule?) -> Unit,
    onToggleProfileRule: (AppProfile, ButtonGesture, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isServiceConnected = uiState.health.accessibilityConnected
    val isServiceEnabledInSettings = uiState.health.accessibilityEnabled
    val shizukuReady = uiState.shizuku.status == ShizukuBridgeStatus.READY
    val hasMultiTapActive = remember(uiState.globalRules) {
        uiState.globalRules.any {
            (it.gesture == ButtonGesture.DOUBLE_PRESS || it.gesture == ButtonGesture.TRIPLE_PRESS) &&
                it.isEnabled &&
                it.actionType != ActionType.NONE
        }
    }
    var expandedPackage by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp)
                .testTag("dashboard_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Compact Material You Status Card
            item {
                Card(
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            StatusPill(
                                label = when {
                                    isServiceConnected -> "REMAPPING ACTIVE"
                                    isServiceEnabledInSettings -> "RECONNECT SERVICE"
                                    else -> "SERVICE INACTIVE"
                                },
                                active = isServiceConnected,
                                activeColor = StatusActiveGreen,
                                inactiveColor = StatusWarningAmber,
                                onClick = onOpenAccessibilitySettings,
                            )
                            StatusPill(
                                label = when (uiState.shizuku.status) {
                                    ShizukuBridgeStatus.READY -> "Shizuku Ready"
                                    ShizukuBridgeStatus.NEEDS_PERMISSION -> "Shizuku Auth"
                                    else -> "Shizuku Idle"
                                },
                                active = shizukuReady,
                                activeColor = StatusActiveGreen,
                                onClick = onOpenSetupTab,
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Essential Key Remapper",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            Text(
                                text = "Listening on ${uiState.settings.targetKeyLabel} · " +
                                    if (hasMultiTapActive) "${uiState.settings.doublePressDelayMs} ms tap window"
                                    else "Instant 0 ms 1-click response",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        AnimatedVisibility(visible = !uiState.health.lastTriggeredBanner.isNullOrBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = uiState.health.lastTriggeredBanner.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                )
                            }
                        }

                        if (!isServiceConnected) {
                            Button(
                                onClick = onOpenAccessibilitySettings,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("enable_accessibility_button"),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_accessibility),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (isServiceEnabledInSettings) "Toggle Accessibility Off & On"
                                    else "Enable Accessibility Service",
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = { onSimulateGesture(ButtonGesture.SINGLE_PRESS) },
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("test_gesture_single_press"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test 1× Click")
                            }
                            OutlinedButton(
                                onClick = onStartKeyCapture,
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("capture_key_button"),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_keyboard),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Keycode ${uiState.settings.targetKeyCode}")
                            }
                        }
                    }
                }
            }

            // Button Gestures Header
            item {
                Text(
                    text = "Button Actions",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }

            items(ButtonGesture.entries, key = { it.name }) { gesture ->
                val rule = uiState.globalRules.firstOrNull { it.gesture == gesture }
                GestureRuleCard(
                    gesture = gesture,
                    rule = rule,
                    testTagPrefix = "global",
                    onEditClick = { onEditGlobalRule(gesture, rule) },
                    onToggleEnabled = { enabled -> onToggleGlobalRule(gesture, enabled) },
                )
            }

            // Compact Per-App Overrides Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 4.dp, end = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "App Overrides",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Optional per-app button rules",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        onClick = onOpenAddAppSheet,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("add_app_profile_button"),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add App")
                    }
                }
            }

            items(uiState.appProfiles, key = { it.packageName }) { profile ->
                val profileRules = uiState.allRules.filter { it.profilePackage == profile.packageName }
                val isExpanded = expandedPackage == profile.packageName

                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedPackage = if (isExpanded) null else profile.packageName
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_apps),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = profile.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Switch(
                                checked = profile.isEnabled,
                                onCheckedChange = { onToggleAppProfile(profile.packageName, it) },
                            )
                            IconButton(onClick = { onDeleteAppProfile(profile) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Remove ${profile.appName}",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                ButtonGesture.entries.forEach { gesture ->
                                    val rule = profileRules.firstOrNull { it.gesture == gesture }
                                    GestureRuleCard(
                                        gesture = gesture,
                                        rule = rule,
                                        testTagPrefix = "profile_${profile.packageName}",
                                        onEditClick = { onEditProfileRule(profile, gesture, rule) },
                                        onToggleEnabled = { enabled ->
                                            onToggleProfileRule(profile, gesture, enabled)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}
