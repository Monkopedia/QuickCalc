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

import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CalculatorPerfInstrumentedTest {

    @Test
    fun startupWarmLaunchAverageIsWithinBudget() {
        val iterations = 5
        var totalNanos = 0L

        repeat(iterations) {
            val startNanos = SystemClock.elapsedRealtimeNanos()
            val scenario = ActivityScenario.launch(Calculator::class.java)
            scenario.onActivity { /* wait for launch */ }
            scenario.close()
            totalNanos += (SystemClock.elapsedRealtimeNanos() - startNanos)
        }

        val averageMs = (totalNanos.toDouble() / iterations) / 1_000_000.0
        assertTrue(
            "Average warm launch exceeded budget: ${"%.2f".format(averageMs)} ms",
            averageMs < 2_000.0
        )
    }

    @Test
    fun evaluatorThroughputAverageIsWithinBudget() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val tokenizer = CalculatorExpressionTokenizer(context)
        val evaluator = CalculatorExpressionEvaluator(tokenizer)
        val expressions = listOf(
            "1+2*3",
            "12/4+5",
            "9*(8-3)",
            "sin(0)+cos(0)",
            "7*7-14/2"
        )
        val iterations = 1_000
        var errorCount = 0

        val startNanos = SystemClock.elapsedRealtimeNanos()
        repeat(iterations) { index ->
            val expression = expressions[index % expressions.size]
            evaluator.evaluate(expression) { _, _, errorResourceId ->
                if (errorResourceId != Calculator.INVALID_RES_ID) {
                    errorCount++
                }
            }
        }
        val elapsedNanos = SystemClock.elapsedRealtimeNanos() - startNanos
        val averageMicros = (elapsedNanos.toDouble() / iterations) / 1_000.0

        assertEquals(0, errorCount)
        assertTrue(
            "Average evaluator time exceeded budget: ${"%.2f".format(averageMicros)} µs",
            averageMicros < 5_000.0
        )
    }
}
