package com.uktobacco

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.uktobacco.data.UserPreferencesRepository

class TobaccoViewModelFactory(
    private val preferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TobaccoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TobaccoViewModel(preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
