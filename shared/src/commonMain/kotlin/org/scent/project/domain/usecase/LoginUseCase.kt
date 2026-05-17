package org.scent.project.domain.usecase

import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.repository.AuthRepository

class LoginUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> =
        repository.login(email, password)
}
