package org.scent.project.domain.model

data class FeedPage(
    val posts: List<Post>,
    val nextCursor: String? = null,
)
