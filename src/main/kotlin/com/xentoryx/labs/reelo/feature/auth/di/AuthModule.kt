package com.xentoryx.labs.reelo.feature.auth.di

import com.xentoryx.labs.reelo.core.security.JwtProvider
import com.xentoryx.labs.reelo.feature.auth.data.local.datasource.AuthLocalDataSource
import com.xentoryx.labs.reelo.feature.auth.data.local.datasource.AuthLocalDataSourceImpl
import com.xentoryx.labs.reelo.feature.auth.data.repository.AuthRepositoryImpl
import com.xentoryx.labs.reelo.feature.auth.domain.repository.AuthRepository
import com.xentoryx.labs.reelo.feature.auth.domain.usecase.*
import org.koin.dsl.module

val authModule = module {
    single { JwtProvider(get()) }
    single<AuthLocalDataSource> { AuthLocalDataSourceImpl(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get()) }
    single { RegisterUserUseCase(get()) }
    single { LoginUserUseCase(get(), get()) }
    single { RefreshTokensUseCase(get(), get()) }
    single { VerifyUserUseCase(get()) }
    single { GetUserUseCase(get()) }
    single { UpdateProfileUseCase(get()) }
}
