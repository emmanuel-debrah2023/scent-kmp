package org.scent.project.domain.usecase

import org.scent.project.domain.repository.AuthRepository
import org.scent.project.domain.util.Result

open class LogoutUseCase(
    private val repository: AuthRepository,
) {
    open suspend operator fun invoke(): Result<Unit> = repository.logout()
}
