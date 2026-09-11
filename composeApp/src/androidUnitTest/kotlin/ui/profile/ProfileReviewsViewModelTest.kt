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
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Review
import org.scent.project.domain.repository.ReviewRepository
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.base.UiState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileReviewsViewModelTest {
    private val reviewRepository = mockk<ReviewRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val reviewsFlow = MutableSharedFlow<Result<List<Review>>>(replay = 1)

    private fun review(id: Int) =
        Review(id = id, fragrance = Fragrance(id = id, name = "F$id", brand = "Brand"), rating = 5)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { reviewRepository.getUserReviewsFlow(1) } returns reviewsFlow
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState transitions to Success once refreshUserReviews resolves and the Flow emits`() =
        runTest {
            coEvery { reviewRepository.refreshUserReviews(1) } coAnswers {
                reviewsFlow.emit(listOf(review(1), review(2)).asRight())
                Unit.asRight()
            }

            val viewModel = ProfileReviewsViewModel(userId = 1, reviewRepository = reviewRepository)

            val state = viewModel.uiState.value
            assertIs<UiState.Success<List<Review>>>(state)
            assertEquals(2, state.data.size)
        }

    @Test
    fun `uiState surfaces an error when refreshUserReviews fails`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()
            coEvery { reviewRepository.refreshUserReviews(1) } returns error.asLeft()

            val viewModel = ProfileReviewsViewModel(userId = 1, reviewRepository = reviewRepository)

            val state = viewModel.uiState.value
            assertIs<UiState.Error>(state)
            assertEquals(error, state.error)
        }
}
