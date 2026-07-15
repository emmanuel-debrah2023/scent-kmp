package org.scent.project.domain.usecase

import org.scent.project.domain.repository.AuthRepository
import org.scent.project.domain.util.Result

class LogoutUseCase(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.logout()
}
