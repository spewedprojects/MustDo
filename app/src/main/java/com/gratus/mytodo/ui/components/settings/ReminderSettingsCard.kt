/*
 * MustDO
 * Copyright (C) 2026 spewedprojects <rkharat98@live.com>
 *
 * This file is part of MustDo Application.
 *
 * MustDo is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * See the LICENSE file for details.
 */

package com.gratus.mytodo.ui.components.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.SoftTodoTheme

/**
 * Reminder settings section card managing permissions status, repeat intervals, and alarm ringtones.
 */
@Composable
fun ReminderSettingsCard(
    isAlarmPermissionGranted: Boolean,
    colorSchemeType: String,
    isNotificationPermissionGranted: Boolean,
    activeInterval: Int,
    ringtoneUri: String?,
    context: Context,
    onIntervalChange: (Int) -> Unit,
    onRingtoneClick: () -> Unit,
    modifier: Modifier = Modifier,
    isFullScreenPermissionGranted: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = when (colorSchemeType) {
            "simple" -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            "system" -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
            )
            else -> BorderStroke(0.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.0f)
            )
        },
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Reminder Settings",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = "System Permissions Status",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                // Alarms & Reminders Permission Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            } else {
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Alarms & Reminders",
                        fontSize = AppFontSizes.medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isAlarmPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isAlarmPermissionGranted) "Active" else "Disabled",
                            color = if (isAlarmPermissionGranted) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = AppFontSizes.extraSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Notification Permission Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {}
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notifications",
                        fontSize = AppFontSizes.medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isNotificationPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isNotificationPermissionGranted) "Active" else "Disabled",
                            color = if (isNotificationPermissionGranted) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = AppFontSizes.extraSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Full Screen Alarms Permission Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            } else {
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            }
                            try { context.startActivity(intent) } catch (_: Exception) {
                                try {
                                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    })
                                } catch (_: Exception) {}
                            }
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Full Screen Alarms",
                        fontSize = AppFontSizes.medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isFullScreenPermissionGranted) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isFullScreenPermissionGranted) "Active" else "Disabled",
                            color = if (isFullScreenPermissionGranted) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = AppFontSizes.extraSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!isAlarmPermissionGranted || !isNotificationPermissionGranted || !isFullScreenPermissionGranted) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Change in System Settings",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = AppFontSizes.small,
                        modifier = Modifier
                            .clickable {
                                val intent = if (!isNotificationPermissionGranted) {
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                } else if (!isAlarmPermissionGranted) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                    } else {
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                    }
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                    } else {
                                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                        }
                                    }
                                }
                                try { context.startActivity(intent) } catch (_: Exception) {}
                            }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Reminder Repeat Interval",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Set how many minutes the alarm waits to repeat itself (applied to all repeat intervals).",
                    fontSize = AppFontSizes.small,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    lineHeight = AppFontSizes.medium
                )
                
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val intervals = listOf(
                        Pair(5, "5m"),
                        Pair(10, "10m"),
                        Pair(15, "15m"),
                        Pair(30, "30m"),
                        Pair(60, "60m")
                    )

                    intervals.forEach { (minutes, label) ->
                        val isSelected = activeInterval == minutes
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onIntervalChange(minutes) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = AppFontSizes.small,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                        else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))

            // Alarm ringtone configuration row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onRingtoneClick() }
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Alarm Ringtone",
                        fontSize = AppFontSizes.medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = getRingtoneTitle(context, ringtoneUri),
                        fontSize = AppFontSizes.small,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Select Alarm Tone",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Reminder Settings Card - Permissions Granted")
@Composable
fun ReminderSettingsCardPermissionsGrantedPreview() {
    SoftTodoTheme {
        ReminderSettingsCard(
            isAlarmPermissionGranted = true,
            colorSchemeType = "simple",
            isNotificationPermissionGranted = true,
            activeInterval = 15,
            ringtoneUri = null,
            context = LocalContext.current,
            onIntervalChange = {},
            onRingtoneClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true, name = "Reminder Settings Card - Permissions Denied")
@Composable
fun ReminderSettingsCardPermissionsDeniedPreview() {
    SoftTodoTheme {
        ReminderSettingsCard(
            isAlarmPermissionGranted = false,
            colorSchemeType = "simple",
            isNotificationPermissionGranted = false,
            activeInterval = 5,
            ringtoneUri = null,
            context = LocalContext.current,
            onIntervalChange = {},
            onRingtoneClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
