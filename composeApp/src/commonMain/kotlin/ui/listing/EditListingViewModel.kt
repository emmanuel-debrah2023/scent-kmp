package ui.listing

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.model.UpdateListingParams
import org.scent.project.domain.usecase.GetListingUseCase
import org.scent.project.domain.usecase.MAX_LISTING_PHOTOS
import org.scent.project.domain.usecase.UpdateListingUseCase
import org.scent.project.domain.usecase.UploadListingPhotoUseCase
import ui.base.BaseViewModel
import ui.base.UiState
import ui.media.PickedImage

/**
 * Drives the Edit Listing form. Three independent [StateFlow]s: [loadState] gates the
 * screen on the initial [GetListingUseCase] fetch (loading/error/loaded), [formState] is
 * "always there" once loaded, [submitState] tracks only the update round-trip — same split
 * as [CreateListingViewModel], with an extra load phase since there's something to fetch.
 */
class EditListingViewModel(
    private val listingId: Int,
    private val getListingUseCase: GetListingUseCase,
    private val updateListingUseCase: UpdateListingUseCase,
    private val uploadListingPhotoUseCase: UploadListingPhotoUseCase,
) : BaseViewModel() {
    private val _loadState = MutableStateFlow<UiState<Listing>>(UiState.Loading)
    val loadState: StateFlow<UiState<Listing>> = _loadState.asStateFlow()

    private val _formState = MutableStateFlow(EditListingFormState())
    val formState: StateFlow<EditListingFormState> = _formState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<Listing>>(UiState.Idle)
    val submitState: StateFlow<UiState<Listing>> = _submitState.asStateFlow()

    /** Negative and decreasing so a locally-added photo's id can never collide with a real
     *  (positive) media item id from [Listing.mediaIds]. */
    private var nextLocalPhotoId = -1

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _loadState.value = UiState.Loading
            getListingUseCase(listingId).handleResult(
                onSuccess = { listing ->
                    _loadState.value = UiState.Success(listing)
                    _formState.value = listing.toFormState()
                },
                onError = { error -> _loadState.value = UiState.Error(error) },
            )
        }
    }

    private fun Listing.toFormState(): EditListingFormState =
        EditListingFormState(
            fragranceDisplayName = "${fragrance.brand} ${fragrance.name}",
            price = price.toString(),
            condition = condition,
            kind = kind,
            nominalSizeMl = nominalSizeMl?.toString() ?: "",
            remainingMl = remainingMl?.toString() ?: "",
            isNegotiable = isNegotiable,
            stockQuantity = stockQuantity.toString(),
            isActive = isActive,
            // Empty mediaIds means photoUrls fell back to catalogue stock imagery — nothing
            // listing-owned to show as editable here (see Listing.mediaIds's doc comment).
            photos =
                if (mediaIds.isEmpty()) {
                    emptyList()
                } else {
                    mediaIds.zip(photoUrls) { mediaId, url ->
                        ListingPhoto(
                            id = mediaId,
                            source = PhotoSource.Remote(url),
                            status = PhotoUploadStatus.Uploaded(mediaId),
                        )
                    }
                },
        )

    fun onPriceChange(price: String) = _formState.update { it.copy(price = price) }

    fun onConditionChange(condition: String) = _formState.update { it.copy(condition = condition) }

    fun onKindChange(kind: ListingKind) = _formState.update { it.copy(kind = kind) }

    fun onNominalSizeChange(value: String) = _formState.update { it.copy(nominalSizeMl = value) }

    fun onRemainingMlChange(value: String) = _formState.update { it.copy(remainingMl = value) }

    fun onNegotiableChange(value: Boolean) = _formState.update { it.copy(isNegotiable = value) }

    fun onStockQuantityChange(value: String) = _formState.update { it.copy(stockQuantity = value) }

    fun onActiveChange(value: Boolean) = _formState.update { it.copy(isActive = value) }

    fun onPhotosPicked(picked: List<PickedImage>) {
        val remainingSlots = MAX_LISTING_PHOTOS - _formState.value.photos.size
        val accepted = picked.take(remainingSlots)
        val newPhotos =
            accepted.map { image ->
                ListingPhoto(
                    id = nextLocalPhotoId--,
                    source = PhotoSource.Local(image),
                    status = PhotoUploadStatus.Uploading,
                )
            }
        _formState.update { it.copy(photos = it.photos + newPhotos) }
        newPhotos.zip(accepted).forEach { (photo, image) -> upload(photo.id, image) }
    }

    fun onPhotoRemoved(id: Int) {
        _formState.update { it.copy(photos = it.photos.filterNot { photo -> photo.id == id }) }
    }

    /** Moves the photo at [id] by [delta] positions (-1 = left/earlier, +1 = right/later). */
    fun onPhotoMoved(
        id: Int,
        delta: Int,
    ) {
        _formState.update { state ->
            val index = state.photos.indexOfFirst { it.id == id }
            val target = index + delta
            if (index < 0 || target !in state.photos.indices) {
                state
            } else {
                val reordered = state.photos.toMutableList()
                reordered.add(target, reordered.removeAt(index))
                state.copy(photos = reordered)
            }
        }
    }

    private fun upload(
        id: Int,
        picked: PickedImage,
    ) {
        viewModelScope.launch {
            uploadListingPhotoUseCase(
                bytes = picked.bytes,
                contentType = picked.contentType,
                onProgress = { bytesSent, totalBytes ->
                    updatePhoto(id) { it.copy(bytesSent = bytesSent, totalBytes = totalBytes) }
                },
            ).handleResult(
                onSuccess = { mediaId ->
                    updatePhoto(id) { it.copy(status = PhotoUploadStatus.Uploaded(mediaId)) }
                },
                onError = { error ->
                    updatePhoto(id) { it.copy(status = PhotoUploadStatus.Failed(error)) }
                },
            )
        }
    }

    private fun updatePhoto(
        id: Int,
        transform: (ListingPhoto) -> ListingPhoto,
    ) {
        _formState.update { state ->
            state.copy(photos = state.photos.map { photo -> if (photo.id == id) transform(photo) else photo })
        }
    }

    fun submit() {
        val state = _formState.value
        val mediaIds = state.photos.mapNotNull { (it.status as? PhotoUploadStatus.Uploaded)?.mediaId }
        if (mediaIds.size != state.photos.size) {
            // canSubmit already gates the button on this; guarded again since submit()
            // is a public entry point a test (or a future caller) can invoke directly.
            _submitState.value = UiState.Error(AppError.ValidationError.InvalidInput(fieldName = "photos"))
            return
        }

        viewModelScope.launch {
            _submitState.value = UiState.Loading
            updateListingUseCase(
                id = listingId,
                params =
                    UpdateListingParams(
                        price = state.price.toDoubleOrNull() ?: 0.0,
                        condition = state.condition,
                        isNegotiable = state.isNegotiable,
                        stockQuantity = state.stockQuantity.toIntOrNull() ?: 1,
                        isActive = state.isActive,
                        mediaIds = mediaIds,
                        kind = state.kind,
                        nominalSizeMl = state.nominalSizeMl.toIntOrNull(),
                        remainingMl = state.remainingMl.toIntOrNull(),
                    ),
            ).handleResult(
                onSuccess = { listing -> _submitState.value = UiState.Success(listing) },
                onError = { error -> _submitState.value = UiState.Error(error) },
            )
        }
    }

    /** [koinViewModel][ui.listing.EditListingScreen] caches this instance for the life of
     *  the ViewModelStore, keyed only by [listingId] — reopening the same listing after a
     *  successful save would otherwise find [submitState] still [UiState.Success] and
     *  immediately re-navigate away. The screen calls this right after consuming that
     *  success so a later reopen starts clean. */
    fun resetSubmitState() {
        _submitState.value = UiState.Idle
    }

    fun onEvent(event: EditListingEvent) {
        when (event) {
            is EditListingEvent.PriceChange -> onPriceChange(event.value)
            is EditListingEvent.ConditionChange -> onConditionChange(event.value)
            is EditListingEvent.KindChange -> onKindChange(event.kind)
            is EditListingEvent.NominalSizeChange -> onNominalSizeChange(event.value)
            is EditListingEvent.RemainingMlChange -> onRemainingMlChange(event.value)
            is EditListingEvent.NegotiableChange -> onNegotiableChange(event.value)
            is EditListingEvent.StockQuantityChange -> onStockQuantityChange(event.value)
            is EditListingEvent.ActiveChange -> onActiveChange(event.value)
            is EditListingEvent.PhotoRemoved -> onPhotoRemoved(event.id)
            is EditListingEvent.PhotoMoved -> onPhotoMoved(event.id, event.delta)
            EditListingEvent.Submit -> submit()
            EditListingEvent.Retry -> load()
        }
    }
}
