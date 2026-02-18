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
import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculatorPropertyBasedTest {

    private lateinit var tokenizer: CalculatorExpressionTokenizer
    private lateinit var evaluator: CalculatorExpressionEvaluator

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        tokenizer = CalculatorExpressionTokenizer(context)
        evaluator = CalculatorExpressionEvaluator(tokenizer)
    }

    @Test
    fun additionIsCommutativeForRandomIntegers() {
        val random = Random(20260216L)
        for (index in 0 until 250) {
            val firstOperand = random.nextInt(2001) - 1000
            val secondOperand = random.nextInt(2001) - 1000

            val left = evaluateValue("$firstOperand+$secondOperand")
            val right = evaluateValue("$secondOperand+$firstOperand")

            assertNotNull(left)
            assertNotNull(right)
            assertEquals(left, right)
        }
    }

    @Test
    fun multiplicationIsCommutativeForRandomIntegers() {
        val random = Random(20260217L)
        for (index in 0 until 250) {
            val firstOperand = random.nextInt(201) - 100
            val secondOperand = random.nextInt(201) - 100

            val left = evaluateValue("$firstOperand*$secondOperand")
            val right = evaluateValue("$secondOperand*$firstOperand")

            assertNotNull(left)
            assertNotNull(right)
            assertEquals(left, right)
        }
    }

    @Test
    fun localizationRoundTripPreservesCanonicalExpression() {
        val random = Random(20260218L)
        for (index in 0 until 300) {
            val canonical = randomCanonicalExpression(random)
            val localized = tokenizer.getLocalizedExpression(canonical)
            val normalized = tokenizer.getNormalizedExpression(localized)

            assertEquals(canonical, normalized)
        }
    }

    private fun randomCanonicalExpression(random: Random): String {
        val tokens = arrayOf(
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
            ".", "+", "-", "*", "/", "(", ")", "sin", "cos", "tan", "ln", "log",
            "Infinity"
        )
        val tokenCount = random.nextInt(12) + 1
        val stringBuilder = StringBuilder()
        for (index in 0 until tokenCount) {
            stringBuilder.append(tokens[random.nextInt(tokens.size)])
        }
        return stringBuilder.toString()
    }

    private fun evaluateValue(expression: String): String? = evaluate(expression).value

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
