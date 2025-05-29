package com.example.demoidimgktx.utils

import android.content.Context
import android.content.res.AssetManager
import android.util.Log
import com.example.demoidimgktx.model.FramesMeta
import com.google.gson.Gson

object FileUtils {
    fun readAllFramesMeta(context: Context): List<FramesMeta> {
        val assetManager = context.assets
        val result = mutableListOf<FramesMeta>()

        try {
            val folders = assetManager.list("data") ?: return emptyList()

            Log.e("TAG", "readAllFramesMeta: ${folders.size}", )
            for (folder in folders) {
                val path = "data/$folder/frames_info.json.json"
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

}