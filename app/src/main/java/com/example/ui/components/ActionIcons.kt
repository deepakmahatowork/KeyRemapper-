package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ActionType
import com.example.model.ButtonGesture
import com.example.model.RemapRule
import com.example.model.StateConstraint

@Composable
fun ActionType.painter(): Painter = when (this) {
    ActionType.VOICE_ASSISTANT -> painterResource(R.drawable.ic_assistant)
    ActionType.TOGGLE_FLASHLIGHT -> painterResource(R.drawable.ic_flashlight)
    ActionType.TAKE_SCREENSHOT -> painterResource(R.drawable.ic_screenshot)
    ActionType.CYCLE_RINGER_MODE -> painterResource(R.drawable.ic_ringer)
    ActionType.MEDIA_PLAY_PAUSE -> rememberVectorPainter(Icons.Rounded.PlayArrow)
    ActionType.MEDIA_NEXT -> painterResource(R.drawable.ic_skip_next)
    ActionType.OPEN_CAMERA -> painterResource(R.drawable.ic_camera)
    ActionType.LOCK_SCREEN -> rememberVectorPainter(Icons.Rounded.Lock)
    ActionType.OPEN_NOTIFICATIONS -> rememberVectorPainter(Icons.Rounded.Notifications)
    ActionType.LAUNCH_APP -> painterResource(R.drawable.ic_apps)
    ActionType.SIMULATE_KEYCODE -> painterResource(R.drawable.ic_keyboard)
    ActionType.NONE -> rememberVectorPainter(Icons.Rounded.Close)
}

@Composable
fun StatusPill(
    label: String,
    active: Boolean,
    activeColor: Color = MaterialTheme.colorScheme.tertiary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val containerColor by animateColorAsState(
        targetValue = if (active) activeColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "status_pill_bg",
    )
    val contentColor by animateColorAsState(
        targetValue = if (active) activeColor else inactiveColor,
        label = "status_pill_fg",
    )

    Surface(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        color = containerColor,
        shape = RoundedCornerShape(50),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(contentColor),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GestureRuleCard(
    gesture: ButtonGesture,
    rule: RemapRule?,
    testTagPrefix: String,
    onEditClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionType = rule?.actionType ?: ActionType.NONE
    val isAssigned = actionType != ActionType.NONE
    val isEnabled = (rule?.isEnabled ?: false) && isAssigned
    val constraint = rule?.constraint ?: StateConstraint.ALWAYS

    val cardColor by animateColorAsState(
        targetValue = if (isEnabled) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainerLowest
        },
        label = "rule_card_bg",
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_${gesture.name.lowercase()}_card")
            .clickable(onClick = onEditClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isEnabled) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = actionType.painter(),
                    contentDescription = actionType.displayName,
                    tint = if (isEnabled) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp),
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        color = if (isEnabled) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = gesture.badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }

                    Text(
                        text = gesture.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (isAssigned) actionType.displayName else "Tap to assign action",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isAssigned) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                val payloadText = rule?.actionPayloadLabel?.takeIf { it.isNotBlank() }
                if (payloadText != null || (isAssigned && constraint != StateConstraint.ALWAYS)) {
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (payloadText != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = payloadText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        if (isAssigned && constraint != StateConstraint.ALWAYS) {
                            Surface(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    text = constraint.shortName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isAssigned) {
                Switch(
                    checked = rule?.isEnabled ?: false,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("${testTagPrefix}_${gesture.name.lowercase()}_switch"),
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Configure ${gesture.displayName}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
    }
}
