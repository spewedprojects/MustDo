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

package com.gratus.mytodo.ui.components.home

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.SoftTodoTheme

/**
 * Permission warning card banner displayed when alarm or notification permissions are missing.
 */
@Composable
fun PermissionWarningCard(
    isAlarmPermissionGranted: Boolean,
    isNotificationPermissionGranted: Boolean,
    colorSchemeType: String,
    context: Context,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (colorSchemeType == "minimal" || colorSchemeType == "colorful") {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (colorSchemeType == "simple" || colorSchemeType == "minimal") {
            androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = if (colorSchemeType == "simple") {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (colorSchemeType == "simple") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Reminder Notifications Disabled",
                    fontWeight = FontWeight.Bold,
                    color = if (colorSchemeType == "simple") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.titleSmall
                )
            }
            
            Text(
                text = if (!isNotificationPermissionGranted && !isAlarmPermissionGranted) {
                    "Both Notification permission and Alarms & Reminders permission are required to trigger notifications for urgent scheduled tasks."
                } else if (!isNotificationPermissionGranted) {
                    "Notification permission is required to trigger notifications for urgent scheduled tasks."
                } else {
                    "Alarms & Reminders permission is required to schedule exact notifications for urgent tasks."
                },
                fontSize = AppFontSizes.small,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = AppFontSizes.medium
            )
            
            Button(
                onClick = {
                    val intent = if (!isNotificationPermissionGranted) {
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        } else {
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        }
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open settings", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (colorSchemeType == "simple") MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                    contentColor = if (colorSchemeType == "simple") MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Grant Permission", fontSize = AppFontSizes.small, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, name = "Permission Warning Card")
@Composable
fun PermissionWarningCardPreview() {
    SoftTodoTheme {
        PermissionWarningCard(
            isAlarmPermissionGranted = false,
            isNotificationPermissionGranted = false,
            colorSchemeType = "minimal",
            context = androidx.compose.ui.platform.LocalContext.current
        )
    }
}
