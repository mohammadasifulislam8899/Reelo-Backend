@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.presentation

import com.xentoryx.labs.reelo.core.db.schema.PlaylistsTable
import com.xentoryx.labs.reelo.core.db.schema.PlaylistVideosTable
import com.xentoryx.labs.reelo.core.db.schema.VideosTable
import com.xentoryx.labs.reelo.core.db.schema.UsersTable
import com.xentoryx.labs.reelo.feature.auth.presentation.dto.MessageResponse
import com.xentoryx.labs.reelo.feature.video.presentation.dto.VideoResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.serialization.Serializable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.firstOrNull
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid
import java.time.LocalDateTime

@Serializable
data class PlaylistResponse(
    val id: String,
    val name: String,
    val description: String?,
    val isPrivate: Boolean,
    val createdAt: String
)

@Serializable
data class CreatePlaylistRequest(
    val name: String,
    val description: String? = null,
    val isPrivate: Boolean = true
)

@Serializable
data class PlaylistDetailsResponse(
    val playlist: PlaylistResponse,
    val videos: List<VideoResponse>
)

fun Route.playlistRoutes() {
    val database by inject<R2dbcDatabase>()

    route("/playlists") {
        authenticate {
            // Get user's playlists
            get {
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.payload?.getClaim("user_id")?.asString()
                if (userIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse("Authentication required"))
                    return@get
                }

                try {
                    val userUuid = Uuid.parse(userIdStr)
                    val playlists = suspendTransaction(db = database) {
                        PlaylistsTable.selectAll()
                            .where { PlaylistsTable.userId eq userUuid }
                            .orderBy(PlaylistsTable.createdAt to SortOrder.DESC)
                            .map { row ->
                                PlaylistResponse(
                                    id = row[PlaylistsTable.id].toString(),
                                    name = row[PlaylistsTable.name],
                                    description = row[PlaylistsTable.description],
                                    isPrivate = row[PlaylistsTable.isPrivate],
                                    createdAt = row[PlaylistsTable.createdAt].toString()
                                )
                            }.toList()
                    }
                    call.respond(HttpStatusCode.OK, playlists)
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to fetch playlists", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("Error: ${e.message}"))
                }
            }

            // Create new playlist
            post {
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.payload?.getClaim("user_id")?.asString()
                if (userIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse("Authentication required"))
                    return@post
                }

                try {
                    val userUuid = Uuid.parse(userIdStr)
                    val request = call.receive<CreatePlaylistRequest>()
                    
                    if (request.name.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, MessageResponse("Playlist name cannot be empty"))
                        return@post
                    }

                    val playlistUuid = Uuid.random()
                    val now = LocalDateTime.now()

                    val createdPlaylist = suspendTransaction(db = database) {
                        PlaylistsTable.insert {
                            it[id] = playlistUuid
                            it[name] = request.name
                            it[description] = request.description
                            it[userId] = userUuid
                            it[isPrivate] = request.isPrivate
                            it[createdAt] = now
                        }

                        PlaylistResponse(
                            id = playlistUuid.toString(),
                            name = request.name,
                            description = request.description,
                            isPrivate = request.isPrivate,
                            createdAt = now.toString()
                        )
                    }

                    call.respond(HttpStatusCode.Created, createdPlaylist)
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to create playlist", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("Error: ${e.message}"))
                }
            }

            // Add video to playlist
            post("/{playlistId}/videos/{videoId}") {
                val playlistIdStr = call.parameters["playlistId"]
                val videoIdStr = call.parameters["videoId"]
                if (playlistIdStr == null || videoIdStr == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing playlist ID or video ID"))
                    return@post
                }

                try {
                    val playlistUuid = Uuid.parse(playlistIdStr)
                    val videoUuid = Uuid.parse(videoIdStr)
                    val now = LocalDateTime.now()

                    suspendTransaction(db = database) {
                        // Check if already exists in playlist to avoid unique constraint failure
                        val exists = PlaylistVideosTable.selectAll()
                            .where { (PlaylistVideosTable.playlistId eq playlistUuid) and (PlaylistVideosTable.videoId eq videoUuid) }
                            .count() > 0

                        if (!exists) {
                            PlaylistVideosTable.insert {
                                it[id] = Uuid.random()
                                it[playlistId] = playlistUuid
                                it[videoId] = videoUuid
                                it[addedAt] = now
                            }
                        }
                    }
                    call.respond(HttpStatusCode.OK, MessageResponse("Video added to playlist"))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to add video to playlist", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("Error: ${e.message}"))
                }
            }

            // Remove video from playlist
            delete("/{playlistId}/videos/{videoId}") {
                val playlistIdStr = call.parameters["playlistId"]
                val videoIdStr = call.parameters["videoId"]
                if (playlistIdStr == null || videoIdStr == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing playlist ID or video ID"))
                    return@delete
                }

                try {
                    val playlistUuid = Uuid.parse(playlistIdStr)
                    val videoUuid = Uuid.parse(videoIdStr)

                    suspendTransaction(db = database) {
                        PlaylistVideosTable.deleteWhere {
                            (PlaylistVideosTable.playlistId eq playlistUuid) and (PlaylistVideosTable.videoId eq videoUuid)
                        }
                    }
                    call.respond(HttpStatusCode.OK, MessageResponse("Video removed from playlist"))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to remove video from playlist", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("Error: ${e.message}"))
                }
            }

            // Get playlist details (with list of videos)
            get("/{playlistId}") {
                val playlistIdStr = call.parameters["playlistId"]
                if (playlistIdStr == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing playlist ID"))
                    return@get
                }

                try {
                    val playlistUuid = Uuid.parse(playlistIdStr)
                    val host = call.request.headers["Host"] ?: "localhost:8080"
                    val baseUrl = "http://$host"

                    val details = suspendTransaction(db = database) {
                        val playlistRow = PlaylistsTable.selectAll()
                            .where { PlaylistsTable.id eq playlistUuid }
                            .firstOrNull() ?: return@suspendTransaction null

                        val playlist = PlaylistResponse(
                            id = playlistRow[PlaylistsTable.id].toString(),
                            name = playlistRow[PlaylistsTable.name],
                            description = playlistRow[PlaylistsTable.description],
                            isPrivate = playlistRow[PlaylistsTable.isPrivate],
                            createdAt = playlistRow[PlaylistsTable.createdAt].toString()
                        )

                        val videos = (PlaylistVideosTable innerJoin VideosTable innerJoin UsersTable)
                            .selectAll()
                            .where { PlaylistVideosTable.playlistId eq playlistUuid }
                            .orderBy(PlaylistVideosTable.addedAt to SortOrder.ASC)
                            .map { row ->
                                val videoUrl = row[VideosTable.videoUrl]
                                val thumbnailUrl = row[VideosTable.thumbnailUrl]
                                val hostUrl = if (videoUrl.startsWith("http")) "" else baseUrl
                                val thumbHostUrl = if (thumbnailUrl.startsWith("http")) "" else baseUrl

                                VideoResponse(
                                    id = row[VideosTable.id].toString(),
                                    title = row[VideosTable.title],
                                    description = row[VideosTable.description],
                                    videoUrl = "$hostUrl$videoUrl",
                                    thumbnailUrl = "$thumbHostUrl$thumbnailUrl",
                                    viewsCount = row[VideosTable.viewsCount],
                                    duration = row[VideosTable.duration],
                                    uploaderId = row[VideosTable.userId].toString(),
                                    uploaderName = row[UsersTable.name],
                                    uploaderAvatarUrl = row[UsersTable.avatarUrl]?.let { if (it.startsWith("http")) it else "$baseUrl$it" },
                                    createdAt = row[VideosTable.createdAt].toString(),
                                    updatedAt = row[VideosTable.updatedAt].toString()
                                )
                            }.toList()

                        PlaylistDetailsResponse(playlist, videos)
                    }

                    if (details != null) {
                        call.respond(HttpStatusCode.OK, details)
                    } else {
                        call.respond(HttpStatusCode.NotFound, MessageResponse("Playlist not found"))
                    }
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to fetch playlist details", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("Error: ${e.message}"))
                }
            }
        }
    }
}
