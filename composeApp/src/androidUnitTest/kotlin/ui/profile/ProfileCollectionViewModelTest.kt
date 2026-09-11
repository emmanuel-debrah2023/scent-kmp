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
import org.scent.project.domain.model.CollectionEntry
import org.scent.project.domain.model.CollectionStatus
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.repository.CollectionRepository
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
class ProfileCollectionViewModelTest {
    private val collectionRepository = mockk<CollectionRepository>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val collectionFlow = MutableSharedFlow<Result<List<CollectionEntry>>>(replay = 1)

    private fun entry(id: Int) =
        CollectionEntry(fragrance = Fragrance(id = id, name = "F$id", brand = "Brand"), status = CollectionStatus.OWNS)

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { collectionRepository.getUserCollectionFlow(1) } returns collectionFlow
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uiState transitions to Success once refreshUserCollection resolves and the Flow emits`() =
        runTest {
            coEvery { collectionRepository.refreshUserCollection(1) } coAnswers {
                collectionFlow.emit(listOf(entry(1), entry(2)).asRight())
                Unit.asRight()
            }

            val viewModel = ProfileCollectionViewModel(userId = 1, collectionRepository = collectionRepository)

            val state = viewModel.uiState.value
            assertIs<UiState.Success<List<CollectionEntry>>>(state)
            assertEquals(2, state.data.size)
        }

    @Test
    fun `uiState surfaces an error when refreshUserCollection fails`() =
        runTest {
            val error = AppError.NetworkError.NoConnection()
            coEvery { collectionRepository.refreshUserCollection(1) } returns error.asLeft()

            val viewModel = ProfileCollectionViewModel(userId = 1, collectionRepository = collectionRepository)

            val state = viewModel.uiState.value
            assertIs<UiState.Error>(state)
            assertEquals(error, state.error)
        }

    @Test
    fun `uiState resolves once ready even if Room already had a cached value before refresh`() =
        runTest {
            // Seed the Flow with a cached value before the refresh resolves — matches
            // Room's "immediate cache read" behavior on collection start.
            collectionFlow.emit(listOf(entry(99)).asRight())

            coEvery { collectionRepository.refreshUserCollection(1) } returns Unit.asRight()

            val viewModel = ProfileCollectionViewModel(userId = 1, collectionRepository = collectionRepository)

            val state = viewModel.uiState.value
            assertIs<UiState.Success<List<CollectionEntry>>>(state)
            assertEquals(listOf(99), state.data.map { it.fragrance.id })
        }

    @Test
    fun `uiState surfaces an error when the Flow itself emits Left after ready`() =
        runTest {
            val error = AppError.Unknown()
            coEvery { collectionRepository.refreshUserCollection(1) } coAnswers {
                collectionFlow.emit(error.asLeft())
                Unit.asRight()
            }

            val viewModel = ProfileCollectionViewModel(userId = 1, collectionRepository = collectionRepository)

            val state = viewModel.uiState.value
            assertIs<UiState.Error>(state)
            assertEquals(error, state.error)
        }
}
