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

package com.android.calculator2

internal data class LegacyResultTransitionMetrics(
    val resultScale: Float,
    val resultTranslationX: Float,
    val resultTranslationY: Float,
    val formulaTranslationY: Float
)

internal data class DisplayResultTransition(
    val movingResultText: String,
    val outgoingFormulaText: String?
)

internal fun resolveDisplayResultTransition(
    previous: CalculatorUiState,
    current: CalculatorUiState
): DisplayResultTransition? {
    val enteringResult =
        previous.phase == CalculatorUiPhase.INPUT &&
            current.phase == CalculatorUiPhase.RESULT &&
            current.resultText.isNotBlank()
    if (!enteringResult) {
        return null
    }

    val movingText = current.resultText.orDisplayText()
    val previousFormulaText = previous.formulaText.orDisplayText()
    return DisplayResultTransition(
        movingResultText = movingText,
        outgoingFormulaText = previousFormulaText.takeIf { it != movingText }
    )
}

internal fun computeLegacyResultTransitionMetrics(
    resultTextSizePx: Float,
    targetFormulaTextSizePx: Float,
    resultViewWidthPx: Float,
    resultViewHeightPx: Float,
    resultPaddingEndPx: Float,
    resultPaddingBottomPx: Float,
    formulaPaddingBottomPx: Float,
    formulaBottomPx: Float,
    resultBottomPx: Float
): LegacyResultTransitionMetrics {
    val safeResultTextSize = resultTextSizePx.coerceAtLeast(1f)
    val resultScale = targetFormulaTextSizePx / safeResultTextSize

    val resultTranslationX =
        (1f - resultScale) * (resultViewWidthPx / 2f - resultPaddingEndPx)
    val resultTranslationY =
        (1f - resultScale) * (resultViewHeightPx / 2f - resultPaddingBottomPx) +
            (formulaBottomPx - resultBottomPx) +
            (resultPaddingBottomPx - formulaPaddingBottomPx)

    return LegacyResultTransitionMetrics(
        resultScale = resultScale,
        resultTranslationX = resultTranslationX,
        resultTranslationY = resultTranslationY,
        formulaTranslationY = -formulaBottomPx
    )
}

internal fun lerpFloat(start: Float, end: Float, progress: Float): Float =
    start + (end - start) * progress

internal fun String?.orDisplayText(): String = this?.ifEmpty { " " } ?: " "
