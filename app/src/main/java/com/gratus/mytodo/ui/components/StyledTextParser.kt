package com.gratus.mytodo.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration

/**
 * Parses user input to create rich Styled Text strings.
 * - Bold: parses standard markdown **text** and custom <**text**> tags.
 * - Italic: parses standard markdown __text__ and custom <__text__> tags.
 * - Bullet list: prefix lines with "- " to compile clean, indented â€¢ dot indicators.
 */
fun parseStyledDescription(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val lines = text.split("\n")
    
    lines.forEachIndexed { index, line ->
        val trimmed = line.trimStart()
        val isBullet = trimmed.startsWith("- ")
        
        // Convert "- " bullet points into visually structured elements
        val processedLine = if (isBullet) {
            "  â€¢ " + trimmed.substring(2)
        } else {
            line
        }

        var i = 0
        val lineLen = processedLine.length
        while (i < lineLen) {
            // Check customized tag match <**bold**> and <__italic__>
            if (i <= lineLen - 4 && processedLine.substring(i, i + 3) == "<**") {
                val endIdx = processedLine.indexOf("**>", i + 3)
                if (endIdx != -1) {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    builder.append(processedLine.substring(i + 3, endIdx))
                    builder.pop()
                    i = endIdx + 3
                    continue
                }
            }
            if (i <= lineLen - 4 && processedLine.substring(i, i + 3) == "<__") {
                val endIdx = processedLine.indexOf("__>", i + 3)
                if (endIdx != -1) {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    builder.append(processedLine.substring(i + 3, endIdx))
                    builder.pop()
                    i = endIdx + 3
                    continue
                }
            }

            // Standard Markdown matching (**bold** and __italic__)
            if (i <= lineLen - 3 && processedLine.substring(i, i + 2) == "**") {
                val endIdx = processedLine.indexOf("**", i + 2)
                if (endIdx != -1) {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    builder.append(processedLine.substring(i + 2, endIdx))
                    builder.pop()
                    i = endIdx + 2
                    continue
                }
            }
            if (i <= lineLen - 3 && processedLine.substring(i, i + 2) == "__") {
                val endIdx = processedLine.indexOf("__", i + 2)
                if (endIdx != -1) {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    builder.append(processedLine.substring(i + 2, endIdx))
                    builder.pop()
                    i = endIdx + 2
                    continue
                }
            }

            builder.append(processedLine[i])
            i++
        }
        
        // Maintain line breaks correctly
        if (index < lines.size - 1) {
            builder.append("\n")
        }
    }
    
    return builder.toAnnotatedString()
}
