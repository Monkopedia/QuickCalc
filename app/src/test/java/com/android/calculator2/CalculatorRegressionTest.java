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
public class CalculatorRegressionTest {

    private CalculatorExpressionTokenizer tokenizer;
    private CalculatorExpressionEvaluator evaluator;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        tokenizer = new CalculatorExpressionTokenizer(context);
        evaluator = new CalculatorExpressionEvaluator(tokenizer);
    }

    @Test
    public void duplicateTrailingOperatorIsCollapsed() {
        final CalculatorExpressionBuilder builder =
                new CalculatorExpressionBuilder("1+", tokenizer, true);

        builder.replace(builder.length(), builder.length(), "*", 0, 1);

        assertEquals("1*", tokenizer.getNormalizedExpression(builder.toString()));
    }

    @Test
    public void duplicateDecimalInSameNumberIsIgnored() {
        final CalculatorExpressionBuilder builder =
                new CalculatorExpressionBuilder("12.3", tokenizer, true);

        builder.replace(builder.length(), builder.length(), ".", 0, 1);

        assertEquals("12.3", tokenizer.getNormalizedExpression(builder.toString()));
    }

    @Test
    public void trailingOperatorReturnsTrimmedExpressionWithoutError() {
        final Result result = evaluate("99/");

        assertEquals("99", result.expr);
        assertNull(result.value);
        assertEquals(Calculator.INVALID_RES_ID, result.errorResId);
    }

    @Test
    public void undefinedExpressionReturnsNanError() {
        final Result result = evaluate("0/0");
        assertEquals(R.string.error_nan, result.errorResId);
    }

    @Test
    public void malformedExpressionReturnsSyntaxError() {
        final Result result = evaluate("1/(");
        assertEquals(R.string.error_syntax, result.errorResId);
    }

    private Result evaluate(String expression) {
        final Result result = new Result();
        evaluator.evaluate(expression, (expr, value, errorResId) -> {
            result.expr = expr;
            result.value = value;
            result.errorResId = errorResId;
        });
        return result;
    }

    private static final class Result {
        private String expr;
        private String value;
        private int errorResId;
    }
}
