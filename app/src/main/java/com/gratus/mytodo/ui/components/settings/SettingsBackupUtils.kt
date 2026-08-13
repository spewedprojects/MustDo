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

package com.gratus.mytodo.ui.components.settings

import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Saves a backup file directly to the device's public Documents directory.
 * Appends the current date in yyyy-MM-dd format to the filename.
 */
fun saveBackupToDocuments(
    context: Context,
    baseFileName: String, // e.g., "todo_backup"
    extension: String,     // e.g., ".json"
    mimeType: String,
    dataWriter: (OutputStream) -> Boolean
): Boolean {
    // 1. Prepare the date-stamped filename
    val dateStamp = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val fullFileName = "${baseFileName}_$dateStamp$extension"

    val resolver = context.contentResolver
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fullFileName)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/MustdoBackups")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    
    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
    } else {
        @Suppress("DEPRECATION")
        val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS + "/MustdoBackups")
        if (!docsDir.exists() && !docsDir.mkdirs()) return false
        val file = File(docsDir, fullFileName)
        try {
            file.outputStream().use { return dataWriter(it) }
        } catch (e: Exception) {
            null
        }
    } ?: return false

    var success = false
    try {
        resolver.openOutputStream(uri)?.use { output ->
            success = dataWriter(output)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }
    } catch (e: Exception) {
        success = false
    }
    return success
}

fun getRingtoneTitle(context: Context, uriString: String?): String {
    if (uriString.isNullOrEmpty()) return "Default Alarm Tone"
    return try {
        val uri = Uri.parse(uriString)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.getTitle(context) ?: "Unknown Tone"
    } catch (e: Exception) {
        "Default Alarm Tone"
    }
}
