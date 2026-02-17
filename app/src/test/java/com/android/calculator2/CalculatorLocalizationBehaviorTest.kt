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
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CalculatorLocalizationBehaviorTest {

    @Test
    fun frenchLocaleUsesDecimalCommaAndRoundTripsToCanonical() {
        val tokenizer = CalculatorExpressionTokenizer(localizedContext(Locale.FRANCE))
        val localized = tokenizer.getLocalizedExpression("3.14")

        assertTrue("Expected decimal comma for French locale", localized.contains(','))
        assertEquals("3.14", tokenizer.getNormalizedExpression(localized))
    }

    @Test
    fun persianLocaleUsesLocalizedDigitsAndRoundTripsToCanonical() {
        val locale = Locale.Builder().setLanguage("fa").setRegion("IR").build()
        val tokenizer = CalculatorExpressionTokenizer(localizedContext(locale))
        val localized = tokenizer.getLocalizedExpression("1234.5")

        assertNotEquals("1234.5", localized)
        assertEquals("1234.5", tokenizer.getNormalizedExpression(localized))
    }

    private fun localizedContext(locale: Locale): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocales(LocaleList(locale))
        return base.createConfigurationContext(configuration)
    }
}
