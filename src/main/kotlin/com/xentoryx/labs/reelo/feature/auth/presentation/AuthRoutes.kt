@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.presentation

import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.post
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.jwt.JWTPrincipal
import com.xentoryx.labs.reelo.feature.auth.domain.usecase.RegisterUserUseCase
import com.xentoryx.labs.reelo.feature.auth.domain.usecase.LoginUserUseCase
import com.xentoryx.labs.reelo.feature.auth.domain.usecase.RefreshTokensUseCase
import com.xentoryx.labs.reelo.feature.auth.domain.usecase.VerifyUserUseCase
import com.xentoryx.labs.reelo.feature.auth.domain.usecase.GetUserUseCase
import com.xentoryx.labs.reelo.feature.auth.domain.usecase.UpdateProfileUseCase
import com.xentoryx.labs.reelo.feature.auth.presentation.dto.*
import com.xentoryx.labs.reelo.feature.auth.presentation.mapper.toUserResponse
import org.koin.ktor.ext.inject
import kotlin.uuid.Uuid

fun Route.authRoutes() {
    val registerUserUseCase by inject<RegisterUserUseCase>()
    val loginUserUseCase by inject<LoginUserUseCase>()
    val refreshTokensUseCase by inject<RefreshTokensUseCase>()
    val verifyUserUseCase by inject<VerifyUserUseCase>()
    val getUserUseCase by inject<GetUserUseCase>()
    val updateProfileUseCase by inject<UpdateProfileUseCase>()

    route("/auth") {
        post("/register") {
            val request = call.receive<RegisterRequest>()
            try {
                val user = registerUserUseCase.execute(request.email, request.password, request.name)
                call.respond(HttpStatusCode.Created, user.toUserResponse())
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Conflict, MessageResponse(e.message ?: "Registration failed"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred"))
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()
            try {
                val result = loginUserUseCase.execute(request.email, request.password)
                call.respond(
                    HttpStatusCode.OK,
                    AuthResponse(
                        accessToken = result.accessToken,
                        refreshToken = result.refreshToken,
                        user = result.user.toUserResponse()
                    )
                )
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.Unauthorized, MessageResponse(e.message ?: "Invalid email or password"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred"))
            }
        }

        post("/refresh") {
            val request = call.receive<RefreshRequest>()
            try {
                val result = refreshTokensUseCase.execute(request.refreshToken)
                call.respond(HttpStatusCode.OK, TokenResponse(result.accessToken, result.refreshToken))
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse(e.message ?: "Invalid refresh token"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred"))
            }
        }

        post("/verify") {
            val request = call.receive<VerificationRequest>()
            try {
                val success = verifyUserUseCase.execute(request.email, request.token)
                if (success) {
                    call.respond(HttpStatusCode.OK, MessageResponse("Verification successful"))
                } else {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse("Invalid or expired verification token"))
                }
            } catch (e: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest, MessageResponse(e.message ?: "Verification failed"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred"))
            }
        }

        authenticate {
            get("/me") {
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.payload?.getClaim("user_id")?.asString()
                if (userIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse("Unauthorized"))
                    return@get
                }

                try {
                    val user = getUserUseCase.execute(Uuid.parse(userIdStr))
                    if (user != null) {
                        call.respond(HttpStatusCode.OK, user.toUserResponse())
                    } else {
                        call.respond(HttpStatusCode.NotFound, MessageResponse("User not found"))
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred"))
                }
            }

            put("/profile") {
                val principal = call.principal<JWTPrincipal>()
                val userIdStr = principal?.payload?.getClaim("user_id")?.asString()
                if (userIdStr == null) {
                    call.respond(HttpStatusCode.Unauthorized, MessageResponse("Unauthorized"))
                    return@put
                }

                try {
                    val request = call.receive<UpdateProfileRequest>()
                    val updatedUser = updateProfileUseCase.execute(
                        id = Uuid.parse(userIdStr),
                        name = request.name,
                        bio = request.bio,
                        avatarUrl = request.avatarUrl,
                        bannerUrl = request.bannerUrl
                    )
                    if (updatedUser != null) {
                        call.respond(HttpStatusCode.OK, updatedUser.toUserResponse())
                    } else {
                        call.respond(HttpStatusCode.NotFound, MessageResponse("User not found"))
                    }
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, MessageResponse(e.message ?: "Invalid request"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, MessageResponse("An unexpected error occurred"))
                }
            }
        }
    }
}
