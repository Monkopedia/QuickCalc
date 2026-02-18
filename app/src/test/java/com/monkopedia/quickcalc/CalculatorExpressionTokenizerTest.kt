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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculatorExpressionTokenizerTest {

    private lateinit var context: Context
    private lateinit var tokenizer: CalculatorExpressionTokenizer

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        tokenizer = CalculatorExpressionTokenizer(context)
    }

    @Test
    fun normalizesLocalizedOperatorSymbols() {
        val localized = "1" +
            context.getString(R.string.op_div) +
            "2" +
            context.getString(R.string.op_mul) +
            "3" +
            context.getString(R.string.op_sub) +
            "4"

        assertEquals("1/2*3-4", tokenizer.getNormalizedExpression(localized))
    }

    @Test
    fun localizesInfinityToken() {
        assertEquals(context.getString(R.string.inf), tokenizer.getLocalizedExpression("Infinity"))
    }
}
