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

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TileSettingsRepositoryTest {

    private lateinit var repository: TileSettingsRepository

    @Before
    fun setUp() {
        repository = TileSettingsRepository(ApplicationProvider.getApplicationContext())
    }

    // --- Enum round-trips ---

    @Test
    fun setThemeMode_roundTrips() = runBlocking {
        repository.setThemeMode(TileThemeMode.DARK)
        assertEquals(TileThemeMode.DARK, repository.snapshot().themeMode)
    }

    @Test
    fun setSizeMode_roundTrips() = runBlocking {
        repository.setSizeMode(TileSizeMode.DYNAMIC)
        assertEquals(TileSizeMode.DYNAMIC, repository.snapshot().sizeMode)
    }

    @Test
    fun setStaticAlignment_roundTrips() = runBlocking {
        repository.setStaticAlignment(TileStaticAlignment.TOP_END)
        assertEquals(TileStaticAlignment.TOP_END, repository.snapshot().staticAlignment)
    }

    @Test
    fun setDialogBackgroundMode_roundTrips() = runBlocking {
        repository.setDialogBackgroundMode(TileDialogBackgroundMode.BLUR_HEAVY)
        assertEquals(
            TileDialogBackgroundMode.BLUR_HEAVY,
            repository.snapshot().dialogBackgroundMode
        )
    }

    // --- Value clamping ---

    @Test
    fun setStaticSizeFraction_clampsAboveMax() = runBlocking {
        repository.setStaticSizeFraction(5.0f)
        assertEquals(MAX_STATIC_SIZE_FRACTION, repository.snapshot().staticSizeFraction, 0.001f)
    }

    @Test
    fun setStaticSizeFraction_clampsBelowMin() = runBlocking {
        repository.setStaticSizeFraction(0.1f)
        assertEquals(MIN_STATIC_SIZE_FRACTION, repository.snapshot().staticSizeFraction, 0.001f)
    }

    @Test
    fun setStaticSizeFraction_validValuePassesThrough() = runBlocking {
        repository.setStaticSizeFraction(0.7f)
        assertEquals(0.7f, repository.snapshot().staticSizeFraction, 0.001f)
    }

    @Test
    fun setDynamicTransform_clampsScale() = runBlocking {
        repository.setDynamicTransform(scale = 99f, offsetXFraction = 0f, offsetYFraction = 0f)
        assertEquals(MAX_DIALOG_SCALE, repository.snapshot().dynamicScale, 0.001f)
    }

    @Test
    fun setDynamicTransform_preservesOffset() = runBlocking {
        repository.setDynamicTransform(
            scale = 1.0f,
            offsetXFraction = 0.3f,
            offsetYFraction = -0.2f
        )
        val settings = repository.snapshot()
        assertEquals(0.3f, settings.dynamicOffsetXFraction, 0.001f)
        assertEquals(-0.2f, settings.dynamicOffsetYFraction, 0.001f)
    }

    @Test
    fun setDialogInactivityTimeout_normalizes() = runBlocking {
        repository.setDialogInactivityTimeoutSeconds(5)
        assertEquals(
            MIN_DIALOG_INACTIVITY_TIMEOUT_SECONDS,
            repository.snapshot().dialogInactivityTimeoutSeconds
        )
    }

    @Test
    fun setDialogInactivityTimeout_zeroMeansOff() = runBlocking {
        repository.setDialogInactivityTimeoutSeconds(0)
        assertEquals(
            DIALOG_INACTIVITY_TIMEOUT_OFF_SECONDS,
            repository.snapshot().dialogInactivityTimeoutSeconds
        )
    }

    // --- Accent color ---

    @Test
    fun setAccentColor_roundTrips() = runBlocking {
        val cyan = 0xFF00BCD4.toInt()
        repository.setAccentColor(cyan)
        assertEquals(cyan, repository.snapshot().accentColorArgb)
    }

    // --- Calculator state persistence ---

    @Test
    fun saveCalculatorState_roundTrips() = runBlocking {
        val state = CalculatorUiState(
            formulaText = "sin(90)",
            resultText = "1",
            phase = CalculatorUiPhase.RESULT
        )
        repository.saveCalculatorState(state)
        val saved = repository.snapshot().savedCalculatorState
        assertEquals("sin(90)", saved.formulaText)
        assertEquals("1", saved.resultText)
        assertEquals(CalculatorUiPhase.RESULT, saved.phase)
    }

    @Test
    fun clearCalculatorState_resetsToDefaults() = runBlocking {
        repository.saveCalculatorState(
            CalculatorUiState(formulaText = "42", resultText = "42")
        )
        repository.clearCalculatorState()
        val saved = repository.snapshot().savedCalculatorState
        assertEquals("", saved.formulaText)
        assertEquals("", saved.resultText)
        assertEquals(CalculatorUiPhase.INPUT, saved.phase)
    }

    // --- Remember state toggle ---

    @Test
    fun setRememberState_enableDoesNotClearState() = runBlocking {
        repository.saveCalculatorState(
            CalculatorUiState(formulaText = "1+1", resultText = "2")
        )
        repository.setRememberCalculatorState(true)
        assertEquals("1+1", repository.snapshot().savedCalculatorState.formulaText)
    }

    @Test
    fun setRememberState_disableClearsState() = runBlocking {
        repository.saveCalculatorState(
            CalculatorUiState(formulaText = "1+1", resultText = "2")
        )
        repository.setRememberCalculatorState(false)
        assertEquals("", repository.snapshot().savedCalculatorState.formulaText)
        assertEquals("", repository.snapshot().savedCalculatorState.resultText)
    }

    // --- snapshot vs settingsFlow consistency ---

    @Test
    fun snapshot_matchesFlowFirst() = runBlocking {
        repository.setThemeMode(TileThemeMode.LIGHT)
        repository.setAccentColor(0xFFFF0000.toInt())
        val fromSnapshot = repository.snapshot()
        val fromFlow = repository.settingsFlow.first()
        assertEquals(fromSnapshot, fromFlow)
    }
}
