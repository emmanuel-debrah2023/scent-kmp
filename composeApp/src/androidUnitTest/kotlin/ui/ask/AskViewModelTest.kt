package ui.ask

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.serialization.json.JsonPrimitive
import org.scent.project.data.mapper.A2uiMessageMapper
import org.scent.project.domain.error.AppError
import org.scent.project.domain.repository.AskSurfaceRepository
import org.scent.project.domain.surface.SurfaceAction
import org.scent.project.domain.surface.SurfaceMessage
import org.scent.project.domain.usecase.SendAskActionUseCase
import org.scent.project.domain.usecase.StartAskUseCase
import org.scent.project.domain.util.Result
import org.scent.project.domain.util.asLeft
import org.scent.project.domain.util.asRight
import ui.surface.ASK_SURFACE_ID
import ui.surface.AskFixtures
import ui.surface.SurfaceEvent
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AskViewModelTest {
    private val startAsk = mockk<StartAskUseCase>()
    private val sendAskAction = mockk<SendAskActionUseCase>()
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: AskViewModel

    private fun batch(json: String): List<SurfaceMessage> =
        requireNotNull(A2uiMessageMapper.decodeBatch(json).getOrNull())

    private val questions = batch(AskFixtures.QUESTIONS)
    private val results = batch(AskFixtures.RESULTS)

    private fun showing() = assertIs<AskPhase.Showing>(viewModel.uiState.value.phase)

    private fun answerEvent() = SurfaceEvent.ActionTriggered(ASK_SURFACE_ID, "submit")

    private fun editEvent() = SurfaceEvent.ActionTriggered(ASK_SURFACE_ID, "summary")

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AskViewModel(startAsk, sendAskAction, timestamp = { "2026-10-20T10:00:00Z" })
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun askAndShowQuestions() {
        coEvery { startAsk("a gift for my dad") } returns questions.asRight()
        viewModel.onQueryChange("a gift for my dad")
        viewModel.submit()
    }

    @Test
    fun `submit shows the surface the server composed`() {
        askAndShowQuestions()

        assertEquals(ASK_SURFACE_ID, showing().surface.id)
        assertNull(showing().pending)
    }

    @Test
    fun `a failed ask lands on Failed and keeps the query`() {
        coEvery { startAsk(any()) } returns AppError.NetworkError.NoConnection().asLeft()
        viewModel.onQueryChange("a gift for my dad")

        viewModel.submit()

        assertIs<AppError.NetworkError.NoConnection>(assertIs<AskPhase.Failed>(viewModel.uiState.value.phase).error)
        assertEquals("a gift for my dad", viewModel.uiState.value.query)
    }

    @Test
    fun `a rejected batch lands on Failed`() {
        // updateComponents for a surface that was never created is rejected whole.
        coEvery { startAsk(any()) } returns questions.drop(1).asRight()
        viewModel.onQueryChange("a gift for my dad")

        viewModel.submit()

        assertIs<AppError.SurfaceError.Invalid>(assertIs<AskPhase.Failed>(viewModel.uiState.value.phase).error)
    }

    @Test
    fun `an invalid query sets queryError and never reaches the repository`() {
        val repository = mockk<AskSurfaceRepository>()
        viewModel = AskViewModel(StartAskUseCase(repository), sendAskAction, timestamp = { "now" })
        viewModel.onQueryChange("   ")

        viewModel.submit()

        assertIs<AppError.ValidationError.RequiredFieldEmpty>(viewModel.uiState.value.queryError)
        assertEquals(AskPhase.Entry, viewModel.uiState.value.phase)
        coVerify(exactly = 0) { repository.ask(any()) }
    }

    @Test
    fun `editing the query clears its error`() {
        val repository = mockk<AskSurfaceRepository>()
        viewModel = AskViewModel(StartAskUseCase(repository), sendAskAction, timestamp = { "now" })
        viewModel.submit()

        viewModel.onQueryChange("a gift")

        assertNull(viewModel.uiState.value.queryError)
    }

    @Test
    fun `asking again replaces the previous surface in one step`() {
        askAndShowQuestions()
        val secondId = "ask-second"
        coEvery { startAsk("something fresh") } returns
            batch(AskFixtures.QUESTIONS.replace(ASK_SURFACE_ID, secondId)).asRight()
        viewModel.onQueryChange("something fresh")

        viewModel.submit()

        assertEquals(secondId, showing().surface.id)
    }

    @Test
    fun `asking again with the same surface id still applies because the old one is deleted first`() {
        askAndShowQuestions()
        coEvery { startAsk("again") } returns questions.asRight()
        viewModel.onQueryChange("again")

        viewModel.submit()

        assertEquals(ASK_SURFACE_ID, showing().surface.id)
    }

    @Test
    fun `a value change edits the surface data`() {
        askAndShowQuestions()

        viewModel.onSurfaceEvent(SurfaceEvent.ValueChanged(ASK_SURFACE_ID, "/answers/family", JsonPrimitive("FRESH")))

        val data = showing().surface.data.toString()
        assertTrue(""""family":"FRESH"""" in data, data)
    }

    @Test
    fun `answer_questions sends the edited answers and applies the reply`() {
        askAndShowQuestions()
        viewModel.onSurfaceEvent(SurfaceEvent.ValueChanged(ASK_SURFACE_ID, "/answers/family", JsonPrimitive("FRESH")))
        val sent = slot<SurfaceAction>()
        coEvery { sendAskAction(capture(sent)) } returns results.asRight()

        viewModel.onSurfaceEvent(answerEvent())

        assertEquals("answer_questions", sent.captured.name)
        assertEquals("2026-10-20T10:00:00Z", sent.captured.timestamp)
        assertEquals(JsonPrimitive("FRESH"), sent.captured.context["family"])
        assertTrue("Cedar & Smoke" in showing().surface.data.toString())
        assertNull(showing().pending)
    }

    @Test
    fun `edit_answers sends the action and shows the reply`() {
        askAndShowQuestions()
        coEvery { sendAskAction(any()) } returns results.asRight()
        viewModel.onSurfaceEvent(answerEvent())
        val sent = slot<SurfaceAction>()
        coEvery { sendAskAction(capture(sent)) } returns questions.drop(1).asRight()

        viewModel.onSurfaceEvent(editEvent())

        assertEquals("edit_answers", sent.captured.name)
        assertNull(showing().pending)
        assertNull(showing().actionError)
    }

    @Test
    fun `the pending kind follows the action`() =
        runTest {
            askAndShowQuestions()
            val gate = CompletableDeferred<Result<List<SurfaceMessage>>>()
            coEvery { sendAskAction(any()) } coAnswers { gate.await() }

            viewModel.onSurfaceEvent(answerEvent())
            assertEquals(Pending.ANSWERING, showing().pending)
            gate.complete(results.asRight())
            assertNull(showing().pending)

            val editGate = CompletableDeferred<Result<List<SurfaceMessage>>>()
            coEvery { sendAskAction(any()) } coAnswers { editGate.await() }
            viewModel.onSurfaceEvent(editEvent())
            assertEquals(Pending.EDITING, showing().pending)
        }

    @Test
    fun `a second tap while an action is pending sends once and edits are ignored`() =
        runTest {
            askAndShowQuestions()
            val gate = CompletableDeferred<Result<List<SurfaceMessage>>>()
            coEvery { sendAskAction(any()) } coAnswers { gate.await() }

            viewModel.onSurfaceEvent(answerEvent())
            viewModel.onSurfaceEvent(answerEvent())
            viewModel.onSurfaceEvent(
                SurfaceEvent.ValueChanged(ASK_SURFACE_ID, "/answers/family", JsonPrimitive("FRESH")),
            )
            gate.complete(results.asRight())

            coVerify(exactly = 1) { sendAskAction(any()) }
            assertFalse("FRESH" in showing().surface.data.toString())
        }

    @Test
    fun `a failed action keeps the surface and answers and retry resends the same action`() {
        askAndShowQuestions()
        viewModel.onSurfaceEvent(SurfaceEvent.ValueChanged(ASK_SURFACE_ID, "/answers/family", JsonPrimitive("FRESH")))
        val sent = mutableListOf<SurfaceAction>()
        coEvery { sendAskAction(capture(sent)) } returns AppError.NetworkError.Timeout().asLeft()

        viewModel.onSurfaceEvent(answerEvent())

        assertIs<AppError.NetworkError.Timeout>(showing().actionError)
        assertNull(showing().pending)
        assertTrue(""""family":"FRESH"""" in showing().surface.data.toString())

        coEvery { sendAskAction(capture(sent)) } returns results.asRight()
        viewModel.retry()

        assertEquals(2, sent.size)
        assertEquals(sent[0], sent[1])
        assertNull(showing().actionError)
    }

    @Test
    fun `retry after a failed ask asks again with the kept query`() {
        coEvery { startAsk("a gift for my dad") } returns AppError.NetworkError.Timeout().asLeft()
        viewModel.onQueryChange("a gift for my dad")
        viewModel.submit()
        coEvery { startAsk("a gift for my dad") } returns questions.asRight()

        viewModel.retry()

        assertEquals(ASK_SURFACE_ID, showing().surface.id)
    }

    @Test
    fun `a component with no action sets actionError and keeps the surface without calling the server`() {
        askAndShowQuestions()
        val before = showing().surface

        viewModel.onSurfaceEvent(SurfaceEvent.ActionTriggered(ASK_SURFACE_ID, "heading"))

        assertIs<AppError.SurfaceError.Invalid>(showing().actionError)
        assertEquals(before, showing().surface)
        coVerify(exactly = 0) { sendAskAction(any()) }
    }

    @Test
    fun `asking shows Asking while the use case runs and Showing once it completes`() =
        runTest {
            val gate = CompletableDeferred<Result<List<SurfaceMessage>>>()
            coEvery { startAsk(any()) } coAnswers { gate.await() }
            viewModel.onQueryChange("a gift for my dad")

            viewModel.submit()
            assertEquals(AskPhase.Asking, viewModel.uiState.value.phase)
            gate.complete(questions.asRight())

            assertEquals(ASK_SURFACE_ID, showing().surface.id)
        }

    @Test
    fun `a failed re-ask from Showing replaces the surface with Failed and retry asks again`() {
        askAndShowQuestions()
        coEvery { startAsk("again") } returns AppError.NetworkError.NoConnection().asLeft()
        viewModel.onQueryChange("again")

        viewModel.submit()

        assertIs<AppError.NetworkError.NoConnection>(assertIs<AskPhase.Failed>(viewModel.uiState.value.phase).error)
        assertEquals("again", viewModel.uiState.value.query)
        coEvery { startAsk("again") } returns questions.asRight()
        viewModel.retry()
        assertEquals(ASK_SURFACE_ID, showing().surface.id)
    }

    @Test
    fun `an action reply the reducer rejects keeps the surface and sets actionError`() {
        askAndShowQuestions()
        val before = showing().surface
        val foreign = batch(AskFixtures.RESULTS.replace(ASK_SURFACE_ID, "ask-other"))
        coEvery { sendAskAction(any()) } returns foreign.asRight()

        viewModel.onSurfaceEvent(answerEvent())

        assertIs<AppError.SurfaceError.Invalid>(showing().actionError)
        assertEquals(before, showing().surface)
        assertNull(showing().pending)
    }

    @Test
    fun `startOver returns to the empty entry and keeps the query`() {
        askAndShowQuestions()

        viewModel.startOver()

        assertEquals(AskPhase.Entry, viewModel.uiState.value.phase)
        assertEquals("a gift for my dad", viewModel.uiState.value.query)
    }

    @Test
    fun `a round trip that runs long is flagged slow and the flag clears when it ends`() =
        runTest {
            val gate = CompletableDeferred<Result<List<SurfaceMessage>>>()
            coEvery { startAsk(any()) } coAnswers { gate.await() }
            viewModel.onQueryChange("a gift for my dad")

            viewModel.submit()
            advanceTimeBy(ASK_SLOW_AFTER_MS - 1)
            assertFalse(viewModel.uiState.value.isSlow)
            advanceTimeBy(2)
            assertTrue(viewModel.uiState.value.isSlow)
            gate.complete(questions.asRight())

            assertFalse(viewModel.uiState.value.isSlow)
        }
}
