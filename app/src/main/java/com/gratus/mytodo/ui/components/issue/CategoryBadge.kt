package com.gratus.mytodo.ui.components.issue

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.AppFontSizes

@Composable
fun getCategoryColor(category: String): Color {
    return when (category) {
        "Issue" -> Color(0xFFE57373)
        "Feature" -> Color(0xFF81C784)
        "Idea" -> Color(0xFF64B5F6)
        else -> MaterialTheme.colorScheme.secondary
    }
}

@Composable
fun CategoryBadge(category: String) {
    val color = getCategoryColor(category)
    Surface(
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, color)
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            fontSize = AppFontSizes.micro,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}
