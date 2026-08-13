package com.gratus.mytodo.ui.components.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.AppFontSizes

/**
 * Helper to match category string with its designated icon.
 */
@Composable
fun getCategoryIcon(category: String): ImageVector {
    val lower = category.lowercase().trim()
    return when {
        lower.contains("work") || lower.contains("job") || lower.contains("office") || lower.contains("meet") || lower.contains("project") -> Icons.Default.Work
        lower.contains("personal") || lower.contains("home") || lower.contains("self") || lower.contains("me") || lower.contains("private") -> Icons.Default.Person
        lower.contains("errand") || lower.contains("shop") || lower.contains("buy") || lower.contains("grocer") || lower.contains("store") || lower.contains("market") -> Icons.Default.ShoppingCart
        lower.contains("gym") || lower.contains("workout") || lower.contains("exercise") || lower.contains("run") || lower.contains("fit") || lower.contains("sport") || lower.contains("fitness") || lower.contains("dumbbell") -> Icons.Default.FitnessCenter
        lower.contains("health") || lower.contains("doctor") || lower.contains("hospital") || lower.contains("med") || lower.contains("medicine") || lower.contains("favorite") -> Icons.Default.Favorite
        lower.contains("learn") || lower.contains("study") || lower.contains("book") || lower.contains("school") || lower.contains("class") || lower.contains("course") || lower.contains("read") -> Icons.Default.School
        else -> Icons.Default.Sell
    }
}

/**
 * Visual chip for displaying and selecting categories.
 */
@Composable
fun CategoryChip(
    category: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onSelect() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = getCategoryIcon(category),
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = category,
            fontSize = AppFontSizes.extraSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor
        )
        if (onDelete != null) {
            Spacer(modifier = Modifier.width(2.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete custom tag",
                tint = contentColor.copy(alpha = 0.6f),
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onDelete() }
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Category Chip - Selected")
@Composable
fun CategoryChipPreview() {
    com.gratus.mytodo.ui.theme.SoftTodoTheme {
        CategoryChip(
            category = "Work",
            isSelected = true,
            onSelect = {}
        )
    }
}
