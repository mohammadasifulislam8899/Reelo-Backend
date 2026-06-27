package com.xentoryx.labs.reelo.feature.video.di

import com.xentoryx.labs.reelo.feature.video.data.local.datasource.VideoLocalDataSource
import com.xentoryx.labs.reelo.feature.video.data.local.datasource.VideoLocalDataSourceImpl
import com.xentoryx.labs.reelo.feature.video.data.repository.VideoRepositoryImpl
import com.xentoryx.labs.reelo.feature.video.domain.repository.VideoRepository
import com.xentoryx.labs.reelo.feature.video.domain.usecase.GetVideoByIdUseCase
import com.xentoryx.labs.reelo.feature.video.domain.usecase.GetVideosUseCase
import com.xentoryx.labs.reelo.feature.video.domain.usecase.UploadVideoUseCase
import com.xentoryx.labs.reelo.feature.video.domain.usecase.SearchVideosUseCase
import com.xentoryx.labs.reelo.feature.video.domain.usecase.GetVideosByUploaderUseCase
import org.koin.dsl.module

val videoModule = module {
    single<VideoLocalDataSource> { VideoLocalDataSourceImpl(get()) }
    single<VideoRepository> { VideoRepositoryImpl(get()) }
    single { GetVideosUseCase(get()) }
    single { GetVideoByIdUseCase(get()) }
    single { UploadVideoUseCase(get(), get()) }
    single { SearchVideosUseCase(get()) }
    single { GetVideosByUploaderUseCase(get()) }
}
