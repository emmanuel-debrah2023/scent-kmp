package org.scent.project.domain.usecase

import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.repository.AuthRepository

class GetCurrentUserUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(): Result<AuthUser> =
        repository.getCurrentUser()
}
