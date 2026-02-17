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

import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.containsString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CalculatorSmokeInstrumentedTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(Calculator::class.java)

    @Test
    fun launchRendersCoreViews() {
        onView(withId(R.id.display)).check(matches(isDisplayed()))
        onView(withId(R.id.formula)).check(matches(isDisplayed()))
        onView(withId(R.id.result)).check(matches(isDisplayed()))
        onView(withId(R.id.pad_pager)).check(matches(isDisplayed()))
    }

    @Test
    fun additionFlowRendersExpectedResult() {
        tap(R.id.digit_1, R.id.op_add, R.id.digit_2, R.id.eq)
        assertResultEventuallyContains("3")
    }

    private fun tap(vararg viewIds: Int) {
        viewIds.forEach { viewId ->
            onView(withId(viewId)).perform(click())
        }
    }

    private fun assertResultEventuallyContains(expected: String, timeoutMs: Long = 5_000) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        var latestResult = ""
        while (SystemClock.elapsedRealtime() < deadline) {
            scenarioRule.scenario.onActivity { activity ->
                latestResult = activity.findViewById<CalculatorEditText>(R.id.result)
                    .text
                    .toString()
            }
            if (latestResult.contains(expected)) {
                return
            }
            Thread.sleep(50)
        }
        onView(withId(R.id.result)).check(matches(withText(containsString(expected))))
    }
}
