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
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculatorFuzzTest {

    private lateinit var evaluator: CalculatorExpressionEvaluator

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val tokenizer = CalculatorExpressionTokenizer(context)
        evaluator = CalculatorExpressionEvaluator(tokenizer)
    }

    @Test
    fun randomMalformedInputsDoNotCrashEvaluator() {
        val random = Random(20260219L)
        for (i in 0 until 1000) {
            val expression = randomInput(random)
            var callbackInvoked = false
            try {
                val callback = booleanArrayOf(false)
                evaluator.evaluate(expression) { _, _, _ ->
                    callback[0] = true
                }
                callbackInvoked = callback[0]
            } catch (throwable: Throwable) {
                fail("Evaluator crashed for fuzz input [$expression]: ${throwable.message}")
            }
            assertTrue("Callback was not invoked for input [$expression]", callbackInvoked)
        }
    }

    private fun randomInput(random: Random): String {
        val alphabet = "0123456789+-*/().,∞√^!e@#$%&abcdefghijklmnopqrstuvwxyz"
        val size = random.nextInt(48)
        val stringBuilder = StringBuilder(size)
        for (index in 0 until size) {
            stringBuilder.append(alphabet[random.nextInt(alphabet.length)])
        }
        return stringBuilder.toString()
    }
}
