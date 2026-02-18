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
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculatorFaultInjectionTest {

    private lateinit var evaluator: CalculatorExpressionEvaluator

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        evaluator = CalculatorExpressionEvaluator(CalculatorExpressionTokenizer(context))
    }

    @Test
    fun operatorMutationChangesResult() {
        val add = evaluate("2+3").value
        val sub = evaluate("2-3").value
        assertNotEquals(add, sub)

        val mul = evaluate("4*5").value
        val div = evaluate("4/5").value
        assertNotEquals(mul, div)

        val sin = evaluate("sin(0)").value
        val cos = evaluate("cos(0)").value
        assertNotEquals(sin, cos)
    }

    @Test
    fun injectedInvalidTokenReturnsSyntaxError() {
        val result = evaluate("1+@2")
        assertEquals(R.string.error_syntax, result.errorResId)
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
