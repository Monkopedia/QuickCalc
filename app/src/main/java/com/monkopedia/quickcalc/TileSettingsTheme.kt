@file:Suppress("ktlint:standard:function-naming")

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

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

@Composable
fun TileSettingsTheme(settings: TileSettings, content: @Composable () -> Unit) {
    val darkTheme = when (settings.themeMode) {
        TileThemeMode.SYSTEM -> isSystemInDarkTheme()
        TileThemeMode.LIGHT -> false
        TileThemeMode.DARK -> true
    }
    val accentColor = resolveTileAccentColor(settings.accentColorArgb, darkTheme)
    MaterialTheme(
        colorScheme = calculatorSettingsColorScheme(darkTheme = darkTheme, accent = accentColor),
        content = content
    )
}

@Composable
fun calculatorDialogPalette(settings: TileSettings, darkTheme: Boolean): CalculatorColorPalette {
    val accent = resolveTileAccentColor(settings.accentColorArgb, darkTheme)
    val displayBackground = if (darkTheme) Color(0xFF121212) else Color(0xFFFFFFFF)
    val formulaColor = if (darkTheme) Color(0xB3FFFFFF) else Color(0x8A000000)
    val resultColor = if (darkTheme) Color(0x99FFFFFF) else Color(0x6C000000)
    val numericPadBackground = if (darkTheme) Color(0xFF2E2E2E) else Color(0xFF434343)
    val operatorPadBackground = if (darkTheme) Color(0xFF424242) else Color(0xFF636363)
    val advancedPadBackground = if (darkTheme) lighten(accent, 0.12f) else accent
    return CalculatorColorPalette(
        accentColor = accent,
        errorColor = Color(0xFFF40056),
        displayBackgroundColor = displayBackground,
        formulaColor = formulaColor,
        resultColor = resultColor,
        numericPadBackgroundColor = numericPadBackground,
        operatorPadBackgroundColor = operatorPadBackground,
        advancedPadBackgroundColor = advancedPadBackground
    )
}

@Composable
fun isDialogDarkTheme(settings: TileSettings): Boolean = when (settings.themeMode) {
    TileThemeMode.SYSTEM -> isSystemInDarkTheme()
    TileThemeMode.LIGHT -> false
    TileThemeMode.DARK -> true
}

fun isDialogDarkTheme(settings: TileSettings, isSystemDark: Boolean): Boolean =
    when (settings.themeMode) {
        TileThemeMode.SYSTEM -> isSystemDark
        TileThemeMode.LIGHT -> false
        TileThemeMode.DARK -> true
    }

@Composable
fun resolveTileAccentColor(accentColorArgb: Int, darkTheme: Boolean): Color {
    if (accentColorArgb != SYSTEM_ACCENT_COLOR_ARGB) {
        return Color(accentColorArgb)
    }
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) {
            dynamicDarkColorScheme(context).primary
        } else {
            dynamicLightColorScheme(context).primary
        }
    } else {
        Color(DEFAULT_ACCENT_COLOR_ARGB)
    }
}

private fun calculatorSettingsColorScheme(darkTheme: Boolean, accent: Color): ColorScheme =
    if (darkTheme) {
        darkColorScheme(
            primary = accent,
            secondary = accent,
            tertiary = accent
        )
    } else {
        lightColorScheme(
            primary = accent,
            secondary = accent,
            tertiary = accent
        )
    }

private fun lighten(color: Color, fraction: Float): Color = Color(
    red = color.red + (1f - color.red) * fraction,
    green = color.green + (1f - color.green) * fraction,
    blue = color.blue + (1f - color.blue) * fraction,
    alpha = color.alpha
)
