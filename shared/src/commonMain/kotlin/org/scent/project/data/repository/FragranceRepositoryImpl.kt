package org.scent.project.data.repository

import org.scent.project.data.mapper.FragranceMapper.toDomainList
import org.scent.project.data.mapper.FragranceMapper.toFragrance
import org.scent.project.data.remote.api.FragranceApi
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.repository.FragranceRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

class FragranceRepositoryImpl(
    private val api: FragranceApi,
) : FragranceRepository {
    override suspend fun searchFragrances(
        query: String,
        cursor: String?,
        limit: Int,
    ): Result<List<Fragrance>> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            val response = api.searchFragrances(query, cursor, limit)
            val fragrances = response.fragrances?.toDomainList() ?: emptyList()
            fragrances.asRight()
        }

    override suspend fun getFragranceDetail(fragranceId: Int): Result<Fragrance> =
        safeApiCall(
            onHttpError = { status ->
                AppError.NetworkError.ServerError(statusCode = status).asLeft()
            },
        ) {
            val response = api.getFragranceDetail(fragranceId)
            response.toFragrance()
        }
}
