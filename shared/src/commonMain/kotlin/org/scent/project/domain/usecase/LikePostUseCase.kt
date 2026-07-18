package org.scent.project.domain.usecase

import org.scent.project.domain.model.LikeResult
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.util.Result

open class LikePostUseCase(
    private val repository: PostRepository,
) {
    open suspend operator fun invoke(postId: String): Result<LikeResult> = repository.likePost(postId)
}
