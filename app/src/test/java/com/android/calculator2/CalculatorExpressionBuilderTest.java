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

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class CalculatorExpressionBuilderTest {

    private CalculatorExpressionTokenizer tokenizer;

    @Before
    public void setUp() {
        final Context context = ApplicationProvider.getApplicationContext();
        tokenizer = new CalculatorExpressionTokenizer(context);
    }

    @Test
    public void firstEditReplacesEntireExpressionWhenNotEdited() {
        final CalculatorExpressionBuilder builder =
                new CalculatorExpressionBuilder("123", tokenizer, false);

        builder.replace(builder.length(), builder.length(), "4", 0, 1);

        assertEquals("4", builder.toString());
    }

    @Test
    public void preventsLeadingBinaryOperator() {
        final CalculatorExpressionBuilder builder =
                new CalculatorExpressionBuilder("", tokenizer, true);

        builder.replace(0, 0, "+", 0, 1);

        assertEquals("", builder.toString());
    }

    @Test
    public void collapsesSuccessiveOperatorsAtEnd() {
        final CalculatorExpressionBuilder builder =
                new CalculatorExpressionBuilder("1+", tokenizer, true);

        builder.replace(builder.length(), builder.length(), "*", 0, 1);

        assertEquals("1*", tokenizer.getNormalizedExpression(builder.toString()));
    }

    @Test
    public void preventsDuplicateDecimalInSingleNumber() {
        final CalculatorExpressionBuilder builder =
                new CalculatorExpressionBuilder("1.2", tokenizer, true);

        builder.replace(builder.length(), builder.length(), ".", 0, 1);

        assertEquals("1.2", tokenizer.getNormalizedExpression(builder.toString()));
    }

    @Test
    public void collapsesPlusMinusSequence() {
        final CalculatorExpressionBuilder builder =
                new CalculatorExpressionBuilder("1+", tokenizer, true);

        builder.replace(builder.length(), builder.length(), "-", 0, 1);

        assertEquals("1-", tokenizer.getNormalizedExpression(builder.toString()));
    }
}
