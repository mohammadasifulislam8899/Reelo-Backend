@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.core.db.schema

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.*

object VideosTable : Table("videos") {
    val id = uuid("id")
    val title = varchar("title", 255)
    val description = text("description").nullable()
    val videoUrl = varchar("video_url", 512)
    val thumbnailUrl = varchar("thumbnail_url", 512)
    val viewsCount = long("views_count").default(0)
    val duration = integer("duration") // seconds
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")

    override val primaryKey = PrimaryKey(id)
}
