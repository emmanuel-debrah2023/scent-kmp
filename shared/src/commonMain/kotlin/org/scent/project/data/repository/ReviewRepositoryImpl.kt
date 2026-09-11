package org.scent.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.scent.project.data.local.TokenStorage
import org.scent.project.data.local.dao.ReviewDao
import org.scent.project.data.mapper.ReviewEntityMapper.toDomainList
import org.scent.project.data.mapper.ReviewEntityMapper.toEntity
import org.scent.project.data.mapper.ReviewEntityMapper.toNoteEntities
import org.scent.project.data.remote.api.ReviewApi
import org.scent.project.data.remote.dto.ReviewDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Review
import org.scent.project.domain.repository.ReviewRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

class ReviewRepositoryImpl(
    private val api: ReviewApi,
    private val tokenStorage: TokenStorage,
    private val reviewDao: ReviewDao,
) : ReviewRepository {
    override fun getUserReviewsFlow(userId: Int): Flow<Result<List<Review>>> =
        reviewDao
            .getUserReviews(userId)
            .map { it.toDomainList() }
            .catch { e -> emit(AppError.Unknown(cause = e).asLeft()) }

    override suspend fun refreshUserReviews(userId: Int): Result<Unit> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            val token = tokenStorage.getToken().getOrNull()
            val response = api.getUserReviews(userId, token)
            val dtos = response.reviews.orEmpty()

            // TODO(fix/review-collection-fk-orphan-filter): toEntity() only requires
            // fragrance?.id but fragranceEntities() also requires a non-blank name and brand,
            // so a fragrance with a blank brand yields a review row whose fragranceId has no
            // parent. ReviewEntity's ForeignKey then throws, @Transaction rolls the whole
            // refresh back, and one bad row errors the entire tab. Filter to mapped ids.
            reviewDao.replaceUserReviews(
                userId = userId,
                reviews = dtos.mapNotNull { it.toEntity()?.copy(reviewerId = userId) },
                fragrances = dtos.fragranceEntities(),
                notes = dtos.noteEntities(),
            )

            Unit.asRight()
        }

    private fun List<ReviewDto>.fragranceEntities() = mapNotNull { it.fragrance?.toEntity() }.distinctBy { it.id }

    private fun List<ReviewDto>.noteEntities() =
        mapNotNull { it.fragrance }
            .distinctBy { it.id }
            .flatMap { it.toNoteEntities() }
}
