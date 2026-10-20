package ui.ask

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import org.scent.project.data.mapper.A2uiMessageMapper
import org.scent.project.domain.error.AppError
import org.scent.project.domain.surface.Surface
import org.scent.project.domain.surface.SurfaceReducer
import org.scent.project.domain.surface.SurfaceState
import ui.surface.ASK_SURFACE_ID
import ui.surface.AskFixtures
import ui.surface.SurfaceEvent
import kotlin.test.Test
import kotlin.test.assertEquals

/** Shared by both platforms; Android hosts it through Robolectric in a thin subclass. */
@OptIn(ExperimentalTestApi::class)
abstract class AskContentTest {
    private fun surfaceAfter(vararg batches: String): Surface {
        var state = SurfaceState()
        batches.forEach { batch ->
            val messages = requireNotNull(A2uiMessageMapper.decodeBatch(batch).getOrNull())
            state = SurfaceReducer.apply(state, messages).state
        }
        return state.surfaces.getValue(ASK_SURFACE_ID)
    }

    private val questions get() = surfaceAfter(AskFixtures.QUESTIONS)
    private val results get() = surfaceAfter(AskFixtures.QUESTIONS, AskFixtures.RESULTS)

    private fun liveRegion(mode: LiveRegionMode) = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, mode)

    @Test
    fun `entry shows the labelled field with its helper and the pane title`() =
        runComposeUiTest {
            setContent { MaterialTheme { AskContent(AskUiState().toUiModel(), AskActions.noOp()) } }

            onNodeWithContentDescription("What are you looking for?").assertExists()
            onNodeWithText("Up to 500 characters").assertExists()
            onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Ask Scent")).assertExists()
        }

    @Test
    fun `the ask button reports a submit`() =
        runComposeUiTest {
            var submits = 0
            val actions = AskActions.noOp().copy(onSubmit = { submits++ })
            setContent { MaterialTheme { AskContent(AskUiState(query = "a gift").toUiModel(), actions) } }

            onNodeWithText("ASK").performClick()

            assertEquals(1, submits)
        }

    @Test
    fun `asking shows a polite status and locks the ask button`() =
        runComposeUiTest {
            setContent {
                MaterialTheme { AskContent(AskUiState(phase = AskPhase.Asking).toUiModel(), AskActions.noOp()) }
            }

            onNodeWithText("Finding your questions…").assert(liveRegion(LiveRegionMode.Polite))
            onNodeWithText("ASK").assertIsNotEnabled()
        }

    @Test
    fun `a slow round trip adds the twenty second note`() =
        runComposeUiTest {
            val state = AskUiState(phase = AskPhase.Asking, isSlow = true)
            setContent { MaterialTheme { AskContent(state.toUiModel(), AskActions.noOp()) } }

            onNodeWithText("This can take up to 20 seconds.").assertExists()
        }

    @Test
    fun `questions render with a ready status and report taps`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()
            val actions = AskActions.noOp().copy(onSurfaceEvent = events::add)
            val state = AskUiState(phase = AskPhase.Showing(questions))
            setContent { MaterialTheme { AskContent(state.toUiModel(), actions) } }

            onNodeWithText("Three quick picks and we'll find him something").assertExists()
            onNodeWithText("Questions ready").assert(liveRegion(LiveRegionMode.Polite))
            onNodeWithText("SHOW GIFTS").performScrollTo().performClick()

            assertEquals(listOf<SurfaceEvent>(SurfaceEvent.ActionTriggered(ASK_SURFACE_ID, "submit")), events)
        }

    @Test
    fun `results announce how many picks were found`() =
        runComposeUiTest {
            val state = AskUiState(phase = AskPhase.Showing(results))
            setContent { MaterialTheme { AskContent(state.toUiModel(), AskActions.noOp()) } }

            onNodeWithText("3 picks found").assert(liveRegion(LiveRegionMode.Polite))
            onNodeWithText("Cedar & Smoke", useUnmergedTree = true).assertExists()
        }

    @Test
    fun `a failed ask shows an assertive error with working retry and start over`() =
        runComposeUiTest {
            var retries = 0
            var startOvers = 0
            val actions = AskActions.noOp().copy(onRetry = { retries++ }, onStartOver = { startOvers++ })
            val state = AskUiState(phase = AskPhase.Failed(AppError.NetworkError.NoConnection()))
            setContent { MaterialTheme { AskContent(state.toUiModel(), actions) } }

            onNode(hasText("You are offline", substring = true) and liveRegion(LiveRegionMode.Assertive))
                .assertExists()
            onNodeWithText("Retry").performScrollTo().performClick()
            onNodeWithText("Start over").performScrollTo().performClick()

            assertEquals(1, retries)
            assertEquals(1, startOvers)
        }

    @Test
    fun `an action failure keeps the surface and offers retry`() =
        runComposeUiTest {
            var retries = 0
            val actions = AskActions.noOp().copy(onRetry = { retries++ })
            val phase = AskPhase.Showing(questions, actionError = AppError.NetworkError.Timeout())
            setContent { MaterialTheme { AskContent(AskUiState(phase = phase).toUiModel(), actions) } }

            onNodeWithText("Three quick picks and we'll find him something").assertExists()
            onNodeWithText("Retry").performScrollTo().performClick()

            assertEquals(1, retries)
        }

    @Test
    fun `a field error shows instead of the helper`() =
        runComposeUiTest {
            val state = AskUiState(queryError = AppError.ValidationError.RequiredFieldEmpty("query"))
            setContent { MaterialTheme { AskContent(state.toUiModel(), AskActions.noOp()) } }

            onNodeWithText("Tell us what you are looking for").assertExists()
        }
}
