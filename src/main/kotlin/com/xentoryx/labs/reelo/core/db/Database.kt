package com.xentoryx.labs.reelo.core.db

import io.ktor.server.application.Application
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.dsl.module
import org.koin.ktor.ext.inject

// Import all centralized database tables
import com.xentoryx.labs.reelo.core.db.schema.*

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

    // Run schema creation on startup to verify/create missing tables and columns
    runBlocking {
        suspendTransaction(db = database) {
            SchemaUtils.createMissingTablesAndColumns(
                UsersTable,
                VerificationTokensTable,
                VideosTable,
                CommentsTable,
                SubscriptionsTable,
                VideoLikesTable,
                PlaylistsTable,
                PlaylistVideosTable
            )
        }
    }
}
