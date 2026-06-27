package com.xentoryx.labs.reelo.feature.auth.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshRequest(
    val refreshToken: String
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val bio: String?,
    val isVerified: Boolean,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse
)

@Serializable
data class TokenResponse(
    val accessToken: String,
    val refreshToken: String
)

@Serializable
data class VerificationRequest(
    val email: String,
    val token: String
)

@Serializable
data class MessageResponse(
    val message: String
)

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null
)
