@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.domain.usecase

import com.xentoryx.labs.reelo.core.security.PasswordHasher
import com.xentoryx.labs.reelo.feature.auth.domain.model.User
import com.xentoryx.labs.reelo.feature.auth.domain.repository.AuthRepository
import java.security.SecureRandom

class RegisterUserUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(email: String, password: String, name: String): User {
        require(email.isNotBlank()) { "Email cannot be blank" }
        require(password.length >= 6) { "Password must be at least 6 characters long" }
        require(name.isNotBlank()) { "Name cannot be blank" }

        val existingUser = authRepository.getUserByEmail(email)
        if (existingUser != null) {
            throw IllegalArgumentException("User with this email already exists")
        }

        val passwordHash = PasswordHasher.hashPassword(password)
        val user = authRepository.createUser(email, passwordHash, name)

        // Generate a simple 6-digit verification code
        val randomToken = (100000 + SecureRandom().nextInt(900000)).toString()
        authRepository.createVerificationToken(user.id, randomToken)

        return user
    }
}
