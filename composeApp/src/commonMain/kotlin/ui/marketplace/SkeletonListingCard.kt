package ui.marketplace

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.components.ScentDivider
import ui.components.shimmerPlaceholder
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/** A shimmering placeholder shaped like [ui.components.ListingCard], shown during first load. */
@Composable
fun SkeletonListingCard(modifier: Modifier = Modifier) {
    val spacing = ScentThemeExtras.spacing

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = ScentThemeExtras.elevation.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(spacing.cardHeroHeight)
                    .shimmerPlaceholder(),
        )

        Column(
            modifier = Modifier.padding(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Box(Modifier.fillMaxWidth(0.4f).height(12.dp).shimmerPlaceholder(MaterialTheme.shapes.extraSmall))
            Box(Modifier.fillMaxWidth(0.7f).height(20.dp).shimmerPlaceholder(MaterialTheme.shapes.extraSmall))
            Box(Modifier.fillMaxWidth(0.35f).height(24.dp).shimmerPlaceholder(MaterialTheme.shapes.extraSmall))

            ScentDivider(modifier = Modifier.padding(vertical = spacing.xxs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.width(80.dp).height(12.dp).shimmerPlaceholder(MaterialTheme.shapes.extraSmall))
                Box(Modifier.width(32.dp).height(12.dp).shimmerPlaceholder(MaterialTheme.shapes.extraSmall))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SkeletonListingCardPreview() {
    ScentTheme {
        SkeletonListingCard(modifier = Modifier.padding(16.dp))
    }
}
