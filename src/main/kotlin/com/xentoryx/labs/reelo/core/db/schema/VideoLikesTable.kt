@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.core.db.schema

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.*

object VideoLikesTable : Table("video_likes") {
    val id = uuid("id")
    val videoId = uuid("video_id").references(VideosTable.id, onDelete = ReferenceOption.CASCADE)
    val userId = uuid("user_id").references(UsersTable.id, onDelete = ReferenceOption.CASCADE)
    val isLike = bool("is_like") // true = like, false = dislike
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("idx_video_user_like", videoId, userId)
    }
}
