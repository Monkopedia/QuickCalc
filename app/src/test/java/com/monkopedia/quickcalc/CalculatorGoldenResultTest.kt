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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculatorGoldenResultTest {

    private lateinit var context: Context
    private lateinit var evaluator: CalculatorExpressionEvaluator

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        evaluator = CalculatorExpressionEvaluator(CalculatorExpressionTokenizer(context))
    }

    @Test
    fun deterministicExpressionGoldenSet() {
        assertGoldenValue("1+2*3", "7")
        assertGoldenValue("(1+2)*3", "9")
        assertGoldenValue("sin(0)", "0")
        assertGoldenValue("log(100)", "2")
        assertGoldenValue("1/0", context.getString(R.string.inf))

        assertGoldenError("0/0", R.string.error_nan)
        assertGoldenError("1/(", R.string.error_syntax)
    }

    private fun assertGoldenValue(expression: String, expectedValue: String) {
        val result = evaluate(expression)
        assertEquals(
            "Unexpected error for expression: $expression",
            INVALID_RES_ID,
            result.errorResId
        )
        assertEquals(
            "Unexpected value for expression: $expression",
            expectedValue,
            result.value
        )
    }

    private fun assertGoldenError(expression: String, expectedErrorResId: Int) {
        val result = evaluate(expression)
        assertEquals(
            "Unexpected error code for expression: $expression",
            expectedErrorResId,
            result.errorResId
        )
        assertNull("Expected null value for failing expression: $expression", result.value)
    }

    private fun evaluate(expression: String): EvalResult {
        val result = EvalResult()
        evaluator.evaluate(expression) { expr, value, errorResId ->
            result.expr = expr
            result.value = value
            result.errorResId = errorResId
        }
        return result
    }

    private data class EvalResult(
        var expr: String = "",
        var value: String? = null,
        var errorResId: Int = 0
    )
}
