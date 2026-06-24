@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.domain.model

import java.time.LocalDateTime
import kotlin.uuid.Uuid

data class Video(
    val id: Uuid,
    val title: String,
    val description: String?,
    val videoUrl: String,
    val thumbnailUrl: String,
    val viewsCount: Long,
    val duration: Int,
    val uploaderId: Uuid,
    val uploaderName: String,
    val uploaderAvatarUrl: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
