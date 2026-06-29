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

package com.gratus.mytodo

import com.gratus.mytodo.ui.utils.DateTimeUtils
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class DateTimeUtilsTest {

    @Test
    fun testDbDateFormattingAndParsing() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 6)
        }
        val dateStr = DateTimeUtils.formatDbDate(calendar)
        assertEquals("2026-06-06", dateStr)

        val parsedDate = DateTimeUtils.parseDbDate("2026-06-06")
        assertNotNull(parsedDate)
        
        val checkCal = Calendar.getInstance().apply { time = parsedDate!! }
        assertEquals(2026, checkCal.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, checkCal.get(Calendar.MONTH))
        assertEquals(6, checkCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testMainHeaderFormatting() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 6)
        }
        val headerStr = DateTimeUtils.formatMainHeader(calendar)
        assertTrue(headerStr.contains("Jun"))
        assertTrue(headerStr.contains("06"))
        assertTrue(headerStr.contains("2026"))
    }

    @Test
    fun testHomeDateLabelFormatting() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 6)
        }
        val labelStr = DateTimeUtils.formatHomeDateLabel(calendar)
        assertTrue(labelStr.contains("Saturday"))
        assertTrue(labelStr.contains("Jun"))
        assertTrue(labelStr.contains("06"))
        assertTrue(labelStr.contains("2026"))
    }

    @Test
    fun testStatsLabelFormatting() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 6)
        }
        val statsStr = DateTimeUtils.formatStatsLabel(calendar)
        assertEquals("06-06", statsStr)
    }

    @Test
    fun testAlarmTimeAndDateFormatting() {
        // Test a specific timestamp: June 6, 2026 10:30 PM (22:30)
        val calendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JUNE)
            set(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val timeStr = DateTimeUtils.formatAlarmTime(calendar.timeInMillis)
        assertEquals("10:30 PM", timeStr.uppercase())

        val dateStr = DateTimeUtils.formatAlarmDate(calendar.timeInMillis)
        assertTrue(dateStr.contains("Jun 06"))
        assertTrue(dateStr.uppercase().contains("10:30 PM"))
    }
}
