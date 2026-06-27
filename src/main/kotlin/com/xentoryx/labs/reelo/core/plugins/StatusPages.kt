package com.xentoryx.labs.reelo.core.plugins

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.requestvalidation.RequestValidationException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.utils.io.ClosedByteChannelException
import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val status: Int,
    val message: String
)

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<RequestValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    status = HttpStatusCode.BadRequest.value,
                    message = cause.reasons.joinToString(", ")
                )
            )
        }
        // ExoPlayer first sends a full GET to probe the video, then immediately
        // cancels and switches to Range requests. This is normal — not an error.
        exception<ClosedByteChannelException> { _, _ ->
            // Client disconnected mid-stream (expected for video streaming)
        }
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Internal Server Error occurred", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    status = HttpStatusCode.InternalServerError.value,
                    message = cause.localizedMessage ?: "An unexpected error occurred."
                )
            )
        }
    }
}
