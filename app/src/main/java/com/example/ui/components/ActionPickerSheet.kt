package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ActionType
import com.example.model.InstalledAppInfo
import com.example.model.KeycodePresets
import com.example.model.StateConstraint
import com.example.ui.viewmodel.EditingRuleTarget

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ActionPickerSheet(
    target: EditingRuleTarget,
    installedApps: List<InstalledAppInfo>,
    onDismiss: () -> Unit,
    onSave: (
        actionType: ActionType,
        payload: String,
        payloadLabel: String,
        constraint: StateConstraint,
    ) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedAction by remember(target) { mutableStateOf(target.rule.actionType) }
    var selectedConstraint by remember(target) { mutableStateOf(target.rule.constraint) }
    var payload by remember(target) { mutableStateOf(target.rule.actionPayload) }
    var payloadLabel by remember(target) { mutableStateOf(target.rule.actionPayloadLabel) }
    var appSearchQuery by remember { mutableStateOf("") }
    var customKeyCodeText by remember(target) {
        mutableStateOf(
            if (target.rule.actionType == ActionType.SIMULATE_KEYCODE) {
                target.rule.actionPayload
            } else {
                "219"
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Text(
                text = "${target.profileLabel} · ${target.rule.gesture.displayName}",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = target.rule.gesture.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(ActionType.entries, key = { it.name }) { action ->
                    val isSelected = selectedAction == action
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedAction = action
                                if (action == ActionType.SIMULATE_KEYCODE && payload.toIntOrNull() == null) {
                                    val code = customKeyCodeText.toIntOrNull() ?: 219
                                    payload = code.toString()
                                    payloadLabel = KeycodePresets.labelForKeyCode(code)
                                } else if (!action.requiresPayload) {
                                    payload = ""
                                    payloadLabel = ""
                                }
                            },
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                painter = action.painter(),
                                contentDescription = null,
                                tint = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = action.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                Text(
                                    text = action.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedAction == ActionType.LAUNCH_APP) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Text(
                        text = if (payloadLabel.isNotBlank()) "Selected App: $payloadLabel" else "Choose App to Launch",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Search installed apps...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val filtered = remember(installedApps, appSearchQuery) {
                        if (appSearchQuery.isBlank()) installedApps
                        else installedApps.filter {
                            it.label.contains(appSearchQuery, ignoreCase = true) ||
                                it.packageName.contains(appSearchQuery, ignoreCase = true)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 140.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(filtered, key = { it.packageName }) { app ->
                            val chosen = payload == app.packageName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (chosen) MaterialTheme.colorScheme.secondaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainer,
                                    )
                                    .clickable {
                                        payload = app.packageName
                                        payloadLabel = app.label
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                if (chosen) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Selected app",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(visible = selectedAction == ActionType.SIMULATE_KEYCODE) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    OutlinedTextField(
                        value = customKeyCodeText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() }
                            customKeyCodeText = filtered
                            val code = filtered.toIntOrNull()
                            if (code != null) {
                                payload = code.toString()
                                payloadLabel = KeycodePresets.labelForKeyCode(code)
                            }
                        },
                        label = { Text("Android KeyEvent Code (e.g. 219 = Assist)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        KeycodePresets.common.drop(1).forEach { preset ->
                            FilterChip(
                                selected = payload == preset.keyCode.toString(),
                                onClick = {
                                    customKeyCodeText = preset.keyCode.toString()
                                    payload = preset.keyCode.toString()
                                    payloadLabel = preset.name
                                },
                                label = { Text(preset.name.removePrefix("KEYCODE_")) },
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Condition",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StateConstraint.entries.forEach { constraint ->
                    FilterChip(
                        selected = selectedConstraint == constraint,
                        onClick = { selectedConstraint = constraint },
                        label = { Text(constraint.shortName) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val canSave = when (selectedAction) {
                ActionType.LAUNCH_APP -> payload.isNotBlank()
                ActionType.SIMULATE_KEYCODE -> payload.toIntOrNull() != null
                else -> true
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        onSave(ActionType.NONE, "", "", StateConstraint.ALWAYS)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("clear_rule_button"),
                ) {
                    Text("Clear")
                }
                Button(
                    onClick = {
                        onSave(selectedAction, payload, payloadLabel, selectedConstraint)
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .testTag("save_rule_button"),
                ) {
                    Text("Save Action")
                }
            }
        }
    }
}
