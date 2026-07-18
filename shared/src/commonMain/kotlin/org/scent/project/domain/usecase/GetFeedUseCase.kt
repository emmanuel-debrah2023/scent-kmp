package org.scent.project.domain.usecase

import org.scent.project.domain.model.FeedPage
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.util.Result

open class GetFeedUseCase(
    private val repository: PostRepository,
) {
    open suspend operator fun invoke(
        cursor: String? = null,
        limit: Int = 20,
    ): Result<FeedPage> = repository.getFeed(cursor, limit)
}
