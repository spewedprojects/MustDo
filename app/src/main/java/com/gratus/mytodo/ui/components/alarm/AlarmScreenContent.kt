package com.gratus.mytodo.ui.components.alarm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.theme.PriorityAmber
import com.gratus.mytodo.ui.theme.PriorityOrange
import com.gratus.mytodo.ui.theme.PriorityRed
import com.gratus.mytodo.ui.theme.PriorityYellow
import com.gratus.mytodo.ui.theme.SoftTodoTheme

@Composable
fun AlarmScreen(
    activeTasks: List<Task>,
    onCompleteTask: (Task) -> Unit,
    onSnoozeTask: (Task, Int) -> Unit,
    onStopTaskAlarm: (Task) -> Unit,
    onBackFallback: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSnoozeTaskId by remember { mutableStateOf<Int?>(null) }
    var showCustomSnooze by remember { mutableStateOf(false) }
    var customSnoozeMinutesText by remember { mutableStateOf("") }

    BackHandler {
        if (activeSnoozeTaskId != null) {
            if (showCustomSnooze) {
                showCustomSnooze = false
                customSnoozeMinutesText = ""
            } else {
                activeSnoozeTaskId = null
            }
        } else {
            onBackFallback()
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Alarm,
                contentDescription = "Alarm Fired",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .size(72.dp)
                    .padding(bottom = 16.dp)
            )

            Text(
                text = "CRITICAL TASK ALARM",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            activeTasks.forEach { task ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(
                            width = 1.dp,
                            color = when (task.priority) {
                                1 -> PriorityRed
                                2 -> PriorityOrange
                                3 -> PriorityAmber
                                4 -> PriorityYellow
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (task.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (activeSnoozeTaskId == task.id) {
                            if (!showCustomSnooze) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Select Snooze Duration",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(bottom = 8.dp)
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
                                                    .clickable {
                                                        onSnoozeTask(task, mins)
                                                    }
                                                    .padding(vertical = 12.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${mins}m",
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                                .clickable { showCustomSnooze = true }
                                                .padding(vertical = 12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+",
                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = { activeSnoozeTaskId = null }) {
                                        Text("Cancel")
                                    }
                                }
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    OutlinedTextField(
                                        value = customSnoozeMinutesText,
                                        onValueChange = { input ->
                                            if (input.isEmpty() || input.all { it.isDigit() }) {
                                                customSnoozeMinutesText = input
                                            }
                                        },
                                        label = { Text("Minutes") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        TextButton(onClick = {
                                            showCustomSnooze = false
                                            customSnoozeMinutesText = ""
                                        }) {
                                            Text("Back")
                                        }
                                        Button(onClick = {
                                            val mins = customSnoozeMinutesText.toIntOrNull()
                                            if (mins != null && mins > 0) {
                                                onSnoozeTask(task, mins)
                                            }
                                        }) {
                                            Text("Set")
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Button(
                                    onClick = { onCompleteTask(task) },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Mark Complete", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { activeSnoozeTaskId = task.id },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(52.dp)
                                ) {
                                    Text("Snooze", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onStopTaskAlarm(task) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.errorContainer,
                                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Stop", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Alarm Screen - Dark Theme")
@Composable
fun AlarmScreenDarkPreview() {
    SoftTodoTheme(themeMode = "dark", colorSchemeType = "minimal") {
        AlarmScreen(
            activeTasks = listOf(
                Task(id = 1, title = "Critical Presentation Draft", description = "Review the slides for the meeting and send to manager", priority = 1, dateAdded = "2026-06-27"),
                Task(id = 2, title = "Call dentist", description = "Schedule checkup", priority = 3, dateAdded = "2026-06-27")
            ),
            onCompleteTask = {},
            onSnoozeTask = { _, _ -> },
            onStopTaskAlarm = {},
            onBackFallback = {}
        )
    }
}

@Preview(showBackground = true, name = "Alarm Screen - Light Theme")
@Composable
fun AlarmScreenLightPreview() {
    SoftTodoTheme(themeMode = "light", colorSchemeType = "minimal") {
        AlarmScreen(
            activeTasks = listOf(
                Task(id = 1, title = "Critical Presentation Draft", description = "Review the slides for the meeting and send to manager", priority = 1, dateAdded = "2026-06-27")
            ),
            onCompleteTask = {},
            onSnoozeTask = { _, _ -> },
            onStopTaskAlarm = {},
            onBackFallback = {}
        )
    }
}
