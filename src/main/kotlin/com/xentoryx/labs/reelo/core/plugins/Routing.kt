package com.xentoryx.labs.reelo.core.plugins

import io.ktor.server.application.Application
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import com.xentoryx.labs.reelo.feature.auth.presentation.authRoutes
import com.xentoryx.labs.reelo.feature.video.presentation.videoRoutes
import io.ktor.server.http.content.staticFiles
import java.io.File

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Welcome to Reelo Backend Server!")
        }
        get("/api/health") {
            call.respond(mapOf("status" to "OK", "service" to "Reelo Backend"))
        }
        authRoutes()
        videoRoutes()
        
        // Serve uploaded files statically
        staticFiles("/uploads", File("uploads"))
    }
}
