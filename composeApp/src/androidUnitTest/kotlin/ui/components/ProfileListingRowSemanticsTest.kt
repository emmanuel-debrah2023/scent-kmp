package ui.components

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ui.testing.composeHostActivityRule

private const val DESCRIPTION = "Aventus by Creed, 285 pounds, or offer"

private val testRow =
    ProfileListingRowUiModel(
        id = 1,
        photoUrl = null,
        brand = "Creed",
        fragranceName = "Aventus",
        priceText = "£285",
        termsText = "or offer",
        metaText = null,
        pillStatus = ListingRowPillStatus.LIVE,
        unlistLabel = "UNLIST",
        accessibilityDescription = DESCRIPTION,
    )

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ProfileListingRowSemanticsTest {
    @get:Rule(order = 0)
    val hostActivity = composeHostActivityRule()

    @get:Rule(order = 1)
    val composeRule = createComposeRule()

    @Test
    fun `row exposes its custom actions`() {
        composeRule.setContent {
            MaterialTheme {
                ProfileListingRow(listing = testRow, onEdit = {}, onUnlist = {}, onDelete = {}, onClick = {})
            }
        }

        composeRule
            .onNode(hasContentDescription(DESCRIPTION))
            .assert(
                SemanticsMatcher("CustomActions labels == [Edit, UNLIST, Delete]") {
                    it.config.getOrNull(SemanticsActions.CustomActions)?.map { action -> action.label } ==
                        listOf("Edit", "UNLIST", "Delete")
                },
            )
    }

    @Test
    fun `row still has a click action`() {
        composeRule.setContent {
            MaterialTheme {
                ProfileListingRow(listing = testRow, onEdit = {}, onUnlist = {}, onDelete = {}, onClick = {})
            }
        }

        composeRule.onNode(hasContentDescription(DESCRIPTION)).assertHasClickAction()
    }

    @Test
    fun `hidden actions define no custom actions`() {
        composeRule.setContent {
            MaterialTheme {
                ProfileListingRow(
                    listing = testRow,
                    onEdit = {},
                    onUnlist = {},
                    onDelete = {},
                    onClick = {},
                    showActions = false,
                )
            }
        }

        composeRule
            .onNode(hasContentDescription(DESCRIPTION))
            .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.CustomActions))
    }
}
