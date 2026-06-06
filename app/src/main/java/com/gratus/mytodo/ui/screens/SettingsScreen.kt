package com.gratus.mytodo.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.ui.MainViewModel
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import java.io.File

/**
 * SettingsScreen includes theme configurations, color scheme selectors, and backups.
 */
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    colorSchemeType: String
) {
    val activeTheme by viewModel.settingsTheme.collectAsState()
    val activeScheme by viewModel.settingsColorScheme.collectAsState()

    SettingsScreenContent(
        activeTheme = activeTheme,
        activeScheme = activeScheme,
        onThemeChange = { viewModel.setTheme(it) },
        onSchemeChange = { viewModel.setColorScheme(it) },
        onExportBackup = { viewModel.exportBackup() },
        onImportBackup = { text, onComplete -> viewModel.importBackup(text, onComplete) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    activeTheme: String,
    activeScheme: String,
    onThemeChange: (String) -> Unit,
    onSchemeChange: (String) -> Unit,
    onExportBackup: () -> String,
    onImportBackup: (String, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Theme & Scheme Configuration Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Aesthetics Settings",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                // Light / Dark / Auto selectors
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Theme Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themeList = listOf(
                            Pair("auto", "System Auto"),
                            Pair("light", "Light"),
                            Pair("dark", "Dark")
                        )

                        themeList.forEach { (mode, label) ->
                            val isSelected = activeTheme == mode
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
                                    .clickable { onThemeChange(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                                            else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Color schemes options (Simple, Colorful, Monet)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Color Schemes Palette", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    
                    val schemes = listOf(
                        Triple("minimal", "Clean Minimalism", "Lavender backing with space-blurry spheres, sleek borders, and elegant state indicators."),
                        Triple("simple", "Simple B&W Only", "Black and white base, accents colored strictly around Priority levels."),
                        Triple("colorful", "Pastel Colorful", "Soft pastel layers with faint radial sweeping neon screen background."),
                        Triple("system", "System Monet", "Dynamic native Material You colors synched directly from Android 12+ wallpaper settings.")
                    )

                    schemes.forEach { (schemeKey, name, desc) ->
                        val isSelected = activeScheme == schemeKey
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f) 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.secondary 
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSchemeChange(schemeKey) }
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = when (schemeKey) {
                                            "minimal" -> Icons.Default.Spa
                                            "simple" -> Icons.Default.BrightnessLow
                                            "colorful" -> Icons.Default.Palette
                                            else -> Icons.Default.SettingsSuggest
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            lineHeight = 13.sp
                                        )
                                    }
                                }

                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSchemeChange(schemeKey) },
                                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.secondary)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Backups & Exports Container
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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

                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Text(
                    text = "Import or export your list entries easily. Alarms will be rescheduled cleanly upon successful restore.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                // Actions Column
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Export to Clipboard Button
                    Button(
                        onClick = {
                            val backupStr = onExportBackup()
                            if (backupStr.isNotBlank()) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Soft To-Do Backup Record", backupStr)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Full Backup copied to system clipboard!", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Backup failed: Database empty or inaccessible", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_clipboard_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export To Clipboard")
                    }

                    // Import from Clipboard Button
                    OutlinedButton(
                        onClick = {
                            showImportDialog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_clipboard_btn")
                    ) {
                        Icon(imageVector = Icons.Default.SettingsBackupRestore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import & Restore Backup")
                    }

                    // Native file writing backup inside /SDCard/Download folder
                    Button(
                        onClick = {
                            try {
                                val backupStr = onExportBackup()
                                if (backupStr.isNotBlank()) {
                                    val folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    val file = File(folder, "soft_todo_backup.json")
                                    file.writeText(backupStr)
                                    Toast.makeText(context, "Saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Nothing to export yet!", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "File path write denied: Permission Required", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(imageVector = Icons.Default.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Backup File to Downloads Folder")
                    }
                }
            }
        }
    }

    // Interactive backing system paste/verify dialogue
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Compile Backup Restore", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste your clean JSON database backup array code below to proceed.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        placeholder = { Text("[ { \"title\": ... } ]") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 10,
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isBlank()) {
                            Toast.makeText(context, "Please paste valid JSON backup text first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onImportBackup(importText) { success ->
                            if (success) {
                                showImportDialog = false
                                importText = ""
                                Toast.makeText(context, "Restoration complete! Alarms recalculated.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Invalid database format. Re-verify text.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("import_confirm_btn")
                ) {
                    Text("Validate & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    SoftTodoTheme {
        SettingsScreenContent(
            activeTheme = "light",
            activeScheme = "minimal",
            onThemeChange = {},
            onSchemeChange = {},
            onExportBackup = { "" },
            onImportBackup = { _, _ -> }
        )
    }
}
