package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.clearedDescription
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A suggested fragrance: [name] and [price] on one line, and [why] it fits underneath. A blank
 * [price] or [why] is left out of both the layout and the announcement.
 *
 * The card is announced as one description ("name, price, reason") and is not a control yet.
 */
@Composable
fun RecommendationCard(
    name: String,
    price: String,
    why: String,
    modifier: Modifier = Modifier,
) {
    val description = listOf(name, price, why).filter(String::isNotBlank).joinToString(", ")
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .clearedDescription(description)
                .padding(ScentThemeExtras.spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = name,
                style = ScentThemeExtras.listingRowFragranceName,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (price.isNotBlank()) {
                Text(
                    text = price,
                    style = ScentThemeExtras.listingRowPrice,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (why.isNotBlank()) {
            Text(
                text = why,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Preview
@Composable
private fun RecommendationCardPreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RecommendationCard(
                name = "Cedar & Smoke",
                price = "£76",
                why = "Dry cedarwood and vetiver: a classic woody gift that stays close to the skin.",
                modifier = Modifier.padding(ScentThemeExtras.spacing.screenPadding),
            )
        }
    }
}
