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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class CalculatorComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val initialFormula = intent.getStringExtra(EXTRA_INITIAL_FORMULA).orEmpty()
        val evaluateInitialExpression = intent.getBooleanExtra(
            EXTRA_EVALUATE_INITIAL_EXPRESSION,
            false
        )
        val initialPadPage = intent.getIntExtra(EXTRA_INITIAL_PAD_PAGE, 0)
        setContent {
            CalculatorComposeRoute(
                initialFormula = initialFormula,
                evaluateInitialExpression = evaluateInitialExpression,
                initialPadPage = initialPadPage
            )
        }
    }

    companion object {
        private const val EXTRA_INITIAL_FORMULA = "extra_initial_formula"
        private const val EXTRA_EVALUATE_INITIAL_EXPRESSION = "extra_evaluate_initial_expression"
        private const val EXTRA_INITIAL_PAD_PAGE = "extra_initial_pad_page"

        @Suppress("ktlint:standard:function-expression-body")
        fun newIntent(
            context: Context,
            initialFormula: String = "",
            evaluateInitialExpression: Boolean = false,
            initialPadPage: Int = 0
        ): Intent {
            return Intent(context, CalculatorComposeActivity::class.java).apply {
                putExtra(EXTRA_INITIAL_FORMULA, initialFormula)
                putExtra(EXTRA_EVALUATE_INITIAL_EXPRESSION, evaluateInitialExpression)
                putExtra(EXTRA_INITIAL_PAD_PAGE, initialPadPage)
            }
        }
    }
}
