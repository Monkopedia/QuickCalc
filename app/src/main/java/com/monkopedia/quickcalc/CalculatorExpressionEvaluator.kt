/*
 * Copyright (C) 2014 The Android Open Source Project
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

import org.javia.arity.Symbols
import org.javia.arity.SyntaxException
import org.javia.arity.Util

sealed interface EvaluationResult {
    val normalizedExpression: String

    data class Empty(override val normalizedExpression: String) : EvaluationResult
    data class Success(override val normalizedExpression: String, val result: String) :
        EvaluationResult

    data class Error(override val normalizedExpression: String, val errorResourceId: Int) :
        EvaluationResult
}

class CalculatorExpressionEvaluator(private val tokenizer: CalculatorExpressionTokenizer) {
    /**
     * The maximum number of significant digits to display.
     */
    private val maxDigits = 12

    /**
     * A [Double] has at least 17 significant digits, we show the first [maxDigits]
     * and use the remaining digits as guard digits to hide floating point precision errors.
     */
    private val roundingDigits = maxOf(17 - maxDigits, 0)

    private val symbols = Symbols()

    fun evaluate(expression: String): EvaluationResult {
        var expr = tokenizer.getNormalizedExpression(expression)

        while (expr.isNotEmpty() && "+-/*".indexOf(expr.last()) != -1) {
            expr = expr.substring(0, expr.length - 1)
        }

        if (expr.isEmpty() || expr.toDoubleOrNull() != null) {
            return EvaluationResult.Empty(expr)
        }

        return try {
            val result = symbols.eval(expr)
            if (result.isNaN()) {
                EvaluationResult.Error(expr, R.string.error_nan)
            } else {
                val resultString = tokenizer.getLocalizedExpression(
                    Util.doubleToString(result, maxDigits, roundingDigits)
                )
                EvaluationResult.Success(expr, resultString)
            }
        } catch (_: SyntaxException) {
            EvaluationResult.Error(expr, R.string.error_syntax)
        }
    }

    fun evaluate(expression: CharSequence, callback: EvaluateCallback) {
        evaluate(expression.toString(), callback)
    }

    fun evaluate(expression: String, callback: EvaluateCallback) {
        when (val result = evaluate(expression)) {
            is EvaluationResult.Empty ->
                callback.onEvaluate(result.normalizedExpression, null, INVALID_RES_ID)
            is EvaluationResult.Success ->
                callback.onEvaluate(result.normalizedExpression, result.result, INVALID_RES_ID)
            is EvaluationResult.Error ->
                callback.onEvaluate(result.normalizedExpression, null, result.errorResourceId)
        }
    }

    fun interface EvaluateCallback {
        fun onEvaluate(expr: String, result: String?, errorResourceId: Int)
    }
}
