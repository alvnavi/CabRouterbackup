package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object PhotoStorageHelper {

    /**
     * Copies a chosen content Uri (from photo picker) to internal app storage
     * so it persists permanently and can be viewed 100% offline.
     */
    fun copyUriToAppStorage(context: Context, sourceUri: Uri): String? {
        return try {
            val photosDir = File(context.filesDir, "location_photos")
            if (!photosDir.exists()) photosDir.mkdirs()
            val fileName = "loc_${System.currentTimeMillis()}.jpg"
            val destFile = File(photosDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Saves a captured Bitmap from the camera to internal app storage.
     */
    fun saveBitmapToAppStorage(context: Context, bitmap: Bitmap): String? {
        return try {
            val photosDir = File(context.filesDir, "location_photos")
            if (!photosDir.exists()) photosDir.mkdirs()
            val fileName = "loc_${System.currentTimeMillis()}.jpg"
            val destFile = File(photosDir, fileName)

            FileOutputStream(destFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
            }
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}
