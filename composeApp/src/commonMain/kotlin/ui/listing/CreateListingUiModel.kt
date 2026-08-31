package ui.listing

import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.usecase.CreateListingUseCase
import org.scent.project.domain.usecase.MAX_LISTING_PHOTOS
import org.scent.project.domain.usecase.MIN_LISTING_PHOTOS

/** Mirrors [ui.profile.ProfileEvent]: one sealed type per user action, routed through
 *  [CreateListingViewModel.onEvent] so [CreateListingContent][ui.listing.CreateListingScreen]
 *  takes a single callback instead of one per field. */
sealed interface CreateListingEvent {
    data class FragranceQueryChange(
        val query: String,
    ) : CreateListingEvent

    data class FragranceSelected(
        val fragrance: Fragrance,
    ) : CreateListingEvent

    data object FragranceSuggestionRetry : CreateListingEvent

    data class PriceChange(
        val value: String,
    ) : CreateListingEvent

    data class ConditionChange(
        val value: String,
    ) : CreateListingEvent

    data class KindChange(
        val kind: ListingKind,
    ) : CreateListingEvent

    data class NominalSizeChange(
        val value: String,
    ) : CreateListingEvent

    data class RemainingMlChange(
        val value: String,
    ) : CreateListingEvent

    data class NegotiableChange(
        val value: Boolean,
    ) : CreateListingEvent

    data class StockQuantityChange(
        val value: String,
    ) : CreateListingEvent

    data class PhotoRemoved(
        val id: Int,
    ) : CreateListingEvent

    data class PhotoMoved(
        val id: Int,
        val delta: Int,
    ) : CreateListingEvent

    data object Submit : CreateListingEvent
}

data class CreateListingFormState(
    val fragranceQuery: String = "",
    val selectedFragrance: Fragrance? = null,
    val price: String = "",
    val condition: String = "NEW",
    val kind: ListingKind = ListingKind.OPENED,
    val nominalSizeMl: String = "",
    val remainingMl: String = "",
    val isNegotiable: Boolean = false,
    val stockQuantity: String = "1",
    val photos: List<ListingPhoto> = emptyList(),
) {
    /** Gates the submit button — server-side validation (price, fill, photo bounds)
     *  still runs in [CreateListingUseCase]; this only blocks obviously-incomplete states. */
    val canSubmit: Boolean
        get() =
            selectedFragrance != null &&
                photos.size in MIN_LISTING_PHOTOS..MAX_LISTING_PHOTOS &&
                photos.none { it.status is PhotoUploadStatus.Uploading }
}
