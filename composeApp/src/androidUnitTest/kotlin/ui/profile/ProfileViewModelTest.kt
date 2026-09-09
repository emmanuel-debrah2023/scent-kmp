package ui.profile

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.CollectionStatus
import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.User
import org.scent.project.domain.repository.UserRepository
import org.scent.project.domain.usecase.GetUserLikesUseCase
import org.scent.project.domain.usecase.GetUserWishlistUseCase
import org.scent.project.domain.usecase.ToggleFollowResult
import org.scent.project.domain.usecase.ToggleFollowUseCase
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.base.UiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val userRepository = mockk<UserRepository>()
    private val toggleFollowUseCase = mockk<ToggleFollowUseCase>()
    private val getUserWishlist = mockk<GetUserWishlistUseCase>()
    private val getUserLikes = mockk<GetUserLikesUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()

    private val profileFlow = MutableSharedFlow<Result<User>>(replay = 1)

    private val sampleAuthUser =
        AuthUser(id = 1, username = "edebrah", displayName = "Emmanuel Debrah", email = "e@example.com", token = "tok")

    private fun testUser(id: Int = 1) =
        User(
            id = id,
            username = "edebrah",
            displayName = "Emmanuel Debrah",
            followerCount = 10,
            followingCount = 5,
            postCount = 2,
        )

    private fun viewModel(): ProfileViewModel {
        every { userRepository.getProfileFlow(1) } returns profileFlow
        coEvery { userRepository.refreshProfile(1) } returns Unit.asRight()
        coEvery { getUserWishlist(1) } returns emptyList<CollectionEntry>().asRight()
        coEvery { getUserLikes(1) } returns emptyList<Post>().asRight()
        return ProfileViewModel(sampleAuthUser, userRepository, toggleFollowUseCase, getUserWishlist, getUserLikes)
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `profileState reflects the Flow once the repository resolves it`() =
        runTest {
            val viewModel = viewModel()
            profileFlow.emit(testUser().asRight())

            val state = viewModel.profileState.value
            assertIs<UiState.Success<User>>(state)
            assertEquals("Emmanuel Debrah", state.data.displayName)
        }

    @Test
    fun `profileState surfaces an error when the Flow emits Left`() =
        runTest {
            val error = AppError.NetworkError.NotFound(message = "User 1 is not cached")
            val viewModel = viewModel()
            profileFlow.emit(error.asLeft())

            val state = viewModel.profileState.value
            assertIs<UiState.Error>(state)
            assertEquals(error, state.error)
        }

    @Test
    fun `wishlistState and likesState load independently of the profile Flow`() =
        runTest {
            coEvery { userRepository.refreshProfile(1) } returns Unit.asRight()
            every { userRepository.getProfileFlow(1) } returns profileFlow
            val entry =
                CollectionEntry(
                    fragrance = Fragrance(id = 1, name = "Aventus", brand = "Creed"),
                    status = CollectionStatus.OWNS,
                )
            coEvery { getUserWishlist(1) } returns listOf(entry).asRight()
            coEvery { getUserLikes(1) } returns listOf(makePost("p1")).asRight()

            val viewModel =
                ProfileViewModel(sampleAuthUser, userRepository, toggleFollowUseCase, getUserWishlist, getUserLikes)

            val wishlistState = viewModel.wishlistState.value
            val likesState = viewModel.likesState.value
            assertIs<UiState.Success<List<CollectionEntry>>>(wishlistState)
            assertEquals(1, wishlistState.data.size)
            assertIs<UiState.Success<List<Post>>>(likesState)
            assertEquals(listOf("p1"), likesState.data.map { it.id })
        }

    @Test
    fun `toggleFollow updates the profile and isFollowing on success`() =
        runTest {
            val viewModel = viewModel()
            profileFlow.emit(testUser(id = 1).asRight())

            val updatedUser = testUser().copy(followerCount = 11)
            every { toggleFollowUseCase(any(), false) } returns
                ToggleFollowResult(user = updatedUser, isFollowing = true).asRight()

            viewModel.toggleFollow()

            assertTrue(viewModel.isFollowing.value)
            val state = viewModel.profileState.value as UiState.Success
            assertEquals(11, state.data.followerCount)
        }

    @Test
    fun `selectTab updates selectedTab`() =
        runTest {
            val viewModel = viewModel()

            viewModel.selectTab(ProfileTab.Reviews)

            assertEquals(ProfileTab.Reviews, viewModel.selectedTab.value)
        }

    @Test
    fun `retry re-invokes refreshProfile wishlist and likes`() =
        runTest {
            val viewModel = viewModel()

            viewModel.retry()

            coEvery { getUserWishlist(1) } returns emptyList<CollectionEntry>().asRight()
            coEvery { getUserLikes(1) } returns emptyList<Post>().asRight()
            assertIs<UiState.Success<List<CollectionEntry>>>(viewModel.wishlistState.value)
            assertIs<UiState.Success<List<Post>>>(viewModel.likesState.value)
        }

    private fun makePost(id: String) =
        Post(
            id = id,
            userId = "1",
            contentFormat = ContentFormat.TEXT,
            textContent = "post $id",
            fragranceIds = emptyList(),
            createdAt = 0L,
        )
}
