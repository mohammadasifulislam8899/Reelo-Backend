@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.domain.usecase

import com.xentoryx.labs.reelo.core.security.JwtProvider
import com.xentoryx.labs.reelo.feature.auth.domain.repository.AuthRepository

data class TokenResult(
    val accessToken: String,
    val refreshToken: String
)

class RefreshTokensUseCase(
    private val authRepository: AuthRepository,
    private val jwtProvider: JwtProvider
) {
    suspend fun execute(refreshToken: String): TokenResult {
        val userId = jwtProvider.verifyRefreshToken(refreshToken)
            ?: throw IllegalArgumentException("Invalid or expired refresh token")

        val user = authRepository.getUserById(userId)
            ?: throw IllegalArgumentException("User not found")

        val newAccessToken = jwtProvider.generateAccessToken(user.id)
        val newRefreshToken = jwtProvider.generateRefreshToken(user.id)

        return TokenResult(newAccessToken, newRefreshToken)
    }
}
