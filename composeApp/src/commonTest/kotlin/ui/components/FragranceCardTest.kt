package ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** Shared by both platforms; Android hosts it through Robolectric in a thin subclass. */
@OptIn(ExperimentalTestApi::class)
abstract class FragranceCardTest {
    private val card =
        FragranceCardUi(
            id = 1,
            name = "Santal 33",
            brand = "Le Labo",
            starCount = 4,
            ratingText = "4.3 (189 reviews)",
            a11yLabel = "Santal 33, Le Labo, rated 4.3, 189 reviews",
            placeholderIndex = 0,
        )

    private fun ComposeUiTest.show(
        isFavourite: Boolean,
        onToggleFavourite: () -> Unit = {},
    ) {
        setContent {
            MaterialTheme {
                FragranceCard(
                    card = card,
                    onClick = {},
                    isFavourite = isFavourite,
                    onToggleFavourite = onToggleFavourite,
                )
            }
        }
    }

    private fun ComposeUiTest.config() = onNodeWithContentDescription(card.a11yLabel).fetchSemanticsNode().config

    @Test
    fun `the card has one description equal to the a11y label`() =
        runComposeUiTest {
            show(isFavourite = false)

            assertEquals(listOf(card.a11yLabel), config()[SemanticsProperties.ContentDescription])
        }

    @Test
    fun `the favourite action offers to add when not a favourite`() =
        runComposeUiTest {
            show(isFavourite = false)

            assertEquals(listOf("Add to favourites"), config()[SemanticsActions.CustomActions].map { it.label })
        }

    @Test
    fun `the favourite action offers to remove when a favourite`() =
        runComposeUiTest {
            show(isFavourite = true)

            assertEquals(listOf("Remove from favourites"), config()[SemanticsActions.CustomActions].map { it.label })
        }

    @Test
    fun `invoking the favourite action reports a toggle`() =
        runComposeUiTest {
            var toggles = 0
            show(isFavourite = false, onToggleFavourite = { toggles++ })

            val action = config()[SemanticsActions.CustomActions].single()
            runOnUiThread { action.action() }

            assertEquals(1, toggles)
        }
}
