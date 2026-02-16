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

package com.android.calculator2;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import java.util.Random;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CalculatorFuzzTest {

    private CalculatorExpressionEvaluator evaluator;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        final CalculatorExpressionTokenizer tokenizer = new CalculatorExpressionTokenizer(context);
        evaluator = new CalculatorExpressionEvaluator(tokenizer);
    }

    @Test
    public void randomMalformedInputsDoNotCrashEvaluator() {
        final Random random = new Random(20260219L);
        for (int i = 0; i < 1000; i++) {
            final String expression = randomInput(random);
            boolean callbackInvoked = false;
            try {
                final boolean[] callback = new boolean[] {false};
                evaluator.evaluate(expression, (expr, result, errorResourceId) -> callback[0] = true);
                callbackInvoked = callback[0];
            } catch (Throwable t) {
                fail("Evaluator crashed for fuzz input [" + expression + "]: " + t.getMessage());
            }
            assertTrue("Callback was not invoked for input [" + expression + "]", callbackInvoked);
        }
    }

    private String randomInput(Random random) {
        final String alphabet = "0123456789+-*/().,∞√^!e@#$%&abcdefghijklmnopqrstuvwxyz";
        final int size = random.nextInt(48);
        final StringBuilder sb = new StringBuilder(size);
        for (int i = 0; i < size; i++) {
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}
