package org.scent.project.domain.usecase

import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.repository.AuthRepository

class AppleAuthUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(
        identityToken: String,
        email: String?,
        givenName: String?
    ): Result<AuthUser> = repository.loginWithApple(identityToken, email, givenName)
}
