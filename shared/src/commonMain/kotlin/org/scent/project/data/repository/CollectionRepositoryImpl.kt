package org.scent.project.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.scent.project.data.local.TokenStorage
import org.scent.project.data.local.dao.CollectionDao
import org.scent.project.data.mapper.CollectionEntryEntityMapper.toDomainList
import org.scent.project.data.mapper.CollectionEntryEntityMapper.toEntity
import org.scent.project.data.mapper.CollectionEntryEntityMapper.toNoteEntities
import org.scent.project.data.mapper.ProfileMapper.toCollection
import org.scent.project.data.remote.api.CollectionApi
import org.scent.project.data.remote.dto.CollectionEntryDto
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.repository.CollectionRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

class CollectionRepositoryImpl(
    private val api: CollectionApi,
    private val tokenStorage: TokenStorage,
    private val collectionDao: CollectionDao,
) : CollectionRepository {
    override fun getUserCollectionFlow(userId: Int): Flow<Result<List<CollectionEntry>>> =
        collectionDao
            .getUserCollection(userId)
            .map { it.toDomainList() }
            .catch { e -> emit(AppError.Unknown(cause = e).asLeft()) }

    override suspend fun getUserCollection(userId: Int): Result<List<CollectionEntry>> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            val token = tokenStorage.getToken().getOrNull()
            api.getUserCollection(userId, token).toCollection().asRight()
        }

    override suspend fun refreshUserCollection(userId: Int): Result<Unit> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            val token = tokenStorage.getToken().getOrNull()
            val response = api.getUserCollection(userId, token)
            val dtos = response.entries.orEmpty()

            collectionDao.replaceUserCollection(
                userId = userId,
                entries = dtos.mapNotNull { it.toEntity(userId) },
                fragrances = dtos.fragranceEntities(),
                notes = dtos.noteEntities(),
            )

            Unit.asRight()
        }

    private fun List<CollectionEntryDto>.fragranceEntities() =
        mapNotNull { it.fragrance?.toEntity() }.distinctBy { it.id }

    private fun List<CollectionEntryDto>.noteEntities() =
        mapNotNull { it.fragrance }
            .distinctBy { it.id }
            .flatMap { it.toNoteEntities() }
}
