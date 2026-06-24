package com.xentoryx.labs.reelo.core.plugins

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt

fun Application.configureSecurity() {
    val config = environment.config
    val jwtSecret = config.propertyOrNull("jwt.secret")?.getString() ?: "secret"
    val jwtAudience = config.propertyOrNull("jwt.audience")?.getString() ?: "jwt-audience"
    val jwtIssuer = config.propertyOrNull("jwt.issuer")?.getString() ?: "http://localhost:8080"
    val jwtRealm = config.propertyOrNull("jwt.realm")?.getString() ?: "reelo-realm"

    authentication {
        jwt {
            realm = jwtRealm
            verifier(
                JWT
                    .require(Algorithm.HMAC256(jwtSecret))
                    .withAudience(jwtAudience)
                    .withIssuer(jwtIssuer)
                    .build()
            )
            validate { credential ->
                val userId = credential.payload.getClaim("user_id").asString()
                val tokenType = credential.payload.getClaim("token_type").asString()
                if (userId != null && tokenType == "access" && credential.payload.audience.contains(jwtAudience)) {
                    JWTPrincipal(credential.payload)
                } else {
                    null
                }
            }
        }
    }
}
