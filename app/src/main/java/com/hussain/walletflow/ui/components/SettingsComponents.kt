package com.hussain.walletflow.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun GroupSurface(
    count: Int,
    modifier: Modifier = Modifier,
    content: @Composable (index: Int, shape: RoundedCornerShape) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (i in 0 until count) {
            val shape = when {
                count == 1 -> RoundedCornerShape(24.dp)
                i == 0 -> RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp
                )
                i == count - 1 -> RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 4.dp,
                    bottomStart = 24.dp,
                    bottomEnd = 24.dp
                )
                else -> RoundedCornerShape(4.dp)
            }
            content(i, shape)
        }
    }
}

@Composable
fun SettingTile(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    icon: Any? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when (icon) {
                        is ImageVector -> {
                            Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                        }
                        is Painter -> {
                            Image(
                                painter = icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title, style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis, maxLines = 1
                )
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis, maxLines = 2
                )
            }
            Spacer(Modifier.width(8.dp))
            HapticSwitch(
                checked = checked,
                onCheckedChange = { onCheckedChange(it) },
                thumbContent = {
                    Icon(
                        if (checked) Icons.Filled.Check else Icons.Filled.Close,
                        null, Modifier.size(SwitchDefaults.IconSize)
                    )
                }
            )
        }
    }
}

@Composable
fun ClickableTile(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    modifier: Modifier = Modifier,
    icon: Any? = null,
    iconColor: Color = MaterialTheme.colorScheme.primary,
    iconContainerColor: Color? = null,
    bottomContent: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = {
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp)
        ) {
            if (icon != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = iconContainerColor ?: iconColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when (icon) {
                            is ImageVector -> {
                                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
                            }
                            is Painter -> {
                                Image(
                                    painter = icon,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(16.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    title, style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Ellipsis, maxLines = 1
                )
                Text(
                    subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    overflow = TextOverflow.Ellipsis, maxLines = 2
                )
                if (bottomContent != null) {
                    bottomContent()
                }
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

@Composable
fun HapticSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    thumbContent: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Switch(
        checked = checked,
        onCheckedChange = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCheckedChange(it)
        },
        thumbContent = thumbContent
    )
}
