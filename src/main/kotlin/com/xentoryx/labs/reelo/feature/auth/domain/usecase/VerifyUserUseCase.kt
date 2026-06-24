package com.xentoryx.labs.reelo.feature.auth.domain.usecase

import com.xentoryx.labs.reelo.feature.auth.domain.repository.AuthRepository

class VerifyUserUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(email: String, token: String): Boolean {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(token.isNotBlank()) { "Token cannot be blank" }
        return authRepository.verifyUser(email, token)
    }
}
