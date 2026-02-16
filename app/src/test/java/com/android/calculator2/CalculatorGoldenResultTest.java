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
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CalculatorGoldenResultTest {

    private Context context;
    private CalculatorExpressionEvaluator evaluator;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        evaluator = new CalculatorExpressionEvaluator(new CalculatorExpressionTokenizer(context));
    }

    @Test
    public void deterministicExpressionGoldenSet() {
        assertGoldenValue("1+2*3", "7");
        assertGoldenValue("(1+2)*3", "9");
        assertGoldenValue("sin(0)", "0");
        assertGoldenValue("log(100)", "2");
        assertGoldenValue("1/0", context.getString(R.string.inf));

        assertGoldenError("0/0", R.string.error_nan);
        assertGoldenError("1/(", R.string.error_syntax);
    }

    private void assertGoldenValue(String expression, String expectedValue) {
        final EvalResult result = evaluate(expression);
        assertEquals("Unexpected error for expression: " + expression,
                Calculator.INVALID_RES_ID, result.errorResId);
        assertEquals("Unexpected value for expression: " + expression, expectedValue, result.value);
    }

    private void assertGoldenError(String expression, int expectedErrorResId) {
        final EvalResult result = evaluate(expression);
        assertEquals("Unexpected error code for expression: " + expression,
                expectedErrorResId, result.errorResId);
        assertNull("Expected null value for failing expression: " + expression, result.value);
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
