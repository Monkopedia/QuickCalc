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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculatorUiReducerTest {

    private lateinit var context: Context
    private lateinit var reducer: CalculatorUiReducer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        reducer = CalculatorUiReducer(context)
    }

    // --- initialState ---

    @Test
    fun initialStateWithBlankFormulaReturnsDefault() {
        val state = reducer.initialState(initialFormula = "", evaluateAsResult = false)
        assertEquals(CalculatorUiState(), state)
    }

    @Test
    fun initialStateWithFormulaReturnsInputPhase() {
        val state = reducer.initialState(initialFormula = "1+2", evaluateAsResult = false)
        assertEquals("1+2", state.formulaText)
        assertEquals("3", state.resultText)
        assertEquals(CalculatorUiPhase.INPUT, state.phase)
    }

    @Test
    fun initialStateWithEvaluateAsResultReturnsResultPhase() {
        val state = reducer.initialState(initialFormula = "1+2", evaluateAsResult = true)
        assertEquals("3", state.formulaText)
        assertEquals("3", state.resultText)
        assertEquals(CalculatorUiPhase.RESULT, state.phase)
    }

    @Test
    fun initialStateWithInvalidFormulaAndEvaluateAsResultReturnsError() {
        val state = reducer.initialState(initialFormula = "1/(", evaluateAsResult = true)
        assertEquals(CalculatorUiPhase.ERROR, state.phase)
    }

    // --- Append ---

    @Test
    fun appendDigitInInputPhase() {
        val state = reducer.reduce(CalculatorUiState(), CalculatorUiEvent.Append("1"))
        assertEquals("1", state.formulaText)
        assertEquals(CalculatorUiPhase.INPUT, state.phase)
    }

    @Test
    fun appendBuildsExpression() {
        var state = CalculatorUiState()
        state = reducer.reduce(state, CalculatorUiEvent.Append("1"))
        state = reducer.reduce(state, CalculatorUiEvent.Append("+"))
        state = reducer.reduce(state, CalculatorUiEvent.Append("2"))
        assertEquals("1+2", state.formulaText)
        assertEquals("3", state.resultText)
        assertEquals(CalculatorUiPhase.INPUT, state.phase)
    }

    @Test
    fun appendWithLeftParenthesis() {
        val state = reducer.reduce(
            CalculatorUiState(),
            CalculatorUiEvent.Append("sin", appendLeftParenthesis = true)
        )
        assertEquals("sin(", state.formulaText)
        assertEquals(CalculatorUiPhase.INPUT, state.phase)
    }

    @Test
    fun appendFromResultPhaseBuildsOnResult() {
        val resultState = CalculatorUiState(
            formulaText = "3",
            resultText = "3",
            phase = CalculatorUiPhase.RESULT
        )
        val state = reducer.reduce(resultState, CalculatorUiEvent.Append("+"))
        assertEquals("3+", state.formulaText)
        assertEquals(CalculatorUiPhase.INPUT, state.phase)
    }

    @Test
    fun appendFromErrorPhaseBuildsOnFormula() {
        val errorState = CalculatorUiState(
            formulaText = "1/(",
            resultText = "Error",
            phase = CalculatorUiPhase.ERROR
        )
        val state = reducer.reduce(errorState, CalculatorUiEvent.Append("2"))
        assertEquals("1/(2", state.formulaText)
        assertEquals(CalculatorUiPhase.INPUT, state.phase)
    }

    // --- Delete ---

    @Test
    fun deleteRemovesLastCharacter() {
        val state = reducer.reduce(
            CalculatorUiState(formulaText = "12", resultText = "", phase = CalculatorUiPhase.INPUT),
            CalculatorUiEvent.Delete
        )
        assertEquals("1", state.formulaText)
        assertEquals(CalculatorUiPhase.INPUT, state.phase)
    }

    @Test
    fun deleteOnEmptyFormulaReturnsSameState() {
        val empty = CalculatorUiState()
        val state = reducer.reduce(empty, CalculatorUiEvent.Delete)
        assertEquals(empty, state)
    }

    @Test
    fun deleteOnSingleCharacterClearsFormula() {
        val state = reducer.reduce(
            CalculatorUiState(formulaText = "5", resultText = "", phase = CalculatorUiPhase.INPUT),
            CalculatorUiEvent.Delete
        )
        assertEquals("", state.formulaText)
        assertEquals("", state.resultText)
    }

    // --- Clear ---

    @Test
    fun clearResetsToDefault() {
        val state = reducer.reduce(
            CalculatorUiState(
                formulaText = "1+2",
                resultText = "3",
                phase = CalculatorUiPhase.INPUT
            ),
            CalculatorUiEvent.Clear
        )
        assertEquals(CalculatorUiState(), state)
    }

    @Test
    fun clearOnEmptyFormulaReturnsSameState() {
        val empty = CalculatorUiState()
        val state = reducer.reduce(empty, CalculatorUiEvent.Clear)
        assertEquals(empty, state)
    }

    @Test
    fun clearFromResultPhaseResetsToDefault() {
        val state = reducer.reduce(
            CalculatorUiState(
                formulaText = "3",
                resultText = "3",
                phase = CalculatorUiPhase.RESULT
            ),
            CalculatorUiEvent.Clear
        )
        assertEquals(CalculatorUiState(), state)
    }

    @Test
    fun clearFromErrorPhaseResetsToDefault() {
        val state = reducer.reduce(
            CalculatorUiState(
                formulaText = "1/(",
                resultText = "Error",
                phase = CalculatorUiPhase.ERROR
            ),
            CalculatorUiEvent.Clear
        )
        assertEquals(CalculatorUiState(), state)
    }

    // --- Equals ---

    @Test
    fun equalsProducesResultPhase() {
        val input = CalculatorUiState(
            formulaText = "1+2",
            resultText = "3",
            phase = CalculatorUiPhase.INPUT
        )
        val state = reducer.reduce(input, CalculatorUiEvent.Equals)
        assertEquals("3", state.formulaText)
        assertEquals("3", state.resultText)
        assertEquals(CalculatorUiPhase.RESULT, state.phase)
    }

    @Test
    fun equalsOnSyntaxErrorProducesErrorPhase() {
        val input = CalculatorUiState(
            formulaText = "1/(",
            resultText = "",
            phase = CalculatorUiPhase.INPUT
        )
        val state = reducer.reduce(input, CalculatorUiEvent.Equals)
        assertEquals(CalculatorUiPhase.ERROR, state.phase)
        assertEquals("1/(", state.formulaText)
    }

    @Test
    fun equalsOnEmptyFormulaReturnsInputPhase() {
        val input = CalculatorUiState(
            formulaText = "",
            resultText = "",
            phase = CalculatorUiPhase.INPUT
        )
        val state = reducer.reduce(input, CalculatorUiEvent.Equals)
        assertEquals(CalculatorUiPhase.INPUT, state.phase)
    }

    @Test
    fun equalsFromResultPhaseReturnsSameState() {
        val result = CalculatorUiState(
            formulaText = "3",
            resultText = "3",
            phase = CalculatorUiPhase.RESULT
        )
        val state = reducer.reduce(result, CalculatorUiEvent.Equals)
        assertEquals(result, state)
    }

    @Test
    fun equalsFromErrorPhaseReturnsSameState() {
        val error = CalculatorUiState(
            formulaText = "1/(",
            resultText = "Error",
            phase = CalculatorUiPhase.ERROR
        )
        val state = reducer.reduce(error, CalculatorUiEvent.Equals)
        assertEquals(error, state)
    }

    // --- UiState derived properties ---

    @Test
    fun showsClearButtonInResultPhase() {
        val state = CalculatorUiState(phase = CalculatorUiPhase.RESULT)
        assertEquals(true, state.showsClearButton)
    }

    @Test
    fun showsClearButtonInErrorPhase() {
        val state = CalculatorUiState(phase = CalculatorUiPhase.ERROR)
        assertEquals(true, state.showsClearButton)
    }

    @Test
    fun doesNotShowClearButtonInInputPhase() {
        val state = CalculatorUiState(phase = CalculatorUiPhase.INPUT)
        assertEquals(false, state.showsClearButton)
    }

    @Test
    fun hasErrorOnlyInErrorPhase() {
        assertEquals(false, CalculatorUiState(phase = CalculatorUiPhase.INPUT).hasError)
        assertEquals(false, CalculatorUiState(phase = CalculatorUiPhase.RESULT).hasError)
        assertEquals(true, CalculatorUiState(phase = CalculatorUiPhase.ERROR).hasError)
        assertEquals(false, CalculatorUiState(phase = CalculatorUiPhase.EVALUATE).hasError)
    }

    // --- Full calculation flow ---

    @Test
    fun fullCalculationFlowEndToEnd() {
        var state = CalculatorUiState()

        // Type "12+8"
        state = reducer.reduce(state, CalculatorUiEvent.Append("1"))
        state = reducer.reduce(state, CalculatorUiEvent.Append("2"))
        state = reducer.reduce(state, CalculatorUiEvent.Append("+"))
        state = reducer.reduce(state, CalculatorUiEvent.Append("8"))
        assertEquals("12+8", state.formulaText)
        assertEquals("20", state.resultText)
        assertEquals(CalculatorUiPhase.INPUT, state.phase)

        // Press equals
        state = reducer.reduce(state, CalculatorUiEvent.Equals)
        assertEquals("20", state.formulaText)
        assertEquals("20", state.resultText)
        assertEquals(CalculatorUiPhase.RESULT, state.phase)

        // Continue with new operation
        state = reducer.reduce(state, CalculatorUiEvent.Append("*"))
        state = reducer.reduce(state, CalculatorUiEvent.Append("3"))
        assertEquals("20*3", state.formulaText)
        assertEquals("60", state.resultText)
        assertEquals(CalculatorUiPhase.INPUT, state.phase)

        // Clear
        state = reducer.reduce(state, CalculatorUiEvent.Clear)
        assertEquals(CalculatorUiState(), state)
    }
}
