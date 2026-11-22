package org.scent.project.domain.usecase

import kotlinx.coroutines.flow.first
import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.FeaturedVideo
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.repository.PostRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asRight

/**
 * The newest video post tagged with one of [candidates], or null when none is. Refreshes
 * the feed first, so a failure there or reading the cache comes back as that `Left`.
 */
open class GetLatestVideoFeatureUseCase(
    private val postRepository: PostRepository,
) {
    open suspend operator fun invoke(candidates: List<Fragrance>): Result<FeaturedVideo?> {
        if (candidates.isEmpty()) return null.asRight()
        return postRepository.refreshFeed().flatMap {
            postRepository.getFeedFlow().first().map { posts ->
                val byId = candidates.associateBy { it.id.toString() }
                posts
                    .filter { it.contentFormat == ContentFormat.VIDEO }
                    .sortedByDescending { it.createdAt }
                    .firstNotNullOfOrNull { post ->
                        post.fragranceIds.firstNotNullOfOrNull { byId[it] }?.let { FeaturedVideo(post.id, it) }
                    }
            }
        }
    }
}
