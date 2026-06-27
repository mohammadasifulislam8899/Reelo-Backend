package com.xentoryx.labs.reelo.core.storage

interface StorageService {
    suspend fun uploadFile(fileName: String, fileBytes: ByteArray, contentType: String): String
}
