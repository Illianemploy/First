package com.uktobacco.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val CIGARETTES_PER_DAY = intPreferencesKey("cigarettes_per_day")
        val PREFERRED_BRAND = stringPreferencesKey("preferred_brand")
        val PRICE_PER_PACK = doublePreferencesKey("price_per_pack")
        val PACKS_PER_MONTH = doublePreferencesKey("packs_per_month")
        val MONTHLY_EXPENDITURE = doublePreferencesKey("monthly_expenditure")
        val YEARLY_EXPENDITURE = doublePreferencesKey("yearly_expenditure")
    }

    val smokingProfileFlow: Flow<SmokingProfile?> = context.dataStore.data.map { preferences ->
        val cigarettesPerDay = preferences[PreferencesKeys.CIGARETTES_PER_DAY]

        if (cigarettesPerDay == null) {
            null
        } else {
            SmokingProfile(
                cigarettesPerDay = cigarettesPerDay,
                preferredBrand = preferences[PreferencesKeys.PREFERRED_BRAND] ?: "",
                pricePerPack = preferences[PreferencesKeys.PRICE_PER_PACK] ?: 14.00,
                packsPerMonth = preferences[PreferencesKeys.PACKS_PER_MONTH] ?: 0.0,
                monthlyExpenditure = preferences[PreferencesKeys.MONTHLY_EXPENDITURE] ?: 0.0,
                yearlyExpenditure = preferences[PreferencesKeys.YEARLY_EXPENDITURE] ?: 0.0
            )
        }
    }

    suspend fun saveSmokingProfile(profile: SmokingProfile) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CIGARETTES_PER_DAY] = profile.cigarettesPerDay
            preferences[PreferencesKeys.PREFERRED_BRAND] = profile.preferredBrand
            preferences[PreferencesKeys.PRICE_PER_PACK] = profile.pricePerPack
            preferences[PreferencesKeys.PACKS_PER_MONTH] = profile.packsPerMonth
            preferences[PreferencesKeys.MONTHLY_EXPENDITURE] = profile.monthlyExpenditure
            preferences[PreferencesKeys.YEARLY_EXPENDITURE] = profile.yearlyExpenditure
        }
    }

    suspend fun clearSmokingProfile() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.CIGARETTES_PER_DAY)
            preferences.remove(PreferencesKeys.PREFERRED_BRAND)
            preferences.remove(PreferencesKeys.PRICE_PER_PACK)
            preferences.remove(PreferencesKeys.PACKS_PER_MONTH)
            preferences.remove(PreferencesKeys.MONTHLY_EXPENDITURE)
            preferences.remove(PreferencesKeys.YEARLY_EXPENDITURE)
        }
    }
}
