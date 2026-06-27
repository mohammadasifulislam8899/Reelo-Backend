package com.xentoryx.labs.reelo.feature.video.domain.usecase

import com.xentoryx.labs.reelo.feature.video.domain.model.Video
import com.xentoryx.labs.reelo.feature.video.domain.repository.VideoRepository

class SearchVideosUseCase(private val videoRepository: VideoRepository) {
    suspend fun execute(query: String): List<Video> {
        if (query.isBlank()) return emptyList()
        
        val matchedVideos = videoRepository.searchVideos(query)
        val queryLower = query.lowercase()

        return matchedVideos.sortedBy { video ->
            val titleLower = video.title.lowercase()
            val descLower = video.description?.lowercase() ?: ""
            when {
                titleLower == queryLower -> 0
                titleLower.startsWith(queryLower) -> 1
                titleLower.contains(queryLower) -> 2
                descLower.contains(queryLower) -> 3
                else -> 4
            }
        }
    }
}
