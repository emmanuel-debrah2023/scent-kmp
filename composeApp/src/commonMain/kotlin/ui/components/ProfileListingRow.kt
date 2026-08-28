package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ui.accessibility.accessibleClickable
import ui.accessibility.clearedDescription
import ui.accessibility.withCustomActions
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/** Which visual treatment the status pill gets — a pure UI concept, not a copy of a
 *  domain enum. See [ProfileListingRowUiModel.pillStatus]'s doc for why the mapper that
 *  produces it (in `ui/profile`) currently only ever emits [LIVE]. */
enum class ListingRowPillStatus { NEW, LIVE, RESERVED }

/**
 * Everything [ProfileListingRow] needs, already display-ready: no domain model, no
 * enum-to-string mapping, no arithmetic. Built by a mapper in `ui/profile` (the screen
 * layer, which has the real [org.scent.project.domain.model.Listing]) — this component
 * only renders strings it's handed, per ADS-STE100's "no presentation logic in UI".
 */
data class ProfileListingRowUiModel(
    val id: Int,
    val photoUrl: String?,
    val brand: String,
    val fragranceName: String,
    val priceText: String,
    val termsText: String,
    /** "Like new · 90% full · 50 ml", or null when nothing to show. */
    val metaText: String?,
    val pillStatus: ListingRowPillStatus,
    /** "UNLIST" or "RELIST", already uppercased for direct display. */
    val unlistLabel: String,
    val accessibilityDescription: String,
)

/**
 * A ruled-list row for the Profile screen's Listings tab — deliberately not the
 * marketplace [ListingCard]: no card, no elevation, no hero image, no seller-name row
 * (the profile header already establishes whose listings these are). Separated from
 * neighbours by a single top hairline; the caller draws the closing hairline after the
 * last row so the group reads as one ruled list.
 *
 * [showActions] hides the edit/unlist/delete row entirely when rendering someone else's
 * profile — the underlying spec only described the owner's own view; a viewer has
 * nothing to act on here, so [onEdit]/[onUnlist]/[onDelete] are simply never invoked in
 * that case (pass no-ops).
 */
@Composable
fun ProfileListingRow(
    listing: ProfileListingRowUiModel,
    onEdit: () -> Unit,
    onUnlist: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
    isActionInFlight: Boolean = false,
) {
    val spacing = ScentThemeExtras.spacing
    val customActions =
        if (!showActions || isActionInFlight) {
            emptyList()
        } else {
            listOf(
                CustomAccessibilityAction("Edit") {
                    onEdit()
                    true
                },
                CustomAccessibilityAction(listing.unlistLabel) {
                    onUnlist()
                    true
                },
                CustomAccessibilityAction("Delete") {
                    onDelete()
                    true
                },
            )
        }

    val hairlineColor = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .wrapContentHeight()
                // Lets the thumbnail match the text column's real height (below) instead
                // of a fixed 90dp forcing dead space under short content — e.g. a listing
                // with no fill/size data has a one-line meta, well under 90dp of text.
                .height(IntrinsicSize.Min)
                .drawBehind {
                    drawLine(
                        color = hairlineColor,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }.clickable(onClick = onClick)
                .clearedDescription(listing.accessibilityDescription)
                .withCustomActions(*customActions.toTypedArray())
                .padding(vertical = spacing.md),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        RowThumbnail(imageUrl = listing.photoUrl)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = listing.brand,
                style = eyebrowStyle(),
                color = ScentThemeExtras.gray400,
            )
            Spacer(Modifier.height(spacing.xxs))
            Text(
                text = listing.fragranceName,
                style = ScentThemeExtras.listingRowFragranceName,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(spacing.xs))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(
                    text = listing.priceText,
                    style = ScentThemeExtras.listingRowPrice,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = listing.termsText,
                    style = MaterialTheme.typography.bodySmall,
                    color = ScentThemeExtras.gray400,
                )
            }
            listing.metaText?.let { line ->
                Spacer(Modifier.height(spacing.profileRowMetaGap))
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showActions) {
                Spacer(Modifier.height(spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.profileRowActionsGap)) {
                    RowActionText(label = "EDIT", onClick = onEdit, enabled = !isActionInFlight)
                    RowActionText(label = listing.unlistLabel, onClick = onUnlist, enabled = !isActionInFlight)
                    RowActionText(label = "DELETE", onClick = onDelete, enabled = !isActionInFlight)
                }
            }
        }

        Box(modifier = Modifier.padding(top = spacing.profileRowPillTopOffset)) {
            StatusPill(listing.pillStatus)
        }
    }
}

@Composable
private fun RowThumbnail(imageUrl: String?) {
    val spacing = ScentThemeExtras.spacing
    val shape = RoundedCornerShape(spacing.xs)
    Box(
        modifier =
            Modifier
                .width(spacing.profileRowThumbnailWidth)
                // Matches the text column's real height (the row measures both with
                // IntrinsicSize.Min) rather than a fixed 90dp that would floor the row
                // taller than short content needs. xxl is just a floor so an unusually
                // short row (e.g. no metadata at all) still gets a visible thumbnail —
                // it is NOT the target height, which is why it's far below 90dp.
                .defaultMinSize(minHeight = spacing.xxl)
                .fillMaxHeight()
                .clip(shape),
    ) {
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().fillMaxHeight(),
                contentScale = ContentScale.Crop,
            )
        } else {
            // Gradient alone is the placeholder — no glyph overlay.
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    ScentThemeExtras.listingPlaceholder,
                                    ScentThemeExtras.listingPlaceholder.copy(alpha = 0.6f),
                                ),
                            ),
                        ),
            )
        }
    }
}

@Composable
private fun RowActionText(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    // Brand forest-green — same token the price uses, per explicit direction.
    val color = MaterialTheme.colorScheme.primary
    Text(
        text = label,
        style = eyebrowStyle(),
        color = if (enabled) color else color.copy(alpha = 0.4f),
        modifier =
            Modifier
                .defaultMinSize(
                    minWidth = ScentThemeExtras.spacing.inlineActionMinTouchTarget,
                    minHeight = ScentThemeExtras.spacing.inlineActionMinTouchTarget,
                ).let { if (enabled) it.accessibleClickable(label = label, onClick = onClick) else it },
    )
}

private data class PillStyle(
    val fill: Color?,
    val border: Color?,
    val labelColor: Color,
    val label: String,
)

@Composable
private fun StatusPill(pill: ListingRowPillStatus) {
    val spacing = ScentThemeExtras.spacing
    val style =
        when (pill) {
            ListingRowPillStatus.NEW ->
                PillStyle(
                    fill = ScentThemeExtras.accent,
                    border = null,
                    labelColor = ScentThemeExtras.onAccent,
                    label = "New",
                )
            ListingRowPillStatus.LIVE ->
                PillStyle(
                    fill = null,
                    border = MaterialTheme.colorScheme.outlineVariant,
                    labelColor = MaterialTheme.colorScheme.primary,
                    label = "Live",
                )
            ListingRowPillStatus.RESERVED ->
                PillStyle(
                    fill = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = null,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "Reserved",
                )
        }

    Box(
        modifier =
            Modifier
                .clip(MaterialTheme.shapes.extraLarge)
                .let { m -> if (style.fill != null) m.background(style.fill) else m }
                .let { m ->
                    if (style.border != null) {
                        m.border(1.dp, style.border, MaterialTheme.shapes.extraLarge)
                    } else {
                        m
                    }
                }.padding(horizontal = spacing.xs, vertical = spacing.profileRowPillPaddingV),
    ) {
        Text(
            text = style.label.uppercase(),
            style =
                MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.9.sp,
                ),
            color = style.labelColor,
        )
    }
}

/** Shared micro-style for the brand line and the three action labels — derived from
 *  [MaterialTheme.typography.labelSmall] (DM Sans Medium) rather than authored from
 *  scratch, overriding only what the row's design spec actually changes. */
@Composable
private fun eyebrowStyle() =
    MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)

private fun previewUiModel(
    fragranceName: String,
    priceText: String,
    termsText: String,
    pillStatus: ListingRowPillStatus,
    photoUrl: String? = null,
) = ProfileListingRowUiModel(
    id = 1,
    photoUrl = photoUrl,
    brand = "DIOR",
    fragranceName = fragranceName,
    priceText = priceText,
    termsText = termsText,
    metaText = "Like new · 90% full · 50 ml",
    pillStatus = pillStatus,
    unlistLabel = "UNLIST",
    accessibilityDescription = "$fragranceName by Dior, $priceText, $termsText",
)

@Preview(showBackground = true)
@Composable
private fun ProfileListingRowNewPreview() {
    ScentTheme {
        ProfileListingRow(
            listing = previewUiModel("Aventus", "£185", "negotiable", ListingRowPillStatus.NEW),
            onEdit = {},
            onUnlist = {},
            onDelete = {},
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileListingRowLiveLongNamePreview() {
    ScentTheme {
        ProfileListingRow(
            listing =
                previewUiModel(
                    "Le Labo Santal 33 Eau de Parfum Travel Set with Extra Refill",
                    "£120",
                    "firm",
                    ListingRowPillStatus.LIVE,
                ),
            onEdit = {},
            onUnlist = {},
            onDelete = {},
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileListingRowReservedNoPhotoPreview() {
    ScentTheme {
        ProfileListingRow(
            listing = previewUiModel("Bleu de Chanel", "£90", "firm", ListingRowPillStatus.RESERVED),
            onEdit = {},
            onUnlist = {},
            onDelete = {},
            onClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileListingRowEmptyStatePreview() {
    ScentTheme {
        EmptyState(
            title = "No active listings",
            message = "List a bottle to sell it to the people already following you.",
            actionLabel = "CREATE LISTING",
            onAction = {},
        )
    }
}
