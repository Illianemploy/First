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

val Context.profileDataStore: DataStore<Preferences> by preferencesDataStore(name = "smoking_profile")

class ProfileDataStore(private val context: Context) {

    companion object {
        private val CIGARETTES_PER_DAY = intPreferencesKey("cigarettes_per_day")
        private val PREFERRED_BRAND = stringPreferencesKey("preferred_brand")
        private val PRICE_PER_PACK = doublePreferencesKey("price_per_pack")
        private val PACKS_PER_MONTH = doublePreferencesKey("packs_per_month")
        private val MONTHLY_EXPENDITURE = doublePreferencesKey("monthly_expenditure")
        private val YEARLY_EXPENDITURE = doublePreferencesKey("yearly_expenditure")
    }

    val smokingProfileFlow: Flow<SmokingProfile?> = context.profileDataStore.data
        .map { preferences ->
            val cigarettesPerDay = preferences[CIGARETTES_PER_DAY]
            if (cigarettesPerDay == null || cigarettesPerDay == 0) {
                null
            } else {
                SmokingProfile(
                    cigarettesPerDay = cigarettesPerDay,
                    preferredBrand = preferences[PREFERRED_BRAND] ?: "",
                    pricePerPack = preferences[PRICE_PER_PACK] ?: 14.00,
                    packsPerMonth = preferences[PACKS_PER_MONTH] ?: 0.0,
                    monthlyExpenditure = preferences[MONTHLY_EXPENDITURE] ?: 0.0,
                    yearlyExpenditure = preferences[YEARLY_EXPENDITURE] ?: 0.0
                )
            }
        }

    suspend fun saveProfile(profile: SmokingProfile) {
        context.profileDataStore.edit { preferences ->
            preferences[CIGARETTES_PER_DAY] = profile.cigarettesPerDay
            preferences[PREFERRED_BRAND] = profile.preferredBrand
            preferences[PRICE_PER_PACK] = profile.pricePerPack
            preferences[PACKS_PER_MONTH] = profile.packsPerMonth
            preferences[MONTHLY_EXPENDITURE] = profile.monthlyExpenditure
            preferences[YEARLY_EXPENDITURE] = profile.yearlyExpenditure
        }
    }

    suspend fun clearProfile() {
        context.profileDataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
