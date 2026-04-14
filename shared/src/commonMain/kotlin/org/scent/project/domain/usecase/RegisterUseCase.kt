package org.scent.project.domain.usecase

import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.repository.AuthRepository

class RegisterUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(username: String, email: String, password: String, displayName: String): Result<AuthUser> =
        repository.register(username, email, password, displayName)
}
