package ui.surface

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
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
    fun `a component not built yet renders the fallback`() =
        runComposeUiTest {
            val surface =
                surfaceOf(
                    SurfaceNode.Column("root", listOf("family")),
                    SurfaceNode.ChoicePicker("family", "Scent family", "/options/family", "/answers/family"),
                )

            setContent { MaterialTheme { ScentSurface(surface, onEvent = {}) } }

            assertEquals(1, fallbackCount())
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
