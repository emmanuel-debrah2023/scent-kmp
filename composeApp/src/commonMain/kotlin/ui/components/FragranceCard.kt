package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.clearedDescription
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

private const val STAR_COUNT = 5
private const val ADD_FAVOURITE_LABEL = "Add to favourites"
private const val REMOVE_FAVOURITE_LABEL = "Remove from favourites"

/**
 * What a [FragranceCard] shows, already formatted. [ratingText] is the visible line and
 * [a11yLabel] the single announcement for the whole card; [placeholderIndex] picks a
 * placeholder tone until fragrances carry images.
 */
data class FragranceCardUi(
    val id: Int,
    val name: String,
    val brand: String,
    val starCount: Int,
    val ratingText: String,
    val a11yLabel: String,
    val placeholderIndex: Int,
)

object FragranceCardDefaults {
    // Placeholder media tones; they stand in until fragrances carry images.
    val PlaceholderColors: List<Color>
        get() = ScentThemeExtras.fragrancePlaceholderTones
}

/**
 * A tappable fragrance summary: media area, name, brand and rating. The whole card is one
 * button announced by [FragranceCardUi.a11yLabel]; the favourite toggle inside it is
 * reachable through a custom accessibility action that follows [isFavourite]. The card is
 * stateless: the caller owns [isFavourite] and flips it in [onToggleFavourite].
 * [placeholderColors] is indexed by [FragranceCardUi.placeholderIndex].
 */
@Composable
fun FragranceCard(
    card: FragranceCardUi,
    onClick: () -> Unit,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
    placeholderColors: List<Color> = FragranceCardDefaults.PlaceholderColors,
) {
    val placeholderColor = placeholderColors[card.placeholderIndex.mod(placeholderColors.size)]

    Card(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .clearedDescription(
                    description = card.a11yLabel,
                    customActions =
                        listOf(
                            CustomAccessibilityAction(
                                if (isFavourite) REMOVE_FAVOURITE_LABEL else ADD_FAVOURITE_LABEL,
                            ) {
                                onToggleFavourite()
                                true
                            },
                        ),
                    onClick = onClick,
                ),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = ScentThemeExtras.elevation.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(ScentThemeExtras.spacing.cardImageHeight)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                placeholderColor,
                                placeholderColor.copy(alpha = ScentThemeExtras.alpha.placeholderFade),
                            ),
                        ),
                    ),
        ) {
            // The button is the full 48dp target; the 36dp scrim circle is only visual.
            IconButton(
                onClick = onToggleFavourite,
                modifier =
                    Modifier
                        .size(ScentThemeExtras.spacing.xxl)
                        .align(Alignment.TopEnd),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(ScentThemeExtras.spacing.favouriteScrimSize)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = ScentThemeExtras.alpha.mediaIconScrim),
                                CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint =
                            if (isFavourite) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeMedium),
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(ScentThemeExtras.spacing.cardPadding)) {
            Text(
                text = card.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = card.brand,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xxs),
                modifier = Modifier.padding(top = ScentThemeExtras.spacing.xxs),
            ) {
                StarRow(starCount = card.starCount)
                Text(
                    text = card.ratingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Five stars, the first [starCount] filled. Decorative: the rating is announced as text. */
@Composable
fun StarRow(
    starCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        repeat(STAR_COUNT) { index ->
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = if (index < starCount) ScentThemeExtras.accent else MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeSmall),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FragranceCardPreview() {
    ScentTheme {
        FragranceCard(
            card =
                FragranceCardUi(
                    id = 1,
                    name = "Santal 33",
                    brand = "Le Labo",
                    starCount = 4,
                    ratingText = "4.3 (189 reviews)",
                    a11yLabel = "Santal 33, Le Labo, rated 4.3, 189 reviews",
                    placeholderIndex = 0,
                ),
            onClick = {},
            isFavourite = false,
            onToggleFavourite = {},
            modifier = Modifier.padding(ScentThemeExtras.spacing.md),
        )
    }
}
