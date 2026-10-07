package ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import ui.testing.composeHostActivityRule
import kotlin.test.assertEquals

// The description is hardcoded: listingContentDescription is private to ListingCard.kt.
private const val DESCRIPTION =
    "Aventus by Creed, NEW condition, £285, or offer, sold by scent_seed_seller, fill not stated"

private val testListing =
    Listing(
        id = 1,
        fragrance = Fragrance(id = 1, name = "Aventus", brand = "Creed"),
        sellerId = 1,
        sellerUsername = "scent_seed_seller",
        price = 285.0,
        condition = "NEW",
        isNegotiable = true,
    )

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ListingCardSemanticsTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun `listing card has a click action`() {
        composeRule.setContent { MaterialTheme { ListingCard(listing = testListing, onClick = {}) } }

        composeRule.onNode(hasContentDescription(DESCRIPTION)).assertHasClickAction()
    }

    @Test
    fun `OnClick semantics action invokes onClick once`() {
        var clicks = 0
        composeRule.setContent { MaterialTheme { ListingCard(listing = testListing, onClick = { clicks++ }) } }

        composeRule.onNode(hasContentDescription(DESCRIPTION)).performSemanticsAction(SemanticsActions.OnClick)

        assertEquals(1, clicks)
    }

    @Test
    fun `listing card with offer has a click action`() {
        composeRule.setContent {
            MaterialTheme {
                ListingCardWithOffer(listing = testListing, onClick = {}, onBuyNow = {}, onMakeOffer = {})
            }
        }

        composeRule.onNode(hasContentDescription(DESCRIPTION)).assertHasClickAction()
    }

    @Test
    fun `owner listing card exposes its custom actions`() {
        composeRule.setContent {
            MaterialTheme {
                OwnerListingCard(listing = testListing, onClick = {}, onToggleActive = {}, onDeleteRequest = {})
            }
        }

        composeRule
            .onNode(hasContentDescription(DESCRIPTION))
            .assert(hasCustomActionLabels(listOf("Unlist", "Delete")))
    }

    @Test
    fun `content description is exactly the hand-written announcement`() {
        composeRule.setContent { MaterialTheme { ListingCard(listing = testListing, onClick = {}) } }

        composeRule
            .onNode(hasContentDescription(DESCRIPTION))
            .assert(
                SemanticsMatcher("ContentDescription == [description]") {
                    it.config.getOrNull(SemanticsProperties.ContentDescription) == listOf(DESCRIPTION)
                },
            )
    }

    @Test
    fun `touch click invokes onClick once`() {
        var clicks = 0
        composeRule.setContent { MaterialTheme { ListingCard(listing = testListing, onClick = { clicks++ }) } }

        composeRule.onNode(hasContentDescription(DESCRIPTION)).performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `child text stays cleared`() {
        composeRule.setContent { MaterialTheme { ListingCard(listing = testListing, onClick = {}) } }

        composeRule.onAllNodesWithText("Aventus").assertCountEquals(0)
    }
}

private fun hasCustomActionLabels(expected: List<String>) =
    SemanticsMatcher("CustomActions labels == $expected") {
        it.config.getOrNull(SemanticsActions.CustomActions)?.map { action -> action.label } == expected
    }
