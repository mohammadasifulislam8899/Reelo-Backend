package com.xentoryx.labs.reelo.feature.video.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class VideoResponse(
    val id: String,
    val title: String,
    val description: String?,
    val videoUrl: String,
    val thumbnailUrl: String,
    val viewsCount: Long,
    val duration: Int,
    val uploaderId: String,
    val uploaderName: String,
    val uploaderAvatarUrl: String?,
    val createdAt: String,
    val updatedAt: String
)
