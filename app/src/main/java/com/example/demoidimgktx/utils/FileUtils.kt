package com.example.demoidimgktx.utils

import android.content.Context
import android.content.res.AssetManager
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import com.example.demoidimgktx.model.FramesMeta
import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    const val FOLDER_APP = "CollageAI"

    private fun internalDir(context: Context): File {
        val file = File(context.filesDir, FOLDER_APP)
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }

    private fun imageFolder(context: Context): File {
        val file = File(internalDir(context = context), "Image")
        if (!file.exists()) {
            file.mkdir()
        }
        return file
    }

    fun readAllFramesMeta(context: Context): List<FramesMeta> {
        val assetManager = context.assets
        val result = mutableListOf<FramesMeta>()

        try {
            val folders = assetManager.list("data") ?: return emptyList()

            Log.e("TAG", "readAllFramesMeta: ${folders.size}", )
            for (folder in folders) {
                val path = "data/$folder/frames_info.json"
                val jsonStr = assetManager.open(path).bufferedReader().use { it.readText() }
                val foregroundPath = findForegroundPath(assetManager, "data/$folder")
                val meta = parseFramesMeta(jsonStr).copy(foregroundPath = foregroundPath ?: return emptyList())
                result.add(meta)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return result
    }

    private fun findForegroundPath(assetManager: AssetManager, folderPath: String): String? {
        return try {
            val files = assetManager.list(folderPath) ?: return null

            val preferred = files.find { it.endsWith(".png", ignoreCase = true) && "foreground" in it.lowercase() }
            preferred ?: files.find { it.endsWith(".png", ignoreCase = true) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }?.let { "$folderPath/$it" }
    }



    fun parseFramesMeta(json: String): FramesMeta {
        val gson = Gson()
        return gson.fromJson(json, FramesMeta::class.java)
    }

    fun copyImageFileFromUri(context: Context, uriString: Uri): File? {
        try {
            val fileName = getFileNameFromUri(context, uriString) ?: (System.currentTimeMillis().toString() + "." +
                    MimeTypeMap.getFileExtensionFromUrl(uriString.toString()))
            val file = File(imageFolder(context), System.currentTimeMillis().toString() + "_" + fileName)
            if (file.exists()) file.delete()
            val outputStream = FileOutputStream(file)
            context.contentResolver.openInputStream(uriString)?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output, DEFAULT_BUFFER_SIZE)
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getFileNameFromUri(context: Context, uri: Uri?): String? {
        var cursor: Cursor? = null
        return try {
            if (uri == null) null
            else {
                context.run {
                    contentResolver.query(uri, null, null, null, null)
                }?.run {
                    cursor = this
                    val nameIndex = getColumnIndex(MediaStore.Images.ImageColumns.DISPLAY_NAME)
                    moveToFirst()
                    getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            null
        } finally {
            cursor?.close()
        }
    }

}