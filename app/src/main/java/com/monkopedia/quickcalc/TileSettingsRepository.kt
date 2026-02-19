/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.monkopedia.quickcalc

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.tileSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tile_settings"
)

class TileSettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.tileSettingsDataStore

    val settingsFlow: Flow<TileSettings> = dataStore.data.map { preferences ->
        TileSettings(
            themeMode =
            preferences[KEY_THEME_MODE]
                ?.runCatching { TileThemeMode.valueOf(this) }
                ?.getOrNull()
                ?: TileThemeMode.SYSTEM,
            accentColorArgb = preferences[KEY_ACCENT_COLOR_ARGB] ?: DEFAULT_ACCENT_COLOR_ARGB,
            sizeMode =
            preferences[KEY_SIZE_MODE]
                ?.runCatching { TileSizeMode.valueOf(this) }
                ?.getOrNull()
                ?: TileSizeMode.STATIC,
            staticAlignment =
            preferences[KEY_STATIC_ALIGNMENT]
                ?.runCatching { TileStaticAlignment.valueOf(this) }
                ?.getOrNull()
                ?: TileStaticAlignment.CENTER,
            staticSizeFraction =
            (preferences[KEY_STATIC_SIZE_FRACTION] ?: DEFAULT_STATIC_SIZE_FRACTION)
                .coerceIn(MIN_STATIC_SIZE_FRACTION, MAX_STATIC_SIZE_FRACTION),
            dynamicScale =
            (preferences[KEY_DYNAMIC_SCALE] ?: DEFAULT_DYNAMIC_SCALE)
                .coerceIn(MIN_DIALOG_SCALE, MAX_DIALOG_SCALE),
            dynamicOffsetXFraction = preferences[KEY_DYNAMIC_OFFSET_X_FRACTION] ?: 0f,
            dynamicOffsetYFraction = preferences[KEY_DYNAMIC_OFFSET_Y_FRACTION] ?: 0f,
            dialogBackgroundMode =
            preferences[KEY_DIALOG_BACKGROUND_MODE]
                ?.runCatching { TileDialogBackgroundMode.valueOf(this) }
                ?.getOrNull()
                ?.let(::canonicalDialogBackgroundMode)
                ?: TileDialogBackgroundMode.CLEAR,
            dialogInactivityTimeoutSeconds = normalizeDialogInactivityTimeoutSeconds(
                preferences[KEY_DIALOG_INACTIVITY_TIMEOUT_SECONDS]
                    ?: DEFAULT_DIALOG_INACTIVITY_TIMEOUT_SECONDS
            ),
            rememberCalculatorState = preferences[KEY_REMEMBER_CALCULATOR_STATE] ?: false,
            savedCalculatorState = CalculatorUiState(
                formulaText = preferences[KEY_SAVED_FORMULA].orEmpty(),
                resultText = preferences[KEY_SAVED_RESULT].orEmpty(),
                phase =
                preferences[KEY_SAVED_PHASE]
                    ?.runCatching { CalculatorUiPhase.valueOf(this) }
                    ?.getOrNull()
                    ?: CalculatorUiPhase.INPUT
            )
        )
    }

    suspend fun snapshot(): TileSettings = settingsFlow.first()

    suspend fun setThemeMode(themeMode: TileThemeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_THEME_MODE] = themeMode.name
        }
    }

    suspend fun setAccentColor(argb: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_ACCENT_COLOR_ARGB] = argb
        }
    }

    suspend fun setSizeMode(sizeMode: TileSizeMode) {
        dataStore.edit { preferences ->
            preferences[KEY_SIZE_MODE] = sizeMode.name
        }
    }

    suspend fun setStaticAlignment(alignment: TileStaticAlignment) {
        dataStore.edit { preferences ->
            preferences[KEY_STATIC_ALIGNMENT] = alignment.name
        }
    }

    suspend fun setStaticSizeFraction(fraction: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_STATIC_SIZE_FRACTION] =
                fraction.coerceIn(MIN_STATIC_SIZE_FRACTION, MAX_STATIC_SIZE_FRACTION)
        }
    }

    suspend fun setDynamicTransform(scale: Float, offsetXFraction: Float, offsetYFraction: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_DYNAMIC_SCALE] = scale.coerceIn(MIN_DIALOG_SCALE, MAX_DIALOG_SCALE)
            preferences[KEY_DYNAMIC_OFFSET_X_FRACTION] = offsetXFraction
            preferences[KEY_DYNAMIC_OFFSET_Y_FRACTION] = offsetYFraction
        }
    }

    suspend fun setDialogBackgroundMode(mode: TileDialogBackgroundMode) {
        dataStore.edit { preferences ->
            preferences[KEY_DIALOG_BACKGROUND_MODE] = canonicalDialogBackgroundMode(mode).name
        }
    }

    suspend fun setDialogInactivityTimeoutSeconds(seconds: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_DIALOG_INACTIVITY_TIMEOUT_SECONDS] =
                normalizeDialogInactivityTimeoutSeconds(seconds)
        }
    }

    suspend fun setRememberCalculatorState(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_REMEMBER_CALCULATOR_STATE] = enabled
            if (!enabled) {
                preferences[KEY_SAVED_FORMULA] = ""
                preferences[KEY_SAVED_RESULT] = ""
                preferences[KEY_SAVED_PHASE] = CalculatorUiPhase.INPUT.name
            }
        }
    }

    suspend fun saveCalculatorState(state: CalculatorUiState) {
        dataStore.edit { preferences ->
            preferences[KEY_SAVED_FORMULA] = state.formulaText
            preferences[KEY_SAVED_RESULT] = state.resultText
            preferences[KEY_SAVED_PHASE] = state.phase.name
        }
    }

    suspend fun clearCalculatorState() {
        dataStore.edit { preferences ->
            preferences[KEY_SAVED_FORMULA] = ""
            preferences[KEY_SAVED_RESULT] = ""
            preferences[KEY_SAVED_PHASE] = CalculatorUiPhase.INPUT.name
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        val KEY_ACCENT_COLOR_ARGB = intPreferencesKey("accent_color_argb")
        val KEY_SIZE_MODE = stringPreferencesKey("size_mode")
        val KEY_STATIC_ALIGNMENT = stringPreferencesKey("static_alignment")
        val KEY_STATIC_SIZE_FRACTION = floatPreferencesKey("static_size_fraction")
        val KEY_DYNAMIC_SCALE = floatPreferencesKey("dynamic_scale")
        val KEY_DYNAMIC_OFFSET_X_FRACTION = floatPreferencesKey("dynamic_offset_x_fraction")
        val KEY_DYNAMIC_OFFSET_Y_FRACTION = floatPreferencesKey("dynamic_offset_y_fraction")
        val KEY_DIALOG_BACKGROUND_MODE = stringPreferencesKey("dialog_background_mode")
        val KEY_DIALOG_INACTIVITY_TIMEOUT_SECONDS =
            intPreferencesKey("dialog_inactivity_timeout_seconds")
        val KEY_REMEMBER_CALCULATOR_STATE = booleanPreferencesKey("remember_calculator_state")
        val KEY_SAVED_FORMULA = stringPreferencesKey("saved_formula")
        val KEY_SAVED_RESULT = stringPreferencesKey("saved_result")
        val KEY_SAVED_PHASE = stringPreferencesKey("saved_phase")
    }
}
