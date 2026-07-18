package org.scent.project.domain.repository

import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.util.Result

interface FragranceRepository {
    suspend fun searchFragrances(
        query: String,
        cursor: String? = null,
        limit: Int = 20,
    ): Result<List<Fragrance>>

    suspend fun getFragranceDetail(fragranceId: Int): Result<Fragrance>
}
