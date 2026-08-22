package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Liquor
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingKind
import ui.accessibility.clearedDescription
import ui.accessibility.withCustomActions
import ui.components.buttons.ScentPrimaryButton
import ui.components.buttons.ScentSecondaryButton
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras
import kotlin.math.roundToInt

private const val CONDITION_MAX_LENGTH = 14

@Composable
fun ListingCard(
    listing: Listing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListingCardScaffold(listing = listing, onClick = onClick, modifier = modifier)
}

@Composable
fun ListingCardWithOffer(
    listing: Listing,
    onClick: () -> Unit,
    onBuyNow: () -> Unit,
    onMakeOffer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListingCardScaffold(listing = listing, onClick = onClick, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = ScentThemeExtras.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
        ) {
            if (listing.isNegotiable) {
                ScentSecondaryButton(
                    text = "Offer",
                    onClick = onMakeOffer,
                    modifier = Modifier.weight(1f),
                )
            }
            ScentPrimaryButton(
                text = "Buy now",
                onClick = onBuyNow,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun OwnerListingCard(
    listing: Listing,
    onClick: () -> Unit,
    onToggleActive: () -> Unit,
    onDeleteRequest: () -> Unit,
    modifier: Modifier = Modifier,
    isActionInFlight: Boolean = false,
) {
    val toggleLabel = if (listing.isActive) "Unlist" else "Relist"
    ListingCardScaffold(
        listing = listing,
        onClick = onClick,
        modifier = modifier,
        // Empty while an action is in flight — mirrors the visible buttons' disabled
        // state so a TalkBack user can't re-trigger unlist/delete mid-request either.
        customActions =
            if (isActionInFlight) {
                emptyList()
            } else {
                listOf(
                    CustomAccessibilityAction(toggleLabel) {
                        onToggleActive()
                        true
                    },
                    CustomAccessibilityAction("Delete") {
                        onDeleteRequest()
                        true
                    },
                )
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = ScentThemeExtras.spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
        ) {
            ScentSecondaryButton(
                text = toggleLabel.uppercase(),
                onClick = onToggleActive,
                modifier = Modifier.weight(1f),
                enabled = !isActionInFlight,
            )
            ScentSecondaryButton(
                text = "DELETE",
                onClick = onDeleteRequest,
                modifier = Modifier.weight(1f),
                enabled = !isActionInFlight,
            )
        }
    }
}

@Composable
private fun ListingCardScaffold(
    listing: Listing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    customActions: List<CustomAccessibilityAction> = emptyList(),
    footer: (@Composable () -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .clearedDescription(listingContentDescription(listing))
                .withCustomActions(*customActions.toTypedArray()),
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = ScentThemeExtras.elevation.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        ListingHero(listing = listing)

        Column(
            modifier = Modifier.padding(ScentThemeExtras.spacing.sm),
            verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xxs),
        ) {
            Text(
                text = listing.fragrance.brand.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = ScentThemeExtras.gray400,
            )
            Text(
                text = listing.fragrance.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            volumeConcentrationLine(listing.fragrance.volume, listing.fragrance.concentration)?.let { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xxs),
            ) {
                Text(
                    text = "£${listing.price.roundToInt()}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (listing.isNegotiable) "or offer" else "firm",
                    style = MaterialTheme.typography.bodySmall,
                    color = ScentThemeExtras.gray400,
                )
            }
            if (listing.stockQuantity > 1) {
                Text(
                    text = "${listing.stockQuantity} available",
                    style = MaterialTheme.typography.bodySmall,
                    color = ScentThemeExtras.gray400,
                )
            }

            ScentDivider(modifier = Modifier.padding(vertical = ScentThemeExtras.spacing.xxs))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = listing.sellerUsername,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (listing.fragrance.rating > 0f) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xxs),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeSmall),
                            tint = ScentThemeExtras.accent,
                        )
                        Text(
                            text = ratingText(listing.fragrance.rating),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            footer?.invoke()
        }
    }
}

@Composable
private fun ListingHero(listing: Listing) {
    Box(
        modifier = Modifier.fillMaxWidth().height(ScentThemeExtras.spacing.cardHeroHeight),
        contentAlignment = Alignment.Center,
    ) {
        val imageUrl = listing.fragrance.imageUrls.firstOrNull()
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    ScentThemeExtras.listingPlaceholder,
                                    ScentThemeExtras.listingPlaceholder.copy(alpha = 0.6f),
                                ),
                            ),
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Liquor,
                    contentDescription = null,
                    modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeLarge),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(ScentThemeExtras.spacing.xs)
                    .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                    .padding(horizontal = ScentThemeExtras.spacing.xs, vertical = ScentThemeExtras.spacing.xxs),
        ) {
            Text(
                text = truncatedCondition(listing.condition),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun volumeConcentrationLine(
    volume: Int?,
    concentration: String?,
): String? =
    when {
        volume != null && concentration != null -> "${volume}ml · $concentration"
        volume != null -> "${volume}ml"
        concentration != null -> concentration
        else -> null
    }

private fun truncatedCondition(condition: String): String =
    if (condition.length <= CONDITION_MAX_LENGTH) {
        condition
    } else {
        condition.take(CONDITION_MAX_LENGTH - 1).trimEnd() + "…"
    }

private fun ratingText(rating: Float): String {
    val tenths = (rating * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10}"
}

// Single hand-written announcement (see clearedDescription) instead of concatenating
// child text, since price/condition/seller read better rewritten than merged.
private fun listingContentDescription(listing: Listing): String {
    val priceTerms = if (listing.isNegotiable) "or offer" else "firm price"
    return "${listing.fragrance.name} by ${listing.fragrance.brand}, " +
        "${listing.condition} condition, £${listing.price.roundToInt()}, $priceTerms, " +
        "sold by ${listing.sellerUsername}, ${fillDescription(listing)}"
}

private fun fillDescription(listing: Listing): String {
    val nominal = listing.nominalSizeMl ?: return "fill not stated"
    return when (listing.kind) {
        ListingKind.SEALED -> "sealed, ${nominal}ml"
        ListingKind.DECANT -> "${nominal}ml decant"
        ListingKind.OPENED, ListingKind.TESTER ->
            listing.remainingMl?.let { remaining -> "$remaining of ${nominal}ml remaining" }
                ?: "fill not stated"
    }
}

@Preview(showBackground = true)
@Composable
private fun ListingCardPreview() {
    ScentTheme {
        ListingCard(
            listing =
                Listing(
                    id = 1,
                    fragrance =
                        Fragrance(
                            id = 1,
                            name = "Aventus",
                            brand = "Creed",
                            volume = 100,
                            concentration = "EDP",
                            rating = 4.5f,
                        ),
                    sellerId = 1,
                    sellerUsername = "scent_collector",
                    price = 185.0,
                    condition = "90% full",
                    isNegotiable = true,
                    stockQuantity = 3,
                ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ListingCardFirmPreview() {
    ScentTheme {
        ListingCard(
            listing =
                Listing(
                    id = 2,
                    fragrance = Fragrance(id = 2, name = "Santal 33", brand = "Le Labo"),
                    sellerId = 2,
                    sellerUsername = "notes_and_musk",
                    price = 120.0,
                    condition = "Unused, sealed box",
                    isNegotiable = false,
                ),
            onClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OwnerListingCardActivePreview() {
    ScentTheme {
        OwnerListingCard(
            listing =
                Listing(
                    id = 1,
                    fragrance =
                        Fragrance(
                            id = 1,
                            name = "Aventus",
                            brand = "Creed",
                            volume = 100,
                            concentration = "EDP",
                        ),
                    sellerId = 1,
                    sellerUsername = "scent_collector",
                    price = 185.0,
                    condition = "90% full",
                    isActive = true,
                    kind = ListingKind.OPENED,
                    nominalSizeMl = 100,
                    remainingMl = 90,
                ),
            onClick = {},
            onToggleActive = {},
            onDeleteRequest = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OwnerListingCardUnlistedPreview() {
    ScentTheme {
        OwnerListingCard(
            listing =
                Listing(
                    id = 2,
                    fragrance = Fragrance(id = 2, name = "Santal 33", brand = "Le Labo"),
                    sellerId = 2,
                    sellerUsername = "notes_and_musk",
                    price = 120.0,
                    condition = "Unused, sealed box",
                    isActive = false,
                    kind = ListingKind.SEALED,
                    nominalSizeMl = 50,
                ),
            onClick = {},
            onToggleActive = {},
            onDeleteRequest = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
