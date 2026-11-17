package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.shizuku.ShizukuBridgeStatus
import com.example.ui.components.StatusPill
import com.example.ui.theme.StatusActiveGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.viewmodel.RemapperUiState
import kotlin.math.roundToInt

@Composable
fun SetupScreen(
    uiState: RemapperUiState,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenDefaultAssistantSettings: () -> Unit,
    onOpenAppInfoSettings: () -> Unit,
    onOpenDndPolicySettings: () -> Unit,
    onOpenShizukuOrStore: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    onDisableNothingEssential: () -> Unit,
    onRestoreNothingEssential: () -> Unit,
    onStartKeyCapture: () -> Unit,
    onUpdateDoublePressDelay: (Int) -> Unit,
    onUpdateLongPressThreshold: (Int) -> Unit,
    onSetHapticFeedback: (Boolean) -> Unit,
    onSetBlockDefaultKey: (Boolean) -> Unit,
    onSetWorkWhenScreenOff: (Boolean) -> Unit,
    onSetDynamicColor: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRestrictedGuide by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }

    val accessibilityReady = uiState.health.accessibilityEnabled || uiState.health.accessibilityConnected
    val shizukuStatus = uiState.shizuku.status

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 840.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        text = "Setup & Calibration",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "Accessibility, Gemini Assistant, Shizuku Nothing OS override, and timing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 1. Accessibility Engine
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_accessibility),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(
                                    text = "1. Accessibility Engine",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            StatusPill(
                                label = if (accessibilityReady) "Enabled" else "Required",
                                active = accessibilityReady,
                                activeColor = StatusActiveGreen,
                                inactiveColor = StatusWarningAmber,
                            )
                        }

                        Text(
                            text = "Required to intercept your hardware key and trigger Gemini, Flashlight, or Screenshots.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = onOpenAccessibilitySettings,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("setup_accessibility_button"),
                            ) {
                                Text(if (accessibilityReady) "Manage Service" else "Enable Service")
                            }
                            OutlinedButton(
                                onClick = { showRestrictedGuide = true },
                                modifier = Modifier.height(48.dp),
                            ) {
                                Text("Android 13+ Fix")
                            }
                        }
                    }
                }
            }

            // 2. Gemini / Default Assistant & DND
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_assistant),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                            Text(
                                text = "2. Gemini Digital Assistant",
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Text(
                            text = "Ensure Gemini (or Google) is selected as your device's Default Digital Assistant so 1× Click opens the assistant overlay just like holding Power.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FilledTonalButton(
                                onClick = onOpenDefaultAssistantSettings,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("default_assistant_button"),
                            ) {
                                Text("Default Assistant")
                            }
                            FilledTonalButton(
                                onClick = onOpenDndPolicySettings,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Text(if (uiState.health.dndPolicyGranted) "DND Granted" else "DND Access")
                            }
                        }
                    }
                }
            }

            // 3. Shizuku & Nothing OS Override
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_terminal),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp),
                                )
                                Text(
                                    text = "3. Shizuku & Essential Key",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                            StatusPill(
                                label = when (shizukuStatus) {
                                    ShizukuBridgeStatus.READY -> "Ready"
                                    ShizukuBridgeStatus.NEEDS_PERMISSION -> "Authorize"
                                    ShizukuBridgeStatus.STOPPED -> "Stopped"
                                    ShizukuBridgeStatus.NOT_INSTALLED -> "Not Installed"
                                },
                                active = shizukuStatus == ShizukuBridgeStatus.READY,
                                activeColor = StatusActiveGreen,
                                inactiveColor = MaterialTheme.colorScheme.outline,
                            )
                        }

                        Text(
                            text = "Disables Nothing OS default Essential Space / Recorder handlers and enables exact Power-button Gemini voice overlay without root.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )

                        when (shizukuStatus) {
                            ShizukuBridgeStatus.NOT_INSTALLED, ShizukuBridgeStatus.STOPPED -> {
                                OutlinedButton(
                                    onClick = onOpenShizukuOrStore,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                ) {
                                    Text(
                                        if (shizukuStatus == ShizukuBridgeStatus.NOT_INSTALLED) "Get Shizuku"
                                        else "Open Shizuku & Start Service",
                                    )
                                }
                            }
                            ShizukuBridgeStatus.NEEDS_PERMISSION -> {
                                Button(
                                    onClick = onRequestShizukuPermission,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("grant_shizuku_button"),
                                ) {
                                    Text("Grant Shizuku Permission")
                                }
                            }
                            ShizukuBridgeStatus.READY -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Button(
                                        onClick = onDisableNothingEssential,
                                        enabled = !uiState.shizukuBusy,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .testTag("disable_nothing_space_button"),
                                    ) {
                                        if (uiState.shizukuBusy) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(18.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        } else {
                                            Text("Disable Default Key")
                                        }
                                    }
                                    FilledTonalButton(
                                        onClick = onRestoreNothingEssential,
                                        enabled = !uiState.shizukuBusy,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                    ) {
                                        Text("Restore Default")
                                    }
                                }
                            }
                        }

                        if (uiState.shizuku.lastCommandOutput.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    text = uiState.shizuku.lastCommandOutput,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(10.dp),
                                )
                            }
                        }
                    }
                }
            }

            // 4. Calibration & Preferences
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Timing & Preferences",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Target Hardware Key", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${uiState.settings.targetKeyLabel} (code ${uiState.settings.targetKeyCode})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            FilledTonalButton(
                                onClick = onStartKeyCapture,
                                modifier = Modifier.height(48.dp),
                            ) {
                                Text("Capture")
                            }
                        }

                        HorizontalDivider()

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Multi-Press Window", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${uiState.settings.doublePressDelayMs} ms",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Slider(
                                value = uiState.settings.doublePressDelayMs.toFloat(),
                                onValueChange = { onUpdateDoublePressDelay(it.roundToInt()) },
                                valueRange = 150f..600f,
                            )
                        }

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("Long-Press Hold Threshold", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = "${uiState.settings.longPressThresholdMs} ms",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Slider(
                                value = uiState.settings.longPressThresholdMs.toFloat(),
                                onValueChange = { onUpdateLongPressThreshold(it.roundToInt()) },
                                valueRange = 300f..1200f,
                            )
                        }

                        HorizontalDivider()

                        SettingSwitchRow(
                            title = "Haptic Confirmation",
                            subtitle = "Tactile click when a gesture triggers",
                            checked = uiState.settings.hapticFeedbackEnabled,
                            onCheckedChange = onSetHapticFeedback,
                        )
                        SettingSwitchRow(
                            title = "Suppress Original Key Action",
                            subtitle = "Prevent the OS from running its default button action",
                            checked = uiState.settings.blockDefaultKey,
                            onCheckedChange = onSetBlockDefaultKey,
                        )
                        SettingSwitchRow(
                            title = "Trigger While Screen Off",
                            subtitle = "Allow mapped actions when display is asleep",
                            checked = uiState.settings.workWhenScreenOff,
                            onCheckedChange = onSetWorkWhenScreenOff,
                        )
                        SettingSwitchRow(
                            title = "Material You Dynamic Color",
                            subtitle = "Adapt theme colors from your wallpaper (Android 12+)",
                            checked = uiState.settings.dynamicColorEnabled,
                            onCheckedChange = onSetDynamicColor,
                        )

                        OutlinedButton(
                            onClick = { showResetConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("reset_defaults_button"),
                        ) {
                            Text("Reset All Rules to Defaults")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showRestrictedGuide) {
        AlertDialog(
            onDismissRequest = { showRestrictedGuide = false },
            title = { Text("Android 13+ Restricted Settings") },
            text = {
                Text(
                    "If Android grays out the Accessibility toggle:\n\n" +
                        "1. Tap \"Open App Info\" below.\n" +
                        "2. Tap the three-dot menu (⋮) in the top-right corner.\n" +
                        "3. Select \"Allow restricted settings\".\n" +
                        "4. Return to Accessibility Settings and enable KeyRemap Engine.",
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRestrictedGuide = false
                        onOpenAppInfoSettings()
                    },
                ) {
                    Text("Open App Info")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestrictedGuide = false }) {
                    Text("Close")
                }
            },
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset Configuration?") },
            text = {
                Text("This restores 1× Click → Gemini / Assistant, Hold → Flashlight, and default timings.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetDefaults()
                    },
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
