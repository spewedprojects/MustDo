package com.gratus.mytodo.ui.components.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.PriorityAmber
import com.gratus.mytodo.ui.theme.PriorityOrange
import com.gratus.mytodo.ui.theme.PriorityRed
import com.gratus.mytodo.ui.theme.PriorityYellow

/**
 * Custom Simple B&W border calculation.
 */
@Composable
fun borderStrokeSimple(isCompleted: Boolean): androidx.compose.foundation.BorderStroke {
    val color = if (isCompleted) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    }
    return androidx.compose.foundation.BorderStroke(1.dp, color)
}

/**
 * Returns prioritized color code from Urgent level 1 down to soft Low level 4.
 */
fun getPriorityBoxColor(priority: Int, isCompleted: Boolean): Color {
    if (isCompleted) return Color.LightGray.copy(alpha = 0.5f)
    return when (priority) {
        1 -> PriorityRed
        2 -> PriorityOrange
        3 -> PriorityAmber
        4 -> PriorityYellow
        else -> Color.Gray
    }
}
