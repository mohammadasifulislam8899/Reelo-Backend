@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.data.repository

import com.xentoryx.labs.reelo.feature.auth.data.local.datasource.AuthLocalDataSource
import com.xentoryx.labs.reelo.feature.auth.domain.model.User
import com.xentoryx.labs.reelo.feature.auth.domain.repository.AuthRepository
import kotlin.uuid.Uuid

class AuthRepositoryImpl(
    private val localDataSource: AuthLocalDataSource
) : AuthRepository {

    override suspend fun getUserByEmail(email: String): User? {
        return localDataSource.getUserByEmail(email)
    }

    override suspend fun getUserById(id: Uuid): User? {
        return localDataSource.getUserById(id)
    }

    override suspend fun createUser(email: String, passwordHash: String, name: String): User {
        return localDataSource.createUser(email, passwordHash, name)
    }

    override suspend fun createVerificationToken(userId: Uuid, token: String): Boolean {
        return localDataSource.createVerificationToken(userId, token)
    }

    override suspend fun verifyUser(email: String, token: String): Boolean {
        return localDataSource.verifyUser(email, token)
    }
}
