package org.scent.project.domain.repository

import org.scent.project.domain.model.CreatePostParams
import org.scent.project.domain.model.FeedPage
import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.model.Post
import org.scent.project.domain.util.Result

interface PostRepository {
    suspend fun getFeed(
        cursor: String? = null,
        limit: Int = 20,
    ): Result<FeedPage>

    suspend fun likePost(postId: String): Result<LikeResult>

    suspend fun createPost(params: CreatePostParams): Result<Post>
}
