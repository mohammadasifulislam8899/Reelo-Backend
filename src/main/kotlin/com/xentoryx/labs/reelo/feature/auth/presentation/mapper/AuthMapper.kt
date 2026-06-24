@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)
package com.xentoryx.labs.reelo.feature.auth.presentation.mapper

import com.xentoryx.labs.reelo.feature.auth.domain.model.User
import com.xentoryx.labs.reelo.feature.auth.presentation.dto.UserResponse
import java.time.format.DateTimeFormatter

fun User.toUserResponse(): UserResponse {
    return UserResponse(
        id = id.toString(),
        email = email,
        name = name,
        avatarUrl = avatarUrl,
        bannerUrl = bannerUrl,
        bio = bio,
        isVerified = isVerified,
        createdAt = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        updatedAt = updatedAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}
