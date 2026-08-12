package com.gratus.mytodo.ui.components.alarm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.dialogContainerColor

@Composable
fun SnoozeDialog(
    colorSchemeType: String,
    onSnooze: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomInput by remember { mutableStateOf(false) }
    var customMinutesText by remember { mutableStateOf("") }

    BackHandler {
        if (showCustomInput) {
            showCustomInput = false
            customMinutesText = ""
        } else {
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier.border(
            width = 1.dp,
            color = if (colorSchemeType == "simple") {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            } else if (colorSchemeType == "minimal") {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            } else {
                Color.Transparent
            },
            shape = RoundedCornerShape(28.dp)
        ),
        containerColor = MaterialTheme.colorScheme.dialogContainerColor,
        title = {
            Text(
                text = if (showCustomInput) "Custom Snooze" else "Snooze Reminder",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            if (!showCustomInput) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Choose snooze duration:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf(5, 10, 15, 30)
                        presets.forEach { mins ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { onSnooze(mins) }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${mins}m",
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .clickable { showCustomInput = true }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Enter duration in minutes:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = customMinutesText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                customMinutesText = input
                            }
                        },
                        label = { Text("Minutes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (showCustomInput) {
                TextButton(
                    onClick = {
                        val mins = customMinutesText.toIntOrNull()
                        if (mins != null && mins > 0) {
                            onSnooze(mins)
                        }
                    }
                ) {
                    Text("Set", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (showCustomInput) {
                        showCustomInput = false
                        customMinutesText = ""
                    } else {
                        onDismiss()
                    }
                }
            ) {
                Text(
                    text = if (showCustomInput) "Back" else "Cancel",
                    color = if (showCustomInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    )
}

@Preview(showBackground = true, name = "Snooze Dialog - Minimal Dark")
@Composable
fun SnoozeDialogMinimalDarkPreview() {
    SoftTodoTheme(themeMode = "dark", colorSchemeType = "minimal") {
        Box(modifier = Modifier.padding(16.dp)) {
            SnoozeDialog(
                colorSchemeType = "minimal",
                onSnooze = {},
                onDismiss = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Snooze Dialog - Simple Light")
@Composable
fun SnoozeDialogSimpleLightPreview() {
    SoftTodoTheme(themeMode = "light", colorSchemeType = "simple") {
        Box(modifier = Modifier.padding(16.dp)) {
            SnoozeDialog(
                colorSchemeType = "simple",
                onSnooze = {},
                onDismiss = {}
            )
        }
    }
}
