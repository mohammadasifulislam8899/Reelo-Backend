@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.domain.usecase

import com.xentoryx.labs.reelo.feature.video.domain.model.Video
import com.xentoryx.labs.reelo.feature.video.domain.repository.VideoRepository
import kotlin.uuid.Uuid

class GetVideoByIdUseCase(private val videoRepository: VideoRepository) {
    suspend fun execute(id: Uuid): Video? {
        return videoRepository.getVideoById(id)
    }
}
