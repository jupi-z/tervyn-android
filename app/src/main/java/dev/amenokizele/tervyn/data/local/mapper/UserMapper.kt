package dev.amenokizele.tervyn.data.local.mapper

import dev.amenokizele.tervyn.data.local.entity.UserEntity
import dev.amenokizele.tervyn.domain.model.User

fun User.toEntity() = UserEntity(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    jobTitle = jobTitle,
    avatarUrl = avatarUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastSyncedAt = lastSyncedAt
)

fun UserEntity.toDomain() = User(
    id = id,
    email = email,
    firstName = firstName,
    lastName = lastName,
    jobTitle = jobTitle,
    avatarUrl = avatarUrl,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastSyncedAt = lastSyncedAt
)
