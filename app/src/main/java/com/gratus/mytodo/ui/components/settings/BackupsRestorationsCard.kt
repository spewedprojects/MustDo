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
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.ui.theme.AppFontSizes
import java.io.OutputStream

/**
 * Backups and restorations card providing export to device and file import actions.
 */
@Composable
fun BackupsRestorationsCard(
    context: Context,
    colorSchemeType: String,
    importLauncher: ManagedActivityResultLauncher<String, android.net.Uri?>,
    onExportJson: (OutputStream) -> Boolean,
    onExportDb: (OutputStream) -> Boolean,
    modifier: Modifier = Modifier
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
                text = "Backups & Restorations",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            Text(
                text = "Import or export your list entries easily. Alarms will be rescheduled cleanly upon successful restore.",
                fontSize = AppFontSizes.small,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val jsonSuccess = saveBackupToDocuments(
                            context,
                            "todo_backup",
                            ".json",
                            "application/json"
                        ) { output ->
                            onExportJson(output)
                        }

                        val dbSuccess = saveBackupToDocuments(
                            context,
                            "todo_backup",
                            ".db",
                            "application/octet-stream"
                        ) { output ->
                            onExportDb(output)
                        }
                        if (jsonSuccess && dbSuccess) {
                            Toast.makeText(
                                context,
                                "Backup files exported to Documents folder!",
                                Toast.LENGTH_LONG
                            ).show()
                        } else if (jsonSuccess) {
                            Toast.makeText(
                                context,
                                "JSON exported, but Database file export failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (dbSuccess) {
                            Toast.makeText(
                                context,
                                "Database exported, but JSON backup failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Export failed. Please check storage.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_device_btn")
                ) {
                    Icon(imageVector = Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export to Device")
                }

                OutlinedButton(
                    onClick = {
                        importLauncher.launch("*/*")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_file_btn")
                ) {
                    Icon(imageVector = Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import & Restore Backup")
                }
            }
        }
    }
}
