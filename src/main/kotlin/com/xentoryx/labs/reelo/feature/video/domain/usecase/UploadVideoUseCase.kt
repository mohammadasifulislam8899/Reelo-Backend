@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.domain.usecase

import com.xentoryx.labs.reelo.feature.video.domain.model.Video
import com.xentoryx.labs.reelo.feature.video.domain.repository.VideoRepository
import com.xentoryx.labs.reelo.core.storage.StorageService
import java.time.LocalDateTime
import kotlin.uuid.Uuid

class UploadVideoUseCase(
    private val videoRepository: VideoRepository,
    private val storageService: StorageService
) {
    suspend fun execute(
        title: String,
        description: String?,
        fileBytes: ByteArray,
        uploaderId: Uuid,
        duration: Int = 10
    ): Video {
        val fileName = "${Uuid.random()}.mp4"
        val videoUrl = storageService.uploadFile(fileName, fileBytes, "video/mp4")
        val thumbnailUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&auto=format&fit=crop&q=60"
        val now = LocalDateTime.now()
        
        val video = Video(
            id = Uuid.random(),
            title = title,
            description = description,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            viewsCount = 0L,
            duration = duration,
            uploaderId = uploaderId,
            uploaderName = "",
            uploaderAvatarUrl = null,
            createdAt = now,
            updatedAt = now
        )
        
        return videoRepository.insertVideo(video)
    }

    suspend fun executeWithUrl(
        title: String,
        description: String?,
        videoUrl: String,
        uploaderId: Uuid,
        duration: Int = 10
    ): Video {
        val thumbnailUrl = "https://images.unsplash.com/photo-1485846234645-a62644f84728?w=800&auto=format&fit=crop&q=60"
        val now = LocalDateTime.now()
        
        val video = Video(
            id = Uuid.random(),
            title = title,
            description = description,
            videoUrl = videoUrl,
            thumbnailUrl = thumbnailUrl,
            viewsCount = 0L,
            duration = duration,
            uploaderId = uploaderId,
            uploaderName = "",
            uploaderAvatarUrl = null,
            createdAt = now,
            updatedAt = now
        )
        
        return videoRepository.insertVideo(video)
    }
}
