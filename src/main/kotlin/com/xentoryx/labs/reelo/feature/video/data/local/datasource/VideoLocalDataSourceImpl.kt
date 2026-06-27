@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.video.data.local.datasource

import com.xentoryx.labs.reelo.core.db.schema.UsersTable
import com.xentoryx.labs.reelo.core.db.schema.VideosTable
import com.xentoryx.labs.reelo.feature.video.domain.model.Video
import org.jetbrains.exposed.v1.r2dbc.R2dbcDatabase
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.insert
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.singleOrNull

class VideoLocalDataSourceImpl(private val database: R2dbcDatabase) : VideoLocalDataSource {

    private fun toVideo(row: ResultRow): Video {
        return Video(
            id = row[VideosTable.id],
            title = row[VideosTable.title],
            description = row[VideosTable.description],
            videoUrl = row[VideosTable.videoUrl],
            thumbnailUrl = row[VideosTable.thumbnailUrl],
            viewsCount = row[VideosTable.viewsCount],
            duration = row[VideosTable.duration],
            uploaderId = row[UsersTable.id],
            uploaderName = row[UsersTable.name],
            uploaderAvatarUrl = row[UsersTable.avatarUrl],
            createdAt = row[VideosTable.createdAt],
            updatedAt = row[VideosTable.updatedAt]
        )
    }

    override suspend fun getVideos(): List<Video> = suspendTransaction(db = database) {
        (VideosTable innerJoin UsersTable)
            .selectAll()
            .map { toVideo(it) }
            .toList()
    }

    override suspend fun getVideoById(id: Uuid): Video? = suspendTransaction(db = database) {
        (VideosTable innerJoin UsersTable)
            .selectAll()
            .where { VideosTable.id eq id }
            .map { toVideo(it) }
            .singleOrNull()
    }

    override suspend fun insertVideo(video: Video): Video = suspendTransaction(db = database) {
        VideosTable.insert {
            it[id] = video.id
            it[title] = video.title
            it[description] = video.description
            it[videoUrl] = video.videoUrl
            it[thumbnailUrl] = video.thumbnailUrl
            it[viewsCount] = video.viewsCount
            it[duration] = video.duration
            it[userId] = video.uploaderId
            it[createdAt] = video.createdAt
            it[updatedAt] = video.updatedAt
        }
        video
    }

    override suspend fun searchVideos(query: String): List<Video> = suspendTransaction(db = database) {
        val queryLower = "%${query.lowercase()}%"
        (VideosTable innerJoin UsersTable)
            .selectAll()
            .where {
                (VideosTable.title.lowerCase() like queryLower) or
                (VideosTable.description.lowerCase() like queryLower)
            }
            .map { toVideo(it) }
            .toList()
    }
}
