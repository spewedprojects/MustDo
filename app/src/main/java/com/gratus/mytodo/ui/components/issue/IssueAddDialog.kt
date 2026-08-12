package com.gratus.mytodo.ui.components.issue

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.gratus.mytodo.data.IssueItem
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.dialogContainerColor

private val previewIssue = IssueItem(
    id = "1342",
    serialNumber = 3,
    title = "Fix crash on login screen",
    description = "NullPointerException when tapping login button rapidly.",
    category = "Issue",
    isClosed = false,
    timestamp = System.currentTimeMillis() - 86400000
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueAddDialog(
    initialItem: IssueItem?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, category: String) -> Unit
) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf(initialItem?.title ?: "") }
    var description by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue(initialItem?.description ?: "")) }
    var category by rememberSaveable { mutableStateOf(initialItem?.category ?: "Issue") }
    
    val categories = listOf("Issue", "Feature", "Idea")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.98f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.dialogContainerColor,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth()
            ) {
                Text(
                    text = if (initialItem == null) "New Tracker Item" else "Edit Item",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Category", fontSize = AppFontSizes.small)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val catColor = getCategoryColor(cat)
                        val isSelected = category == cat

                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.2f),
                                selectedLabelColor = catColor,
                                containerColor = Color.Transparent,
                                labelColor = catColor.copy(alpha = 0.6f)
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = catColor.copy(alpha = 0.4f),
                                selectedBorderColor = catColor,
                                borderWidth = 1.5.dp
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = {
                        val text = description.text
                        val selection = description.selection
                        if (selection.start != selection.end) {
                            val selectedText = text.substring(selection.start, selection.end)
                            val formatted = "**$selectedText**"
                            val newText = text.replaceRange(selection.start, selection.end, formatted)
                            description = TextFieldValue(newText, TextRange(selection.start + formatted.length))
                        } else {
                            Toast.makeText(context, "Select text to format bold", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Bold")
                    }
                    IconButton(
                        onClick = {
                        val text = description.text
                        val selection = description.selection
                        if (selection.start != selection.end) {
                            val selectedText = text.substring(selection.start, selection.end)
                            val formatted = "__${selectedText}__"
                            val newText = text.replaceRange(selection.start, selection.end, formatted)
                            description = TextFieldValue(
                                text = newText,
                                selection = TextRange(selection.start + 2, selection.start + 2 + selectedText.length)
                            )
                        } else {
                            Toast.makeText(context, "Select text to format italic", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.FormatItalic, contentDescription = "Italic")
                    }
                    IconButton(onClick = {
                        val text = description.text
                        val selection = description.selection
                        val newText = text.replaceRange(selection.start, selection.start, "- ")
                        description = TextFieldValue(newText, TextRange(selection.start + 2))
                    }) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Bullet List")
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(title, description.text, category) },
                        enabled = title.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Add/Edit Issue Dialog")
@Composable
fun IssueAddDialogPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        Box(modifier = Modifier.fillMaxSize()) {
            IssueAddDialog(
                initialItem = previewIssue,
                onDismiss = {},
                onSave = { _, _, _ -> }
            )
        }
    }
}
