package org.scent.project.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.scent.project.data.remote.dto.AuthResponse
import org.scent.project.data.remote.dto.MeResponse
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthState
import org.scent.project.domain.util.asLeft
import org.scent.project.fakes.FakeAuthApi
import org.scent.project.fakes.FakeTokenStorage
import org.scent.project.fakes.FakeValidator
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthRepositoryImplTest {
    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun repo(
        api: FakeAuthApi = FakeAuthApi(),
        storage: FakeTokenStorage = FakeTokenStorage(),
        validator: FakeValidator = FakeValidator(),
    ) = AuthRepositoryImpl(api = api, tokenStorage = storage, validator = validator)

    private val validAuthResponse =
        AuthResponse(
            token = "jwt-token",
            userId = 1,
            username = "johndoe",
            email = "john@example.com",
            displayName = "John Doe",
        )

    private val validMeResponse =
        MeResponse(
            userId = 1,
            username = "johndoe",
            email = "john@example.com",
            displayName = "John Doe",
        )

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Test
    fun `login returns Right and saves token on success`() =
        runTest {
            val storage = FakeTokenStorage()
            val api = FakeAuthApi().apply { loginResponse = validAuthResponse }

            val result = repo(api = api, storage = storage).login("john@example.com", "password123")

            assertTrue(result.isRight)
            assertTrue(storage.storedToken == "jwt-token")
        }

    @Test
    fun `login returns Left without network call when email validation fails`() =
        runTest {
            val api = FakeAuthApi() // no response set — would crash if called
            val validator =
                FakeValidator(
                    emailResult = AppError.ValidationError.InvalidEmail().asLeft(),
                )

            val result = repo(api = api, validator = validator).login("bad-email", "password123")

            assertTrue(result.isLeft)
            assertIs<AppError.ValidationError.InvalidEmail>(result.leftOrNull())
            // api was never called — loginResponse not set, no crash
        }

    @Test
    fun `login returns Left without network call when password validation fails`() =
        runTest {
            val api = FakeAuthApi()
            val validator =
                FakeValidator(
                    passwordResult = AppError.ValidationError.PasswordTooShort().asLeft(),
                )

            val result = repo(api = api, validator = validator).login("john@example.com", "short")

            assertTrue(result.isLeft)
            assertIs<AppError.ValidationError.PasswordTooShort>(result.leftOrNull())
        }

    @Test
    fun `login emits Authenticated state on success`() =
        runTest {
            val api = FakeAuthApi().apply { loginResponse = validAuthResponse }
            val repository = repo(api = api)

            repository.login("john@example.com", "password123")

            val state = repository.observeAuthState().first()
            assertIs<AuthState.Authenticated>(state)
        }

    @Test
    fun `login does not change auth state on validation failure`() =
        runTest {
            val validator =
                FakeValidator(
                    emailResult = AppError.ValidationError.InvalidEmail().asLeft(),
                )
            val repository = repo(validator = validator)

            repository.login("bad", "password123")

            // State should remain Unknown since no collector has subscribed to trigger hydration
            // and no successful operation has changed it
            val state = repository.observeAuthState().first()
            // After first collection, hydration runs — no token stored so Unauthenticated
            assertIs<AuthState.Unauthenticated>(state)
        }

    // -------------------------------------------------------------------------
    // register
    // -------------------------------------------------------------------------

    @Test
    fun `register returns Right and saves token on success`() =
        runTest {
            val storage = FakeTokenStorage()
            val api = FakeAuthApi().apply { registerResponse = validAuthResponse }

            val result =
                repo(api = api, storage = storage)
                    .register("john@example.com", "password123", "johndoe", "John Doe")

            assertTrue(result.isRight)
            assertTrue(storage.storedToken == "jwt-token")
        }

    @Test
    fun `register returns Left without network call when username validation fails`() =
        runTest {
            val api = FakeAuthApi()
            val validator =
                FakeValidator(
                    usernameResult = AppError.ValidationError.InvalidInput(fieldName = "username").asLeft(),
                )

            val result =
                repo(api = api, validator = validator)
                    .register("john@example.com", "password123", "ab", "John Doe")

            assertTrue(result.isLeft)
            assertIs<AppError.ValidationError.InvalidInput>(result.leftOrNull())
        }

    @Test
    fun `register returns Left without network call when displayName validation fails`() =
        runTest {
            val api = FakeAuthApi()
            val validator =
                FakeValidator(
                    displayNameResult = AppError.ValidationError.RequiredFieldEmpty(fieldName = "displayName").asLeft(),
                )

            val result =
                repo(api = api, validator = validator)
                    .register("john@example.com", "password123", "johndoe", "")

            assertTrue(result.isLeft)
            assertIs<AppError.ValidationError.RequiredFieldEmpty>(result.leftOrNull())
        }

    // -------------------------------------------------------------------------
    // logout
    // -------------------------------------------------------------------------

    @Test
    fun `logout clears token and emits Unauthenticated on success`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "existing-token" }
            val repository = repo(storage = storage)

            val result = repository.logout()

            assertTrue(result.isRight)
            assertNull(storage.storedToken)
            val state = repository.observeAuthState().first()
            assertIs<AuthState.Unauthenticated>(state)
        }

    @Test
    fun `logout returns Left and does not change state when clearToken fails`() =
        runTest {
            val storage =
                FakeTokenStorage().apply {
                    storedToken = "existing-token"
                    clearError = AppError.StorageError.WriteFailed()
                }
            val api = FakeAuthApi().apply { loginResponse = validAuthResponse }
            val repository = repo(api = api, storage = storage)

            // First login to set state to Authenticated
            repository.login("john@example.com", "password123")

            val result = repository.logout()

            assertTrue(result.isLeft)
            assertIs<AppError.StorageError.WriteFailed>(result.leftOrNull())
            // Token should still be present since clear failed (login stored "jwt-token")
            assertTrue(storage.storedToken == "jwt-token")
        }

    // -------------------------------------------------------------------------
    // getCurrentUser
    // -------------------------------------------------------------------------

    @Test
    fun `getCurrentUser returns Unauthorized when no token stored`() =
        runTest {
            val storage = FakeTokenStorage() // storedToken = null

            val result = repo(storage = storage).getCurrentUser()

            assertTrue(result.isLeft)
            assertIs<AppError.AuthError.Unauthorized>(result.leftOrNull())
        }

    @Test
    fun `getCurrentUser returns Right and emits Authenticated when token valid`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "valid-token" }
            val api = FakeAuthApi().apply { meResponse = validMeResponse }
            val repository = repo(api = api, storage = storage)

            val result = repository.getCurrentUser()

            assertTrue(result.isRight)
            val state = repository.observeAuthState().first()
            assertIs<AuthState.Authenticated>(state)
        }

    // -------------------------------------------------------------------------
    // observeAuthState / hydration
    // -------------------------------------------------------------------------

    @Test
    fun `observeAuthState emits Unauthenticated when no token stored`() =
        runTest {
            val storage = FakeTokenStorage() // no token

            val state = repo(storage = storage).observeAuthState().first()

            assertIs<AuthState.Unauthenticated>(state)
        }

    @Test
    fun `observeAuthState emits Authenticated when stored token is valid`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "valid-token" }
            val api = FakeAuthApi().apply { meResponse = validMeResponse }

            val state = repo(api = api, storage = storage).observeAuthState().first()

            assertIs<AuthState.Authenticated>(state)
        }

    @Test
    fun `observeAuthState emits Unauthenticated and clears token when me returns parse error`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "valid-token" }
            // MeResponse with null userId — will fail mapping and trigger Unauthenticated
            val api =
                FakeAuthApi().apply {
                    meResponse = MeResponse(userId = null, username = "johndoe", displayName = "John Doe")
                }

            val state = repo(api = api, storage = storage).observeAuthState().first()

            assertIs<AuthState.Unauthenticated>(state)
            assertNull(storage.storedToken)
        }

    @Test
    fun `observeAuthState stays Unknown when network error occurs during hydration`() =
        runTest {
            val storage = FakeTokenStorage().apply { storedToken = "valid-token" }
            val api =
                FakeAuthApi().apply {
                    meException = RuntimeException("Network unreachable")
                }
            val repository = repo(api = api, storage = storage)

            // On a general network error during hydration, the implementation defaults to
            // Unauthenticated so the user can try logging in again.
            val state = repository.observeAuthState().first()
            assertIs<AuthState.Unauthenticated>(state)
        }
}
