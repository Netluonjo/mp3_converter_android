package com.sondeptrai.mp3converter.data.server

import android.content.Context
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WifiHttpServer(
    port: Int = 8080,
    private val context: Context,
    private val fileManager: AudioFileManager,
    private val onLogEvent: (String) -> Unit,
    private val onFilesChanged: () -> Unit
) : NanoHTTPD(port) {

    private var cachedHtml: String? = null

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        return try {
            when {
                method == Method.GET && uri == "/" -> {
                    serveHtmlPage()
                }
                method == Method.GET && uri == "/api/files" -> {
                    serveFileListJson()
                }
                method == Method.POST && uri == "/api/upload" -> {
                    handleFileUpload(session)
                }
                method == Method.GET && (uri == "/api/download" || uri == "/api/stream") -> {
                    handleFileDownload(session, isStream = uri == "/api/stream")
                }
                method == Method.POST && uri == "/api/delete" -> {
                    handleFileDelete(session)
                }
                else -> {
                    newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "404 Not Found")
                }
            }
        } catch (e: Exception) {
            newFixedLengthResponse(
                Response.Status.INTERNAL_ERROR,
                MIME_PLAINTEXT,
                "Server error: ${e.localizedMessage}"
            )
        }
    }

    private fun serveHtmlPage(): Response {
        val html = cachedHtml ?: run {
            try {
                context.assets.open("web_transfer.html").bufferedReader(Charsets.UTF_8).use { it.readText() }.also {
                    cachedHtml = it
                }
            } catch (_: Exception) {
                "<!DOCTYPE html><html><body><h2>MP3 Converter WiFi Transfer</h2></body></html>"
            }
        }
        return newFixedLengthResponse(Response.Status.OK, "text/html; charset=utf-8", html)
    }

    private fun serveFileListJson(): Response {
        val dir = fileManager.exportsDir
        val files = dir.listFiles()?.filter { it.isFile && it.length() > 0 } ?: emptyList()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val array = JSONArray()
        for (file in files.sortedByDescending { it.lastModified() }) {
            val obj = JSONObject()
            obj.put("name", file.name)
            obj.put("size", file.length())
            obj.put("formattedSize", formatBytes(file.length()))
            obj.put("date", dateFormat.format(Date(file.lastModified())))
            obj.put("extension", file.extension.uppercase())
            array.put(obj)
        }

        return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", array.toString())
    }

    private fun handleFileUpload(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        session.parseBody(files)

        var uploadedCount = 0
        val targetDir = fileManager.exportsDir
        if (!targetDir.exists()) targetDir.mkdirs()

        for ((key, tempPath) in files) {
            val tempFile = File(tempPath)
            if (!tempFile.exists()) continue

            // Original filename is stored in parms with the same key
            var rawName = session.parms[key] ?: "upload_${System.currentTimeMillis()}.mp3"
            rawName = File(rawName).name // Sanitize path
            if (rawName.isBlank() || rawName == "null") {
                rawName = "Track_${System.currentTimeMillis()}.mp3"
            }

            val destFile = File(targetDir, rawName)
            tempFile.copyTo(destFile, overwrite = true)
            tempFile.delete()
            uploadedCount++

            onLogEvent("Đã nhận file từ PC: ${destFile.name} (${formatBytes(destFile.length())})")
        }

        if (uploadedCount > 0) {
            onFilesChanged()
        }

        val jsonResponse = JSONObject()
        jsonResponse.put("success", true)
        jsonResponse.put("uploadedCount", uploadedCount)
        return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", jsonResponse.toString())
    }

    private fun handleFileDownload(session: IHTTPSession, isStream: Boolean): Response {
        val params = session.parameters
        val rawName = params["file"]?.firstOrNull()
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing 'file' parameter")

        val decodedName = URLDecoder.decode(rawName, "UTF-8")
        val cleanName = File(decodedName).name
        val targetFile = File(fileManager.exportsDir, cleanName)

        if (!targetFile.exists() || !targetFile.isFile) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "File not found")
        }

        val mimeType = getMimeType(targetFile.extension)
        val fis = FileInputStream(targetFile)
        val response = newChunkedResponse(Response.Status.OK, mimeType, fis)

        if (isStream) {
            response.addHeader("Content-Disposition", "inline; filename=\"${targetFile.name}\"")
        } else {
            response.addHeader("Content-Disposition", "attachment; filename=\"${targetFile.name}\"")
            onLogEvent("PC đã tải về: ${targetFile.name} (${formatBytes(targetFile.length())})")
        }
        response.addHeader("Content-Length", targetFile.length().toString())
        return response
    }

    private fun handleFileDelete(session: IHTTPSession): Response {
        val files = HashMap<String, String>()
        session.parseBody(files)

        val rawName = session.parms["file"]
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing 'file' parameter")

        val cleanName = File(rawName).name
        val targetFile = File(fileManager.exportsDir, cleanName)

        val deleted = if (targetFile.exists()) targetFile.delete() else false
        if (deleted) {
            onLogEvent("PC đã xóa file: $cleanName")
            onFilesChanged()
        }

        val res = JSONObject()
        res.put("success", deleted)
        return newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", res.toString())
    }

    private fun getMimeType(ext: String): String {
        return when (ext.lowercase()) {
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            else -> "application/octet-stream"
        }
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 3)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }
}
