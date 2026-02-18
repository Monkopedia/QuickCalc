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
class CalculatorRegressionTest {

    private lateinit var tokenizer: CalculatorExpressionTokenizer
    private lateinit var evaluator: CalculatorExpressionEvaluator

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        tokenizer = CalculatorExpressionTokenizer(context)
        evaluator = CalculatorExpressionEvaluator(tokenizer)
    }

    @Test
    fun duplicateTrailingOperatorIsCollapsed() {
        val builder = CalculatorExpressionBuilder("1+", tokenizer, true)

        builder.replace(builder.length, builder.length, "*", 0, 1)

        assertEquals("1*", tokenizer.getNormalizedExpression(builder.toString()))
    }

    @Test
    fun duplicateDecimalInSameNumberIsIgnored() {
        val builder = CalculatorExpressionBuilder("12.3", tokenizer, true)

        builder.replace(builder.length, builder.length, ".", 0, 1)

        assertEquals("12.3", tokenizer.getNormalizedExpression(builder.toString()))
    }

    @Test
    fun trailingOperatorReturnsTrimmedExpressionWithoutError() {
        val result = evaluate("99/")

        assertEquals("99", result.expr)
        assertNull(result.value)
        assertEquals(INVALID_RES_ID, result.errorResId)
    }

    @Test
    fun undefinedExpressionReturnsNanError() {
        val result = evaluate("0/0")
        assertEquals(R.string.error_nan, result.errorResId)
    }

    @Test
    fun malformedExpressionReturnsSyntaxError() {
        val result = evaluate("1/(")
        assertEquals(R.string.error_syntax, result.errorResId)
    }

    private fun evaluate(expression: String): Result {
        val result = Result()
        evaluator.evaluate(expression) { expr, value, errorResId ->
            result.expr = expr
            result.value = value
            result.errorResId = errorResId
        }
        return result
    }

    private data class Result(
        var expr: String = "",
        var value: String? = null,
        var errorResId: Int = 0
    )
}
