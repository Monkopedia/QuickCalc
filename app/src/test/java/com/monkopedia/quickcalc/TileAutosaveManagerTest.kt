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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TileAutosaveManagerTest {

    private lateinit var repository: TileSettingsRepository
    private lateinit var scope: CoroutineScope
    private lateinit var manager: TileAutosaveManager

    private var currentSnapshot = AutosaveSnapshot(
        transform = null,
        calculatorState = CalculatorUiState(),
        rememberState = false
    )

    @Before
    fun setUp() {
        repository = TileSettingsRepository(ApplicationProvider.getApplicationContext())
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        manager = TileAutosaveManager(repository, scope) { currentSnapshot }
    }

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun persistNow_writesDynamicTransform() = runBlocking {
        val transform =
            DynamicTransform(scale = 1.2f, offsetXFraction = 0.2f, offsetYFraction = 0.3f)
        val snapshot = AutosaveSnapshot(
            transform = transform,
            calculatorState = CalculatorUiState(),
            rememberState = false
        )

        manager.persistNow(snapshot)

        val settings = repository.settingsFlow.first()
        assertEquals(1.2f, settings.dynamicScale, 0.001f)
        assertEquals(0.2f, settings.dynamicOffsetXFraction, 0.001f)
        assertEquals(0.3f, settings.dynamicOffsetYFraction, 0.001f)
    }

    @Test
    fun persistNow_savesCalculatorState_whenRememberEnabled() = runBlocking {
        val state = CalculatorUiState(
            formulaText = "1+2",
            resultText = "3",
            phase = CalculatorUiPhase.RESULT
        )
        val snapshot = AutosaveSnapshot(
            transform = null,
            calculatorState = state,
            rememberState = true
        )

        manager.persistNow(snapshot)

        val settings = repository.settingsFlow.first()
        assertEquals("1+2", settings.savedCalculatorState.formulaText)
        assertEquals("3", settings.savedCalculatorState.resultText)
        assertEquals(CalculatorUiPhase.RESULT, settings.savedCalculatorState.phase)
    }

    @Test
    fun persistNow_clearsCalculatorState_whenRememberDisabled() = runBlocking {
        // First persist with remember enabled
        manager.persistNow(
            AutosaveSnapshot(
                transform = null,
                calculatorState = CalculatorUiState(formulaText = "1+2", resultText = "3"),
                rememberState = true
            )
        )

        // Then persist with remember disabled
        manager.persistNow(
            AutosaveSnapshot(
                transform = null,
                calculatorState = CalculatorUiState(formulaText = "1+2", resultText = "3"),
                rememberState = false
            )
        )

        val settings = repository.settingsFlow.first()
        assertEquals("", settings.savedCalculatorState.formulaText)
        assertEquals("", settings.savedCalculatorState.resultText)
    }

    @Test
    fun persistNow_skipsTransformWrite_whenNull() = runBlocking {
        // Set initial transform (within valid scale range)
        repository.setDynamicTransform(scale = 1.2f, offsetXFraction = 0.5f, offsetYFraction = 0.5f)

        // Persist with null transform — should not overwrite
        manager.persistNow(
            AutosaveSnapshot(
                transform = null,
                calculatorState = CalculatorUiState(),
                rememberState = false
            )
        )

        val settings = repository.settingsFlow.first()
        assertEquals(1.2f, settings.dynamicScale, 0.001f)
        assertEquals(0.5f, settings.dynamicOffsetXFraction, 0.001f)
    }

    @Test
    fun persistNow_withForce_alwaysWrites() = runBlocking {
        val snapshot = AutosaveSnapshot(
            transform = DynamicTransform(1.0f, 0f, 0f),
            calculatorState = CalculatorUiState(formulaText = "42"),
            rememberState = true
        )

        // Persist twice with same snapshot — force should still write
        manager.persistNow(snapshot)
        manager.persistNow(snapshot)

        val settings = repository.settingsFlow.first()
        assertEquals("42", settings.savedCalculatorState.formulaText)
    }

    @Test
    fun persistNow_toggleRememberState_clearsOnDisable() = runBlocking {
        // Enable remember and save state
        manager.persistNow(
            AutosaveSnapshot(
                transform = null,
                calculatorState = CalculatorUiState(formulaText = "sin(45)"),
                rememberState = true
            )
        )
        assertEquals("sin(45)", repository.settingsFlow.first().savedCalculatorState.formulaText)

        // Disable remember
        manager.persistNow(
            AutosaveSnapshot(
                transform = null,
                calculatorState = CalculatorUiState(formulaText = "sin(45)"),
                rememberState = false
            )
        )
        assertEquals("", repository.settingsFlow.first().savedCalculatorState.formulaText)

        // Re-enable with new state
        manager.persistNow(
            AutosaveSnapshot(
                transform = null,
                calculatorState = CalculatorUiState(formulaText = "cos(60)"),
                rememberState = true
            )
        )
        assertEquals("cos(60)", repository.settingsFlow.first().savedCalculatorState.formulaText)
    }
}
