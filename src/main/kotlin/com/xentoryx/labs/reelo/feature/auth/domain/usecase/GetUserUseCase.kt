@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.domain.usecase

import com.xentoryx.labs.reelo.feature.auth.domain.model.User
import com.xentoryx.labs.reelo.feature.auth.domain.repository.AuthRepository
import kotlin.uuid.Uuid

class GetUserUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(id: Uuid): User? {
        return authRepository.getUserById(id)
    }
}
