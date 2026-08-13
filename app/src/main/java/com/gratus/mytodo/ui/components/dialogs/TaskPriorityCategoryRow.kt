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

package com.gratus.mytodo.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gratus.mytodo.ui.theme.PriorityAmber
import com.gratus.mytodo.ui.theme.PriorityOrange
import com.gratus.mytodo.ui.theme.PriorityRed
import com.gratus.mytodo.ui.theme.PriorityYellow
import com.gratus.mytodo.ui.theme.dialogContainerColor

/**
 * Two-column row containing Priority selector dropdown on left and Category dropdown on right.
 */
@Composable
fun TaskPriorityCategoryRow(
    priority: Int,
    selectedCategory: String?,
    customCategories: List<String>,
    onPriorityChange: (Int) -> Unit,
    onCategoryChange: (String?) -> Unit,
    onShowAddCategoryDialog: () -> Unit,
    onDeleteCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPriorityDropdown by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Column 1: Priority selector
        Row(
            modifier = Modifier
                .weight(1.3f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { showPriorityDropdown = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Priority Level",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (priority) {
                        1 -> "Level 1: Urgent (Red)"
                        2 -> "Level 2: High (Orange)"
                        3 -> "Level 3: Medium (Amber)"
                        4 -> "Level 4: Low (Yellow)"
                        else -> "Level 1: Urgent (Red)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (priority) {
                                1 -> PriorityRed
                                2 -> PriorityOrange
                                3 -> PriorityAmber
                                4 -> PriorityYellow
                                else -> Color.Gray
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = priority.toString(),
                        fontWeight = FontWeight.Bold,
                        color = if (priority == 4) Color.DarkGray else Color.White
                    )
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Select Priority",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            DropdownMenu(
                expanded = showPriorityDropdown,
                onDismissRequest = { showPriorityDropdown = false },
                shape = RoundedCornerShape(10.dp),
                containerColor = MaterialTheme.colorScheme.dialogContainerColor,
                shadowElevation = 8.dp,
            ) {
                (1..4).forEach { p ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (p) {
                                    1 -> "Level 1: Urgent (Red)"
                                    2 -> "Level 2: High (Orange)"
                                    3 -> "Level 3: Medium (Amber)"
                                    4 -> "Level 4: Low (Yellow)"
                                    else -> ""
                                }
                            )
                        },
                        onClick = {
                            onPriorityChange(p)
                            showPriorityDropdown = false
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (p) {
                                            1 -> PriorityRed
                                            2 -> PriorityOrange
                                            3 -> PriorityAmber
                                            4 -> PriorityYellow
                                            else -> Color.Gray
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p.toString(),
                                    fontWeight = FontWeight.Bold,
                                    color = if (p == 4) Color.DarkGray else Color.White,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    )
                }
            }
        }

        // Vertical Divider
        VerticalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        )

        // Column 2: Category Selector
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { showCategoryDropdown = true }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Category / Tags",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCategory != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                                .clickable { onCategoryChange(null) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = selectedCategory,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "None",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select Category",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )

            DropdownMenu(
                expanded = showCategoryDropdown,
                onDismissRequest = { showCategoryDropdown = false },
                shape = RoundedCornerShape(10.dp),
                containerColor = MaterialTheme.colorScheme.dialogContainerColor,
                shadowElevation = 8.dp,
                modifier = Modifier.heightIn(max = 350.dp)
            ) {
                DropdownMenuItem(
                    text = { Text("None") },
                    onClick = {
                        onCategoryChange(null)
                        showCategoryDropdown = false
                    }
                )
                val defaultCats = listOf("Sticky", "Personal", "Work", "Errands", "Health", "Learning")
                val allCategories = (defaultCats + customCategories).distinct()
                allCategories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = {
                            onCategoryChange(cat)
                            showCategoryDropdown = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(cat),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = if (customCategories.contains(cat)) {
                            {
                                IconButton(
                                    onClick = {
                                        onDeleteCategory(cat)
                                        if (selectedCategory == cat) {
                                            onCategoryChange(null)
                                        }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete custom tag",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else null
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("+ Add custom category", color = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showCategoryDropdown = false
                        onShowAddCategoryDialog()
                    }
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Task Priority & Category Row")
@Composable
fun TaskPriorityCategoryRowPreview() {
    com.gratus.mytodo.ui.theme.SoftTodoTheme {
        TaskPriorityCategoryRow(
            priority = 1,
            selectedCategory = "Work",
            customCategories = listOf("Work", "Fitness", "Errands"),
            onPriorityChange = {},
            onCategoryChange = {},
            onShowAddCategoryDialog = {},
            onDeleteCategory = {}
        )
    }
}
