package ui.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
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
import org.koin.core.parameter.parametersOf
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.usecase.MAX_LISTING_PHOTOS
import ui.accessibility.accessibleLiveRegion
import ui.accessibility.accessiblePane
import ui.accessibility.accessibleToggle
import ui.base.UiState
import ui.components.CONDITION_OPTIONS
import ui.components.ErrorState
import ui.components.ErrorStateVariant
import ui.components.ScentTextField
import ui.components.SelectableChip
import ui.components.buttons.ScentPrimaryButton
import ui.components.buttons.ScentSecondaryButton
import ui.media.rememberImagePicker
import ui.theme.ScentTheme
import ui.theme.ScentThemeExtras

@Composable
fun EditListingScreen(
    listingId: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Keyed by listingId: koinViewModel caches by class alone otherwise, so editing a
    // different listing (or reopening this one) would reuse a stale cached instance —
    // wrong listing's data, or a submitState left over from a prior successful save.
    val viewModel: EditListingViewModel =
        koinViewModel(key = "EditListing-$listingId", parameters = { parametersOf(listingId) })
    val loadState by viewModel.loadState.collectAsState()
    val formState by viewModel.formState.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    LaunchedEffect(submitState) {
        if (submitState is UiState.Success<Listing>) {
            // Reset before navigating: a cached instance whose submitState is still
            // Success would otherwise re-fire this effect the instant this screen (or
            // another edit of the same listing) reopens, bouncing straight back out.
            viewModel.resetSubmitState()
            onSaved()
        }
    }

    // See CreateListingScreen's identical comment: MAX_LISTING_PHOTOS, not the remaining
    // count — PickMultipleVisualMedia requires maxItems > 1 and throws otherwise.
    val imagePicker =
        rememberImagePicker(maxItems = MAX_LISTING_PHOTOS) { picked ->
            viewModel.onPhotosPicked(picked)
        }

    when (loadState) {
        is UiState.Idle, is UiState.Loading ->
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        is UiState.Error -> {
            val error = (loadState as UiState.Error).error
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                ErrorState(
                    variant = ErrorStateVariant.Error,
                    title = "Couldn't load this listing",
                    message = error.message,
                    actionLabel = "RETRY",
                    onAction = { viewModel.onEvent(EditListingEvent.Retry) },
                )
            }
        }
        is UiState.Success ->
            EditListingContent(
                formState = formState,
                submitState = submitState,
                onBack = onBack,
                onAddPhotos = imagePicker::launch,
                onEvent = viewModel::onEvent,
                modifier = modifier,
            )
    }
}

/** Mirrors [CreateListingContent]: stateless, driven by a single [EditListingEvent]
 *  callback, so it stays previewable without a ViewModel. */
@Composable
private fun EditListingContent(
    formState: EditListingFormState,
    submitState: UiState<Listing>,
    onBack: () -> Unit,
    onAddPhotos: () -> Unit,
    onEvent: (EditListingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val submitError = (submitState as? UiState.Error)?.error

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .accessiblePane("Edit listing")
                .padding(ScentThemeExtras.spacing.md),
        verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(text = "Edit Listing", style = MaterialTheme.typography.titleLarge)
        }

        Text(
            text = formState.fragranceDisplayName,
            style = MaterialTheme.typography.titleMedium,
        )

        ListingPhotoGrid(
            photos = formState.photos,
            onRemove = { id -> onEvent(EditListingEvent.PhotoRemoved(id)) },
            onMove = { id, delta -> onEvent(EditListingEvent.PhotoMoved(id, delta)) },
        )
        if (formState.photos.size < MAX_LISTING_PHOTOS) {
            ScentSecondaryButton(text = "Add photos", onClick = onAddPhotos)
        }

        ScentTextField(
            value = formState.price,
            onValueChange = { value -> onEvent(EditListingEvent.PriceChange(value)) },
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
                        onClick = { onEvent(EditListingEvent.ConditionChange(value)) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs)) {
            Text(text = "Kind", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(ScentThemeExtras.spacing.xs)) {
                ListingKind.entries.forEach { kind ->
                    SelectableChip(
                        label = kind.displayLabel(),
                        selected = formState.kind == kind,
                        onClick = { onEvent(EditListingEvent.KindChange(kind)) },
                    )
                }
            }
        }

        ScentTextField(
            value = formState.nominalSizeMl,
            onValueChange = { value -> onEvent(EditListingEvent.NominalSizeChange(value)) },
            label = "Bottle size (ml)",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            error = (submitError as? AppError.ValidationError.MissingNominalSize)?.message,
        )

        // Same reasoning as CreateListingScreen: SEALED/DECANT fill is derived server-side.
        if (formState.kind == ListingKind.OPENED || formState.kind == ListingKind.TESTER) {
            ScentTextField(
                value = formState.remainingMl,
                onValueChange = { value -> onEvent(EditListingEvent.RemainingMlChange(value)) },
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
                onCheckedChange = { value -> onEvent(EditListingEvent.NegotiableChange(value)) },
                modifier =
                    Modifier.accessibleToggle(
                        value = formState.isNegotiable,
                        label = "Negotiable",
                        onValueChange = { value -> onEvent(EditListingEvent.NegotiableChange(value)) },
                    ),
            )
        }

        ScentTextField(
            value = formState.stockQuantity,
            onValueChange = { value -> onEvent(EditListingEvent.StockQuantityChange(value)) },
            label = "Quantity",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Active", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = formState.isActive,
                onCheckedChange = { value -> onEvent(EditListingEvent.ActiveChange(value)) },
                modifier =
                    Modifier.accessibleToggle(
                        value = formState.isActive,
                        label = "Active",
                        onValueChange = { value -> onEvent(EditListingEvent.ActiveChange(value)) },
                    ),
            )
        }

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
            text = if (submitState is UiState.Loading) "Saving…" else "Save changes",
            onClick = { onEvent(EditListingEvent.Submit) },
            enabled = formState.canSubmit && submitState !is UiState.Loading,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditListingLoadedPreview() {
    val photos =
        listOf(
            ListingPhoto(
                id = 101,
                source = PhotoSource.Remote("https://example.com/photo-1.jpg"),
                status = PhotoUploadStatus.Uploaded(mediaId = 101),
            ),
            ListingPhoto(
                id = 102,
                source = PhotoSource.Remote("https://example.com/photo-2.jpg"),
                status = PhotoUploadStatus.Uploaded(mediaId = 102),
            ),
        )

    ScentTheme {
        EditListingContent(
            formState =
                EditListingFormState(
                    fragranceDisplayName = "Dior — Sauvage",
                    price = "120",
                    kind = ListingKind.OPENED,
                    remainingMl = "80",
                    nominalSizeMl = "100",
                    isNegotiable = true,
                    isActive = true,
                    photos = photos,
                ),
            submitState = UiState.Idle,
            onBack = {},
            onAddPhotos = {},
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EditListingValidationErrorPreview() {
    ScentTheme {
        EditListingContent(
            formState = EditListingFormState(fragranceDisplayName = "Dior — Sauvage", price = "0"),
            submitState = UiState.Error(AppError.ValidationError.InvalidPrice(rawValue = "0")),
            onBack = {},
            onAddPhotos = {},
            onEvent = {},
        )
    }
}
