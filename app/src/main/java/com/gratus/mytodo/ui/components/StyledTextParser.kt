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

package com.gratus.mytodo.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.sp

/**
 * Parses user input to create rich Styled Text strings.
 * - Bold: parses standard markdown **text** and custom <**text**> tags.
 * - Italic: parses standard markdown __text__ and custom <__text__> tags.
 * - Bullet list: prefix lines with "- " to compile clean, indented • dot indicators.
 */
fun parseStyledDescription(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val lines = text.split("\n")
    var prevIsBullet = false
    
    lines.forEachIndexed { index, line ->
        val trimmed = line.trimStart()
        
        // Count consecutive dashes at the start of the trimmed line
        var dashCount = 0
        while (dashCount < trimmed.length && trimmed[dashCount] == '-') {
            dashCount++
        }

        // Must have at least one dash followed by a space
        val isBullet = dashCount > 0 && dashCount < trimmed.length && trimmed[dashCount] == ' '

        if (isBullet) {
            val baseIndentValue = (dashCount - 1) * 12
            val baseIndent = baseIndentValue.sp
            val indentSize = (baseIndentValue + 12).sp
            
            // Choose bullet character based on level
            val bulletChar = when (dashCount) {
                1 -> "•"
                2 -> "•"
                3 -> "◦"
                else -> "▪"
            }

            // Apply ParagraphStyle for the entire line to handle wrapping
            builder.pushStyle(
                ParagraphStyle(
                    // firstLine puts the bullet at the current indentation level
                    // restLine pushes wrapped lines in further to align with text
                    textIndent = TextIndent(firstLine = baseIndent, restLine = indentSize),
                    lineHeight = 20.sp
                )
            )

            // Append the bullet and the rest of the text
            builder.append("$bulletChar  ")
            builder.append(parseInlineStyles(trimmed.substring(dashCount + 1)))

            builder.pop() // Remove ParagraphStyle
        } else {
            if (index > 0) {
                // Only append a newline if both the previous and current lines are normal (unstyled).
                // If transitioning from a bullet to normal, Compose automatically breaks the line 
                // because it's a boundary between a ParagraphStyle and an unstyled paragraph.
                if (!prevIsBullet) {
                    builder.append("\n")
                }
            }
            builder.append(parseInlineStyles(line))
        }

        prevIsBullet = isBullet
    }

    return builder.toAnnotatedString()
}

private fun parseInlineStyles(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    val lineLen = text.length
    while (i < lineLen) {
        // Check customized tag match <**bold**> and <__italic__>
        if (i <= lineLen - 4 && text.substring(i, i + 3) == "<**") {
            val endIdx = text.indexOf("**>", i + 3)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(parseInlineStyles(text.substring(i + 3, endIdx)))
                builder.pop()
                i = endIdx + 3
                continue
            }
        }
        if (i <= lineLen - 4 && text.substring(i, i + 3) == "<__") {
            val endIdx = text.indexOf("__>", i + 3)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(parseInlineStyles(text.substring(i + 3, endIdx)))
                builder.pop()
                i = endIdx + 3
                continue
            }
        }

        // Standard Markdown matching (**bold** and __italic__)
        if (i <= lineLen - 3 && text.substring(i, i + 2) == "**") {
            val endIdx = text.indexOf("**", i + 2)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                builder.append(parseInlineStyles(text.substring(i + 2, endIdx)))
                builder.pop()
                i = endIdx + 2
                continue
            }
        }
        if (i <= lineLen - 3 && text.substring(i, i + 2) == "__") {
            val endIdx = text.indexOf("__", i + 2)
            if (endIdx != -1) {
                builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                builder.append(parseInlineStyles(text.substring(i + 2, endIdx)))
                builder.pop()
                i = endIdx + 2
                continue
            }
        }

        builder.append(text[i])
        i++
    }
    return builder.toAnnotatedString()
}
