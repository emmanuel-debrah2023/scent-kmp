package ui.profile

import app.cash.turbine.test
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.model.AuthUser
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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
        viewModel = ProfileViewModel()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ─────────────────────────────────────────────
    // Initial state
    // ─────────────────────────────────────────────

    @Test
    fun `initial state is loading with no user`() =
        runTest {
            val state = viewModel.uiState.value
            assertTrue(state.isLoading)
            assertNull(state.user)
        }

    @Test
    fun `initial selected tab is Posts`() =
        runTest {
            assertEquals(ProfileTab.Posts, viewModel.uiState.value.selectedTab)
        }

    // ─────────────────────────────────────────────
    // load
    // ─────────────────────────────────────────────

    @Test
    fun `load maps AuthUser fields onto User domain model`() =
        runTest {
            viewModel.load(sampleAuthUser, isOwnProfile = true)

            val user = viewModel.uiState.value.user
            assertNotNull(user)
            assertEquals(sampleAuthUser.id, user.id)
            assertEquals(sampleAuthUser.username, user.username)
            assertEquals(sampleAuthUser.displayName, user.displayName)
            assertEquals(sampleAuthUser.email, user.email)
        }

    @Test
    fun `load sets isOwnProfile flag correctly`() =
        runTest {
            viewModel.load(sampleAuthUser, isOwnProfile = true)
            assertTrue(viewModel.uiState.value.isOwnProfile)

            viewModel.load(sampleAuthUser, isOwnProfile = false)
            assertFalse(viewModel.uiState.value.isOwnProfile)
        }

    @Test
    fun `load clears loading flag after completion`() =
        runTest {
            viewModel.load(sampleAuthUser)
            assertFalse(viewModel.uiState.value.isLoading)
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
    fun `ToggleFollow with no user loaded is a no-op`() =
        runTest {
            viewModel.uiState.test {
                awaitItem() // initial state — no user
                viewModel.onEvent(ProfileEvent.ToggleFollow)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `ToggleFollow sets isFollowing true when not following`() =
        runTest {
            viewModel.load(sampleAuthUser, isOwnProfile = false)
            assertFalse(viewModel.uiState.value.isFollowing)

            viewModel.onEvent(ProfileEvent.ToggleFollow)

            assertTrue(viewModel.uiState.value.isFollowing)
        }

    @Test
    fun `ToggleFollow sets isFollowing false when already following`() =
        runTest {
            viewModel.load(sampleAuthUser, isOwnProfile = false)
            viewModel.onEvent(ProfileEvent.ToggleFollow) // follow
            viewModel.onEvent(ProfileEvent.ToggleFollow) // unfollow

            assertFalse(viewModel.uiState.value.isFollowing)
        }

    @Test
    fun `ToggleFollow increments follower count optimistically`() =
        runTest {
            viewModel.load(sampleAuthUser, isOwnProfile = false)
            val initialCount = viewModel.uiState.value.user!!.followerCount

            viewModel.onEvent(ProfileEvent.ToggleFollow)

            assertEquals(initialCount + 1, viewModel.uiState.value.user!!.followerCount)
        }

    @Test
    fun `ToggleFollow decrements follower count on unfollow and does not go below zero`() =
        runTest {
            viewModel.load(sampleAuthUser, isOwnProfile = false)
            // follow then unfollow — count starts at 0 so should clamp to 0
            viewModel.onEvent(ProfileEvent.ToggleFollow)
            viewModel.onEvent(ProfileEvent.ToggleFollow)

            val count = viewModel.uiState.value.user!!.followerCount
            assertTrue(count >= 0, "Follower count should not go below 0, was $count")
        }

    // ─────────────────────────────────────────────
    // Retry
    // ─────────────────────────────────────────────

    @Test
    fun `Retry reloads using last loaded AuthUser`() =
        runTest {
            viewModel.load(sampleAuthUser)
            // Simulate a state reset that might happen in error recovery
            viewModel.onEvent(ProfileEvent.Retry)

            val user = viewModel.uiState.value.user
            assertNotNull(user)
            assertEquals(sampleAuthUser.username, user.username)
        }

    @Test
    fun `Retry before any load is a no-op`() =
        runTest {
            viewModel.uiState.test {
                awaitItem() // initial state
                viewModel.onEvent(ProfileEvent.Retry)
                expectNoEvents()
                cancelAndIgnoreRemainingEvents()
            }
        }
}
