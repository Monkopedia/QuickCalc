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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorComposeDisplayMotionTest {

    @Test
    fun resolveTransition_triggersOnlyForInputToResult() {
        val previous = CalculatorUiState(formulaText = "1+2", resultText = "3", phase = CalculatorUiPhase.INPUT)
        val current = CalculatorUiState(formulaText = "3", resultText = "3", phase = CalculatorUiPhase.RESULT)
        val transition = resolveDisplayResultTransition(previous, current)

        assertNotNull(transition)
        assertEquals("3", transition?.movingResultText)
        assertEquals("1+2", transition?.outgoingFormulaText)

        val nonTransition = resolveDisplayResultTransition(
            previous = current,
            current = current.copy(phase = CalculatorUiPhase.RESULT)
        )
        assertNull(nonTransition)
    }

    @Test
    fun resolveTransition_omitsOutgoingFormulaWhenTextIsSame() {
        val previous = CalculatorUiState(formulaText = "7", resultText = "7", phase = CalculatorUiPhase.INPUT)
        val current = CalculatorUiState(formulaText = "7", resultText = "7", phase = CalculatorUiPhase.RESULT)

        val transition = resolveDisplayResultTransition(previous, current)

        assertNotNull(transition)
        assertEquals("7", transition?.movingResultText)
        assertNull(transition?.outgoingFormulaText)
    }

    @Test
    fun computeLegacyMetrics_matchesLegacyEquations() {
        val metrics = computeLegacyResultTransitionMetrics(
            resultTextSizePx = 36f,
            targetFormulaTextSizePx = 54f,
            resultViewWidthPx = 1080f,
            resultViewHeightPx = 120f,
            resultPaddingEndPx = 16f,
            resultPaddingBottomPx = 18f,
            formulaPaddingBottomPx = 12f,
            formulaBottomPx = 140f,
            resultBottomPx = 260f
        )

        val expectedScale = 1.5f
        val expectedTranslationX = (1f - expectedScale) * (1080f / 2f - 16f)
        val expectedTranslationY =
            (1f - expectedScale) * (120f / 2f - 18f) + (140f - 260f) + (18f - 12f)

        assertEquals(expectedScale, metrics.resultScale, 0.0001f)
        assertEquals(expectedTranslationX, metrics.resultTranslationX, 0.0001f)
        assertEquals(expectedTranslationY, metrics.resultTranslationY, 0.0001f)
        assertEquals(-140f, metrics.formulaTranslationY, 0.0001f)
    }

    @Test
    fun lerpFloat_respectsEndpoints() {
        assertEquals(5f, lerpFloat(5f, 30f, 0f), 0.0001f)
        assertEquals(30f, lerpFloat(5f, 30f, 1f), 0.0001f)
        assertEquals(17.5f, lerpFloat(5f, 30f, 0.5f), 0.0001f)
    }
}
