package org.scent.project.domain.model

data class User(
    val id: Int,
    val username: String,
    val displayName: String,
    val email: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val isSeller: Boolean = false,
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val postCount: Int = 0,
)
