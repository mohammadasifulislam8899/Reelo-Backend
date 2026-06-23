package com.xentoryx.labs.reelo.core.db

import io.ktor.server.application.Application
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.koin.dsl.module
import org.koin.ktor.ext.inject

val databaseModule = module {
    single<R2dbcDatabase> {
        val config = get<Application>().environment.config
        val r2dbcUrl = config.propertyOrNull("database.r2dbcUrl")?.getString() ?: "r2dbc:h2:file:///./h2db"
        val user = config.propertyOrNull("database.user")?.getString() ?: "root"
        val password = config.propertyOrNull("database.password")?.getString() ?: ""

        get<Application>().environment.log.info("Connecting to Exposed database using R2DBC URL: $r2dbcUrl")
        R2dbcDatabase.connect(
            url = r2dbcUrl,
            user = user,
            password = password
        )
    }
}

fun Application.configureDatabase() {
    // Triggers the database connection immediately on startup
    val database by inject<R2dbcDatabase>()
}
