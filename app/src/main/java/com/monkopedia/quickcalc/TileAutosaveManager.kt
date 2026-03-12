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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AUTOSAVE_DEBOUNCE_MS = 180L

internal data class AutosaveSnapshot(
    val transform: DynamicTransform?,
    val calculatorState: CalculatorUiState,
    val rememberState: Boolean
)

internal data class DynamicTransform(
    val scale: Float,
    val offsetXFraction: Float,
    val offsetYFraction: Float
)

internal class TileAutosaveManager(
    private val settingsRepository: TileSettingsRepository,
    scope: CoroutineScope,
    private val snapshotProvider: () -> AutosaveSnapshot
) {
    private val autosaveSignals = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var lastPersistedTransform: DynamicTransform? = null
    private var lastPersistedCalculatorState: CalculatorUiState? = null
    private var lastPersistedRememberState: Boolean? = null

    init {
        scope.launch {
            autosaveSignals
                .collectLatest {
                    delay(AUTOSAVE_DEBOUNCE_MS)
                    val snapshot = snapshotProvider()
                    withContext(Dispatchers.IO) {
                        persistSnapshot(snapshot)
                    }
                }
        }
    }

    fun schedule() {
        autosaveSignals.tryEmit(Unit)
    }

    suspend fun persistNow(snapshot: AutosaveSnapshot) {
        persistSnapshot(snapshot, force = true)
    }

    private suspend fun persistSnapshot(snapshot: AutosaveSnapshot, force: Boolean = false) {
        val transformSnapshot = snapshot.transform
        val calculatorStateSnapshot = snapshot.calculatorState
        val rememberStateSnapshot = snapshot.rememberState

        if (transformSnapshot != null &&
            (force || transformSnapshot != lastPersistedTransform)
        ) {
            settingsRepository.setDynamicTransform(
                scale = transformSnapshot.scale,
                offsetXFraction = transformSnapshot.offsetXFraction,
                offsetYFraction = transformSnapshot.offsetYFraction
            )
            lastPersistedTransform = transformSnapshot
        }

        if (rememberStateSnapshot) {
            if (force ||
                calculatorStateSnapshot != lastPersistedCalculatorState ||
                lastPersistedRememberState != true
            ) {
                settingsRepository.saveCalculatorState(calculatorStateSnapshot)
                lastPersistedCalculatorState = calculatorStateSnapshot
            }
            lastPersistedRememberState = true
            return
        }

        if (force || lastPersistedRememberState != false) {
            settingsRepository.clearCalculatorState()
            lastPersistedCalculatorState = CalculatorUiState()
            lastPersistedRememberState = false
        }
    }
}
