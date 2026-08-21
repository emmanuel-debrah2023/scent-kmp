package ui.accessibility

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/** Sets a content description on a non-interactive element (e.g. a static icon carrying meaning). */
fun Modifier.accessibleLabel(label: String): Modifier = semantics { contentDescription = label }

/** A clickable element with its own announced label, distinct from any child text. */
fun Modifier.accessibleClickable(
    label: String,
    role: Role = Role.Button,
    onClick: () -> Unit,
): Modifier =
    clickable(onClickLabel = label, role = role, onClick = onClick)
        .semantics { contentDescription = label }

/** A clickable element that also exposes a long-click, both independently announced. */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.accessibleCombinedClickable(
    label: String,
    role: Role = Role.Button,
    onLongClickLabel: String? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier =
    combinedClickable(
        role = role,
        onClickLabel = label,
        onLongClickLabel = onLongClickLabel,
        onLongClick = onLongClick,
        onClick = onClick,
    ).semantics { contentDescription = label }

/**
 * Groups non-interactive text children (e.g. a card's metadata row) into a single
 * announcement, concatenated from the real child text. Never pair with a hand-written
 * `contentDescription` — that drifts out of sync when a child's text changes.
 */
fun Modifier.mergedGroup(): Modifier = semantics(mergeDescendants = true) {}

/**
 * Replaces a subtree of purely decorative children with one hand-written announcement
 * (e.g. a listing card whose price/condition/name read better rewritten than concatenated).
 * Other modifiers on the same element (like `clickable`) still contribute their own
 * role/action semantics — only descendant composables are cleared.
 */
fun Modifier.clearedDescription(description: String): Modifier =
    clearAndSetSemantics { contentDescription = description }

/**
 * A toggle control (favorite/wishlist/like) with one unified announcement and an
 * accessible checked state, backed by `Modifier.toggleable` for the click action.
 */
fun Modifier.accessibleToggle(
    value: Boolean,
    label: String,
    role: Role = Role.Checkbox,
    onValueChange: (Boolean) -> Unit,
): Modifier =
    toggleable(value = value, onValueChange = onValueChange, role = role)
        .semantics {
            contentDescription = label
        }

/** Exposes extra actions beyond the default click (e.g. "Remove from wishlist" on a card). */
fun Modifier.withCustomActions(vararg actions: CustomAccessibilityAction): Modifier =
    semantics { customActions = actions.toList() }

/** Marks a LazyColumn/LazyRow/grid container so item position is trackable by a11y services. */
fun Modifier.collectionContainer(
    rowCount: Int,
    columnCount: Int = 1,
): Modifier = semantics { collectionInfo = CollectionInfo(rowCount = rowCount, columnCount = columnCount) }

/** Marks a single item inside a `collectionContainer` with its position. */
fun Modifier.collectionItem(
    rowIndex: Int,
    columnIndex: Int = 0,
): Modifier =
    semantics {
        collectionItemInfo =
            CollectionItemInfo(
                rowIndex = rowIndex,
                rowSpan = 1,
                columnIndex = columnIndex,
                columnSpan = 1,
            )
    }

/** Marks an element as a heading for TalkBack section navigation. */
fun Modifier.accessibleHeading(): Modifier = semantics { heading() }

/** Gives a bottom sheet or dialog a unique, screen-specific pane title. */
fun Modifier.accessiblePane(title: String): Modifier = semantics { paneTitle = title }

/** Announces a dynamic state beyond checked/unchecked (e.g. "3 of 5 selected"). */
fun Modifier.accessibleState(description: String): Modifier = semantics { stateDescription = description }

/**
 * Marks a region whose content changes should be announced without the user moving
 * focus — e.g. a typeahead's suggestion list appearing under the field being typed in.
 * Polite queues behind whatever is currently being spoken; assertive interrupts, which
 * is almost never right for content the user did not explicitly request.
 */
fun Modifier.accessibleLiveRegion(polite: Boolean = true): Modifier =
    semantics { liveRegion = if (polite) LiveRegionMode.Polite else LiveRegionMode.Assertive }

/** Hides a purely decorative container (background shape, divider, gradient) from a11y services. */
fun Modifier.decorative(): Modifier = clearAndSetSemantics {}
