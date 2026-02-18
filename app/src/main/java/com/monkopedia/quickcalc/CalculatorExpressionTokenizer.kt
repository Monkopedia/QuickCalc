/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.monkopedia.quickcalc

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.text.DecimalFormatSymbols
import java.util.Locale

class CalculatorExpressionTokenizer(context: Context) {

    private val replacementMap: MutableMap<String, String> = linkedMapOf()

    init {
        var locale = currentLocale(context.resources.configuration)
        if (!context.resources.getBoolean(R.bool.use_localized_digits)) {
            locale = Locale.Builder()
                .setLocale(locale)
                .setUnicodeLocaleKeyword("nu", "latn")
                .build()
        }

        val symbols = DecimalFormatSymbols(locale)
        val zeroDigit = symbols.zeroDigit

        replacementMap["."] = symbols.decimalSeparator.toString()

        for (i in 0..9) {
            replacementMap[i.toString()] = (i + zeroDigit.code).toChar().toString()
        }

        replacementMap["/"] = context.getString(R.string.op_div)
        replacementMap["*"] = context.getString(R.string.op_mul)
        replacementMap["-"] = context.getString(R.string.op_sub)

        replacementMap["cos"] = context.getString(R.string.fun_cos)
        replacementMap["ln"] = context.getString(R.string.fun_ln)
        replacementMap["log"] = context.getString(R.string.fun_log)
        replacementMap["sin"] = context.getString(R.string.fun_sin)
        replacementMap["tan"] = context.getString(R.string.fun_tan)

        replacementMap["Infinity"] = context.getString(R.string.inf)
    }

    private fun currentLocale(configuration: Configuration): Locale =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val locales = configuration.locales
            if (!locales.isEmpty) locales[0] else Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }

    fun getNormalizedExpression(expression: String): String {
        var expr = expression
        for ((canonical, localized) in replacementMap) {
            expr = expr.replace(localized, canonical)
        }
        return expr
    }

    fun getLocalizedExpression(expression: String): String {
        var expr = expression
        for ((canonical, localized) in replacementMap) {
            expr = expr.replace(canonical, localized)
        }
        return expr
    }
}
