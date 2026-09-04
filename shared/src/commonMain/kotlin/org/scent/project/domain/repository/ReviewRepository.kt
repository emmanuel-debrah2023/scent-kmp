package org.scent.project.domain.repository

import kotlinx.coroutines.flow.Flow
import org.scent.project.domain.model.Review
import org.scent.project.domain.util.Result

/**
 * Fragrance reviews by a user. Lives in Room, backed by [ReviewDao].
 * Per ADR-0001.
 */
interface ReviewRepository {
    fun getUserReviewsFlow(userId: Int): Flow<Result<List<Review>>>

    suspend fun refreshUserReviews(userId: Int): Result<Unit>

    /**
     * Transitional suspend read for ProfileViewModel's async/await pattern.
     *
     * TODO(chore/profile-viewmodel-split): superseded by [getUserReviewsFlow].
     */
    suspend fun getUserReviews(userId: Int): Result<List<Review>>
}
