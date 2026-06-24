package com.xentoryx.labs.reelo.feature.video.domain.usecase

import com.xentoryx.labs.reelo.feature.video.domain.model.Video
import com.xentoryx.labs.reelo.feature.video.domain.repository.VideoRepository

class GetVideosUseCase(private val videoRepository: VideoRepository) {
    suspend fun execute(): List<Video> {
        return videoRepository.getVideos()
    }
}
