package com.uktobacco

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uktobacco.data.SmokingProfile
import com.uktobacco.data.UserPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SortOption {
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    NAME_A_TO_Z,
    BRAND,
    RETAILER,
    LAST_UPDATED
}

enum class FilterType {
    ALL,
    CIGARETTES,
    ROLLING_TOBACCO
}

data class UiState(
    val products: List<TobaccoProduct> = emptyList(),
    val filteredProducts: List<TobaccoProduct> = emptyList(),
    val searchQuery: String = "",
    val selectedType: FilterType = FilterType.ALL,
    val selectedRetailer: String? = null,
    val sortOption: SortOption = SortOption.PRICE_LOW_TO_HIGH,
    val isLoading: Boolean = true,
    val availableRetailers: List<String> = emptyList(),
    val favorites: Set<String> = emptySet(),
    val smokingProfile: SmokingProfile? = null
)

class TobaccoViewModel(
    private val preferencesRepository: UserPreferencesRepository? = null
) : ViewModel() {

    private val repository = TobaccoRepository()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        observeRealtimeUpdates()
        loadSmokingProfile()
    }

    private fun loadSmokingProfile() {
        preferencesRepository?.let { repo ->
            viewModelScope.launch {
                repo.smokingProfileFlow.collect { profile ->
                    _uiState.update { it.copy(smokingProfile = profile) }
                }
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            val products = repository.getAllProducts()
            val retailers = products.map { it.retailer }.distinct().sorted()

            _uiState.update { state ->
                state.copy(
                    products = products,
                    filteredProducts = products,
                    availableRetailers = retailers,
                    isLoading = false
                )
            }
            applyFiltersAndSort()
        }
    }

    private fun observeRealtimeUpdates() {
        viewModelScope.launch {
            repository.observeProducts().collect { products ->
                _uiState.update { state ->
                    state.copy(products = products)
                }
                applyFiltersAndSort()
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFiltersAndSort()
    }

    fun onTypeFilterChange(type: FilterType) {
        _uiState.update { it.copy(selectedType = type) }
        applyFiltersAndSort()
    }

    fun onRetailerFilterChange(retailer: String?) {
        _uiState.update { it.copy(selectedRetailer = retailer) }
        applyFiltersAndSort()
    }

    fun onSortOptionChange(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        val state = _uiState.value
        var filtered = state.products

        // Apply search filter
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                it.brand.contains(state.searchQuery, ignoreCase = true) ||
                it.retailer.contains(state.searchQuery, ignoreCase = true)
            }
        }

        // Apply type filter
        filtered = when (state.selectedType) {
            FilterType.CIGARETTES -> filtered.filter { it.type == TobaccoType.CIGARETTES }
            FilterType.ROLLING_TOBACCO -> filtered.filter { it.type == TobaccoType.ROLLING_TOBACCO }
            FilterType.ALL -> filtered
        }

        // Apply retailer filter
        state.selectedRetailer?.let { retailer ->
            filtered = filtered.filter { it.retailer == retailer }
        }

        // Apply sorting
        filtered = when (state.sortOption) {
            SortOption.PRICE_LOW_TO_HIGH -> filtered.sortedBy { it.price }
            SortOption.PRICE_HIGH_TO_LOW -> filtered.sortedByDescending { it.price }
            SortOption.NAME_A_TO_Z -> filtered.sortedBy { it.name }
            SortOption.BRAND -> filtered.sortedBy { it.brand }
            SortOption.RETAILER -> filtered.sortedBy { it.retailer }
            SortOption.LAST_UPDATED -> filtered.sortedByDescending { it.lastUpdated }
        }

        _uiState.update { it.copy(filteredProducts = filtered) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                selectedType = FilterType.ALL,
                selectedRetailer = null,
                sortOption = SortOption.PRICE_LOW_TO_HIGH
            )
        }
        applyFiltersAndSort()
    }

    fun toggleFavorite(productId: String) {
        _uiState.update { state ->
            val newFavorites = if (productId in state.favorites) {
                state.favorites - productId
            } else {
                state.favorites + productId
            }
            state.copy(favorites = newFavorites)
        }
    }

    fun isFavorite(productId: String): Boolean {
        return productId in _uiState.value.favorites
    }

    fun getFavoriteProducts(): List<TobaccoProduct> {
        return _uiState.value.products.filter { it.id in _uiState.value.favorites }
    }

    fun getProductById(productId: String): TobaccoProduct? {
        return _uiState.value.products.find { it.id == productId }
    }

    fun updateSmokingProfile(profile: SmokingProfile) {
        _uiState.update { it.copy(smokingProfile = profile) }
        viewModelScope.launch {
            preferencesRepository?.saveSmokingProfile(profile)
        }
    }

    fun getSmokingProfile(): SmokingProfile? {
        return _uiState.value.smokingProfile
    }

    fun clearSmokingProfile() {
        _uiState.update { it.copy(smokingProfile = null) }
        viewModelScope.launch {
            preferencesRepository?.clearSmokingProfile()
        }
    }
}
