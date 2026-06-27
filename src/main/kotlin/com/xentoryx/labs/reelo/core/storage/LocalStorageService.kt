package com.xentoryx.labs.reelo.core.storage

import java.io.File

class LocalStorageService(private val baseUrl: String) : StorageService {
    override suspend fun uploadFile(fileName: String, fileBytes: ByteArray, contentType: String): String {
        val dir = File("uploads/videos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, fileName)
        file.writeBytes(fileBytes)
        val formattedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return "${formattedBaseUrl}uploads/videos/$fileName"
    }
}
