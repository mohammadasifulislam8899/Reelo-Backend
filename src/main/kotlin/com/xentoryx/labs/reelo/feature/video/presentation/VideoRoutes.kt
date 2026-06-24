@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.presentation

import com.xentoryx.labs.reelo.feature.video.domain.usecase.GetVideoByIdUseCase
import com.xentoryx.labs.reelo.feature.video.domain.usecase.GetVideosUseCase
import com.xentoryx.labs.reelo.feature.video.presentation.dto.VideoResponse
import com.xentoryx.labs.reelo.feature.video.presentation.mapper.toVideoResponse
import com.xentoryx.labs.reelo.feature.auth.presentation.dto.MessageResponse
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.get
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Route.videoRoutes() {
    val getVideosUseCase by inject<GetVideosUseCase>()
    val getVideoByIdUseCase by inject<GetVideoByIdUseCase>()

    route("/videos") {
        get {
            try {
                val videos = getVideosUseCase.execute()
                val response = videos.map { it.toVideoResponse() }
                call.respond(HttpStatusCode.OK, response)
            } catch (e: Exception) {
                call.application.environment.log.error("Failed to fetch videos", e)
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
                    call.respond(HttpStatusCode.OK, video.toVideoResponse())
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
    }
}
