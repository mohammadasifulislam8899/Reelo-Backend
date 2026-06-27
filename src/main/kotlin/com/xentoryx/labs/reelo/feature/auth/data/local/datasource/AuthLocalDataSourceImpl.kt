@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.data.local.datasource

import com.xentoryx.labs.reelo.core.db.schema.UsersTable
import com.xentoryx.labs.reelo.core.db.schema.VerificationTokensTable
import com.xentoryx.labs.reelo.feature.auth.domain.model.User
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.selectAll
import java.time.LocalDateTime
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull

class AuthLocalDataSourceImpl(private val database: R2dbcDatabase) : AuthLocalDataSource {

    private fun toUser(row: ResultRow): User {
        return User(
            id = row[UsersTable.id],
            email = row[UsersTable.email],
            passwordHash = row[UsersTable.passwordHash],
            name = row[UsersTable.name],
            avatarUrl = row[UsersTable.avatarUrl],
            bannerUrl = row[UsersTable.bannerUrl],
            bio = row[UsersTable.bio],
            isVerified = row[UsersTable.isVerified],
            createdAt = row[UsersTable.createdAt],
            updatedAt = row[UsersTable.updatedAt]
        )
    }

    override suspend fun getUserByEmail(email: String): User? = suspendTransaction(db = database) {
        UsersTable.selectAll()
            .where { UsersTable.email eq email }
            .map { toUser(it) }
            .singleOrNull()
    }

    override suspend fun getUserById(id: Uuid): User? = suspendTransaction(db = database) {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .map { toUser(it) }
            .singleOrNull()
    }

    override suspend fun createUser(email: String, passwordHash: String, name: String): User = suspendTransaction(db = database) {
        val newId = Uuid.random()
        val now = LocalDateTime.now()
        
        UsersTable.insert {
            it[id] = newId
            it[UsersTable.email] = email
            it[UsersTable.passwordHash] = passwordHash
            it[UsersTable.name] = name
            it[avatarUrl] = null
            it[bannerUrl] = null
            it[bio] = null
            it[isVerified] = false
            it[createdAt] = now
            it[updatedAt] = now
        }
        
        User(
            id = newId,
            email = email,
            passwordHash = passwordHash,
            name = name,
            avatarUrl = null,
            bannerUrl = null,
            bio = null,
            isVerified = false,
            createdAt = now,
            updatedAt = now
        )
    }

    override suspend fun createVerificationToken(userId: Uuid, token: String): Boolean = suspendTransaction(db = database) {
        val userRow = UsersTable.selectAll()
            .where { UsersTable.id eq userId }
            .singleOrNull() ?: return@suspendTransaction false
        val email = userRow[UsersTable.email]
        val now = LocalDateTime.now()
        val expiresAtTime = now.plusHours(24)

        // Delete existing tokens for this email
        VerificationTokensTable.deleteWhere { VerificationTokensTable.email eq email }

        VerificationTokensTable.insert {
            it[id] = Uuid.random()
            it[VerificationTokensTable.email] = email
            it[VerificationTokensTable.token] = token
            it[type] = "EMAIL_VERIFICATION"
            it[expiresAt] = expiresAtTime
        }
        true
    }

    override suspend fun verifyUser(email: String, token: String): Boolean = suspendTransaction(db = database) {
        val tokenRow = VerificationTokensTable.selectAll()
            .where {
                (VerificationTokensTable.email eq email) and (VerificationTokensTable.token eq token) and (VerificationTokensTable.type eq "EMAIL_VERIFICATION")
            }.singleOrNull() ?: return@suspendTransaction false

        val expiresAtVal = tokenRow[VerificationTokensTable.expiresAt]
        if (expiresAtVal.isBefore(LocalDateTime.now())) {
            return@suspendTransaction false
        }

        // Update user
        UsersTable.update({ UsersTable.email eq email }) {
            it[isVerified] = true
            it[updatedAt] = LocalDateTime.now()
        }

        // Delete token
        VerificationTokensTable.deleteWhere { VerificationTokensTable.email eq email }

        true
    }

    override suspend fun updateUser(
        id: Uuid,
        name: String,
        bio: String?,
        avatarUrl: String?,
        bannerUrl: String?
    ): User? = suspendTransaction(db = database) {
        UsersTable.update({ UsersTable.id eq id }) {
            it[UsersTable.name] = name
            it[UsersTable.bio] = bio
            if (avatarUrl != null) {
                it[UsersTable.avatarUrl] = avatarUrl
            }
            if (bannerUrl != null) {
                it[UsersTable.bannerUrl] = bannerUrl
            }
            it[updatedAt] = LocalDateTime.now()
        }
        getUserById(id)
    }
}
