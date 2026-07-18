package org.scent.project.domain.usecase

import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.repository.AuthRepository
import org.scent.project.domain.util.Result

open class LoginUseCase(
    private val repository: AuthRepository,
) {
    open suspend operator fun invoke(
        email: String,
        password: String,
    ): Result<AuthUser> = repository.login(email, password)
}
