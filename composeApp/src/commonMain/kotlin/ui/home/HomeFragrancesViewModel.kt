package ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.scent.project.domain.model.Fragrance
import org.scent.project.domain.usecase.GetLatestVideoFeatureUseCase
import org.scent.project.domain.usecase.SearchFragrancesUseCase
import ui.base.BaseViewModel
import ui.base.UiState

class HomeFragrancesViewModel(
    private val searchFragrancesUseCase: SearchFragrancesUseCase,
    private val getLatestVideoFeatureUseCase: GetLatestVideoFeatureUseCase,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow<UiState<HomeFragrancesState>>(UiState.Idle)
    val uiState: StateFlow<UiState<HomeFragrancesState>> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun load() {
        loadJob?.cancel()
        _uiState.value = UiState.Loading
        loadJob =
            viewModelScope.launch {
                // An empty query is the newest active fragrances; see FragranceRepository.searchFragrances.
                searchFragrancesUseCase(query = "").fold(
                    ifLeft = { error -> _uiState.value = UiState.Error(error) },
                    ifRight = { fragrances ->
                        val hero = findHero(fragrances)
                        _uiState.value =
                            UiState.Success(
                                HomeFragrancesState(
                                    hero = hero,
                                    cards = fragrances.map { it.toFragranceCardUi() },
                                ),
                            )
                    },
                )
            }
    }

    fun retry() {
        load()
    }

    // The hero is decoration on top of the list, so a failure is reported through [error]
    // and leaves the tab without a hero instead of failing it.
    private suspend fun findHero(fragrances: List<Fragrance>): HeroUi? =
        getLatestVideoFeatureUseCase(fragrances).fold(
            ifLeft = { error ->
                handleError(error)
                null
            },
            ifRight = { featured -> featured?.let { it.fragrance.toHeroUi(it.postId) } },
        )
}
