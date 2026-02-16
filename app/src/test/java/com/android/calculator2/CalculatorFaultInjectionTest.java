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
import static org.junit.Assert.assertNotEquals;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CalculatorFaultInjectionTest {

    private CalculatorExpressionEvaluator evaluator;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        evaluator = new CalculatorExpressionEvaluator(new CalculatorExpressionTokenizer(context));
    }

    @Test
    public void operatorMutationChangesResult() {
        final String add = evaluate("2+3").value;
        final String sub = evaluate("2-3").value;
        assertNotEquals(add, sub);

        final String mul = evaluate("4*5").value;
        final String div = evaluate("4/5").value;
        assertNotEquals(mul, div);

        final String sin = evaluate("sin(0)").value;
        final String cos = evaluate("cos(0)").value;
        assertNotEquals(sin, cos);
    }

    @Test
    public void injectedInvalidTokenReturnsSyntaxError() {
        final EvalResult result = evaluate("1+@2");
        assertEquals(R.string.error_syntax, result.errorResId);
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
