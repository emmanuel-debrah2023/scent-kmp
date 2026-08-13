package org.scent.project.domain.usecase

import org.scent.project.domain.model.Review
import org.scent.project.domain.repository.ProfileRepository
import org.scent.project.domain.util.Result

open class GetUserReviewsUseCase(
    private val repository: ProfileRepository,
) {
    open suspend operator fun invoke(userId: Int): Result<List<Review>> = repository.getUserReviews(userId)
}
