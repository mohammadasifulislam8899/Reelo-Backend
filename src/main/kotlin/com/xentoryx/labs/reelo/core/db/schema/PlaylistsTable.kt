@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.core.db.schema

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.*

object PlaylistsTable : Table("playlists") {
    val id = uuid("id")
    val name = varchar("name", 150)
    val description = text("description").nullable()
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val isPrivate = bool("is_private").default(true)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)
}
