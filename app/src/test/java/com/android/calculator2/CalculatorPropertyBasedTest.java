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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
public class CalculatorPropertyBasedTest {

    private CalculatorExpressionTokenizer tokenizer;
    private CalculatorExpressionEvaluator evaluator;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        tokenizer = new CalculatorExpressionTokenizer(context);
        evaluator = new CalculatorExpressionEvaluator(tokenizer);
    }

    @Test
    public void additionIsCommutativeForRandomIntegers() {
        final Random random = new Random(20260216L);
        for (int i = 0; i < 250; i++) {
            final int a = random.nextInt(2001) - 1000;
            final int b = random.nextInt(2001) - 1000;

            final String left = evaluateValue(a + "+" + b);
            final String right = evaluateValue(b + "+" + a);

            assertNotNull(left);
            assertNotNull(right);
            assertEquals(left, right);
        }
    }

    @Test
    public void multiplicationIsCommutativeForRandomIntegers() {
        final Random random = new Random(20260217L);
        for (int i = 0; i < 250; i++) {
            final int a = random.nextInt(201) - 100;
            final int b = random.nextInt(201) - 100;

            final String left = evaluateValue(a + "*" + b);
            final String right = evaluateValue(b + "*" + a);

            assertNotNull(left);
            assertNotNull(right);
            assertEquals(left, right);
        }
    }

    @Test
    public void localizationRoundTripPreservesCanonicalExpression() {
        final Random random = new Random(20260218L);
        for (int i = 0; i < 300; i++) {
            final String canonical = randomCanonicalExpression(random);
            final String localized = tokenizer.getLocalizedExpression(canonical);
            final String normalized = tokenizer.getNormalizedExpression(localized);

            assertEquals(canonical, normalized);
        }
    }

    private String randomCanonicalExpression(Random random) {
        final String[] tokens = {
                "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
                ".", "+", "-", "*", "/", "(", ")", "sin", "cos", "tan", "ln", "log",
                "Infinity"
        };
        final int tokenCount = random.nextInt(12) + 1;
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tokenCount; i++) {
            sb.append(tokens[random.nextInt(tokens.length)]);
        }
        return sb.toString();
    }

    private String evaluateValue(String expression) {
        final EvalResult result = evaluate(expression);
        return result.value;
    }

    private EvalResult evaluate(String expression) {
        final EvalResult result = new EvalResult();
        evaluator.evaluate(expression, (expr, value, errorResId) -> {
            result.expr = expr;
            result.value = value;
            result.errorResId = errorResId;
        });
        return result;
    }

    private static final class EvalResult {
        private String expr;
        private String value;
        private int errorResId;
    }
}
