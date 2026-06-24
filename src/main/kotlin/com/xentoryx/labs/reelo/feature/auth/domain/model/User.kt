@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.domain.model

import java.time.LocalDateTime
import kotlin.uuid.Uuid

data class User(
    val id: Uuid,
    val email: String,
    val passwordHash: String,
    val name: String,
    val avatarUrl: String?,
    val bannerUrl: String?,
    val bio: String?,
    val isVerified: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
