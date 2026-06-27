package com.xentoryx.labs.reelo.core.storage

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials

class S3StorageService(
    private val bucketName: String,
    private val regionName: String,
    private val accessKeyId: String,
    private val secretAccessKey: String
) : StorageService {

    override suspend fun uploadFile(fileName: String, fileBytes: ByteArray, contentType: String): String {
        val s3 = S3Client {
            region = regionName
            credentialsProvider = StaticCredentialsProvider(
                Credentials(accessKeyId, secretAccessKey)
            )
        }
        
        s3.use { s3Client ->
            s3Client.putObject(
                PutObjectRequest {
                    bucket = bucketName
                    key = "videos/$fileName"
                    body = ByteStream.fromBytes(fileBytes)
                    this.contentType = contentType
                }
            )
        }
        
        return "https://$bucketName.s3.$regionName.amazonaws.com/videos/$fileName"
    }
}
