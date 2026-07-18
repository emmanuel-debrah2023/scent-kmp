package org.scent.project.domain.usecase

import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.repository.AuthRepository
import org.scent.project.domain.util.Result

open class RegisterUseCase(
    private val repository: AuthRepository,
) {
    open suspend operator fun invoke(
        email: String,
        password: String,
        username: String,
        displayName: String,
    ): Result<AuthUser> = repository.register(email, password, username, displayName)
}
