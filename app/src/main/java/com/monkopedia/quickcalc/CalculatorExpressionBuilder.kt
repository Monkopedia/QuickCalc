/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.text.SpannableStringBuilder
import android.text.TextUtils

class CalculatorExpressionBuilder(
    text: CharSequence,
    private val tokenizer: CalculatorExpressionTokenizer,
    private var isEdited: Boolean
) : SpannableStringBuilder(text) {

    override fun replace(
        start: Int,
        end: Int,
        text: CharSequence?,
        tbstart: Int,
        tbend: Int
    ): SpannableStringBuilder {
        var replacementStart = start
        if (replacementStart != length || end != length) {
            isEdited = true
            return super.replace(replacementStart, end, text, tbstart, tbend)
        }

        var appendExpr = tokenizer.getNormalizedExpression(
            text?.subSequence(tbstart, tbend)?.toString().orEmpty()
        )
        if (appendExpr.length == 1) {
            val expr = tokenizer.getNormalizedExpression(toString())
            when (appendExpr[0]) {
                '.' -> {
                    val index = expr.lastIndexOf('.')
                    if (index != -1 &&
                        TextUtils.isDigitsOnly(expr.substring(index + 1, replacementStart))
                    ) {
                        appendExpr = ""
                    }
                }
                '+', '*', '/' -> {
                    if (replacementStart == 0) {
                        appendExpr = ""
                    } else {
                        while (replacementStart > 0 &&
                            "+-/*".indexOf(expr[replacementStart - 1]) != -1
                        ) {
                            --replacementStart
                        }
                    }
                    if (appendExpr.isEmpty()) {
                        val localized = tokenizer.getLocalizedExpression(appendExpr)
                        return super.replace(replacementStart, end, localized, 0, localized.length)
                    }
                    if (replacementStart > 0 && "+-".indexOf(expr[replacementStart - 1]) != -1) {
                        --replacementStart
                    }
                    isEdited = true
                }
                '-' -> {
                    if (replacementStart > 0 && "+-".indexOf(expr[replacementStart - 1]) != -1) {
                        --replacementStart
                    }
                    isEdited = true
                }
            }
        }

        if (!isEdited && appendExpr.isNotEmpty()) {
            replacementStart = 0
            isEdited = true
        }

        val localized = tokenizer.getLocalizedExpression(appendExpr)
        return super.replace(replacementStart, end, localized, 0, localized.length)
    }
}
