package ui.profile

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.AuthUser
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.ContentFormat
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.Post
import org.scent.project.domain.model.Review
import org.scent.project.domain.usecase.DeleteListingUseCase
import org.scent.project.domain.usecase.GetMyListingsUseCase
import org.scent.project.domain.usecase.GetUserCollectionUseCase
import org.scent.project.domain.usecase.GetUserLikesUseCase
import org.scent.project.domain.usecase.GetUserPostsUseCase
import org.scent.project.domain.usecase.GetUserReviewsUseCase
import org.scent.project.domain.usecase.GetUserWishlistUseCase
import org.scent.project.domain.usecase.SetListingActiveUseCase
import org.scent.project.domain.usecase.ToggleFollowUseCase
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.base.UiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    private val getUserPosts: GetUserPostsUseCase = mockk()
    private val getUserCollection: GetUserCollectionUseCase = mockk()
    private val getUserWishlist: GetUserWishlistUseCase = mockk()
    private val getUserReviews: GetUserReviewsUseCase = mockk()
    private val getUserLikes: GetUserLikesUseCase = mockk()
    private val getMyListings: GetMyListingsUseCase = mockk()
    private val setListingActive: SetListingActiveUseCase = mockk()
    private val deleteListing: DeleteListingUseCase = mockk()

    private val sampleAuthUser =
        AuthUser(
            id = 1,
            username = "edebrah",
            displayName = "Emmanuel Debrah",
            email = "e@scent.dev",
            token = "test-token",
        )

    private fun sampleListing(
        id: Int = 1,
        isActive: Boolean = true,
    ) = Listing(
        id = id,
        fragrance = Fragrance(id = 10, name = "Aventus", brand = "Creed"),
        sellerId = sampleAuthUser.id,
        price = 185.0,
        condition = "90% full",
        isActive = isActive,
    )

    private fun createViewModel(): ProfileViewModel =
        ProfileViewModel(
            authUser = sampleAuthUser,
            toggleFollowUseCase = ToggleFollowUseCase(),
            getUserPosts = getUserPosts,
            getUserCollection = getUserCollection,
            getUserWishlist = getUserWishlist,
            getUserReviews = getUserReviews,
            getUserLikes = getUserLikes,
            getMyListings = getMyListings,
            setListingActiveUseCase = setListingActive,
            deleteListingUseCase = deleteListing,
        )

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getUserPosts(any()) } returns emptyList<Post>().asRight()
        coEvery { getUserCollection(any()) } returns emptyList<CollectionEntry>().asRight()
        coEvery { getUserWishlist(any()) } returns emptyList<CollectionEntry>().asRight()
        coEvery { getUserReviews(any()) } returns emptyList<Review>().asRight()
        coEvery { getUserLikes(any()) } returns emptyList<Post>().asRight()
        coEvery { getMyListings() } returns emptyList<Listing>().asRight()
        viewModel = createViewModel()
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
    fun `initial selected tab is Posts`() =
        runTest {
            assertEquals(ProfileTab.Posts, viewModel.uiState.value.selectedTab)
        }

    @Test
    fun `profile is in Success state after init`() =
        runTest {
            assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile)
        }

    // ─────────────────────────────────────────────
    // load — happy path
    // ─────────────────────────────────────────────

    @Test
    fun `load maps AuthUser fields onto User domain model`() =
        runTest {
            val data = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data
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
    fun `load populates posts collection wishlist reviews likes and listings from use cases`() =
        runTest {
            val post =
                Post(
                    id = "1",
                    userId = "1",
                    contentFormat = ContentFormat.TEXT,
                    fragranceIds = emptyList(),
                    createdAt = 0L,
                )
            val review =
                Review(
                    id = 10,
                    fragrance = Fragrance(id = 1, name = "Test", brand = "Brand"),
                    rating = 4,
                )
            coEvery { getUserPosts(sampleAuthUser.id) } returns listOf(post).asRight()
            coEvery { getUserLikes(sampleAuthUser.id) } returns listOf(post).asRight()
            coEvery { getUserReviews(sampleAuthUser.id) } returns listOf(review).asRight()
            coEvery { getMyListings() } returns listOf(sampleListing()).asRight()

            // Re-create VM so init picks up the new stubs
            viewModel = createViewModel()

            val data = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data
            assertEquals(1, data.posts.size)
            assertEquals(1, data.likes.size)
            assertEquals(1, data.reviews.size)
            assertEquals(1, data.listings.size)
            assertTrue(data.collection.isEmpty())
            assertTrue(data.wishlist.isEmpty())
        }

    // ─────────────────────────────────────────────
    // load — error paths
    // ─────────────────────────────────────────────

    @Test
    fun `load still completes when one use case returns an error`() =
        runTest {
            coEvery { getUserPosts(any()) } returns AppError.NetworkError.ServerError(statusCode = 500).asLeft()

            viewModel = createViewModel()

            val data = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data
            assertNotNull(data.user)
            assertTrue(data.posts.isEmpty())
        }

    @Test
    fun `load uses empty list fallback when all use cases fail`() =
        runTest {
            val err: Result<List<Post>> = AppError.NetworkError.NoConnection().asLeft()
            val errCol: Result<List<CollectionEntry>> = AppError.NetworkError.NoConnection().asLeft()
            val errRev: Result<List<Review>> = AppError.NetworkError.NoConnection().asLeft()
            val errListings: Result<List<Listing>> = AppError.NetworkError.NoConnection().asLeft()
            coEvery { getUserPosts(any()) } returns err
            coEvery { getUserCollection(any()) } returns errCol
            coEvery { getUserWishlist(any()) } returns errCol
            coEvery { getUserReviews(any()) } returns errRev
            coEvery { getUserLikes(any()) } returns err
            coEvery { getMyListings() } returns errListings

            viewModel = createViewModel()

            val data = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data
            assertTrue(data.posts.isEmpty())
            assertTrue(data.collection.isEmpty())
            assertTrue(data.wishlist.isEmpty())
            assertTrue(data.reviews.isEmpty())
            assertTrue(data.likes.isEmpty())
            assertTrue(data.listings.isEmpty())
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
            val count =
                assertIs<UiState.Success<ProfileData>>(
                    viewModel.uiState.value.profile,
                ).data.user.followerCount
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

    @Test
    fun `Retry before any explicit load still shows profile from init`() =
        runTest {
            viewModel.uiState.test {
                awaitItem() // initial Success from init{}
                viewModel.onEvent(ProfileEvent.Retry)
                // Loading → Success emitted
                val loadingState = awaitItem()
                assertTrue(loadingState.profile is UiState.Loading)
                val successState = awaitItem()
                assertIs<UiState.Success<ProfileData>>(successState.profile)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ─────────────────────────────────────────────
    // UnlistListing / RelistListing
    // ─────────────────────────────────────────────

    @Test
    fun `UnlistListing updates the listing in place on success`() =
        runTest {
            coEvery { getMyListings() } returns listOf(sampleListing(id = 1, isActive = true)).asRight()
            viewModel = createViewModel()
            coEvery { setListingActive(1, false) } returns sampleListing(id = 1, isActive = false).asRight()

            viewModel.onEvent(ProfileEvent.UnlistListing(1))

            val data = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data
            assertFalse(data.listings.single { it.id == 1 }.isActive)
            assertNull(viewModel.uiState.value.actionInFlightId)
        }

    @Test
    fun `RelistListing updates the listing in place on success`() =
        runTest {
            coEvery { getMyListings() } returns listOf(sampleListing(id = 1, isActive = false)).asRight()
            viewModel = createViewModel()
            coEvery { setListingActive(1, true) } returns sampleListing(id = 1, isActive = true).asRight()

            viewModel.onEvent(ProfileEvent.RelistListing(1))

            val data = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data
            assertTrue(data.listings.single { it.id == 1 }.isActive)
        }

    @Test
    fun `UnlistListing sets actionError and clears actionInFlightId on failure`() =
        runTest {
            coEvery { getMyListings() } returns listOf(sampleListing(id = 1)).asRight()
            viewModel = createViewModel()
            coEvery { setListingActive(1, false) } returns AppError.AuthError.Forbidden().asLeft()

            viewModel.onEvent(ProfileEvent.UnlistListing(1))

            assertIs<AppError.AuthError.Forbidden>(viewModel.uiState.value.actionError)
            assertNull(viewModel.uiState.value.actionInFlightId)
            // The listing itself is untouched — a failed unlist must not silently flip state.
            val data = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data
            assertTrue(data.listings.single { it.id == 1 }.isActive)
        }

    @Test
    fun `UnlistListing does not emit on the BaseViewModel error SharedFlow`() =
        runTest {
            coEvery { getMyListings() } returns listOf(sampleListing(id = 1)).asRight()
            viewModel = createViewModel()
            coEvery { setListingActive(1, false) } returns AppError.AuthError.Forbidden().asLeft()

            viewModel.error.test {
                viewModel.onEvent(ProfileEvent.UnlistListing(1))
                expectNoEvents()
            }
        }

    // ─────────────────────────────────────────────
    // RequestDelete / ConfirmDelete / DismissConfirm
    // ─────────────────────────────────────────────

    @Test
    fun `RequestDelete sets pendingDeleteId without calling the use case`() =
        runTest {
            viewModel.onEvent(ProfileEvent.RequestDelete(1))

            assertEquals(1, viewModel.uiState.value.pendingDeleteId)
        }

    @Test
    fun `DismissConfirm clears pendingDeleteId`() =
        runTest {
            viewModel.onEvent(ProfileEvent.RequestDelete(1))
            viewModel.onEvent(ProfileEvent.DismissConfirm)

            assertNull(viewModel.uiState.value.pendingDeleteId)
        }

    @Test
    fun `ConfirmDelete removes the listing on success and clears pendingDeleteId`() =
        runTest {
            coEvery { getMyListings() } returns listOf(sampleListing(id = 1), sampleListing(id = 2)).asRight()
            viewModel = createViewModel()
            coEvery { deleteListing(1) } returns Unit.asRight()

            viewModel.onEvent(ProfileEvent.RequestDelete(1))
            viewModel.onEvent(ProfileEvent.ConfirmDelete)

            val data = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data
            assertEquals(listOf(2), data.listings.map { it.id })
            assertNull(viewModel.uiState.value.pendingDeleteId)
            assertNull(viewModel.uiState.value.actionInFlightId)
        }

    @Test
    fun `ConfirmDelete with no pending id does nothing`() =
        runTest {
            viewModel.onEvent(ProfileEvent.ConfirmDelete)

            assertNull(viewModel.uiState.value.actionInFlightId)
        }

    @Test
    fun `ConfirmDelete sets actionError and keeps the listing on failure`() =
        runTest {
            coEvery { getMyListings() } returns listOf(sampleListing(id = 1)).asRight()
            viewModel = createViewModel()
            coEvery { deleteListing(1) } returns AppError.NetworkError.NotFound().asLeft()

            viewModel.onEvent(ProfileEvent.RequestDelete(1))
            viewModel.onEvent(ProfileEvent.ConfirmDelete)

            assertIs<AppError.NetworkError.NotFound>(viewModel.uiState.value.actionError)
            val data = assertIs<UiState.Success<ProfileData>>(viewModel.uiState.value.profile).data
            assertEquals(listOf(1), data.listings.map { it.id })
        }
}
