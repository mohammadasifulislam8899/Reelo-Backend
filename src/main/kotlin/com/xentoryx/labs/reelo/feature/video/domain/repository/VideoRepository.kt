@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.domain.repository

import com.xentoryx.labs.reelo.feature.video.domain.model.Video
import kotlin.uuid.Uuid

interface VideoRepository {
    suspend fun getVideos(): List<Video>
    suspend fun getVideoById(id: Uuid): Video?
}
