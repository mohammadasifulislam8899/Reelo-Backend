@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.core.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import java.util.Date
import kotlin.uuid.Uuid

class JwtProvider(private val application: Application) {
    private val config = application.environment.config
    private val secret = config.propertyOrNull("jwt.secret")?.getString() ?: "secret"
    private val audience = config.propertyOrNull("jwt.audience")?.getString() ?: "jwt-audience"
    private val issuer = config.propertyOrNull("jwt.issuer")?.getString() ?: "http://localhost:8080"
    
    private val accessTokenExpSeconds = config.propertyOrNull("jwt.accessTokenExpiration")?.getString()?.toLong() ?: 900L
    private val refreshTokenExpSeconds = config.propertyOrNull("jwt.refreshTokenExpiration")?.getString()?.toLong() ?: 2592000L

    fun generateAccessToken(userId: Uuid): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("user_id", userId.toString())
            .withClaim("token_type", "access")
            .withExpiresAt(Date(System.currentTimeMillis() + accessTokenExpSeconds * 1000))
            .sign(Algorithm.HMAC256(secret))
    }

    fun generateRefreshToken(userId: Uuid): String {
        return JWT.create()
            .withAudience(audience)
            .withIssuer(issuer)
            .withClaim("user_id", userId.toString())
            .withClaim("token_type", "refresh")
            .withExpiresAt(Date(System.currentTimeMillis() + refreshTokenExpSeconds * 1000))
            .sign(Algorithm.HMAC256(secret))
    }

    fun verifyRefreshToken(token: String): Uuid? {
        return try {
            val verifier = JWT.require(Algorithm.HMAC256(secret))
                .withAudience(audience)
                .withIssuer(issuer)
                .build()
            val jwt = verifier.verify(token)
            val tokenType = jwt.getClaim("token_type").asString()
            val userIdStr = jwt.getClaim("user_id").asString()
            if (tokenType == "refresh" && userIdStr != null) {
                Uuid.parse(userIdStr)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
