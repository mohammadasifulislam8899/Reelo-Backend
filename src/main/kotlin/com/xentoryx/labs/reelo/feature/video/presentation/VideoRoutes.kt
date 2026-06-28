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
import com.xentoryx.labs.reelo.core.db.schema.VideosTable
import com.xentoryx.labs.reelo.core.db.schema.VideoLikesTable
import com.xentoryx.labs.reelo.core.db.schema.CommentsTable
import com.xentoryx.labs.reelo.core.db.schema.SubscriptionsTable
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.SortOrder
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
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid
import java.io.File
import kotlinx.serialization.Serializable

@Serializable
data class UploadInitResponse(val uploadId: String)

@Serializable
data class VideoSocialState(
    val likesCount: Long,
    val dislikesCount: Long,
    val userLikeStatus: String, // "LIKE", "DISLIKE", "NONE"
    val isSubscribed: Boolean,
    val subscribersCount: Long
)

@Serializable
data class CommentResponse(
    val id: String,
    val content: String,
    val createdAt: String,
    val authorName: String,
    val authorAvatarUrl: String?
)

@Serializable
data class AddCommentRequest(val content: String)


private val viewCooldowns = java.util.concurrent.ConcurrentHashMap<String, Long>()

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
            post("/{id}/view") {
                val idStr = call.parameters["id"]
                if (idStr == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing video ID"))
                    return@post
                }

                try {
                    val videoId = Uuid.parse(idStr)
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("user_id")?.asString()
                    val ip = call.request.local.remoteHost
                    
                    val identityKey = if (userId != null) "user:$userId" else "ip:$ip"
                    val cacheKey = "$identityKey:$idStr"
                    
                    val currentTime = System.currentTimeMillis()
                    val lastViewTime = viewCooldowns[cacheKey] ?: 0L
                    
                    // 1-hour cooldown
                    if (currentTime - lastViewTime >= 3600000L) {
                        viewCooldowns[cacheKey] = currentTime
                        
                        // Increment views count in DB
                        suspendTransaction(db = database) {
                            VideosTable.update({ VideosTable.id eq videoId }) {
                                it[viewsCount] = VideosTable.viewsCount + 1
                            }
                        }
                    }
                    
                    call.respond(HttpStatusCode.OK, MessageResponse("View registered successfully"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid video ID format"))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to register video view", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred: ${e.message}"))
                }
            }

            get("/{id}/social") {
                val idStr = call.parameters["id"]
                if (idStr == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing video ID"))
                    return@get
                }

                try {
                    val videoId = Uuid.parse(idStr)
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("user_id")?.asString()

                    val socialState = suspendTransaction(db = database) {
                        val likes = VideoLikesTable.selectAll()
                            .where { (VideoLikesTable.videoId eq videoId) and (VideoLikesTable.isLike eq true) }
                            .count()
                        
                        val dislikes = VideoLikesTable.selectAll()
                            .where { (VideoLikesTable.videoId eq videoId) and (VideoLikesTable.isLike eq false) }
                            .count()

                        val creatorId = VideosTable.selectAll()
                            .where { VideosTable.id eq videoId }
                            .map { it[VideosTable.userId] }
                            .firstOrNull() ?: throw Exception("Uploader not found")

                        val subscribers = SubscriptionsTable.selectAll()
                            .where { SubscriptionsTable.channelId eq creatorId }
                            .count()

                        var userLikeStatus = "NONE"
                        var isSubscribed = false

                        if (userId != null) {
                            val currentUserId = Uuid.parse(userId)
                            val userLike = VideoLikesTable.selectAll()
                                .where { (VideoLikesTable.videoId eq videoId) and (VideoLikesTable.userId eq currentUserId) }
                                .map { it[VideoLikesTable.isLike] }
                                .firstOrNull()
                            
                            if (userLike != null) {
                                userLikeStatus = if (userLike) "LIKE" else "DISLIKE"
                            }

                            isSubscribed = SubscriptionsTable.selectAll()
                                .where { (SubscriptionsTable.subscriberId eq currentUserId) and (SubscriptionsTable.channelId eq creatorId) }
                                .count() > 0
                        }

                        VideoSocialState(likes, dislikes, userLikeStatus, isSubscribed, subscribers)
                    }

                    call.respond(HttpStatusCode.OK, socialState)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid video ID format"))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to fetch social state", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred: ${e.message}"))
                }
            }

            post("/{id}/like") {
                val idStr = call.parameters["id"]
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.payload?.getClaim("user_id")?.asString()

                if (userIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse("Authentication required to like"))
                    return@post
                }
                if (idStr == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing video ID"))
                    return@post
                }

                try {
                    val videoId = Uuid.parse(idStr)
                    val currentUserId = Uuid.parse(userIdStr)

                    suspendTransaction(db = database) {
                        val existingLike = VideoLikesTable.selectAll()
                            .where { (VideoLikesTable.videoId eq videoId) and (VideoLikesTable.userId eq currentUserId) }
                            .map { Pair(it[VideoLikesTable.id], it[VideoLikesTable.isLike]) }
                            .firstOrNull()

                        if (existingLike == null) {
                            VideoLikesTable.insert {
                                it[id] = Uuid.random()
                                it[VideoLikesTable.videoId] = videoId
                                it[VideoLikesTable.userId] = currentUserId
                                it[isLike] = true
                                it[createdAt] = java.time.LocalDateTime.now()
                            }
                        } else {
                            val (likeId, isLike) = existingLike
                            if (isLike) {
                                // Toggle off
                                VideoLikesTable.deleteWhere { VideoLikesTable.id eq likeId }
                            } else {
                                // Change from dislike to like
                                VideoLikesTable.update({ VideoLikesTable.id eq likeId }) {
                                    it[VideoLikesTable.isLike] = true
                                }
                            }
                        }
                    }

                    call.respond(HttpStatusCode.OK, MessageResponse("Like status updated"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid ID format"))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to update like status", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("Failed: ${e.message}"))
                }
            }

            post("/{id}/dislike") {
                val idStr = call.parameters["id"]
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.payload?.getClaim("user_id")?.asString()

                if (userIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse("Authentication required to dislike"))
                    return@post
                }
                if (idStr == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing video ID"))
                    return@post
                }

                try {
                    val videoId = Uuid.parse(idStr)
                    val currentUserId = Uuid.parse(userIdStr)

                    suspendTransaction(db = database) {
                        val existingLike = VideoLikesTable.selectAll()
                            .where { (VideoLikesTable.videoId eq videoId) and (VideoLikesTable.userId eq currentUserId) }
                            .map { Pair(it[VideoLikesTable.id], it[VideoLikesTable.isLike]) }
                            .firstOrNull()

                        if (existingLike == null) {
                            VideoLikesTable.insert {
                                it[id] = Uuid.random()
                                it[VideoLikesTable.videoId] = videoId
                                it[VideoLikesTable.userId] = currentUserId
                                it[isLike] = false
                                it[createdAt] = java.time.LocalDateTime.now()
                            }
                        } else {
                            val (likeId, isLike) = existingLike
                            if (!isLike) {
                                // Toggle off
                                VideoLikesTable.deleteWhere { VideoLikesTable.id eq likeId }
                            } else {
                                // Change from like to dislike
                                VideoLikesTable.update({ VideoLikesTable.id eq likeId }) {
                                    it[VideoLikesTable.isLike] = false
                                }
                            }
                        }
                    }

                    call.respond(HttpStatusCode.OK, MessageResponse("Dislike status updated"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid ID format"))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to update dislike status", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("Failed: ${e.message}"))
                }
            }

            post("/subscribe/{channelId}") {
                val channelIdStr = call.parameters["channelId"]
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.payload?.getClaim("user_id")?.asString()

                if (userIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse("Authentication required to subscribe"))
                    return@post
                }
                if (channelIdStr == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing channel ID"))
                    return@post
                }

                try {
                    val channelId = Uuid.parse(channelIdStr)
                    val subscriberId = Uuid.parse(userIdStr)

                    if (channelId == subscriberId) {
                        call.respond(HttpStatusCode.BadRequest, MessageResponse("You cannot subscribe to yourself"))
                        return@post
                    }

                    suspendTransaction(db = database) {
                        val existingSub = SubscriptionsTable.selectAll()
                            .where { (SubscriptionsTable.subscriberId eq subscriberId) and (SubscriptionsTable.channelId eq channelId) }
                            .map { it[SubscriptionsTable.id] }
                            .firstOrNull()

                        if (existingSub == null) {
                            SubscriptionsTable.insert {
                                it[id] = Uuid.random()
                                it[SubscriptionsTable.subscriberId] = subscriberId
                                it[SubscriptionsTable.channelId] = channelId
                                it[createdAt] = java.time.LocalDateTime.now()
                            }
                        } else {
                            SubscriptionsTable.deleteWhere { SubscriptionsTable.id eq existingSub }
                        }
                    }

                    call.respond(HttpStatusCode.OK, MessageResponse("Subscription status updated"))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid channel ID format"))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to update subscription", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("Failed: ${e.message}"))
                }
            }

            get("/{id}/comments") {
                val idStr = call.parameters["id"]
                if (idStr == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing video ID"))
                    return@get
                }

                try {
                    val videoId = Uuid.parse(idStr)

                    val comments = suspendTransaction(db = database) {
                        (CommentsTable innerJoin UsersTable)
                            .selectAll()
                            .where { CommentsTable.videoId eq videoId }
                            .orderBy(CommentsTable.createdAt to SortOrder.DESC)
                            .map { row ->
                                CommentResponse(
                                    id = row[CommentsTable.id].toString(),
                                    content = row[CommentsTable.content],
                                    createdAt = row[CommentsTable.createdAt].toString(),
                                    authorName = row[UsersTable.name],
                                    authorAvatarUrl = row[UsersTable.avatarUrl]
                                )
                            }
                    }

                    call.respond(HttpStatusCode.OK, comments)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid video ID format"))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to fetch comments", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred: ${e.message}"))
                }
            }

            post("/{id}/comments") {
                val idStr = call.parameters["id"]
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.payload?.getClaim("user_id")?.asString()

                if (userIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse("Authentication required to post comments"))
                    return@post
                }
                if (idStr == null) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Missing video ID"))
                    return@post
                }

                try {
                    val videoId = Uuid.parse(idStr)
                    val currentUserId = Uuid.parse(userIdStr)
                    val request = call.receive<AddCommentRequest>()

                    if (request.content.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, MessageResponse("Comment content cannot be empty"))
                        return@post
                    }

                    val commentResponse = suspendTransaction(db = database) {
                        val commentId = Uuid.random()
                        val now = java.time.LocalDateTime.now()

                        CommentsTable.insert {
                            it[id] = commentId
                            it[CommentsTable.videoId] = videoId
                            it[CommentsTable.userId] = currentUserId
                            it[content] = request.content
                            it[createdAt] = now
                        }

                        val userRow = UsersTable.selectAll()
                            .where { UsersTable.id eq currentUserId }
                            .firstOrNull() ?: throw Exception("User not found")

                        CommentResponse(
                            id = commentId.toString(),
                            content = request.content,
                            createdAt = now.toString(),
                            authorName = userRow[UsersTable.name],
                            authorAvatarUrl = userRow[UsersTable.avatarUrl]
                        )
                    }

                    call.respond(HttpStatusCode.Created, commentResponse)
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid video ID format"))
                } catch (e: Exception) {
                    call.application.environment.log.error("Failed to post comment", e)
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred: ${e.message}"))
                }
            }

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
