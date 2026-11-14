package ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import ui.accessibility.clearedDescription
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

/**
 * A suggested fragrance: [name] and [price] on one line, its [meta] facts under them, and up to three
 * [reasons] it fits as bullets underneath. A blank [price] or [meta] is left out of both the layout and
 * the announcement.
 *
 * The card is announced once, as "name, price, meta. reason. reason". With [onClick] it is a button
 * ("Open listing") at least as tall as a button, with a chevron; without it, it is plain content.
 */
@Composable
fun RecommendationCard(
    name: String,
    price: String,
    reasons: List<String>,
    modifier: Modifier = Modifier,
    meta: String = "",
    onClick: (() -> Unit)? = null,
) {
    val description = announcement(name, price, meta, reasons)
    val shape = MaterialTheme.shapes.large
    val lifted =
        modifier
            .fillMaxWidth()
            .shadow(
                elevation = ScentThemeExtras.elevation.card,
                shape = shape,
                ambientColor = ScentThemeExtras.cardShadow,
                spotColor = ScentThemeExtras.cardShadow,
            )
    val content: @Composable () -> Unit = {
        RecommendationCardContent(name, price, meta, reasons, showChevron = onClick != null)
    }
    if (onClick == null) {
        Card(
            modifier = lifted.clearedDescription(description),
            shape = shape,
            colors = cardColors(),
            elevation = cardElevation(),
        ) { content() }
    } else {
        Card(
            onClick = onClick,
            modifier =
                lifted
                    .heightIn(min = ScentThemeExtras.spacing.buttonHeight)
                    .semantics { role = Role.Button }
                    .clearedDescription(description, onClick = onClick, onClickLabel = OPEN_LISTING_LABEL),
            shape = shape,
            colors = cardColors(),
            elevation = cardElevation(),
        ) { content() }
    }
}

private const val OPEN_LISTING_LABEL = "Open listing"
private const val META_SEPARATOR = " · "

// The facts first, then each reason as its own sentence. The meta line's dots read badly aloud, so they
// become commas.
private fun announcement(
    name: String,
    price: String,
    meta: String,
    reasons: List<String>,
): String {
    val facts = listOf(name, price, meta.replace(META_SEPARATOR, ", ")).filter(String::isNotBlank)
    return (listOf(facts.joinToString(", ")) + reasons.filter(String::isNotBlank)).joinToString(". ")
}

@Composable
private fun cardColors() = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)

// The warm shadow is drawn by Modifier.shadow; the card's own elevation would add a grey one on top.
@Composable
private fun cardElevation() = CardDefaults.cardElevation(defaultElevation = ScentThemeExtras.elevation.none)

@Composable
private fun RecommendationCardContent(
    name: String,
    price: String,
    meta: String,
    reasons: List<String>,
    showChevron: Boolean,
) {
    Column(
        modifier = Modifier.padding(ScentThemeExtras.spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xxs),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
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
            if (showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeMedium),
                )
            }
        }
        if (meta.isNotBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (reasons.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = ScentThemeExtras.spacing.xxs),
                verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xxs),
            ) {
                reasons.forEach { ReasonBullet(it) }
            }
        }
    }
}

@Composable
private fun ReasonBullet(reason: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(ScentThemeExtras.spacing.xxs)
                .background(ScentThemeExtras.accent, CircleShape),
        )
        Text(
            text = reason,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
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
                reasons = listOf("Dry cedarwood and vetiver", "Easy to wear every day", "A classic woody gift"),
                modifier = Modifier.padding(ScentThemeExtras.spacing.screenPadding),
                meta = "Sealed · 100 ml · Atelier Nord",
            )
        }
    }
}

@Preview
@Composable
private fun RecommendationCardClickablePreview() {
    ScentTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            RecommendationCard(
                name = "Cedar & Smoke",
                price = "£76",
                reasons = listOf("Dry cedarwood and vetiver"),
                modifier = Modifier.padding(ScentThemeExtras.spacing.screenPadding),
                onClick = {},
            )
        }
    }
}
