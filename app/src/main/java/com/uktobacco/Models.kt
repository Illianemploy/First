package com.uktobacco

enum class FilterType {
    ALL,
    CIGARETTES,
    ROLLING_TOBACCO
}

enum class SortOption {
    PRICE_LOW_TO_HIGH,
    PRICE_HIGH_TO_LOW,
    NAME_A_TO_Z,
    BRAND,
    RETAILER,
    LAST_UPDATED
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
    val smokingProfile: com.uktobacco.data.SmokingProfile? = null
)
