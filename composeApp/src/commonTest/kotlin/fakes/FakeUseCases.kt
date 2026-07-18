package fakes

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.usecase.GetCurrentUserUseCase
import org.scent.project.domain.usecase.LoginUseCase
import org.scent.project.domain.usecase.LogoutUseCase
import org.scent.project.domain.usecase.ObserveAuthStateUseCase
import org.scent.project.domain.usecase.RegisterUseCase
import org.scent.project.domain.util.Result

class FakeLoginUseCase : LoginUseCase(FakeAuthRepository()) {
    var result: Result<AuthUser>? = null
    var lastEmail: String? = null
    var lastPassword: String? = null
    var onInvoke: (() -> Unit)? = null

    override suspend fun invoke(
        email: String,
        password: String,
    ): Result<AuthUser> {
        lastEmail = email
        lastPassword = password
        onInvoke?.invoke()
        return result ?: error("FakeLoginUseCase.result not set")
    }
}

class FakeRegisterUseCase : RegisterUseCase(FakeAuthRepository()) {
    var result: Result<AuthUser>? = null
    var onInvoke: (() -> Unit)? = null

    override suspend fun invoke(
        email: String,
        password: String,
        username: String,
        displayName: String,
    ): Result<AuthUser> {
        onInvoke?.invoke()
        return result ?: error("FakeRegisterUseCase.result not set")
    }
}

class FakeGetCurrentUserUseCase : GetCurrentUserUseCase(FakeAuthRepository()) {
    var result: Result<AuthUser>? = null

    override suspend fun invoke(): Result<AuthUser> = result ?: error("FakeGetCurrentUserUseCase.result not set")
}

class FakeObserveAuthStateUseCase : ObserveAuthStateUseCase(FakeAuthRepository()) {
    var flow = MutableStateFlow<AuthState>(AuthState.Unknown)

    override fun invoke(): Flow<AuthState> = flow
}

class FakeLogoutUseCase : LogoutUseCase(FakeAuthRepository()) {
    var result: Result<Unit>? = null
    var invocationCount = 0

    override suspend fun invoke(): Result<Unit> {
        invocationCount++
        return result ?: error("FakeLogoutUseCase.result not set")
    }
}
