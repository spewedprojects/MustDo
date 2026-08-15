package com.gratus.mytodo.ui.components.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.data.Task
import com.gratus.mytodo.ui.components.dialogs.getCategoryIcon
import com.gratus.mytodo.ui.theme.MinimalDarkCardBorder
import com.gratus.mytodo.ui.theme.MinimalLightCardBorder
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.theme.getCategoryAccentColor

/**
 * Category group header card component.
 */
@Composable
fun CategoryCard(
    category: String,
    tasks: List<Task>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onQuickAdd: () -> Unit,
    colorSchemeType: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.red < 0.2f
    val accentColor = getCategoryAccentColor(category, colorSchemeType, isDark)
    val icon = getCategoryIcon(category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .testTag("category_card_$category"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = when (colorSchemeType) {
            "simple" -> BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
            "minimal" -> BorderStroke(1.dp, if (isDark) MinimalDarkCardBorder else MinimalLightCardBorder
            )
            "system" -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
            else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tasks.size.toString(),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = accentColor
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                IconButton(
                    onClick = { onQuickAdd() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Quick Add in Category",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (isExpanded) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                    thickness = 1.dp
                )
                content()
            }
        }
    }
}

@Preview(showBackground = false, name = "Category Card")
@Composable
fun CategoryCardPreview() {
    SoftTodoTheme (colorSchemeType = "simple"){
        CategoryCard(
            category = "Work",
            tasks = listOf(
                Task(id = 1, title = "Task 1", description = "", priority = 1, dateAdded = "2026-08-13")
            ),
            isExpanded = true,
            onToggleExpand = {},
            onQuickAdd = {},
            colorSchemeType = "simple"
        ) {
            Text("Category Task Content", modifier = Modifier.padding(16.dp))
        }
    }
}
