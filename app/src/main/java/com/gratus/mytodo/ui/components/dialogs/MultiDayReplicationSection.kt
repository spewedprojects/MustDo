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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gratus.mytodo.ui.theme.AppFontSizes
import com.gratus.mytodo.ui.theme.SoftTodoTheme
import com.gratus.mytodo.ui.utils.DateTimeUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Formats a list of date strings ("yyyy-MM-dd") into a human-readable date range string according to month and year rules.
 */
fun formatDateRangeChipText(dateStrings: List<String>): String {
    if (dateStrings.isEmpty()) return "Set a date range"

    val sorted = dateStrings.mapNotNull { DateTimeUtils.parseDbDate(it) }.sorted()
    if (sorted.isEmpty()) return "Set a date range"

    val startDate = sorted.first()
    val endDate = sorted.last()

    val startCal = Calendar.getInstance().apply { time = startDate }
    val endCal = Calendar.getInstance().apply { time = endDate }

    val startDay = startCal.get(Calendar.DAY_OF_MONTH)
    val endDay = endCal.get(Calendar.DAY_OF_MONTH)
    val startMonth = startCal.get(Calendar.MONTH)
    val endMonth = endCal.get(Calendar.MONTH)
    val startYear = startCal.get(Calendar.YEAR)
    val endYear = endCal.get(Calendar.YEAR)

    val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    val startMonthName = monthFormat.format(startDate)
    val endMonthName = monthFormat.format(endDate)

    if (sorted.size == 1 || (startDay == endDay && startMonth == endMonth && startYear == endYear)) {
        return "$startDay $startMonthName $startYear"
    }

    return if (startMonth == endMonth && startYear == endYear) {
        // Same month & same year: 1 instance of month name and year text
        "$startDay – $endDay $startMonthName $startYear"
    } else if (startYear == endYear) {
        // Same year, different month: 1 instance of year text
        "$startDay $startMonthName – $endDay $endMonthName $startYear"
    } else {
        // Different year
        "$startDay $startMonthName $startYear – $endDay $endMonthName $endYear"
    }
}

/**
 * Multi-day task replication controls section featuring preset chips and custom date range picker chip.
 */
@Composable
fun MultiDayReplicationSection(
    isStickyCategory: Boolean,
    everydayCount: Int,
    replicationDates: List<String>,
    onEverydayCountChange: (Int) -> Unit,
    onReplicationDatesChange: (List<String>) -> Unit,
    onShowRangePickerDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

        Text(
            text = "Multi-day replication",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )

        // 1st Row: 3 Chips - Everyday/Today, +7 days, +30 days
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val presetChips = if (isStickyCategory) {
                listOf(
                    Triple(0, "Everyday", "Continuous everyday sticky task"),
                    Triple(7, "+7 days", "Sticky for next 7 days"),
                    Triple(30, "+30 days", "Sticky for next 30 days")
                )
            } else {
                listOf(
                    Triple(0, "Today", "Single day task without auto-replication"),
                    Triple(7, "+7 days", "Replicate for next 7 days"),
                    Triple(30, "+30 days", "Replicate for next 30 days")
                )
            }

            presetChips.forEach { (days, label, _) ->
                val selected = (everydayCount == days) && replicationDates.isEmpty()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.primaryContainer 
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .border(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary 
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onEverydayCountChange(days)
                            onReplicationDatesChange(emptyList())
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        textAlign = TextAlign.Center,
                        fontSize = AppFontSizes.extraSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer 
                                else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }

        // 2nd Row: Single differently colored chip for custom date range
        val isRangeActive = replicationDates.isNotEmpty()
        val chipText = formatDateRangeChipText(replicationDates)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isRangeActive) MaterialTheme.colorScheme.secondary 
                    else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                )
                .border(
                    1.dp,
                    if (isRangeActive) MaterialTheme.colorScheme.secondary 
                    else MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                    RoundedCornerShape(10.dp)
                )
                .clickable { onShowRangePickerDialog() }
                .padding(vertical = 8.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = if (isRangeActive) MaterialTheme.colorScheme.onSecondary 
                           else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = chipText,
                    fontSize = AppFontSizes.small,
                    fontWeight = FontWeight.Bold,
                    color = if (isRangeActive) MaterialTheme.colorScheme.onSecondary 
                            else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Multi-Day Replication Section", backgroundColor = 0xfff)
@Composable
fun MultiDayReplicationSectionPreview() {
    SoftTodoTheme {
        MultiDayReplicationSection(
            isStickyCategory = false,
            everydayCount = 0,
            replicationDates = listOf("2026-08-13", "2026-08-14"),
            onEverydayCountChange = {},
            onReplicationDatesChange = {},
            onShowRangePickerDialog = {}
        )
    }
}
