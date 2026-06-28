@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.presentation

import com.xentoryx.labs.reelo.feature.video.domain.usecase.GetVideoByIdUseCase
import com.xentoryx.labs.reelo.feature.video.domain.usecase.GetVideosUseCase
import com.xentoryx.labs.reelo.feature.video.domain.usecase.UploadVideoUseCase
import com.xentoryx.labs.reelo.feature.video.domain.usecase.SearchVideosUseCase
import com.xentoryx.labs.reelo.feature.video.domain.usecase.GetVideosByUploaderUseCase
import com.xentoryx.labs.reelo.feature.video.presentation.dto.VideoResponse
import com.xentoryx.labs.reelo.feature.video.presentation.mapper.toVideoResponse
import com.xentoryx.labs.reelo.feature.auth.presentation.dto.MessageResponse
import com.xentoryx.labs.reelo.core.db.schema.UsersTable
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.core.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid
import java.io.File
import kotlinx.serialization.Serializable

@Serializable
data class UploadInitResponse(val uploadId: String)


fun Route.videoRoutes() {
    val getVideosUseCase by inject<GetVideosUseCase>()
    val getVideoByIdUseCase by inject<GetVideoByIdUseCase>()
    val uploadVideoUseCase by inject<UploadVideoUseCase>()
    val searchVideosUseCase by inject<SearchVideosUseCase>()
    val getVideosByUploaderUseCase by inject<GetVideosByUploaderUseCase>()
    val database by inject<R2dbcDatabase>()

    route("/videos") {
        get {
            try {
                val host = call.request.headers["Host"] ?: "localhost:8080"
                val baseUrl = "http://$host"
                val videos = getVideosUseCase.execute()
                val response = videos.map { it.toVideoResponse(baseUrl) }
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.application.environment.log.error("Failed to fetch videos", e)
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred: ${e.message}"))
            }
        }

        get("/search") {
            val query = call.request.queryParameters["q"]
            if (query.isNullOrBlank()) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Query parameter 'q' is required"))
                return@get
            }
            try {
                val host = call.request.headers["Host"] ?: "localhost:8080"
                val baseUrl = "http://$host"
                val videos = searchVideosUseCase.execute(query)
                val response = videos.map { it.toVideoResponse(baseUrl) }
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.application.environment.log.error("Failed to search videos", e)
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred: ${e.message}"))
            }
        }

        get("/user/{userId}") {
            val userIdStr = call.parameters["userId"]
            if (userIdStr == null) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing user ID"))
                return@get
            }
            try {
                val uploaderId = Uuid.parse(userIdStr)
                val host = call.request.headers["Host"] ?: "localhost:8080"
                val baseUrl = "http://$host"
                val videos = getVideosByUploaderUseCase.execute(uploaderId)
                val response = videos.map { it.toVideoResponse(baseUrl) }
                call.respond(HttpStatusCode.OK, response)
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid user ID format"))
            } catch (e: Exception) {
                call.application.environment.log.error("Failed to fetch videos by user", e)
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred: ${e.message}"))
            }
        }

        get("/{id}") {
            val idStr = call.parameters["id"]
            if (idStr == null) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing video ID"))
                return@get
            }

            try {
                val videoId = Uuid.parse(idStr)
                val video = getVideoByIdUseCase.execute(videoId)
                if (video != null) {
                    val host = call.request.headers["Host"] ?: "localhost:8080"
                    val baseUrl = "http://$host"
                    call.respond(HttpStatusCode.OK, video.toVideoResponse(baseUrl))
                } else {
                    call.respond(HttpStatusCode.NotFound, MessageResponse("Video not found"))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid video ID format"))
            } catch (e: Exception) {
                call.application.environment.log.error("Failed to fetch video details", e)
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred: ${e.message}"))
            }
        }

        authenticate(optional = true) {
            post("/upload/init") {
                val uploadId = Uuid.random().toString()
                // Ensure temp folder exists
                val tempDir = File("uploads/temp")
                if (!tempDir.exists()) {
                    tempDir.mkdirs()
                }
                // Pre-create the temp file to ensure it's empty
                val tempFile = File(tempDir, uploadId)
                if (tempFile.exists()) {
                    tempFile.delete()
                }
                tempFile.createNewFile()
                call.respond(HttpStatusCode.Created, UploadInitResponse(uploadId))
            }

            put("/upload/chunk") {
                val uploadId = call.request.queryParameters["uploadId"]
                val chunkIndexStr = call.request.queryParameters["chunkIndex"]
                if (uploadId.isNullOrBlank() || chunkIndexStr.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing uploadId or chunkIndex"))
                    return@put
                }

                val tempFile = File("uploads/temp", uploadId)
                if (!tempFile.exists()) {
                    call.respond(HttpStatusCode.NotFound, MessageResponse("Upload session not found or expired"))
                    return@put
                }

                try {
                    // Stream chunk bytes directly into the file in APPEND mode
                    java.io.FileOutputStream(tempFile, true).use { fos ->
                        call.receiveChannel().toInputStream().use { input ->
                            input.copyTo(fos)
                        }
                    }
                    call.respond(HttpStatusCode.OK, MessageResponse("Chunk $chunkIndexStr uploaded successfully"))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to append chunk $chunkIndexStr for upload $uploadId", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("Failed to write chunk: ${e.message}"))
                }
            }

            post("/upload/complete") {
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.payload?.getClaim("user_id")?.asString()
                
                val uploaderId = if (userIdStr != null) {
                    Uuid.parse(userIdStr)
                } else {
                    suspendTransaction(db = database) {
                        UsersTable.selectAll()
                            .where { UsersTable.email eq "creator@reelo.com" }
                            .map { it[UsersTable.id] }
                            .firstOrNull()
                    } ?: Uuid.random()
                }

                val uploadId = call.request.queryParameters["uploadId"]
                val title = call.request.queryParameters["title"]
                val description = call.request.queryParameters["description"]

                if (uploadId.isNullOrBlank() || title.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing uploadId or title"))
                    return@post
                }

                val tempFile = File("uploads/temp", uploadId)
                if (!tempFile.exists()) {
                    call.respond(HttpStatusCode.NotFound, MessageResponse("Temporary upload file not found"))
                    return@post
                }

                try {
                    // Ensure final videos directory exists
                    val finalDir = File("uploads/videos")
                    if (!finalDir.exists()) {
                        finalDir.mkdirs()
                    }

                    // Move temp file to final destination
                    val finalFileName = "$uploadId.mp4"
                    val finalFile = File(finalDir, finalFileName)
                    if (finalFile.exists()) {
                        finalFile.delete()
                    }
                    
                    if (!tempFile.renameTo(finalFile)) {
                        // Fallback to copy + delete if rename fails (e.g., across mount points)
                        tempFile.copyTo(finalFile, overwrite = true)
                        tempFile.delete()
                    }

                    val host = call.request.headers["Host"] ?: "localhost:8080"
                    val baseUrl = "http://$host"
                    val videoUrl = "$baseUrl/uploads/videos/$finalFileName"

                    // Register in Database
                    val video = uploadVideoUseCase.executeWithUrl(
                        title = title,
                        description = description?.takeIf { it.isNotBlank() },
                        videoUrl = videoUrl,
                        uploaderId = uploaderId
                    )

                    call.respond(HttpStatusCode.Created, video.toVideoResponse(baseUrl))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to finalize upload $uploadId", e)
                    // Cleanup temp file on failure
                    if (tempFile.exists()) {
                        tempFile.delete()
                    }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        MessageResponse("Failed to complete upload: ${e.message}")
                    )
                }
            }

            // Keep monolithic /upload endpoint for backwards compatibility (e.g., profile picture, mock files)
            post("/upload") {
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.payload?.getClaim("user_id")?.asString()
                
                val uploaderId = if (userIdStr != null) {
                    Uuid.parse(userIdStr)
                } else {
                    suspendTransaction(db = database) {
                        UsersTable.selectAll()
                            .where { UsersTable.email eq "creator@reelo.com" }
                            .map { it[UsersTable.id] }
                            .firstOrNull()
                    } ?: Uuid.random()
                }

                try {
                    val multipart = call.receiveMultipart()
                    var title = ""
                    var description: String? = null
                    var fileBytes: ByteArray? = null

                    multipart.forEachPart { part ->
                        when (part) {
                            is PartData.FormItem -> {
                                if (part.name == "title") title = part.value
                                if (part.name == "description") description = part.value
                            }
                            is PartData.FileItem -> {
                                fileBytes = part.streamProvider().readBytes()
                            }
                            else -> {}
                        }
                        part.dispose()
                    }

                    if (title.isEmpty() || fileBytes == null) {
                        call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing video title or file"))
                        return@post
                    }

                    val video = uploadVideoUseCase.execute(
                        title = title,
                        description = description,
                        fileBytes = fileBytes!!,
                        uploaderId = uploaderId
                    )

                    val host = call.request.headers["Host"] ?: "localhost:8080"
                    val baseUrl = "http://$host"
                    call.respond(HttpStatusCode.Created, video.toVideoResponse(baseUrl))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to upload video", e)
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        MessageResponse("An unexpected error occurred during upload: ${e.message}")
                    )
                }
            }
        }
    }
}
