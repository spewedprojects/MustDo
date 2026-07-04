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

package com.gratus.mytodo.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gratus.mytodo.data.IssueItem
import com.gratus.mytodo.data.IssueComment
import com.gratus.mytodo.data.IssueTrackerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class IssueFilter { ALL, OPEN, CLOSED }

class IssueTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = IssueTrackerRepository(application)

    private val _issues = MutableStateFlow<List<IssueItem>>(emptyList())
    val issues: StateFlow<List<IssueItem>> = _issues.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _filter = MutableStateFlow(IssueFilter.ALL)
    val filter: StateFlow<IssueFilter> = _filter.asStateFlow()

    init {
        loadIssues()
    }

    private fun loadIssues() {
        viewModelScope.launch {
            val loadedIssues = repository.getIssues()

            // Check if any existing issues have the default serialNumber 0
            if (loadedIssues.isNotEmpty() && loadedIssues.any { it.serialNumber == 0 }) {
                val migratedIssues = migrateExistingIssues(loadedIssues)
                _issues.value = migratedIssues
                repository.saveIssues(migratedIssues)
            } else {
                _issues.value = loadedIssues
            }
        }
    }

    private fun migrateExistingIssues(oldList: List<IssueItem>): List<IssueItem> {
        // 1. Sort by timestamp so the oldest issue gets #1
        // 2. Map through and assign indices + 1
        return oldList.sortedBy { it.timestamp }.mapIndexed { index, item ->
            if (item.serialNumber == 0) {
                item.copy(serialNumber = index + 1)
            } else {
                item
            }
        }
    }
    fun addIssue(title: String, description: String, category: String) {
        val currentMaxNumber = _issues.value.maxOfOrNull { it.serialNumber } ?: 0
        val appVersionStr = try {
            getApplication<Application>().getString(com.gratus.mytodo.R.string.app_version)
        } catch (e: Exception) {
            "Unknown"
        }
        val newItem = IssueItem(
            title = title,
            serialNumber = currentMaxNumber + 1,
            description = description,
            category = category,
            appVersion = appVersionStr
        )
        val updatedList = listOf(newItem) + _issues.value
        _issues.value = updatedList
        saveToDisk(updatedList)
    }

    fun updateIssue(item: IssueItem) {
        val updatedList = _issues.value.map { if (it.id == item.id) item else it }
        _issues.value = updatedList
        saveToDisk(updatedList)
    }

    fun deleteIssue(item: IssueItem) {
        val updatedList = _issues.value.filter { it.id != item.id }
        _issues.value = updatedList
        saveToDisk(updatedList)
    }

    fun toggleStatus(item: IssueItem) {
        val newClosedTimestamp = if (!item.isClosed) System.currentTimeMillis() else null
        updateIssue(item.copy(isClosed = !item.isClosed, closedTimestamp = newClosedTimestamp))
    }

    fun addComment(item: IssueItem, comment: String) {
        val newComments = item.comments + IssueComment(comment)
        updateIssue(item.copy(comments = newComments))
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: IssueFilter) {
        _filter.value = filter
    }

    private fun saveToDisk(list: List<IssueItem>) {
        viewModelScope.launch {
            repository.saveIssues(list)
        }
    }

    fun exportAndShare(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get pretty json directly from disk to share
                val jsonList = repository.getIssues()
                repository.saveIssues(jsonList) // Ensure it's up to date
                val file = java.io.File(context.filesDir, "issues.json")
                if (!file.exists()) return@launch

                val jsonContent = file.readText()
                val fullFileName = "issues_export_${System.currentTimeMillis()}.json"
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MustdoBackups")
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(jsonContent.toByteArray())
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(uri, contentValues, null, null)
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Exported to Documents/MustdoBackups", Toast.LENGTH_SHORT).show()
                        
                        // Share Intent
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/json"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Issues Backup"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to export: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
