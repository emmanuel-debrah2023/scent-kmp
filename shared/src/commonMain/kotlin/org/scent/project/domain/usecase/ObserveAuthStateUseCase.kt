package org.scent.project.domain.usecase

import kotlinx.coroutines.flow.Flow
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.repository.AuthRepository

class ObserveAuthStateUseCase(private val repository: AuthRepository) {
    operator fun invoke(): Flow<AuthState> = repository.observeAuthState()
}
