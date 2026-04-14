package org.scent.project.domain.usecase

import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.repository.AuthRepository

class GoogleAuthUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(idToken: String): Result<AuthUser> =
        repository.loginWithGoogle(idToken)
}
