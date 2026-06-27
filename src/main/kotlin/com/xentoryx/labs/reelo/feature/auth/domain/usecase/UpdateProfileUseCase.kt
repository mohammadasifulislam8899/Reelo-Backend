@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.domain.usecase

import com.xentoryx.labs.reelo.feature.auth.domain.model.User
import com.xentoryx.labs.reelo.feature.auth.domain.repository.AuthRepository
import kotlin.uuid.Uuid

class UpdateProfileUseCase(private val authRepository: AuthRepository) {
    suspend fun execute(
        id: Uuid,
        name: String,
        bio: String?,
        avatarUrl: String?,
        bannerUrl: String?
    ): User? {
        if (name.isBlank()) {
            throw IllegalArgumentException("Name cannot be empty")
        }
        return authRepository.updateUser(id, name, bio, avatarUrl, bannerUrl)
    }
}
