package org.scent.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.repository.AuthRepository

open class ObserveAuthStateUseCase(
    private val repository: AuthRepository,
) {
    open operator fun invoke(): Flow<AuthState> = repository.observeAuthState()
}
