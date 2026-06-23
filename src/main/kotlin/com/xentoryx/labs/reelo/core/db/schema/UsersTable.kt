@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.core.db.schema

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.*

object UsersTable : Table("users") {
    val id = uuid("id")
    val email = varchar("email", 255).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 100)
    val avatarUrl = varchar("avatar_url", 512).nullable()
    val bannerUrl = varchar("banner_url", 512).nullable()
    val bio = text("bio").nullable()
    val isVerified = bool("is_verified").default(false)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}
