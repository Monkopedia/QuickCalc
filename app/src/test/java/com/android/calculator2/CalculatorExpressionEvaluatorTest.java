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
public class CalculatorExpressionEvaluatorTest {

    private Context context;
    private CalculatorExpressionEvaluator evaluator;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        evaluator = new CalculatorExpressionEvaluator(new CalculatorExpressionTokenizer(context));
    }

    @Test
    public void evaluatesOperatorPrecedence() {
        final EvaluationResult result = evaluate("1+2*3");
        assertEquals("1+2*3", result.expr);
        assertEquals("7", result.result);
        assertEquals(Calculator.INVALID_RES_ID, result.errorResourceId);
    }

    @Test
    public void trimsTrailingOperatorBeforeEvaluation() {
        final EvaluationResult result = evaluate("1+");
        assertEquals("1", result.expr);
        assertNull(result.result);
        assertEquals(Calculator.INVALID_RES_ID, result.errorResourceId);
    }

    @Test
    public void reportsSyntaxError() {
        final EvaluationResult result = evaluate("1/(");
        assertEquals(R.string.error_syntax, result.errorResourceId);
        assertNull(result.result);
    }

    @Test
    public void reportsNanError() {
        final EvaluationResult result = evaluate("0/0");
        assertEquals(R.string.error_nan, result.errorResourceId);
        assertNull(result.result);
    }

    @Test
    public void localizesInfinityResult() {
        final EvaluationResult result = evaluate("1/0");
        assertEquals(context.getString(R.string.inf), result.result);
        assertEquals(Calculator.INVALID_RES_ID, result.errorResourceId);
    }

    private EvaluationResult evaluate(String expression) {
        final EvaluationResult holder = new EvaluationResult();
        evaluator.evaluate(expression, (expr, result, errorResourceId) -> {
            holder.expr = expr;
            holder.result = result;
            holder.errorResourceId = errorResourceId;
        });
        return holder;
    }

    private static final class EvaluationResult {
        private String expr;
        private String result;
        private int errorResourceId = Calculator.INVALID_RES_ID;
    }
}
