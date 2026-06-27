@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.domain.usecase

import com.xentoryx.labs.reelo.feature.video.domain.model.Video
import com.xentoryx.labs.reelo.feature.video.domain.repository.VideoRepository
import kotlin.uuid.Uuid

class GetVideosByUploaderUseCase(private val videoRepository: VideoRepository) {
    suspend fun execute(uploaderId: Uuid): List<Video> {
        return videoRepository.getVideosByUploader(uploaderId)
    }
}
