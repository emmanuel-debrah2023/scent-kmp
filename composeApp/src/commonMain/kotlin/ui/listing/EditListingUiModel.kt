package ui.listing

import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.usecase.MAX_LISTING_PHOTOS
import org.scent.project.domain.usecase.MIN_LISTING_PHOTOS
import org.scent.project.domain.usecase.UpdateListingUseCase

/** Mirrors [CreateListingEvent]'s shape — one sealed type per user action, routed through
 *  [EditListingViewModel.onEvent]. Fragrance isn't here: it isn't part of
 *  [org.scent.project.domain.model.UpdateListingParams], so it's display-only in edit mode. */
sealed interface EditListingEvent {
    data class PriceChange(
        val value: String,
    ) : EditListingEvent

    data class ConditionChange(
        val value: String,
    ) : EditListingEvent

    data class KindChange(
        val kind: ListingKind,
    ) : EditListingEvent

    data class NominalSizeChange(
        val value: String,
    ) : EditListingEvent

    data class RemainingMlChange(
        val value: String,
    ) : EditListingEvent

    data class NegotiableChange(
        val value: Boolean,
    ) : EditListingEvent

    data class StockQuantityChange(
        val value: String,
    ) : EditListingEvent

    data class ActiveChange(
        val value: Boolean,
    ) : EditListingEvent

    data class PhotoRemoved(
        val id: Int,
    ) : EditListingEvent

    data class PhotoMoved(
        val id: Int,
        val delta: Int,
    ) : EditListingEvent

    data object Submit : EditListingEvent

    data object Retry : EditListingEvent
}

data class EditListingFormState(
    /** Read-only display — fragrance identity isn't editable once a listing exists. */
    val fragranceDisplayName: String = "",
    val price: String = "",
    val condition: String = "NEW",
    val kind: ListingKind = ListingKind.OPENED,
    val nominalSizeMl: String = "",
    val remainingMl: String = "",
    val isNegotiable: Boolean = false,
    val stockQuantity: String = "1",
    val isActive: Boolean = true,
    val photos: List<ListingPhoto> = emptyList(),
) {
    /** Gates the submit button — server-side validation still runs in
     *  [UpdateListingUseCase]; this only blocks obviously-incomplete states. */
    val canSubmit: Boolean
        get() =
            photos.size in MIN_LISTING_PHOTOS..MAX_LISTING_PHOTOS &&
                photos.none { it.status is PhotoUploadStatus.Uploading }
}
