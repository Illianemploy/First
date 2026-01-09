package com.uktobacco

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uktobacco.data.SmokingProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TobaccoViewModel : ViewModel() {

    private val repository = TobaccoRepository()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        loadProducts()
        observeRealtimeUpdates()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                val products = repository.getAllProducts()

                if (products.isEmpty()) {
                    println("Warning: No products loaded from repository")
                }

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
            } catch (e: Exception) {
                println("Error: Failed to load products: ${e.message}")
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        products = emptyList(),
                        filteredProducts = emptyList()
                    )
                }
            }
        }
    }

    private fun observeRealtimeUpdates() {
        viewModelScope.launch {
            try {
                repository.observeProducts().collect { products ->
                    try {
                        _uiState.update { state ->
                            state.copy(products = products)
                        }
                        applyFiltersAndSort()
                    } catch (e: Exception) {
                        println("Error: Failed to update products in UI state: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("Error: Real-time product updates failed: ${e.message}")
                // Don't crash the app, just log the error
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
        try {
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
        } catch (e: Exception) {
            println("Error: Failed to apply filters and sorting: ${e.message}")
            // Keep the current filtered products if filtering fails
        }
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
        return try {
            if (productId.isBlank()) {
                println("Error: Product ID cannot be blank")
                return null
            }
            val product = _uiState.value.products.find { it.id == productId }
            if (product == null) {
                println("Warning: Product with ID '$productId' not found")
            }
            product
        } catch (e: Exception) {
            println("Error: Failed to get product by ID '$productId': ${e.message}")
            null
        }
    }

    fun updateSmokingProfile(profile: SmokingProfile) {
        _uiState.update { it.copy(smokingProfile = profile) }
    }

    fun getSmokingProfile(): SmokingProfile? {
        return _uiState.value.smokingProfile
    }
}
