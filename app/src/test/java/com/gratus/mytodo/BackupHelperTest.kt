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

import com.gratus.mytodo.data.Task
import com.gratus.mytodo.data.utils.BackupHelper
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupHelperTest {

    @Test
    fun testBackupExportAndImportSymmetry() {
        val tasks = listOf(
            Task(
                id = 1,
                title = "Task One",
                description = "Detail one",
                priority = 1,
                dateAdded = "2026-06-06",
                isCompleted = false,
                reminderTime = 1717672200000L,
                isRecurring = false,
                createdSeq = 1000L
            ),
            Task(
                id = 2,
                title = "Task Two",
                description = "Detail two",
                priority = 4,
                dateAdded = "2026-06-07",
                isCompleted = true,
                reminderTime = null,
                isRecurring = true,
                createdSeq = 2000L
            )
        )

        // Export to JSON
        val jsonStr = BackupHelper.exportTasksToJson(tasks)
        assertNotNull(jsonStr)
        assertTrue(jsonStr.contains("Task One"))
        assertTrue(jsonStr.contains("Task Two"))

        // Import back from JSON
        val importedTasks = BackupHelper.importTasksFromJson(jsonStr)
        assertEquals(2, importedTasks.size)

        // Verify task 1 properties
        val t1 = importedTasks.first { it.id == 1 }
        assertEquals("Task One", t1.title)
        assertEquals("Detail one", t1.description)
        assertEquals(1, t1.priority)
        assertEquals("2026-06-06", t1.dateAdded)
        assertFalse(t1.isCompleted)
        assertEquals(1717672200000L, t1.reminderTime)
        assertFalse(t1.isRecurring)
        assertEquals(1000L, t1.createdSeq)

        // Verify task 2 properties
        val t2 = importedTasks.first { it.id == 2 }
        assertEquals("Task Two", t2.title)
        assertEquals("Detail two", t2.description)
        assertEquals(4, t2.priority)
        assertEquals("2026-06-07", t2.dateAdded)
        assertTrue(t2.isCompleted)
        assertNull(t2.reminderTime)
        assertTrue(t2.isRecurring)
        assertEquals(2000L, t2.createdSeq)
    }
}
