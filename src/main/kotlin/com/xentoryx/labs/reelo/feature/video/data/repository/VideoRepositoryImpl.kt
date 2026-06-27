@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.data.repository

import com.xentoryx.labs.reelo.feature.video.data.local.datasource.VideoLocalDataSource
import com.xentoryx.labs.reelo.feature.video.domain.model.Video
import com.xentoryx.labs.reelo.feature.video.domain.repository.VideoRepository
import kotlin.uuid.Uuid

class VideoRepositoryImpl(
    private val localDataSource: VideoLocalDataSource
) : VideoRepository {

    override suspend fun getVideos(): List<Video> {
        return localDataSource.getVideos()
    }

    override suspend fun getVideoById(id: Uuid): Video? {
        return localDataSource.getVideoById(id)
    }

    override suspend fun insertVideo(video: Video): Video {
        return localDataSource.insertVideo(video)
    }
}
