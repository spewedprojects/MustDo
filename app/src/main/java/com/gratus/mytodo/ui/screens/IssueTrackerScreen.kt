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

package com.gratus.mytodo.ui.screens

import com.gratus.mytodo.ui.components.issue.CategoryBadge
import com.gratus.mytodo.ui.components.issue.IssueAddDialog
import com.gratus.mytodo.ui.components.issue.IssueCard
import com.gratus.mytodo.ui.components.issue.getCategoryColor

import android.R
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import kotlinx.coroutines.delay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gratus.mytodo.data.IssueItem
import com.gratus.mytodo.data.IssueComment
import com.gratus.mytodo.ui.IssueFilter
import com.gratus.mytodo.ui.IssueTrackerViewModel
import com.gratus.mytodo.ui.components.parseStyledDescription
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.dialogContainerColor
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueTrackerScreen(
    onOpenDrawer: () -> Unit,
    colorSchemeType: String,
    viewModel: IssueTrackerViewModel = viewModel()
) {
    val context = LocalContext.current
    val issues by viewModel.issues.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()

    IssueTrackerScreenContent(
        colorSchemeType = colorSchemeType,
        issues = issues,
        searchQuery = searchQuery,
        currentFilter = currentFilter,
        onSearchQueryChange = { viewModel.setSearchQuery(it) },
        onFilterChange = { viewModel.setFilter(it) },
        onExport = { viewModel.exportAndShare(context) },
        onToggleIssue = { viewModel.toggleStatus(it) },
        onDeleteIssue = { viewModel.deleteIssue(it) },
        onUpdateIssue = { viewModel.updateIssue(it) },
        onAddIssue = { title, desc, cat -> viewModel.addIssue(title, desc, cat) },
        onAddComment = { issue, comment -> viewModel.addComment(issue, comment) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueTrackerScreenContent(
    colorSchemeType: String,
    issues: List<IssueItem>,
    searchQuery: String,
    currentFilter: IssueFilter,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (IssueFilter) -> Unit,
    onExport: () -> Unit,
    onToggleIssue: (IssueItem) -> Unit,
    onDeleteIssue: (IssueItem) -> Unit,
    onUpdateIssue: (IssueItem) -> Unit,
    onAddIssue: (String, String, String) -> Unit,
    onAddComment: (IssueItem, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var itemToEditId by rememberSaveable { mutableStateOf<String?>(null) }

    val filteredIssues = issues.filter {
        val matchesFilter = when (currentFilter) {
            IssueFilter.ALL -> true
            IssueFilter.OPEN -> !it.isClosed
            IssueFilter.CLOSED -> it.isClosed
        }
        val matchesSearch = it.title.contains(searchQuery, ignoreCase = true) || 
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.serialNumber.toString() == searchQuery || // Exact match for ID
                "#${it.serialNumber}".contains(searchQuery, ignoreCase = true) || // Match for "#12"
                it.appVersion?.contains(searchQuery, ignoreCase = true) == true
        matchesFilter && matchesSearch
    }.sortedByDescending { it.timestamp }

    Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Issue")
                }
            }
        ) { paddingValues ->
            // Use a Column to stack the Header (Static) and the LazyColumn (Scrollable)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ){
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            placeholder = { Text("Search issues...") },
                            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search, capitalization = KeyboardCapitalization.Sentences)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = onExport)
                            {
                                Text(
                                    text = "Export",
                                    fontSize = AppFontSizes.small,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Export",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        RoundedCornerShape(8.dp)
                                    )
                            ) {
                                listOf(
                                    Pair(IssueFilter.ALL, "All (${issues.size})"),
                                    Pair(IssueFilter.OPEN, "Open (${issues.count { !it.isClosed }})"),
                                    Pair(IssueFilter.CLOSED, "Closed (${issues.count { it.isClosed }})")
                                ).forEach { (opt, label) ->
                                    val active = currentFilter == opt
                                    Box(
                                        modifier = Modifier
                                            .clickable { onFilterChange(opt) }
                                            .background(
                                                if (active) MaterialTheme.colorScheme.secondaryContainer
                                                else Color.Transparent
                                            )
                                            .padding(horizontal = 6.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = AppFontSizes.extraSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (issues.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "No issues", modifier = Modifier.size(82.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            Text("No issues tracked yet", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                            Text("Tap the + button to add a new issue", fontSize = AppFontSizes.small, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        }
                    }
                } else if (filteredIssues.isEmpty() && searchQuery.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No issues match your search", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                } else if (filteredIssues.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No issues match the current filter", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 85.dp, start = 16.dp, end = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredIssues, key = { it.id }) { issue ->
                            IssueCard(
                                issue = issue,
                                onToggle = { onToggleIssue(issue) },
                                onDelete = { onDeleteIssue(issue) },
                                onEdit = { itemToEditId = issue.id; showAddDialog = true },
                                onAddComment = { comment -> onAddComment(issue, comment) }
                            )
                        }
                    }
                }
            }
    }

    if (showAddDialog) {
        val itemToEdit = issues.find { it.id == itemToEditId }
        IssueAddDialog(
            initialItem = itemToEdit,
            onDismiss = {
                showAddDialog = false
                itemToEditId = null
            },
            onSave = { title, desc, cat ->
                if (itemToEdit != null) {
                    onUpdateIssue(itemToEdit.copy(title = title, description = desc, category = cat))
                } else {
                    onAddIssue(title, desc, cat)
                }
                showAddDialog = false
                itemToEditId = null
            }
        )
    }
}


private val previewIssues = listOf(
    IssueItem(
        id = "1342",
        serialNumber = 3,
        title = "Fix crash on login screen",
        description = "NullPointerException when tapping login button rapidly.\n- Reproducible on Android 12\n- Need to check login flow thread locks.",
        category = "Issue",
        isClosed = false,
        timestamp = System.currentTimeMillis() - 86400000,
        comments = listOf(IssueComment("Assigned to dev team"), IssueComment("Adding test logs..."))
    ),
    IssueItem(
        id = "23254",
        serialNumber = 2,
        title = "Implement biometric authentication",
        description = "Allow users to log in using fingerprint or face unlock for faster access.",
        category = "Feature",
        isClosed = false,
        timestamp = System.currentTimeMillis() - 7200000L,
        comments = emptyList()
    ),
    IssueItem(
        id = "35342",
        serialNumber = 1,
        title = "Snooze presets customization",
        description = "Idea to allow users to edit custom snooze duration presets in settings.",
        category = "Idea",
        isClosed = true,
        timestamp = System.currentTimeMillis() - 172800000L,
        comments = listOf(IssueComment("Good idea, completed in v2.4"))
    )
)

@Preview(showBackground = true, showSystemUi = true, name = "Issue Tracker Screen - Navigable Light Mode")
@Composable
fun IssueTrackerScreenPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "light") {
        IssueTrackerScreenContent(
            colorSchemeType = "minimal",
            issues = previewIssues,
            searchQuery = "",
            currentFilter = IssueFilter.ALL,
            onSearchQueryChange = {},
            onFilterChange = {},
            onExport = {},
            onToggleIssue = {},
            onDeleteIssue = {},
            onUpdateIssue = {},
            onAddIssue = { _, _, _ -> },
            onAddComment = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Issue Tracker Screen - Navigable Dark Mode")
@Composable
fun IssueTrackerScreenDarkPreview() {
    SoftTodoTheme(colorSchemeType = "minimal", themeMode = "dark") {
        IssueTrackerScreenContent(
            colorSchemeType = "minimal",
            issues = previewIssues,
            searchQuery = "",
            currentFilter = IssueFilter.ALL,
            onSearchQueryChange = {},
            onFilterChange = {},
            onExport = {},
            onToggleIssue = {},
            onDeleteIssue = {},
            onUpdateIssue = {},
            onAddIssue = { _, _, _ -> },
            onAddComment = { _, _ -> }
        )
    }
}
