package ui.listing

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.scent.project.domain.error.AppError
import org.scent.project.domain.model.CreateListingParams
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.model.Listing
import org.scent.project.domain.model.ListingKind
import org.scent.project.domain.usecase.CreateListingUseCase
import org.scent.project.domain.usecase.MAX_LISTING_PHOTOS
import org.scent.project.domain.usecase.SearchFragrancesUseCase
import org.scent.project.domain.usecase.UploadListingPhotoUseCase
import ui.base.BaseViewModel
import ui.base.TypeaheadEngine
import ui.base.UiState
import ui.media.PickedImage

private const val MIN_FRAGRANCE_QUERY_LENGTH = 2
private const val FRAGRANCE_SUGGESTION_LIMIT = 8

/**
 * Drives the Create Listing form. Two independent [StateFlow]s rather than one combined
 * state, matching [ui.auth.AuthViewModel]'s login/register split: [formState] is "always
 * there" (not a [UiState]) while the user edits, [submitState] tracks only the create
 * round-trip. Fragrance typeahead is embedded directly via [TypeaheadEngine] rather than a
 * second ViewModel — see [ui.marketplace.BrandSuggestionViewModel]'s doc comment for why
 * that split exists there (independent grid lifecycle) and doesn't apply here.
 */
class CreateListingViewModel(
    private val createListingUseCase: CreateListingUseCase,
    private val uploadListingPhotoUseCase: UploadListingPhotoUseCase,
    searchFragrancesUseCase: SearchFragrancesUseCase,
) : BaseViewModel() {
    private val _formState = MutableStateFlow(CreateListingFormState())
    val formState: StateFlow<CreateListingFormState> = _formState.asStateFlow()

    private val _submitState = MutableStateFlow<UiState<Listing>>(UiState.Idle)
    val submitState: StateFlow<UiState<Listing>> = _submitState.asStateFlow()

    private var nextPhotoId = 0

    private val fragranceEngine =
        TypeaheadEngine<Fragrance>(
            scope = viewModelScope,
            minQueryLength = MIN_FRAGRANCE_QUERY_LENGTH,
        ) { query -> searchFragrancesUseCase(query = query, limit = FRAGRANCE_SUGGESTION_LIMIT) }

    val fragranceSuggestions: StateFlow<UiState<List<Fragrance>>> = fragranceEngine.uiState

    fun onFragranceQueryChange(query: String) {
        _formState.update { it.copy(fragranceQuery = query, selectedFragrance = null) }
        fragranceEngine.onQueryChange(query)
    }

    fun onFragranceSelected(fragrance: Fragrance) {
        _formState.update { it.copy(fragranceQuery = fragrance.name, selectedFragrance = fragrance) }
        fragranceEngine.onSuggestionAccepted(fragrance.name)
    }

    fun onFragranceSuggestionRetry() = fragranceEngine.onRetry()

    fun onPriceChange(price: String) = _formState.update { it.copy(price = price) }

    fun onConditionChange(condition: String) = _formState.update { it.copy(condition = condition) }

    fun onKindChange(kind: ListingKind) = _formState.update { it.copy(kind = kind) }

    fun onNominalSizeChange(value: String) = _formState.update { it.copy(nominalSizeMl = value) }

    fun onRemainingMlChange(value: String) = _formState.update { it.copy(remainingMl = value) }

    fun onNegotiableChange(value: Boolean) = _formState.update { it.copy(isNegotiable = value) }

    fun onStockQuantityChange(value: String) = _formState.update { it.copy(stockQuantity = value) }

    fun onPhotosPicked(picked: List<PickedImage>) {
        val remainingSlots = MAX_LISTING_PHOTOS - _formState.value.photos.size
        val newPhotos =
            picked.take(remainingSlots).map { image ->
                ListingPhoto(id = nextPhotoId++, picked = image, status = PhotoUploadStatus.Uploading)
            }
        _formState.update { it.copy(photos = it.photos + newPhotos) }
        newPhotos.forEach(::upload)
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

    private fun upload(photo: ListingPhoto) {
        viewModelScope.launch {
            uploadListingPhotoUseCase(
                bytes = photo.picked.bytes,
                contentType = photo.picked.contentType,
                onProgress = { bytesSent, totalBytes ->
                    updatePhoto(photo.id) { it.copy(bytesSent = bytesSent, totalBytes = totalBytes) }
                },
            ).handleResult(
                onSuccess = { mediaId ->
                    updatePhoto(photo.id) { it.copy(status = PhotoUploadStatus.Uploaded(mediaId)) }
                },
                onError = { error ->
                    updatePhoto(photo.id) { it.copy(status = PhotoUploadStatus.Failed(error)) }
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
        val fragrance =
            state.selectedFragrance ?: run {
                _submitState.value = UiState.Error(AppError.ValidationError.RequiredFieldEmpty(fieldName = "fragrance"))
                return
            }

        val mediaIds = state.photos.mapNotNull { (it.status as? PhotoUploadStatus.Uploaded)?.mediaId }
        if (mediaIds.size != state.photos.size) {
            // canSubmit already gates the button on this; guarded again since submit()
            // is a public entry point a test (or a future caller) can invoke directly.
            _submitState.value = UiState.Error(AppError.ValidationError.InvalidInput(fieldName = "photos"))
            return
        }

        viewModelScope.launch {
            _submitState.value = UiState.Loading
            createListingUseCase(
                CreateListingParams(
                    fragranceId = fragrance.id,
                    price = state.price.toDoubleOrNull() ?: 0.0,
                    condition = state.condition,
                    isNegotiable = state.isNegotiable,
                    stockQuantity = state.stockQuantity.toIntOrNull() ?: 1,
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

    fun resetSubmitState() {
        _submitState.value = UiState.Idle
    }

    fun onEvent(event: CreateListingEvent) {
        when (event) {
            is CreateListingEvent.FragranceQueryChange -> onFragranceQueryChange(event.query)
            is CreateListingEvent.FragranceSelected -> onFragranceSelected(event.fragrance)
            CreateListingEvent.FragranceSuggestionRetry -> onFragranceSuggestionRetry()
            is CreateListingEvent.PriceChange -> onPriceChange(event.value)
            is CreateListingEvent.ConditionChange -> onConditionChange(event.value)
            is CreateListingEvent.KindChange -> onKindChange(event.kind)
            is CreateListingEvent.NominalSizeChange -> onNominalSizeChange(event.value)
            is CreateListingEvent.RemainingMlChange -> onRemainingMlChange(event.value)
            is CreateListingEvent.NegotiableChange -> onNegotiableChange(event.value)
            is CreateListingEvent.StockQuantityChange -> onStockQuantityChange(event.value)
            is CreateListingEvent.PhotoRemoved -> onPhotoRemoved(event.id)
            is CreateListingEvent.PhotoMoved -> onPhotoMoved(event.id, event.delta)
            CreateListingEvent.Submit -> submit()
        }
    }
}
