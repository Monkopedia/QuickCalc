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

class CalculatorUiReducer(context: Context) {

    private val evaluator = CalculatorExpressionEvaluator(CalculatorExpressionTokenizer(context))
    private val applicationContext = context.applicationContext

    fun initialState(initialFormula: String, evaluateAsResult: Boolean): CalculatorUiState {
        if (initialFormula.isBlank()) {
            return CalculatorUiState()
        }
        val evaluatedInputState = evaluateForInput(initialFormula)
        return if (evaluateAsResult) {
            evaluateForEquals(evaluatedInputState)
        } else {
            evaluatedInputState
        }
    }

    fun reduce(previous: CalculatorUiState, event: CalculatorUiEvent): CalculatorUiState =
        when (event) {
            is CalculatorUiEvent.Append -> {
                val suffix = if (event.appendLeftParenthesis) "(" else ""
                evaluateForInput(previous.formulaText + event.token + suffix)
            }

            CalculatorUiEvent.Delete -> {
                if (previous.formulaText.isEmpty()) {
                    previous
                } else {
                    evaluateForInput(previous.formulaText.dropLast(1))
                }
            }

            CalculatorUiEvent.Clear -> {
                if (previous.formulaText.isEmpty()) {
                    previous
                } else {
                    CalculatorUiState()
                }
            }

            CalculatorUiEvent.Equals -> evaluateForEquals(previous)
        }

    private fun evaluateForInput(formulaText: String): CalculatorUiState {
        val result = evaluator.evaluate(formulaText)
        return CalculatorUiState(
            formulaText = formulaText,
            resultText = (result as? EvaluationResult.Success)?.result.orEmpty(),
            phase = CalculatorUiPhase.INPUT
        )
    }

    private fun evaluateForEquals(previous: CalculatorUiState): CalculatorUiState {
        if (previous.phase != CalculatorUiPhase.INPUT) {
            return previous
        }

        return when (val result = evaluator.evaluate(previous.formulaText)) {
            is EvaluationResult.Error -> CalculatorUiState(
                formulaText = previous.formulaText,
                resultText = applicationContext.getString(result.errorResourceId),
                phase = CalculatorUiPhase.ERROR
            )

            is EvaluationResult.Success -> CalculatorUiState(
                formulaText = result.result,
                resultText = result.result,
                phase = CalculatorUiPhase.RESULT
            )

            is EvaluationResult.Empty -> CalculatorUiState(
                formulaText = previous.formulaText,
                resultText = "",
                phase = CalculatorUiPhase.INPUT
            )
        }
    }
}
