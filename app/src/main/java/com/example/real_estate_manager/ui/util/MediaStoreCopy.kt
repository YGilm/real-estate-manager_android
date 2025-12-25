package com.example.real_estate_manager.ui.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

fun copyUriToAppStorage(context: Context, uri: Uri, folder: String = "media"): String? {
    val input = context.contentResolver.openInputStream(uri) ?: return null
    val ext = guessExtension(context, uri)
    val dir = File(context.filesDir, folder)
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val fileName = "img_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.$ext"
    val outFile = File(dir, fileName)
    input.use { src ->
        outFile.outputStream().use { dst ->
            src.copyTo(dst)
        }
    }
    return Uri.fromFile(outFile).toString()
}

private fun guessExtension(context: Context, uri: Uri): String {
    val type = context.contentResolver.getType(uri) ?: ""
    return when (type.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> {
            val last = uri.lastPathSegment ?: return "jpg"
            val dot = last.lastIndexOf('.')
            if (dot != -1 && dot < last.length - 1) last.substring(dot + 1) else "jpg"
        }
    }
}
