package org.scent.project.data.repository

import org.scent.project.data.local.TokenStorage
import org.scent.project.data.mapper.ProfileMapper.toCollection
import org.scent.project.data.mapper.ProfileMapper.toPosts
import org.scent.project.data.remote.api.ProfileApi
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.Post
import org.scent.project.domain.repository.ProfileRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

class ProfileRepositoryImpl(
    private val api: ProfileApi,
    private val tokenStorage: TokenStorage,
) : ProfileRepository {
    override suspend fun getUserWishlist(userId: Int): Result<List<CollectionEntry>> =
        safeApiCall(
            onHttpError = { status -> AppError.NetworkError.ServerError(statusCode = status).asLeft() },
        ) {
            val token = tokenStorage.getToken().getOrNull()
            api.getUserWishlist(userId, token).toCollection().asRight()
        }

    override suspend fun getUserLikes(userId: Int): Result<List<Post>> =
        safeApiCall(
            onHttpError = { status -> AppError.NetworkError.ServerError(statusCode = status).asLeft() },
        ) {
            val token = tokenStorage.getToken().getOrNull()
            api.getUserLikes(userId, token).toPosts().asRight()
        }
}
