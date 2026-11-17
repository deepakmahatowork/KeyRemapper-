package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KeyCaptureDialog(
    currentKeyCode: Int,
    currentKeyLabel: String,
    onSelectPreset: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val quickPresets = listOf(
        0 to "Essential Key (0)",
        24 to "Volume Up (24)",
        25 to "Volume Down (25)",
        27 to "Camera Key (27)",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_keyboard),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
        },
        title = {
            Text(text = "Listening for Hardware Key")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "Press the physical button you want to remap now.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Current: $currentKeyLabel (code $currentKeyCode)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Quick presets:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    quickPresets.forEach { (code, label) ->
                        FilterChip(
                            selected = currentKeyCode == code,
                            onClick = {
                                onSelectPreset(code)
                                onDismiss()
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
