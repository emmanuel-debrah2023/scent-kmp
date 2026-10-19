package ui.surface

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.scent.project.domain.surface.ActionTemplate
import org.scent.project.domain.surface.Surface
import org.scent.project.domain.surface.SurfaceNode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The cases, shared by both platforms. Android needs Robolectric to host `runComposeUiTest` and
 * `@RunWith` cannot sit in commonTest, so each platform runs these through a thin subclass.
 */
@OptIn(ExperimentalTestApi::class)
abstract class ScentSurfaceTest {
    private fun surfaceOf(vararg nodes: SurfaceNode) =
        Surface("s1", "catalog", nodes.associateBy { it.id }, JsonObject(emptyMap()))

    private fun withData(
        data: String,
        vararg nodes: SurfaceNode,
    ) = Surface("s1", "catalog", nodes.associateBy { it.id }, Json.parseToJsonElement(data))

    private val picker = SurfaceNode.ChoicePicker("family", "Scent family", "/options/family", "/answers/family")

    private val pickerData =
        """{"options":{"family":[{"value":"FRESH","label":"Fresh"},{"value":"WOODY","label":"Woody"}]},""" +
            """"answers":{"family":"WOODY"}}"""

    private val slider =
        SurfaceNode.RangeSlider("budget", "Budget", "/budget/min", "/budget/max", "/budget/step", "/answers/budget")

    // Five stops: 30, 35, 40, 45, 50.
    private fun sliderData(stored: String?) =
        """{"budget":{"min":30,"max":50,"step":5}""" + (stored?.let { ""","answers":{"budget":$it}""" } ?: "") + "}"

    // ScentPrimaryButton upper-cases its label.
    private val submitText = "SHOW PICKS"

    private val submit = SurfaceNode.Button("submit", "Show picks", ActionTemplate("answer_questions", emptyMap()))

    @Test
    fun `column renders its children in order`() =
        runComposeUiTest {
            val surface =
                surfaceOf(
                    SurfaceNode.Column("root", listOf("heading", "body")),
                    SurfaceNode.Text("heading", "A few quick questions", isHeading = true),
                    SurfaceNode.Text("body", "Pick a family", isHeading = false),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithText("A few quick questions").assertExists()
            onNodeWithText("Pick a family").assertExists()
        }

    @Test
    fun `heading text is marked as a heading and body text is not`() =
        runComposeUiTest {
            val surface =
                surfaceOf(
                    SurfaceNode.Column("root", listOf("heading", "body")),
                    SurfaceNode.Text("heading", "Title", isHeading = true),
                    SurfaceNode.Text("body", "Body", isHeading = false),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithText("Title").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            onNodeWithText("Body").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
        }

    @Test
    fun `tapping a button emits ActionTriggered once with the surface and component ids`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()
            val surface = surfaceOf(SurfaceNode.Column("root", listOf("submit")), submit)

            setContent { MaterialTheme { ScentSurface(surface, onEvent = events::add) } }
            onNodeWithText(submitText).performClick()

            assertEquals(listOf<SurfaceEvent>(SurfaceEvent.ActionTriggered("s1", "submit")), events)
        }

    @Test
    fun `a button is at least 44dp tall`() =
        runComposeUiTest {
            val surface = surfaceOf(SurfaceNode.Column("root", listOf("submit")), submit)

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithText(submitText).assertHeightIsAtLeast(44.dp)
        }

    @Test
    fun `a button with an action is enabled`() =
        runComposeUiTest {
            val surface = surfaceOf(SurfaceNode.Column("root", listOf("submit")), submit)

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithText(submitText).assertIsEnabled()
        }

    @Test
    fun `a button without an action is disabled and emits nothing`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()
            val surface =
                surfaceOf(
                    SurfaceNode.Column("root", listOf("submit")),
                    SurfaceNode.Button("submit", "Show picks", action = null),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = events::add) } }
            onNodeWithText(submitText).assertIsNotEnabled().performClick()

            assertEquals(emptyList(), events)
        }

    @Test
    fun `an unsupported component renders the fallback`() =
        runComposeUiTest {
            val surface =
                surfaceOf(
                    SurfaceNode.Column("root", listOf("mystery")),
                    SurfaceNode.Unsupported("mystery"),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            assertEquals(1, fallbackCount())
        }

    @Test
    fun `a choice picker with no options renders the fallback`() =
        runComposeUiTest {
            val surface = withData("{}", SurfaceNode.Column("root", listOf("family")), picker)

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            assertEquals(1, fallbackCount())
        }

    // choice picker

    @Test
    fun `a choice picker shows its options with the stored answer on`() =
        runComposeUiTest {
            val surface = withData(pickerData, SurfaceNode.Column("root", listOf("family")), picker)

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithText("SCENT FAMILY").assertExists()
            onNodeWithContentDescription("Woody").assertIsOn()
            onNodeWithContentDescription("Fresh").assertIsOff()
        }

    @Test
    fun `picking an option writes its value to the answer path and sends no action`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()
            val surface = withData(pickerData, SurfaceNode.Column("root", listOf("family")), picker)

            setContent { MaterialTheme { ScentSurface(surface, onEvent = events::add) } }
            onNodeWithContentDescription("Fresh").performClick()

            assertEquals(
                listOf<SurfaceEvent>(SurfaceEvent.ValueChanged("s1", "/answers/family", JsonPrimitive("FRESH"))),
                events,
            )
        }

    // range slider

    @Test
    fun `a slider shows the stored amount and both ends in pounds`() =
        runComposeUiTest {
            val surface = withData(sliderData("40.0"), SurfaceNode.Column("root", listOf("budget")), slider)

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithText("up to £40").assertExists()
            onNodeWithText("£30").assertExists()
            onNodeWithText("£50").assertExists()
        }

    @Test
    fun `moving the slider writes the chosen amount to the answer path`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()
            val surface = withData(sliderData("40.0"), SurfaceNode.Column("root", listOf("budget")), slider)

            setContent { MaterialTheme { ScentSurface(surface, onEvent = events::add) } }
            onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                .performSemanticsAction(SemanticsActions.SetProgress) { it(3f) }

            assertEquals(
                listOf<SurfaceEvent>(SurfaceEvent.ValueChanged("s1", "/answers/budget", JsonPrimitive(45.0))),
                events,
            )
        }

    @Test
    fun `a slider with no stored amount writes its starting amount once`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()
            val surface = withData(sliderData(null), SurfaceNode.Column("root", listOf("budget")), slider)

            setContent { MaterialTheme { ScentSurface(surface, onEvent = events::add) } }
            waitForIdle()

            assertEquals(
                listOf<SurfaceEvent>(SurfaceEvent.ValueChanged("s1", "/answers/budget", JsonPrimitive(50.0))),
                events,
            )
        }

    @Test
    fun `a slider with a bad range renders the fallback`() =
        runComposeUiTest {
            val surface =
                withData(
                    """{"budget":{"min":50,"max":30,"step":5}}""",
                    SurfaceNode.Column("root", listOf("budget")),
                    slider,
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            assertEquals(1, fallbackCount())
        }

    // preference summary

    @Test
    fun `a summary shows the answers and Change asks for its action`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()
            val surface =
                withData(
                    """{"summary":["Woody","up to £80"]}""",
                    SurfaceNode.Column("root", listOf("summary")),
                    SurfaceNode.PreferenceSummary("summary", "/summary", ActionTemplate("edit_answers", emptyMap())),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = events::add) } }
            onNodeWithText("Woody · up to £80").assertExists()
            onNodeWithContentDescription("Change").performClick()

            assertEquals(listOf<SurfaceEvent>(SurfaceEvent.ActionTriggered("s1", "summary")), events)
        }

    // recommendation card

    @Test
    fun `a card is announced as its name then price then reason`() =
        runComposeUiTest {
            val surface =
                withData(
                    """{"results":[{"name":"Cedar & Smoke","price":"£76","why":"Dry wood"}]}""",
                    SurfaceNode.Column("root", listOf("card")),
                    SurfaceNode.RecommendationCard("card", "/results/0/name", "/results/0/price", "/results/0/why"),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithContentDescription("Cedar & Smoke, £76, Dry wood").assertExists()
        }

    @Test
    fun `a card with no name renders the fallback`() =
        runComposeUiTest {
            val surface =
                withData(
                    """{"results":[{}]}""",
                    SurfaceNode.Column("root", listOf("card")),
                    SurfaceNode.RecommendationCard("card", "/results/0/name", "/results/0/price", "/results/0/why"),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            assertEquals(1, fallbackCount())
        }

    // the whole ask flow, on a surface that answers its own edits

    @Test
    fun `answering the questions changes only local data and sends no action`() =
        runComposeUiTest {
            val fixture = SurfaceFixture(AskFixtures.QUESTIONS)
            val actions = mutableListOf<SurfaceEvent.ActionTriggered>()

            setContent {
                MaterialTheme { ScentSurfaceFixtureHost(fixture, ASK_SURFACE_ID, onAction = actions::add) }
            }
            onNodeWithContentDescription("Spicy").performClick()
            onNodeWithContentDescription("Decant set to try").performClick()

            onNodeWithContentDescription("Spicy").assertIsOn()
            onNodeWithContentDescription("Woody").assertIsOff()
            onNodeWithContentDescription("Decant set to try").assertIsOn()
            assertEquals(emptyList(), actions)
        }

    @Test
    fun `the Show gifts button asks for the answers action`() =
        runComposeUiTest {
            val fixture = SurfaceFixture(AskFixtures.QUESTIONS)
            val actions = mutableListOf<SurfaceEvent.ActionTriggered>()

            setContent {
                MaterialTheme { ScentSurfaceFixtureHost(fixture, ASK_SURFACE_ID, onAction = actions::add) }
            }
            onNodeWithText("SHOW GIFTS").performClick()

            assertEquals(listOf(SurfaceEvent.ActionTriggered(ASK_SURFACE_ID, "submit")), actions)
        }

    @Test
    fun `the results replace the questions on the same surface`() =
        runComposeUiTest {
            val fixture = SurfaceFixture(AskFixtures.QUESTIONS)

            setContent { MaterialTheme { ScentSurfaceFixtureHost(fixture, ASK_SURFACE_ID) } }
            runOnIdle { fixture.apply(AskFixtures.RESULTS) }

            onNodeWithText("Woody · up to £80 · Full bottle").assertExists()
            onNodeWithContentDescription(
                "Birch Ledger, £64, Smoky birch with a soft musk finish, easy to wear every day.",
            ).assertExists()
            onNodeWithText("SCENT FAMILY").assertDoesNotExist()
            assertEquals(0, fallbackCount())
        }

    @Test
    fun `a child id with no node renders the fallback`() =
        runComposeUiTest {
            val surface = surfaceOf(SurfaceNode.Column("root", listOf("ghost")))

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            assertEquals(1, fallbackCount())
        }

    @Test
    fun `a surface with no root yet renders nothing`() =
        runComposeUiTest {
            setContent { MaterialTheme { ScentSurface(surfaceOf(), onEvent = {}) } }

            assertEquals(0, fallbackCount())
        }

    @Test
    fun `a column that contains itself stops at the fallback instead of looping`() =
        runComposeUiTest {
            val surface = surfaceOf(SurfaceNode.Column("root", listOf("root")))

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            assertEquals(1, fallbackCount())
        }

    private fun androidx.compose.ui.test.ComposeUiTest.fallbackCount() =
        onAllNodesWithTag(SURFACE_FALLBACK_TAG).fetchSemanticsNodes().size
}
