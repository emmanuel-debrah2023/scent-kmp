package ui.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.usecase.MAX_LISTING_PHOTOS
import ui.accessibility.accessibleClickable
import ui.accessibility.accessibleLiveRegion
import ui.accessibility.accessiblePane
import ui.accessibility.accessibleState
import ui.accessibility.accessibleToggle
import ui.accessibility.collectionContainer
import ui.accessibility.collectionItem
import ui.base.UiState
import ui.components.CONDITION_OPTIONS
import ui.components.ScentTextField
import ui.components.SelectableChip
import ui.components.buttons.ScentPrimaryButton
import ui.components.buttons.ScentSecondaryButton
import ui.media.PickedImage
import ui.media.rememberImagePicker
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

private const val MAX_VISIBLE_FRAGRANCE_SUGGESTIONS = 8

@Composable
fun CreateListingScreen(
    onBack: () -> Unit,
    onCreated: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: CreateListingViewModel = koinViewModel()
    val formState by viewModel.formState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    val fragranceSuggestions by viewModel.fragranceSuggestions.collectAsState()

    LaunchedEffect(submitState) {
        val success = submitState as? UiState.Success<Listing> ?: return@LaunchedEffect
        onCreated(success.data.id)
    }

    // MAX_LISTING_PHOTOS (6), not the remaining count: Android's PickMultipleVisualMedia
    // contract requires maxItems > 1 and throws on recomposition otherwise — this would
    // crash once a single slot remained. The ViewModel truncates to remaining capacity.
    val imagePicker =
        rememberImagePicker(maxItems = MAX_LISTING_PHOTOS) { picked ->
            viewModel.onPhotosPicked(picked)
        }

    CreateListingContent(
        formState = formState,
        submitState = submitState,
        fragranceSuggestions = fragranceSuggestions,
        onBack = onBack,
        onAddPhotos = imagePicker::launch,
        onEvent = viewModel::onEvent,
        modifier = modifier,
    )
}

/** Mirrors [ui.profile.ProfileEvent]'s shape: one sealed event type routed through a single
 *  [CreateListingViewModel.onEvent], so this stays previewable without a ViewModel — the photo
 *  picker is platform-bound so it's the one callback kept separate from [onEvent]. */
@Composable
private fun CreateListingContent(
    formState: CreateListingFormState,
    submitState: UiState<Listing>,
    fragranceSuggestions: UiState<List<Fragrance>>,
    onBack: () -> Unit,
    onAddPhotos: () -> Unit,
    onEvent: (CreateListingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val submitError = (submitState as? UiState.Error)?.error

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .accessiblePane("Create listing")
                .padding(ScentThemeExtras.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(text = "Create Listing", style = MaterialTheme.typography.titleLarge)
        }

        ListingPhotoGrid(
            photos = formState.photos,
            onRemove = { id -> onEvent(CreateListingEvent.PhotoRemoved(id)) },
            onMove = { id, delta -> onEvent(CreateListingEvent.PhotoMoved(id, delta)) },
        )
        if (formState.photos.size < MAX_LISTING_PHOTOS) {
            ScentSecondaryButton(text = "Add photos", onClick = onAddPhotos)
        }

        Column(verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs)) {
            ScentTextField(
                value = formState.fragranceQuery,
                onValueChange = { query -> onEvent(CreateListingEvent.FragranceQueryChange(query)) },
                label = "Fragrance",
            )
            FragranceSuggestionList(
                state = fragranceSuggestions,
                query = formState.fragranceQuery,
                onSelect = { fragrance -> onEvent(CreateListingEvent.FragranceSelected(fragrance)) },
                onRetry = { onEvent(CreateListingEvent.FragranceSuggestionRetry) },
            )
        }

        ScentTextField(
            value = formState.price,
            onValueChange = { value -> onEvent(CreateListingEvent.PriceChange(value)) },
            label = "Price",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            error = (submitError as? AppError.ValidationError.InvalidPrice)?.message,
        )

        Column(verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs)) {
            Text(text = "Condition", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs)) {
                CONDITION_OPTIONS.forEach { (value, label) ->
                    SelectableChip(
                        label = label,
                        selected = formState.condition == value,
                        onClick = { onEvent(CreateListingEvent.ConditionChange(value)) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs)) {
            Text(text = "Kind", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs)) {
                ListingKind.entries.forEach { kind ->
                    SelectableChip(
                        label = kind.name,
                        selected = formState.kind == kind,
                        onClick = { onEvent(CreateListingEvent.KindChange(kind)) },
                    )
                }
            }
        }

        ScentTextField(
            value = formState.nominalSizeMl,
            onValueChange = { value -> onEvent(CreateListingEvent.NominalSizeChange(value)) },
            label = "Bottle size (ml)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            error = (submitError as? AppError.ValidationError.MissingNominalSize)?.message,
        )

        // SEALED/DECANT fill is derived server-side from nominal size — asking here
        // would just be discarded, so only OPENED/TESTER need a fill field at all.
        if (formState.kind == ListingKind.OPENED || formState.kind == ListingKind.TESTER) {
            ScentTextField(
                value = formState.remainingMl,
                onValueChange = { value -> onEvent(CreateListingEvent.RemainingMlChange(value)) },
                label = "Remaining (ml)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                error =
                    (submitError as? AppError.ValidationError.MissingFillLevel)?.message
                        ?: (submitError as? AppError.ValidationError.FillExceedsNominal)?.message,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Negotiable", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = formState.isNegotiable,
                onCheckedChange = { value -> onEvent(CreateListingEvent.NegotiableChange(value)) },
                modifier =
                    Modifier.accessibleToggle(
                        value = formState.isNegotiable,
                        label = "Negotiable",
                        onValueChange = { value -> onEvent(CreateListingEvent.NegotiableChange(value)) },
                    ),
            )
        }

        ScentTextField(
            value = formState.stockQuantity,
            onValueChange = { value -> onEvent(CreateListingEvent.StockQuantityChange(value)) },
            label = "Quantity",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        if (submitError != null &&
            submitError !is AppError.ValidationError.InvalidPrice &&
            submitError !is AppError.ValidationError.MissingNominalSize &&
            submitError !is AppError.ValidationError.MissingFillLevel &&
            submitError !is AppError.ValidationError.FillExceedsNominal
        ) {
            Text(
                text = submitError.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.accessibleLiveRegion(),
            )
        }

        ScentPrimaryButton(
            text = if (submitState is UiState.Loading) "Publishing…" else "Publish listing",
            onClick = { onEvent(CreateListingEvent.Submit) },
            enabled = formState.canSubmit && submitState !is UiState.Loading,
        )
    }
}

/** Inline, not a Popup/DropdownMenu — an overlay fights this form's own scroll/IME
 *  insets the same way it would inside a bottom sheet. See
 *  [ui.marketplace.MarketplaceFilterSheet]'s BrandSuggestionList for the precedent this
 *  mirrors. Plain Column, not LazyColumn: the parent already scrolls, and eight rows
 *  never needs virtualization. */
@Composable
private fun FragranceSuggestionList(
    state: UiState<List<Fragrance>>,
    query: String,
    onSelect: (Fragrance) -> Unit,
    onRetry: () -> Unit,
) {
    when (state) {
        is UiState.Idle -> Unit
        is UiState.Loading ->
            Text(
                text = "Searching fragrances…",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(ScentThemeExtras.spacing.xs),
            )
        is UiState.Error ->
            Text(
                text = "Couldn't load suggestions — tap to retry",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier =
                    Modifier
                        .padding(ScentThemeExtras.spacing.xs)
                        .accessibleClickable(label = "Retry fragrance suggestions", onClick = onRetry),
            )
        is UiState.Success -> {
            val suggestions = state.data.take(MAX_VISIBLE_FRAGRANCE_SUGGESTIONS)
            if (suggestions.isEmpty()) {
                Text(
                    text = "No fragrances match \"$query\"",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(ScentThemeExtras.spacing.xs),
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .collectionContainer(rowCount = suggestions.size)
                            .accessibleLiveRegion()
                            .accessibleState("${suggestions.size} fragrance suggestions available"),
                ) {
                    suggestions.forEachIndexed { index, fragrance ->
                        Text(
                            text = "${fragrance.brand} — ${fragrance.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .collectionItem(rowIndex = index)
                                    .accessibleClickable(
                                        label = "Use ${fragrance.brand} ${fragrance.name}",
                                        onClick = { onSelect(fragrance) },
                                    ).padding(ScentThemeExtras.spacing.sm),
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateListingEmptyPreview() {
    ScentTheme {
        CreateListingContent(
            formState = CreateListingFormState(),
            submitState = UiState.Idle,
            fragranceSuggestions = UiState.Idle,
            onBack = {},
            onAddPhotos = {},
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateListingFilledWithPhotosPreview() {
    val fragrance = Fragrance(id = 1, name = "Sauvage", brand = "Dior")
    val photos =
        listOf(
            ListingPhoto(
                id = 0,
                picked = PickedImage(bytes = ByteArray(0), contentType = "image/jpeg"),
                status = PhotoUploadStatus.Uploaded(mediaId = 1),
            ),
            ListingPhoto(
                id = 1,
                picked = PickedImage(bytes = ByteArray(0), contentType = "image/jpeg"),
                status = PhotoUploadStatus.Uploading,
            ),
            ListingPhoto(
                id = 2,
                picked = PickedImage(bytes = ByteArray(0), contentType = "image/jpeg"),
                status = PhotoUploadStatus.Failed(error = AppError.ContentError.UploadFailed()),
            ),
        )

    ScentTheme {
        CreateListingContent(
            formState =
                CreateListingFormState(
                    fragranceQuery = fragrance.name,
                    selectedFragrance = fragrance,
                    price = "120",
                    kind = ListingKind.OPENED,
                    remainingMl = "80",
                    nominalSizeMl = "100",
                    isNegotiable = true,
                    photos = photos,
                ),
            submitState = UiState.Idle,
            fragranceSuggestions = UiState.Idle,
            onBack = {},
            onAddPhotos = {},
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CreateListingValidationErrorPreview() {
    ScentTheme {
        CreateListingContent(
            formState = CreateListingFormState(price = "0"),
            submitState = UiState.Error(AppError.ValidationError.InvalidPrice(rawValue = "0")),
            fragranceSuggestions = UiState.Idle,
            onBack = {},
            onAddPhotos = {},
            onEvent = {},
        )
    }
}
