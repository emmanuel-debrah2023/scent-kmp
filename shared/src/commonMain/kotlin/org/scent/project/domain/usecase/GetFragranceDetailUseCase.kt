package org.scent.project.domain.usecase

import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.repository.FragranceRepository
import org.scent.project.domain.util.Result

open class GetFragranceDetailUseCase(
    private val repository: FragranceRepository,
) {
    open suspend operator fun invoke(fragranceId: Int): Result<Fragrance> = repository.getFragranceDetail(fragranceId)
}
