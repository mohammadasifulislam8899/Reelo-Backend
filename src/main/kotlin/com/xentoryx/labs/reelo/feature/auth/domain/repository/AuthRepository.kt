@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.domain.repository

import com.xentoryx.labs.reelo.feature.auth.domain.model.User
import kotlin.uuid.Uuid

interface AuthRepository {
    suspend fun getUserByEmail(email: String): User?
    suspend fun getUserById(id: Uuid): User?
    suspend fun createUser(email: String, passwordHash: String, name: String): User
    suspend fun createVerificationToken(userId: Uuid, token: String): Boolean
    suspend fun verifyUser(email: String, token: String): Boolean
}
