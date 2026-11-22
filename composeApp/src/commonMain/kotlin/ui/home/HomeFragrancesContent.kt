package ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ui.accessibility.accessibleHeading
import ui.accessibility.accessibleLabel
import ui.accessibility.accessibleToggle
import ui.accessibility.clearedDescription
import ui.accessibility.collectionItem
import ui.base.UiState
import ui.components.EmptyState
import ui.components.ErrorState
import ui.components.ErrorStateVariant
import ui.components.FragranceCard
import ui.components.FragranceCardUi
import ui.components.StarRow
import ui.components.buttons.ScentPrimaryButton
import ui.components.shimmerPlaceholder
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

private const val BOTTOM_SCRIM_SOLID_STOP = 0.3f

/**
 * The Fragrances tab body as items of the Home list: a skeleton while loading, an error with
 * retry, an empty state, or the optional hero followed by the fragrance cards.
 */
internal fun LazyListScope.homeFragrancesContent(
    state: UiState<HomeFragrancesState>,
    onOpenFragrance: (fragranceId: Int) -> Unit,
    onOpenVideo: (postId: String) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        UiState.Idle, UiState.Loading -> item(key = "fragrances-loading") { FragrancesSkeleton() }
        is UiState.Error ->
            item(key = "fragrances-error") {
                ErrorState(
                    variant = ErrorStateVariant.Error,
                    title = "Couldn't load fragrances",
                    message = state.error.message,
                    actionLabel = "RETRY",
                    onAction = onRetry,
                    announce = true,
                )
            }
        is UiState.Success -> {
            val (hero, cards) = state.data
            if (cards.isEmpty()) {
                item(key = "fragrances-empty") {
                    EmptyState(
                        title = "No fragrances yet",
                        message = "New fragrances will show up here as they are added.",
                    )
                }
            } else {
                if (hero != null) {
                    item(key = "fragrances-hero") {
                        // TODO(feature/fragrance-favourites): the toggle is local state, so it is lost when the
                        // list is rebuilt; persist it once favourites exist.
                        var isFavourite by rememberSaveable(hero.fragranceId) { mutableStateOf(false) }
                        HeroItem(
                            hero = hero,
                            isFavourite = isFavourite,
                            onToggleFavourite = { isFavourite = !isFavourite },
                            onOpenFragrance = onOpenFragrance,
                            onOpenVideo = onOpenVideo,
                        )
                    }
                }
                item(key = "fragrances-heading") {
                    // TODO(feature/trending-fragrances): heading says trending but the list is newest-first.
                    Text(
                        text = "Trending now",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .accessibleHeading()
                                .fillMaxWidth()
                                .padding(horizontal = ScentThemeExtras.spacing.screenPadding)
                                .padding(top = ScentThemeExtras.spacing.lg, bottom = ScentThemeExtras.spacing.xs),
                    )
                    // TODO(feature/fragrance-catalog-screen): "See all" removed until a catalog destination exists.
                }
                itemsIndexed(cards, key = { _, card -> card.id }) { index, card ->
                    // TODO(feature/fragrance-favourites): the toggle is local state, so it is lost when the
                    // list is rebuilt; persist it once favourites exist.
                    var isFavourite by rememberSaveable(card.id) { mutableStateOf(false) }
                    FragranceCard(
                        card = card,
                        onClick = { onOpenFragrance(card.id) },
                        isFavourite = isFavourite,
                        onToggleFavourite = { isFavourite = !isFavourite },
                        modifier =
                            Modifier
                                .collectionItem(rowIndex = index)
                                .padding(
                                    horizontal = ScentThemeExtras.spacing.screenPadding,
                                    vertical = ScentThemeExtras.spacing.xs,
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun FragrancesSkeleton() {
    Column(
        modifier =
            Modifier
                .accessibleLabel("Loading fragrances")
                .fillMaxWidth()
                .padding(horizontal = ScentThemeExtras.spacing.screenPadding, vertical = ScentThemeExtras.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
    ) {
        repeat(2) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(ScentThemeExtras.spacing.cardImageHeight)
                        .shimmerPlaceholder(MaterialTheme.shapes.large),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(SKELETON_TITLE_FRACTION)
                        .height(ScentThemeExtras.spacing.skeletonLineMedium)
                        .shimmerPlaceholder(MaterialTheme.shapes.extraSmall),
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth(SKELETON_META_FRACTION)
                        .height(ScentThemeExtras.spacing.skeletonLineSmall)
                        .shimmerPlaceholder(MaterialTheme.shapes.extraSmall),
            )
        }
    }
}

private const val SKELETON_TITLE_FRACTION = 0.6f
private const val SKELETON_META_FRACTION = 0.4f

// ─────────────────────────────────────────────
// Hero
// ─────────────────────────────────────────────

@Composable
private fun HeroItem(
    hero: HeroUi,
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
    onOpenFragrance: (fragranceId: Int) -> Unit,
    onOpenVideo: (postId: String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    // A minimum, not a fixed height: at large text the title and stacked actions outgrow it and would be squeezed.
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = ScentThemeExtras.spacing.heroHeight),
    ) {
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(ScentThemeExtras.heroPlaceholderStart, ScentThemeExtras.heroPlaceholderEnd),
                        ),
                    ),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(ScentThemeExtras.spacing.heroTopScrimHeight)
                    .align(Alignment.TopStart)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                colorScheme.background,
                                colorScheme.background.copy(alpha = ScentThemeExtras.alpha.heroTopScrimMid),
                                Color.Transparent,
                            ),
                        ),
                    ),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(ScentThemeExtras.spacing.heroBottomScrimHeight)
                    .align(Alignment.BottomStart)
                    .background(
                        // Solid early: the label sits near the top of this scrim and needs the cream
                        // to dominate the amber behind it even at large text sizes.
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            BOTTOM_SCRIM_SOLID_STOP to
                                colorScheme.background.copy(alpha = ScentThemeExtras.alpha.heroBottomScrimSolid),
                            1f to colorScheme.background,
                        ),
                    ),
        )

        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        horizontal = ScentThemeExtras.spacing.screenPadding,
                        vertical = ScentThemeExtras.spacing.lg,
                    ),
            verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
        ) {
            Text(
                text = "LATEST VIDEO",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
            )

            // The title and the meta row read as one announcement; the buttons below stay separate.
            Column(
                modifier = Modifier.clearedDescription(hero.a11yLabel),
                verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
            ) {
                Text(
                    text = hero.name,
                    style = MaterialTheme.typography.displaySmall,
                    color = colorScheme.primary,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs),
                ) {
                    Text(
                        text = hero.brand,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colorScheme.onSurface,
                    )
                    Box(
                        modifier =
                            Modifier
                                .size(ScentThemeExtras.spacing.xxs)
                                .background(colorScheme.outline, CircleShape),
                    )
                    StarRow(starCount = hero.starCount)
                    Text(
                        text = hero.ratingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            // At large text the button would leave the icon buttons no room beside it, so they wrap below.
            val largeText = LocalDensity.current.fontScale >= HERO_ACTIONS_LARGE_TEXT_SCALE
            val viewFragrance: @Composable (Modifier) -> Unit = { buttonModifier ->
                ScentPrimaryButton(
                    text = "VIEW FRAGRANCE",
                    onClick = { onOpenFragrance(hero.fragranceId) },
                    modifier = buttonModifier,
                )
            }
            val iconButtons: @Composable () -> Unit = {
                HeroFavouriteButton(isFavourite = isFavourite, onToggleFavourite = onToggleFavourite)
                HeroPlayButton(onClick = { onOpenVideo(hero.postId) })
            }
            if (largeText) {
                Column(verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.sm)) {
                    viewFragrance(Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.sm)) { iconButtons() }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The button fills its width, so it takes what the two icon buttons leave.
                    viewFragrance(Modifier.weight(1f))
                    iconButtons()
                }
            }
        }
    }
}

@Composable
private fun HeroFavouriteButton(
    isFavourite: Boolean,
    onToggleFavourite: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedIconToggleButton(
        checked = isFavourite,
        onCheckedChange = { onToggleFavourite() },
        modifier =
            Modifier
                .size(ScentThemeExtras.spacing.xxl)
                .accessibleToggle(value = isFavourite, label = "Favourite") { onToggleFavourite() },
        border = BorderStroke(width = 1.dp, color = colorScheme.outline),
    ) {
        Icon(
            imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = null,
            tint = if (isFavourite) colorScheme.error else colorScheme.primary,
            modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeMedium),
        )
    }
}

@Composable
private fun HeroPlayButton(onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.size(ScentThemeExtras.spacing.xxl),
        border = BorderStroke(width = 1.dp, color = colorScheme.outline),
    ) {
        Icon(
            imageVector = Icons.Outlined.PlayArrow,
            contentDescription = "Play video",
            tint = colorScheme.primary,
            modifier = Modifier.size(ScentThemeExtras.spacing.iconSizeMedium),
        )
    }
}

// Font scale from which the hero's action row stacks instead of sharing one line.
private const val HERO_ACTIONS_LARGE_TEXT_SCALE = 1.3f

// ─────────────────────────────────────────────
// Previews
// ─────────────────────────────────────────────

private fun previewCard(
    id: Int,
    name: String,
    brand: String,
) = FragranceCardUi(
    id = id,
    name = name,
    brand = brand,
    starCount = 4,
    ratingText = "4.3 (189 reviews)",
    a11yLabel = "$name, $brand, rated 4.3, 189 reviews",
    placeholderIndex = id,
)

@Preview(device = "spec:width=412dp,height=892dp")
@Composable
private fun HomeFragrancesContentPreview() {
    ScentTheme {
        LazyColumn {
            homeFragrancesContent(
                state =
                    UiState.Success(
                        HomeFragrancesState(
                            hero =
                                HeroUi(
                                    postId = "p1",
                                    fragranceId = 1,
                                    name = "Velvet Orchid",
                                    brand = "Tom Ford",
                                    starCount = 4,
                                    ratingText = "4.5 (215 reviews)",
                                    a11yLabel = "Velvet Orchid, Tom Ford, rated 4.5, 215 reviews",
                                ),
                            cards =
                                listOf(
                                    previewCard(1, "Santal 33", "Le Labo"),
                                    previewCard(2, "Oud Wood", "Tom Ford"),
                                    previewCard(3, "Bal d'Afrique", "Byredo"),
                                ),
                        ),
                    ),
                onOpenFragrance = {},
                onOpenVideo = {},
                onRetry = {},
            )
        }
    }
}
