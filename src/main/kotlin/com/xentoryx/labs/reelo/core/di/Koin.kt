package com.xentoryx.labs.reelo.core.di

import io.ktor.server.application.Application
import io.ktor.server.application.install
import com.xentoryx.labs.reelo.core.db.databaseModule
import com.xentoryx.labs.reelo.feature.auth.di.authModule
import com.xentoryx.labs.reelo.feature.video.di.videoModule
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

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
                // We will inject our services and repositories here
            }
        )
    }
}
