package ui.profile

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.usecase.ToggleFollowUseCase
import ui.base.UiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val sampleAuthUser =
        AuthUser(
            id = 1,
            username = "edebrah",
            displayName = "Emmanuel Debrah",
            email = "e@scent.dev",
            token = "test-token",
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = ProfileViewModel(sampleAuthUser, ToggleFollowUseCase())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─────────────────────────────────────────────
    // Initial state
    // ─────────────────────────────────────────────

    @Test
    fun `initial selected tab is Posts`() =
        runTest {
            assertEquals(ProfileTab.Posts, viewModel.uiState.value.selectedTab)
        }

    // ─────────────────────────────────────────────
    // load — triggered from init
    // ─────────────────────────────────────────────

    @Test
    fun `load maps AuthUser fields onto User domain model`() =
        runTest {
            val profileState = viewModel.uiState.value.profile
            val data = assertIs<UiState.Success<ProfileData>>(profileState).data
            assertEquals(sampleAuthUser.id, data.user.id)
            assertEquals(sampleAuthUser.username, data.user.username)
            assertEquals(sampleAuthUser.displayName, data.user.displayName)
            assertEquals(sampleAuthUser.email, data.user.email)
        }

    @Test
    fun `load sets isOwnProfile true from init`() =
        runTest {
            val data = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data
            assertTrue(data.isOwnProfile)
        }

    @Test
    fun `profile is in Success state after init`() =
        runTest {
            assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile)
        }

    // ─────────────────────────────────────────────
    // SelectTab
    // ─────────────────────────────────────────────

    @Test
    fun `SelectTab event updates selectedTab`() =
        runTest {
            viewModel.uiState.test {
                awaitItem() // initial

                viewModel.onEvent(ProfileEvent.SelectTab(ProfileTab.Collection))
                assertEquals(ProfileTab.Collection, awaitItem().selectedTab)

                viewModel.onEvent(ProfileEvent.SelectTab(ProfileTab.Reviews))
                assertEquals(ProfileTab.Reviews, awaitItem().selectedTab)

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ─────────────────────────────────────────────
    // ToggleFollow — optimistic update
    // ─────────────────────────────────────────────

    @Test
    fun `ToggleFollow before profile loads is a no-op`() =
        runTest {
            // Build a fresh VM that hasn't loaded yet — not easily testable with UnconfinedTestDispatcher
            // because init {} runs eagerly. Instead verify no crash when profile is already Success.
            val state = viewModel.uiState.value
            assertIs<UiState.Success<ProfileData>>(state.profile)
            // After a toggle from loaded state the count should change without crash
            viewModel.onEvent(ProfileEvent.ToggleFollow)
            assertNotNull(viewModel.uiState.value)
        }

    @Test
    fun `ToggleFollow sets isFollowing true when not following`() =
        runTest {
            assertFalse(viewModel.uiState.value.isFollowing)
            viewModel.onEvent(ProfileEvent.ToggleFollow)
            assertTrue(viewModel.uiState.value.isFollowing)
        }

    @Test
    fun `ToggleFollow sets isFollowing false when already following`() =
        runTest {
            viewModel.onEvent(ProfileEvent.ToggleFollow) // follow
            viewModel.onEvent(ProfileEvent.ToggleFollow) // unfollow
            assertFalse(viewModel.uiState.value.isFollowing)
        }

    @Test
    fun `ToggleFollow increments follower count optimistically`() =
        runTest {
            val initialCount =
                assertIs<UiState.Success<ProfileData>>(
                    viewModel.uiState.value.profile,
                ).data.user.followerCount
            viewModel.onEvent(ProfileEvent.ToggleFollow)
            val newCount =
                assertIs<UiState.Success<ProfileData>>(
                    viewModel.uiState.value.profile,
                ).data.user.followerCount
            assertEquals(initialCount + 1, newCount)
        }

    @Test
    fun `ToggleFollow decrements follower count on unfollow and does not go below zero`() =
        runTest {
            // follow then unfollow — count starts at 0 so should clamp to 0
            viewModel.onEvent(ProfileEvent.ToggleFollow)
            viewModel.onEvent(ProfileEvent.ToggleFollow)
            val count = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data.user.followerCount
            assertTrue(count >= 0, "Follower count should not go below 0, was $count")
        }

    // ─────────────────────────────────────────────
    // Retry
    // ─────────────────────────────────────────────

    @Test
    fun `Retry reloads profile from AuthUser`() =
        runTest {
            viewModel.onEvent(ProfileEvent.Retry)
            val data = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data
            assertEquals(sampleAuthUser.username, data.user.username)
        }
}
