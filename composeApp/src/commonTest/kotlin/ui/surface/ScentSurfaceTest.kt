package ui.surface

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.scent.project.domain.surface.ActionTemplate
import org.scent.project.domain.surface.Surface
import org.scent.project.domain.surface.SurfaceNode
import org.scent.project.domain.surface.SurfaceProvenance
import org.scent.project.domain.surface.SurfaceProvenanceSubject
import org.scent.project.domain.surface.SurfaceTextStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
                    SurfaceNode.Text("heading", "A few quick questions", SurfaceTextStyle.HEADING),
                    SurfaceNode.Text("body", "Pick a family", SurfaceTextStyle.BODY),
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
                    SurfaceNode.Text("heading", "Title", SurfaceTextStyle.HEADING),
                    SurfaceNode.Text("body", "Body", SurfaceTextStyle.BODY),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithText("Title").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            onNodeWithText("Body").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
        }

    @Test
    fun `a caption shows its wording in the caption treatment and is not a heading`() =
        runComposeUiTest {
            val surface =
                surfaceOf(
                    SurfaceNode.Column("root", listOf("caption")),
                    SurfaceNode.Text("caption", "Question written by AI", SurfaceTextStyle.CAPTION),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithTag(SURFACE_CAPTION_TAG).assertExists()
            onNodeWithText("Question written by AI").assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Heading))
        }

    @Test
    fun `body text does not get the caption treatment`() =
        runComposeUiTest {
            val surface =
                surfaceOf(
                    SurfaceNode.Column("root", listOf("body")),
                    SurfaceNode.Text("body", "Pick a family", SurfaceTextStyle.BODY),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithTag(SURFACE_CAPTION_TAG).assertDoesNotExist()
        }

    @Test
    fun `a raised column draws its children in the raised section`() =
        runComposeUiTest {
            val surface =
                surfaceOf(
                    SurfaceNode.Column("root", listOf("questions")),
                    SurfaceNode.Column("questions", listOf("body"), raised = true),
                    SurfaceNode.Text("body", "Pick a family", SurfaceTextStyle.BODY),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithTag(SURFACE_RAISED_TAG).assertExists()
            onNodeWithText("Pick a family").assertExists()
        }

    @Test
    fun `a plain column has no raised section`() =
        runComposeUiTest {
            val surface =
                surfaceOf(
                    SurfaceNode.Column("root", listOf("body")),
                    SurfaceNode.Text("body", "Pick a family", SurfaceTextStyle.BODY),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithTag(SURFACE_RAISED_TAG).assertDoesNotExist()
        }

    @Test
    fun `a raised section keeps each child separately reachable and tappable`() =
        runComposeUiTest {
            val surface =
                surfaceOf(
                    SurfaceNode.Column("root", listOf("questions")),
                    SurfaceNode.Column("questions", listOf("body", "submit"), raised = true),
                    SurfaceNode.Text("body", "Pick a family", SurfaceTextStyle.BODY),
                    submit,
                )
            val events = mutableListOf<SurfaceEvent>()

            setContent { MaterialTheme { ScentSurface(surface, onEvent = { events += it }) } }

            onNodeWithText("Pick a family").assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            onNodeWithText(submitText).performClick()
            assertEquals(listOf<SurfaceEvent>(SurfaceEvent.ActionTriggered("s1", "submit")), events)
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
    fun `a card is announced as its name then price, then its reason`() =
        runComposeUiTest {
            val surface =
                withData(
                    """{"results":[{"name":"Cedar & Smoke","price":"£76","why":"Dry wood"}]}""",
                    SurfaceNode.Column("root", listOf("card")),
                    SurfaceNode.RecommendationCard(
                        "card",
                        "/results/0/name",
                        "/results/0/price",
                        "/results/0/why",
                        null,
                    ),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithContentDescription("Cedar & Smoke, £76. Dry wood").assertExists()
        }

    @Test
    fun `a card with a meta line is announced as its name then price then meta, then its reason`() =
        runComposeUiTest {
            val surface =
                withData(
                    """{"results":[{"name":"Cedar & Smoke","price":"£76","why":"Dry wood","meta":"Sealed · 100 ml"}]}""",
                    SurfaceNode.Column("root", listOf("card")),
                    SurfaceNode.RecommendationCard(
                        "card",
                        "/results/0/name",
                        "/results/0/price",
                        "/results/0/why",
                        null,
                        "/results/0/meta",
                    ),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithContentDescription("Cedar & Smoke, £76, Sealed, 100 ml. Dry wood").assertExists()
        }

    private val openListing = ActionTemplate("open_listing", mapOf("listingId" to "/results/0/listingId"))

    private fun cardWith(listingId: String?): Surface {
        val id = listingId?.let { ""","listingId":$it""" }.orEmpty()
        return withData(
            """{"results":[{"name":"Cedar & Smoke","price":"£76","why":"Dry wood"$id}]}""",
            SurfaceNode.Column("root", listOf("card")),
            SurfaceNode.RecommendationCard(
                "card",
                "/results/0/name",
                "/results/0/price",
                "/results/0/why",
                openListing,
            ),
        )
    }

    @Test
    fun `a card with a valid listing id can be tapped and reports the card once`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()

            setContent { MaterialTheme { ScentSurface(cardWith("7"), onEvent = events::add) } }
            onNodeWithContentDescription("Cedar & Smoke, £76. Dry wood").assertHasClickAction().performClick()

            assertEquals(listOf<SurfaceEvent>(SurfaceEvent.ActionTriggered("s1", "card")), events)
        }

    @Test
    fun `a card whose listing id is missing or invalid has no click action`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()
            val surface = mutableStateOf(cardWith(null))

            setContent { MaterialTheme { ScentSurface(surface.value, onEvent = events::add) } }

            listOf(null, "\"7\"", "0", "-1", "1.5").forEach { bad ->
                surface.value = cardWith(bad)
                waitForIdle()
                onNodeWithContentDescription("Cedar & Smoke, £76. Dry wood")
                    .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
            }
            assertEquals(emptyList(), events)
        }

    @Test
    fun `a card with no name renders the fallback`() =
        runComposeUiTest {
            val surface =
                withData(
                    """{"results":[{}]}""",
                    SurfaceNode.Column("root", listOf("card")),
                    SurfaceNode.RecommendationCard(
                        "card",
                        "/results/0/name",
                        "/results/0/price",
                        "/results/0/why",
                        null,
                    ),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            assertEquals(1, fallbackCount())
        }

    @Test
    fun `a card with several reasons announces each after the facts`() =
        runComposeUiTest {
            val surface =
                withData(
                    """{"results":[{"name":"Light Blue","price":"£7","reasons":["Bright citrus","Easy to wear"]}]}""",
                    SurfaceNode.Column("root", listOf("card")),
                    SurfaceNode.RecommendationCard(
                        "card",
                        "/results/0/name",
                        "/results/0/price",
                        "/results/0/reasons",
                        null,
                    ),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithContentDescription("Light Blue, £7. Bright citrus. Easy to wear").assertExists()
        }

    // provenance note

    private val rateAction = ActionTemplate("rate_reasons", mapOf("rating" to "/feedback/rating"))

    private fun noteSurface(
        source: SurfaceProvenance,
        action: ActionTemplate? = null,
        data: String = "{}",
        subject: SurfaceProvenanceSubject = SurfaceProvenanceSubject.REASONS,
    ) = withData(
        data,
        SurfaceNode.Column("root", listOf("note")),
        SurfaceNode.ProvenanceNote("note", source, subject, action, "/feedback/rating"),
    )

    @Test
    fun `an omitted node draws nothing, not even the fallback`() =
        runComposeUiTest {
            val surface =
                surfaceOf(
                    SurfaceNode.Column("root", listOf("note", "body")),
                    SurfaceNode.Omitted("note"),
                    SurfaceNode.Text("body", "Pick a family", SurfaceTextStyle.BODY),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            onNodeWithText("Pick a family").assertExists()
            assertEquals(0, fallbackCount())
        }

    @Test
    fun `an AI-written reasons note says who wrote them and offers both thumbs`() =
        runComposeUiTest {
            setContent {
                MaterialTheme { ScentSurface(noteSurface(SurfaceProvenance.AI_WRITTEN, rateAction), onEvent = {}) }
            }

            onNodeWithText("Reasons written by AI").assertExists()
            onNodeWithText("Prices and stock come from Scent").assertExists()
            onNodeWithContentDescription("These reasons helped")
                .assertIsOff()
                .assertHeightIsAtLeast(44.dp)
            onNodeWithContentDescription("These reasons did not help").assertIsOff()
        }

    @Test
    fun `tapping a thumb stores the rating, then sends the note's action`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()

            setContent {
                MaterialTheme {
                    ScentSurface(noteSurface(SurfaceProvenance.AI_WRITTEN, rateAction), onEvent = events::add)
                }
            }
            onNodeWithContentDescription("These reasons did not help").performClick()

            assertEquals(
                listOf(
                    SurfaceEvent.ValueChanged("s1", "/feedback/rating", JsonPrimitive("down")),
                    SurfaceEvent.ActionTriggered("s1", "note"),
                ),
                events,
            )
        }

    @Test
    fun `a stored rating shows its thumb as chosen and tapping it again sends nothing`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()
            val surface = noteSurface(SurfaceProvenance.AI_WRITTEN, rateAction, """{"feedback":{"rating":"up"}}""")

            setContent { MaterialTheme { ScentSurface(surface, onEvent = events::add) } }
            onNodeWithContentDescription("These reasons helped").assertIsOn().performClick()

            onNodeWithContentDescription("These reasons did not help").assertIsOff()
            assertEquals(emptyList(), events)
        }

    @Test
    fun `an AI-written note without a rating action offers no thumbs`() =
        runComposeUiTest {
            setContent { MaterialTheme { ScentSurface(noteSurface(SurfaceProvenance.AI_WRITTEN), onEvent = {}) } }

            onNodeWithText("Reasons written by AI").assertExists()
            onNodeWithContentDescription("These reasons helped").assertDoesNotExist()
        }

    @Test
    fun `a paused note is announced politely and has nothing to tap`() =
        runComposeUiTest {
            setContent { MaterialTheme { ScentSurface(noteSurface(SurfaceProvenance.AI_PAUSED), onEvent = {}) } }

            onNode(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
                .assertExists()
            onNodeWithText("AI help paused until tomorrow").assertExists()
            onNodeWithContentDescription("Try again").assertDoesNotExist()
            onNodeWithContentDescription("These reasons helped").assertDoesNotExist()
        }

    @Test
    fun `an unavailable note's Try again reports the note once`() =
        runComposeUiTest {
            val events = mutableListOf<SurfaceEvent>()
            val retry = ActionTemplate("retry_ask", mapOf("family" to "/answers/family"))

            setContent {
                MaterialTheme {
                    ScentSurface(noteSurface(SurfaceProvenance.AI_UNAVAILABLE, retry), onEvent = events::add)
                }
            }
            onNode(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
                .assertExists()
            onNodeWithText("Matched on fragrance notes").assertExists()
            onNodeWithContentDescription("Try again").assertHeightIsAtLeast(44.dp).performClick()

            assertEquals(listOf<SurfaceEvent>(SurfaceEvent.ActionTriggered("s1", "note")), events)
        }

    private fun androidx.compose.ui.test.ComposeUiTest.retryAtFontScale(fontScale: Float): Pair<Float, Float> {
        val retry = ActionTemplate("retry_ask", mapOf("family" to "/answers/family"))
        setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme { ScentSurface(noteSurface(SurfaceProvenance.AI_UNAVAILABLE, retry), onEvent = {}) }
            }
        }
        val detailBottom = onNodeWithText("AI couldn’t answer this time").getUnclippedBoundsInRoot().bottom
        val actionTop = onNodeWithContentDescription("Try again").getUnclippedBoundsInRoot().top
        return detailBottom.value to actionTop.value
    }

    @Test
    fun `at normal text size Try again sits on the detail line`() =
        runComposeUiTest {
            val (detailBottom, actionTop) = retryAtFontScale(1f)

            assertTrue(actionTop < detailBottom, "action top $actionTop, detail bottom $detailBottom")
        }

    @Test
    fun `at large text size Try again moves below the detail line`() =
        runComposeUiTest {
            val (detailBottom, actionTop) = retryAtFontScale(1.8f)

            assertTrue(actionTop >= detailBottom, "action top $actionTop, detail bottom $detailBottom")
        }

    @Test
    fun `an AI-written note is not a live region`() =
        runComposeUiTest {
            setContent {
                MaterialTheme { ScentSurface(noteSurface(SurfaceProvenance.AI_WRITTEN, rateAction), onEvent = {}) }
            }

            onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)).assertDoesNotExist()
        }

    // the whole ask flow, on a surface that answers its own edits

    @Test
    fun `answering the questions changes only local data and sends no action`() =
        runComposeUiTest {
            val fixture = SurfaceFixture(AskFixtures.QUESTIONS)
            val actions = mutableListOf<SurfaceEvent.ActionTriggered>()

            setContent {
                MaterialTheme {
                    // The Ask screen scrolls, and the questions are taller than a small test screen.
                    ScentSurfaceFixtureHost(
                        fixture,
                        ASK_SURFACE_ID,
                        Modifier.verticalScroll(rememberScrollState()),
                        onAction = actions::add,
                    )
                }
            }
            onNodeWithContentDescription("Spicy").performScrollTo().performClick()
            onNodeWithContentDescription("Decant set to try").performScrollTo().performClick()

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
                MaterialTheme {
                    // The Ask screen scrolls, and the questions are taller than a small test screen.
                    ScentSurfaceFixtureHost(
                        fixture,
                        ASK_SURFACE_ID,
                        Modifier.verticalScroll(rememberScrollState()),
                        onAction = actions::add,
                    )
                }
            }
            onNodeWithText("SHOW GIFTS").performScrollTo().performClick()

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
                "Birch Ledger, £64, Sealed, 75 ml, Atelier Nord. Smoky birch with a soft musk finish. Easy to wear every day",
            ).assertExists()
            onNodeWithText("Reasons written by AI").assertExists()
            onNodeWithText("Questions written by AI").assertDoesNotExist()
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
