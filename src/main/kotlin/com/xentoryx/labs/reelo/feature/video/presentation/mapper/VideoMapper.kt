@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.presentation.mapper

import com.xentoryx.labs.reelo.feature.video.domain.model.Video
import com.xentoryx.labs.reelo.feature.video.presentation.dto.VideoResponse
import java.time.format.DateTimeFormatter

fun Video.toVideoResponse(): VideoResponse {
    return VideoResponse(
        id = id.toString(),
        title = title,
        description = description,
        videoUrl = videoUrl,
        thumbnailUrl = thumbnailUrl,
        viewsCount = viewsCount,
        duration = duration,
        uploaderId = uploaderId.toString(),
        uploaderName = uploaderName,
        uploaderAvatarUrl = uploaderAvatarUrl,
        createdAt = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updatedAt = updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}
