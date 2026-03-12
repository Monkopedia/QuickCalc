/*
 * Copyright (C) 2026 The QuickCalc Project
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

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TileSettingsFunctionsTest {

    // --- normalizeDialogInactivityTimeoutSeconds ---

    @Test
    fun normalizeTimeout_zeroMeansOff() {
        assertEquals(0, normalizeDialogInactivityTimeoutSeconds(0))
    }

    @Test
    fun normalizeTimeout_negativeMeansOff() {
        assertEquals(0, normalizeDialogInactivityTimeoutSeconds(-5))
    }

    @Test
    fun normalizeTimeout_belowMinClampsToMin() {
        assertEquals(
            MIN_DIALOG_INACTIVITY_TIMEOUT_SECONDS,
            normalizeDialogInactivityTimeoutSeconds(5)
        )
    }

    @Test
    fun normalizeTimeout_aboveMaxClampsToMax() {
        assertEquals(
            MAX_DIALOG_INACTIVITY_TIMEOUT_SECONDS,
            normalizeDialogInactivityTimeoutSeconds(999)
        )
    }

    @Test
    fun normalizeTimeout_validValuePassesThrough() {
        assertEquals(60, normalizeDialogInactivityTimeoutSeconds(60))
    }

    // --- dialogWindowOverlayColorArgb ---

    @Test
    fun overlayColor_clearIsTransparent() {
        assertEquals(
            Color.TRANSPARENT,
            dialogWindowOverlayColorArgb(TileDialogBackgroundMode.CLEAR, darkTheme = false)
        )
        assertEquals(
            Color.TRANSPARENT,
            dialogWindowOverlayColorArgb(TileDialogBackgroundMode.CLEAR, darkTheme = true)
        )
    }

    @Test
    fun overlayColor_lightModeIgnoresDarkTheme() {
        val light = dialogWindowOverlayColorArgb(TileDialogBackgroundMode.LIGHT, false)
        val dark = dialogWindowOverlayColorArgb(TileDialogBackgroundMode.LIGHT, true)
        assertEquals(light, dark)
    }

    @Test
    fun overlayColor_blurSubtleDarkerInDarkTheme() {
        val lightAlpha = Color.alpha(
            dialogWindowOverlayColorArgb(TileDialogBackgroundMode.BLUR_SUBTLE, false)
        )
        val darkAlpha = Color.alpha(
            dialogWindowOverlayColorArgb(TileDialogBackgroundMode.BLUR_SUBTLE, true)
        )
        assertTrue("Dark theme overlay should be darker", darkAlpha > lightAlpha)
    }

    @Test
    fun overlayColor_darkModeDarkerThanLight() {
        val lightAlpha = Color.alpha(
            dialogWindowOverlayColorArgb(TileDialogBackgroundMode.LIGHT, false)
        )
        val darkAlpha = Color.alpha(
            dialogWindowOverlayColorArgb(TileDialogBackgroundMode.DARK, false)
        )
        assertTrue("DARK mode should be more opaque than LIGHT", darkAlpha > lightAlpha)
    }

    // --- dialogWindowBackgroundBlurRadiusPx ---

    @Test
    fun blurRadius_nonBlurModesReturnZero() {
        assertEquals(0, dialogWindowBackgroundBlurRadiusPx(TileDialogBackgroundMode.CLEAR))
        assertEquals(0, dialogWindowBackgroundBlurRadiusPx(TileDialogBackgroundMode.LIGHT))
        assertEquals(0, dialogWindowBackgroundBlurRadiusPx(TileDialogBackgroundMode.DARK))
    }

    @Test
    fun blurRadius_subtleLessThanLight() {
        assertTrue(
            dialogWindowBackgroundBlurRadiusPx(TileDialogBackgroundMode.BLUR_SUBTLE) <
                dialogWindowBackgroundBlurRadiusPx(TileDialogBackgroundMode.BLUR_LIGHT)
        )
    }

    @Test
    fun blurRadius_lightLessThanHeavy() {
        assertTrue(
            dialogWindowBackgroundBlurRadiusPx(TileDialogBackgroundMode.BLUR_LIGHT) <
                dialogWindowBackgroundBlurRadiusPx(TileDialogBackgroundMode.BLUR_HEAVY)
        )
    }

    @Test
    fun blurRadius_allPositiveForBlurModes() {
        assertTrue(dialogWindowBackgroundBlurRadiusPx(TileDialogBackgroundMode.BLUR_SUBTLE) > 0)
        assertTrue(dialogWindowBackgroundBlurRadiusPx(TileDialogBackgroundMode.BLUR_LIGHT) > 0)
        assertTrue(dialogWindowBackgroundBlurRadiusPx(TileDialogBackgroundMode.BLUR_HEAVY) > 0)
    }

    // --- isDialogDarkTheme ---

    @Test
    fun darkTheme_lightModeAlwaysFalse() {
        val settings = TileSettings(themeMode = TileThemeMode.LIGHT)
        assertFalse(isDialogDarkTheme(settings, isSystemDark = false))
        assertFalse(isDialogDarkTheme(settings, isSystemDark = true))
    }

    @Test
    fun darkTheme_darkModeAlwaysTrue() {
        val settings = TileSettings(themeMode = TileThemeMode.DARK)
        assertTrue(isDialogDarkTheme(settings, isSystemDark = false))
        assertTrue(isDialogDarkTheme(settings, isSystemDark = true))
    }

    @Test
    fun darkTheme_systemModeFollowsSystem() {
        val settings = TileSettings(themeMode = TileThemeMode.SYSTEM)
        assertFalse(isDialogDarkTheme(settings, isSystemDark = false))
        assertTrue(isDialogDarkTheme(settings, isSystemDark = true))
    }

    // --- canonicalDialogBackgroundMode ---

    @Test
    fun canonicalMode_allModesPassThrough() {
        TileDialogBackgroundMode.entries.forEach { mode ->
            assertEquals(mode, canonicalDialogBackgroundMode(mode))
        }
    }

    // --- dynamicMaxOffsetFraction ---

    @Test
    fun dynamicMaxOffset_smallerScaleGivesLargerOffset() {
        val small = dynamicMaxOffsetFraction(baseFraction = 0.5f, scale = 0.5f)
        val large = dynamicMaxOffsetFraction(baseFraction = 0.5f, scale = 1.5f)
        assertTrue("Smaller scale should allow larger offset", small > large)
    }

    @Test
    fun dynamicMaxOffset_neverBelowOffscreenFraction() {
        val result = dynamicMaxOffsetFraction(baseFraction = 1.0f, scale = 1.5f)
        assertTrue("Offset should be at least the offscreen drag fraction", result >= 0.22f)
    }
}
