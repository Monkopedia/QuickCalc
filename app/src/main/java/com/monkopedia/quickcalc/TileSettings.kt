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

enum class TileThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class TileSizeMode {
    STATIC,
    DYNAMIC
}

enum class TileDialogBackgroundMode {
    CLEAR,
    LIGHT,
    DARK,
    BLUR_SUBTLE,
    BLUR_LIGHT,
    BLUR_HEAVY
}

enum class TileStaticAlignment {
    TOP_START,
    TOP_CENTER,
    TOP_END,
    CENTER_START,
    CENTER,
    CENTER_END,
    BOTTOM_START,
    BOTTOM_CENTER,
    BOTTOM_END
}

data class TileSettings(
    val themeMode: TileThemeMode = TileThemeMode.SYSTEM,
    val accentColorArgb: Int = DEFAULT_ACCENT_COLOR_ARGB,
    val sizeMode: TileSizeMode = TileSizeMode.STATIC,
    val staticAlignment: TileStaticAlignment = TileStaticAlignment.CENTER,
    val staticSizeFraction: Float = DEFAULT_STATIC_SIZE_FRACTION,
    val dynamicScale: Float = DEFAULT_DYNAMIC_SCALE,
    val dynamicOffsetXFraction: Float = 0f,
    val dynamicOffsetYFraction: Float = 0f,
    val dialogBackgroundMode: TileDialogBackgroundMode = TileDialogBackgroundMode.CLEAR,
    val dialogInactivityTimeoutSeconds: Int = DEFAULT_DIALOG_INACTIVITY_TIMEOUT_SECONDS,
    val rememberCalculatorState: Boolean = false,
    val savedCalculatorState: CalculatorUiState = CalculatorUiState()
)

data class TileAccentOption(val labelResId: Int, val colorArgb: Int)

val TILE_ACCENT_OPTIONS = listOf(
    TileAccentOption(R.string.settings_accent_system, SYSTEM_ACCENT_COLOR_ARGB),
    TileAccentOption(R.string.settings_accent_cyan, 0xFF00BCD4.toInt()),
    TileAccentOption(R.string.settings_accent_blue, 0xFF2196F3.toInt()),
    TileAccentOption(R.string.settings_accent_green, 0xFF4CAF50.toInt()),
    TileAccentOption(R.string.settings_accent_orange, 0xFFFF9800.toInt()),
    TileAccentOption(R.string.settings_accent_pink, 0xFFE91E63.toInt()),
    TileAccentOption(R.string.settings_accent_red, 0xFFF44336.toInt()),
    TileAccentOption(R.string.settings_accent_purple, 0xFF673AB7.toInt()),
    TileAccentOption(R.string.settings_accent_teal, 0xFF009688.toInt())
)

const val SYSTEM_ACCENT_COLOR_ARGB = Int.MIN_VALUE
const val DEFAULT_ACCENT_COLOR_ARGB = 0xFF00BCD4.toInt()
const val DEFAULT_STATIC_SIZE_FRACTION = 0.8f
const val DEFAULT_DYNAMIC_SCALE = 0.85f
const val DEFAULT_DIALOG_INACTIVITY_TIMEOUT_SECONDS = 60
const val DIALOG_INACTIVITY_TIMEOUT_OFF_SECONDS = 0
const val MIN_DIALOG_INACTIVITY_TIMEOUT_SECONDS = 15
const val MAX_DIALOG_INACTIVITY_TIMEOUT_SECONDS = 300
const val MIN_DIALOG_SCALE = 0.45f
const val MAX_DIALOG_SCALE = 1.35f
const val MIN_STATIC_SIZE_FRACTION = 0.4f
const val MAX_STATIC_SIZE_FRACTION = 1.0f

fun normalizeDialogInactivityTimeoutSeconds(seconds: Int): Int =
    if (seconds <= DIALOG_INACTIVITY_TIMEOUT_OFF_SECONDS) {
        DIALOG_INACTIVITY_TIMEOUT_OFF_SECONDS
    } else {
        seconds.coerceIn(
            MIN_DIALOG_INACTIVITY_TIMEOUT_SECONDS,
            MAX_DIALOG_INACTIVITY_TIMEOUT_SECONDS
        )
    }

fun canonicalDialogBackgroundMode(mode: TileDialogBackgroundMode): TileDialogBackgroundMode = mode
