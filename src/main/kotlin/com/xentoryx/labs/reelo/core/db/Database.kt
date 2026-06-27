package com.xentoryx.labs.reelo.core.db

import io.ktor.server.application.Application
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.SchemaUtils
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import java.time.LocalDateTime
import kotlin.uuid.Uuid

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

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
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

            // Seed dummy video and user data if videos are empty
            val hasVideos = VideosTable.selectAll().firstOrNull() != null
            if (!hasVideos) {
                val existingUser = UsersTable.selectAll().firstOrNull()
                val uploaderId = if (existingUser != null) {
                    existingUser[UsersTable.id]
                } else {
                    val newUserId = Uuid.random()
                    UsersTable.insert {
                        it[id] = newUserId
                        it[email] = "creator@reelo.com"
                        it[passwordHash] = "dummy_hash"
                        it[name] = "Reelo Creator"
                        it[avatarUrl] = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200&auto=format&fit=crop&q=80"
                        it[bannerUrl] = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800&auto=format&fit=crop&q=80"
                        it[bio] = "Official Reelo Creator channel for test streams."
                        it[isVerified] = true
                        it[createdAt] = LocalDateTime.now()
                        it[updatedAt] = LocalDateTime.now()
                    }
                    newUserId
                }

                val now = LocalDateTime.now()
                val dummyVideos = listOf(
                    Triple(
                        "Big Buck Bunny",
                        "A large and lovable rabbit deals with bullying forest creatures in this classic animated short.",
                        "https://www.w3schools.com/html/mov_bbb.mp4"
                    ),
                    Triple(
                        "Bear Video",
                        "A sweet bear scratching itself against a tree in the wild.",
                        "https://www.w3schools.com/html/movie.mp4"
                    ),
                    Triple(
                        "Car Detection",
                        "A computer vision test video featuring car detection on a highway.",
                        "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/car-detection.mp4"
                    ),
                    Triple(
                        "Bottle Detection",
                        "A computer vision test video featuring bottle detection on a conveyor belt.",
                        "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/bottle-detection.mp4"
                    )
                )

                val thumbnails = listOf(
                    "https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?w=800&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1607604276583-eef5d076aa5f?w=800&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&auto=format&fit=crop&q=60",
                    "https://images.unsplash.com/photo-1543002588-bfa74002ed7e?w=800&auto=format&fit=crop&q=60"
                )

                val durations = listOf(10, 12, 30, 10)

                dummyVideos.forEachIndexed { index, (title, desc, url) ->
                    VideosTable.insert {
                        it[id] = Uuid.random()
                        it[VideosTable.title] = title
                        it[description] = desc
                        it[videoUrl] = url
                        it[thumbnailUrl] = thumbnails[index]
                        it[viewsCount] = (1000..50000).random().toLong()
                        it[duration] = durations[index]
                        it[userId] = uploaderId
                        it[createdAt] = now.minusDays(index.toLong())
                        it[updatedAt] = now.minusDays(index.toLong())
                    }
                }
            }
        }
    }
}
