package org.scent.project.domain.usecase

import org.scent.project.domain.model.Post
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.util.Result

open class GetUserPostsUseCase(
    private val repository: PostRepository,
) {
    open suspend operator fun invoke(userId: Int): Result<List<Post>> = repository.getUserPosts(userId.toString())
}
