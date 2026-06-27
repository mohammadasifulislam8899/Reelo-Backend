@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.presentation.mapper

import com.xentoryx.labs.reelo.feature.video.domain.model.Video
import com.xentoryx.labs.reelo.feature.video.presentation.dto.VideoResponse
import java.time.format.DateTimeFormatter

fun Video.toVideoResponse(baseUrl: String): VideoResponse {
    val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
    
    val formattedVideoUrl = if (videoUrl.contains("localhost:8080")) {
        videoUrl.replace("http://localhost:8080/", cleanBaseUrl).replace("https://localhost:8080/", cleanBaseUrl)
    } else if (videoUrl.startsWith("http://") || videoUrl.startsWith("https://")) {
        videoUrl
    } else {
        val cleanPath = if (videoUrl.startsWith("/")) videoUrl.substring(1) else videoUrl
        "$cleanBaseUrl$cleanPath"
    }

    val formattedThumbnailUrl = if (thumbnailUrl.contains("localhost:8080")) {
        thumbnailUrl.replace("http://localhost:8080/", cleanBaseUrl).replace("https://localhost:8080/", cleanBaseUrl)
    } else if (thumbnailUrl.startsWith("http://") || thumbnailUrl.startsWith("https://")) {
        thumbnailUrl
    } else {
        val cleanPath = if (thumbnailUrl.startsWith("/")) thumbnailUrl.substring(1) else thumbnailUrl
        "$cleanBaseUrl$cleanPath"
    }

    return VideoResponse(
        id = id.toString(),
        title = title,
        description = description,
        videoUrl = formattedVideoUrl,
        thumbnailUrl = formattedThumbnailUrl,
        viewsCount = viewsCount,
        duration = duration,
        uploaderId = uploaderId.toString(),
        uploaderName = uploaderName,
        uploaderAvatarUrl = uploaderAvatarUrl,
        createdAt = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updatedAt = updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}
