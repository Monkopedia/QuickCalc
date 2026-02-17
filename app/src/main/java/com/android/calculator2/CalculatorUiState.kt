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

enum class CalculatorUiPhase {
    INPUT,
    EVALUATE,
    RESULT,
    ERROR
}

data class CalculatorUiState(
    val formulaText: String = "",
    val resultText: String = "",
    val phase: CalculatorUiPhase = CalculatorUiPhase.INPUT
) {
    val showsClearButton: Boolean
        get() = phase == CalculatorUiPhase.RESULT || phase == CalculatorUiPhase.ERROR

    val hasError: Boolean
        get() = phase == CalculatorUiPhase.ERROR
}

sealed interface CalculatorUiEvent {
    data class Append(val token: String, val appendLeftParenthesis: Boolean = false) :
        CalculatorUiEvent

    data object Delete : CalculatorUiEvent

    data object Clear : CalculatorUiEvent

    data object Equals : CalculatorUiEvent
}
