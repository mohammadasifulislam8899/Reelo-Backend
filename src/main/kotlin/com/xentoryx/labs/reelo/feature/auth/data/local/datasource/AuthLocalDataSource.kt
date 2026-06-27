@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.data.local.datasource

import com.xentoryx.labs.reelo.feature.auth.domain.model.User
import kotlin.uuid.Uuid

interface AuthLocalDataSource {
    suspend fun getUserByEmail(email: String): User?
    suspend fun getUserById(id: Uuid): User?
    suspend fun createUser(email: String, passwordHash: String, name: String): User
    suspend fun createVerificationToken(userId: Uuid, token: String): Boolean
    suspend fun verifyUser(email: String, token: String): Boolean
    suspend fun updateUser(id: Uuid, name: String, bio: String?, avatarUrl: String?, bannerUrl: String?): User?
}
