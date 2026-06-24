@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.data.local.datasource

import com.xentoryx.labs.reelo.feature.video.domain.model.Video
import kotlin.uuid.Uuid

interface VideoLocalDataSource {
    suspend fun getVideos(): List<Video>
    suspend fun getVideoById(id: Uuid): Video?
}
