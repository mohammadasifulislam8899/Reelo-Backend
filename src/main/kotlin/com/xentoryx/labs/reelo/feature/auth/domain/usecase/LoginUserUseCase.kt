@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.domain.usecase

import com.xentoryx.labs.reelo.core.security.JwtProvider
import com.xentoryx.labs.reelo.core.security.PasswordHasher
import com.xentoryx.labs.reelo.feature.auth.domain.model.User
import com.xentoryx.labs.reelo.feature.auth.domain.repository.AuthRepository

data class LoginResult(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)

class LoginUserUseCase(
    private val authRepository: AuthRepository,
    private val jwtProvider: JwtProvider
) {
    suspend fun execute(email: String, password: String): LoginResult {
        val user = authRepository.getUserByEmail(email)
            ?: throw IllegalArgumentException("Invalid email or password")

        val isPasswordCorrect = PasswordHasher.verifyPassword(password, user.passwordHash)
        if (!isPasswordCorrect) {
            throw IllegalArgumentException("Invalid email or password")
        }

        val accessToken = jwtProvider.generateAccessToken(user.id)
        val refreshToken = jwtProvider.generateRefreshToken(user.id)

        return LoginResult(accessToken, refreshToken, user)
    }
}
