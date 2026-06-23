@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.core.db.schema

import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.javatime.*

object PlaylistVideosTable : Table("playlist_videos") {
    val id = uuid("id")
    val playlistId = uuid("playlist_id").references(PlaylistsTable.id, onDelete = ReferenceOption.CASCADE)
    val videoId = uuid("video_id").references(VideosTable.id, onDelete = ReferenceOption.CASCADE)
    val addedAt = datetime("added_at")

    override val primaryKey = PrimaryKey(id)

    init {
        uniqueIndex("idx_playlist_video", playlistId, videoId)
    }
}
