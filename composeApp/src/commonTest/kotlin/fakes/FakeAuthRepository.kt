package fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.repository.AuthRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight

class FakeAuthRepository : AuthRepository {
    private var result: Result<AuthUser> = AppError.Unknown().asLeft()
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)

    fun setResult(result: Result<AuthUser>) {
        this.result = result
    }

    fun setAuthState(state: AuthState) {
        _authState.value = state
    }

    override fun observeAuthState(): Flow<AuthState> = _authState

    override suspend fun register(
        email: String,
        password: String,
        username: String,
        displayName: String
    ): Result<AuthUser> = result

    override suspend fun login(email: String, password: String): Result<AuthUser> = result

    override suspend fun getCurrentUser(): Result<AuthUser> = result

    override suspend fun logout(): Result<Unit> = Unit.asRight()
}
