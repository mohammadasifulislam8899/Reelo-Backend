package com.xentoryx.labs.reelo.core.di

import io.ktor.server.application.Application
import io.ktor.server.application.install
import com.xentoryx.labs.reelo.core.db.databaseModule
import com.xentoryx.labs.reelo.feature.auth.di.authModule
import com.xentoryx.labs.reelo.feature.video.di.videoModule
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

import com.xentoryx.labs.reelo.core.storage.StorageService
import com.xentoryx.labs.reelo.core.storage.LocalStorageService
import com.xentoryx.labs.reelo.core.storage.S3StorageService

fun Application.configureKoin() {
    install(Koin) {
        slf4jLogger()
        modules(
            module {
                single<Application> { this@configureKoin }
            },
            databaseModule,
            authModule,
            videoModule,
            module {
                single<StorageService> {
                    val app = get<Application>()
                    val config = app.environment.config
                    val storageType = config.propertyOrNull("storage.type")?.getString() ?: "local"
                    if (storageType == "s3") {
                        val bucket = config.property("storage.s3.bucket").getString()
                        val region = config.property("storage.s3.region").getString()
                        val accessKey = config.property("storage.s3.accessKeyId").getString()
                        val secretKey = config.property("storage.s3.secretAccessKey").getString()
                        S3StorageService(bucket, region, accessKey, secretKey)
                    } else {
                        val baseUrl = config.propertyOrNull("storage.baseUrl")?.getString() ?: "http://localhost:8080"
                        LocalStorageService(baseUrl)
                    }
                }
            }
        )
    }
}
