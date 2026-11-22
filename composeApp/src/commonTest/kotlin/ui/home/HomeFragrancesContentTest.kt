package ui.home

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.scent.project.domain.error.AppError
import ui.base.UiState
import ui.components.FragranceCardUi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Shared by both platforms; Android hosts it through Robolectric in a thin subclass. */
@OptIn(ExperimentalTestApi::class)
abstract class HomeFragrancesContentTest {
    private val santal =
        FragranceCardUi(
            id = 7,
            name = "Santal 33",
            brand = "Le Labo",
            starCount = 4,
            ratingText = "4.3 (189 reviews)",
            a11yLabel = "Santal 33, Le Labo, rated 4.3, 189 reviews",
            placeholderIndex = 0,
        )
    private val hero =
        HeroUi(
            postId = "post-1",
            fragranceId = 9,
            name = "Velvet Orchid",
            brand = "Tom Ford",
            starCount = 4,
            ratingText = "4.5 (215 reviews)",
            a11yLabel = "Velvet Orchid, Tom Ford, rated 4.5, 215 reviews",
        )

    private fun androidx.compose.ui.test.ComposeUiTest.show(
        state: UiState<HomeFragrancesState>,
        onOpenFragrance: (Int) -> Unit = {},
        onOpenVideo: (String) -> Unit = {},
        onRetry: () -> Unit = {},
    ) {
        setContent {
            MaterialTheme {
                LazyColumn {
                    homeFragrancesContent(state, onOpenFragrance, onOpenVideo, onRetry)
                }
            }
        }
    }

    @Test
    fun `a card is announced once with its single label`() =
        runComposeUiTest {
            show(UiState.Success(HomeFragrancesState(hero = null, cards = listOf(santal))))

            onNodeWithContentDescription(santal.a11yLabel).assertExists()
            onAllNodesWithText("Santal 33").assertCountEquals(0)
        }

    @Test
    fun `a card exposes one description and the favourite action and nothing else`() =
        runComposeUiTest {
            show(UiState.Success(HomeFragrancesState(hero = null, cards = listOf(santal))))

            val config = onNodeWithContentDescription(santal.a11yLabel).fetchSemanticsNode().config
            assertEquals(listOf(santal.a11yLabel), config[SemanticsProperties.ContentDescription])
            assertEquals(
                listOf("Add to favourites"),
                config[SemanticsActions.CustomActions].map { it.label },
            )
            onAllNodesWithContentDescription("Favourite").assertCountEquals(0)
        }

    @Test
    fun `cards are numbered by position in the list`() =
        runComposeUiTest {
            val second = santal.copy(id = 8, a11yLabel = "Oud Wood, Tom Ford, no reviews yet")
            show(UiState.Success(HomeFragrancesState(hero = null, cards = listOf(santal, second))))

            fun rowOf(label: String) =
                onNodeWithContentDescription(label)
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.CollectionItemInfo]
                    .rowIndex

            assertEquals(0, rowOf(santal.a11yLabel))
            assertEquals(1, rowOf(second.a11yLabel))
        }

    @Test
    fun `tapping a card opens its fragrance`() =
        runComposeUiTest {
            var opened: Int? = null
            show(
                UiState.Success(HomeFragrancesState(hero = null, cards = listOf(santal))),
                onOpenFragrance = { opened = it },
            )

            onNodeWithContentDescription(santal.a11yLabel).performClick()

            assertEquals(7, opened)
        }

    @Test
    fun `the error state offers a retry`() =
        runComposeUiTest {
            var retries = 0
            show(UiState.Error(AppError.NetworkError.NoConnection()), onRetry = { retries++ })

            onNodeWithText("RETRY").performClick()

            assertEquals(1, retries)
        }

    @Test
    fun `no fragrances shows the empty state`() =
        runComposeUiTest {
            show(UiState.Success(HomeFragrancesState(hero = null, cards = emptyList())))

            onNodeWithText("No fragrances yet").assertExists()
        }

    @Test
    fun `hero play opens the post`() =
        runComposeUiTest {
            var played: String? = null
            show(
                UiState.Success(HomeFragrancesState(hero = hero, cards = listOf(santal))),
                onOpenVideo = { played = it },
            )

            onNodeWithContentDescription("Play video").performClick()

            assertEquals("post-1", played)
        }

    @Test
    fun `hero view fragrance opens the fragrance`() =
        runComposeUiTest {
            var opened: Int? = null
            show(
                UiState.Success(HomeFragrancesState(hero = hero, cards = listOf(santal))),
                onOpenFragrance = { opened = it },
            )

            onNodeWithText("View Fragrance").performClick()

            assertEquals(9, opened)
        }

    @Test
    fun `hero is announced as one label`() =
        runComposeUiTest {
            show(UiState.Success(HomeFragrancesState(hero = hero, cards = listOf(santal))))

            onNodeWithContentDescription(hero.a11yLabel).assertExists()
        }

    @Test
    fun `at double text size the hero icon buttons keep their labels and a 48dp target`() =
        runComposeUiTest {
            setContent {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                    MaterialTheme {
                        LazyColumn(Modifier.width(360.dp)) {
                            homeFragrancesContent(
                                UiState.Success(HomeFragrancesState(hero = hero, cards = listOf(santal))),
                                {},
                                {},
                                {},
                            )
                        }
                    }
                }
            }

            listOf("Favourite", "Play video").forEach { label ->
                onNodeWithContentDescription(label)
                    .assertIsDisplayed()
                    .assertWidthIsEqualTo(48.dp)
                    .assertHeightIsEqualTo(48.dp)
            }
            // Beside the button the icons sit at the far end of a 360dp row; wrapped below it they start the line.
            val favouriteLeft = onNodeWithContentDescription("Favourite").getUnclippedBoundsInRoot().left
            assertTrue(favouriteLeft < 90.dp, "Favourite left $favouriteLeft")
            onNodeWithText("VIEW FRAGRANCE").assertIsDisplayed()
        }
}
