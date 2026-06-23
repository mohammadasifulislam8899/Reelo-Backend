@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.core.db.schema

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.*

object VerificationTokensTable : Table("verification_tokens") {
    val id = uuid("id")
    val email = varchar("email", 255)
    val token = varchar("token", 255)
    val type = varchar("type", 50) // e.g., "EMAIL_VERIFICATION", "PASSWORD_RESET"
    val expiresAt = datetime("expires_at")

    override val primaryKey = PrimaryKey(id)
}
