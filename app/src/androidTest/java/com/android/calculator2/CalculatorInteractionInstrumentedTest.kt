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

import android.content.ClipboardManager
import android.content.pm.ActivityInfo
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CalculatorInteractionInstrumentedTest {

    @get:Rule
    val scenarioRule = ActivityScenarioRule(Calculator::class.java)

    @Test
    fun allDigitAndOperatorPathsAcceptInput() {
        tap(
            R.id.digit_0,
            R.id.digit_1,
            R.id.digit_2,
            R.id.digit_3,
            R.id.digit_4,
            R.id.digit_5,
            R.id.digit_6,
            R.id.digit_7,
            R.id.digit_8,
            R.id.digit_9
        )
        onView(withId(R.id.formula)).check(matches(withText(containsString("0123456789"))))

        tap(R.id.op_add, R.id.op_sub, R.id.op_mul, R.id.op_div)
        onView(withId(R.id.formula)).check(matches(withText(containsString("÷"))))
    }

    @Test
    fun orientationChangeAndRecreateKeepsFormulaState() {
        tap(R.id.digit_9, R.id.op_mul, R.id.digit_8)
        scenarioRule.scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        scenarioRule.scenario.recreate()

        onView(withId(R.id.formula)).check(matches(withText(containsString("9"))))
        onView(withId(R.id.formula)).check(matches(withText(containsString("8"))))
    }

    @Test
    fun backgroundForegroundTransitionKeepsAppResponsive() {
        scenarioRule.scenario.moveToState(Lifecycle.State.STARTED)
        scenarioRule.scenario.moveToState(Lifecycle.State.RESUMED)

        tap(R.id.digit_4, R.id.op_add, R.id.digit_5, R.id.eq)
        onView(withId(R.id.result)).check(matches(withText(containsString("9"))))
    }

    @Test
    fun pasteFromClipboardUpdatesFormula() {
        scenarioRule.scenario.onActivity { activity ->
            val clipboard = activity.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("expr", "123"))
            val formula = activity.findViewById<CalculatorEditText>(R.id.formula)
            formula.requestFocus()
            formula.onTextContextMenuItem(android.R.id.paste)

            formula.selectAll()
            formula.onTextContextMenuItem(android.R.id.copy)
            val copiedText = clipboard.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(activity)
                ?.toString()
                .orEmpty()
            assertTrue(copiedText.contains("123"))
        }

        onView(withId(R.id.formula)).check(matches(withText(containsString("123"))))
    }

    @Suppress("DEPRECATION")
    @Test
    fun keyButtonsExposeAccessibilityClickAction() {
        scenarioRule.scenario.onActivity { activity ->
            val buttons = listOf(
                activity.findViewById<android.view.View>(R.id.digit_1),
                activity.findViewById<android.view.View>(R.id.op_add),
                activity.findViewById<android.view.View>(R.id.eq)
            )
            buttons.forEach { button ->
                val node = AccessibilityNodeInfo.obtain()
                button.onInitializeAccessibilityNodeInfo(node)
                try {
                    val hasClickAction = node.actionList.any { action ->
                        action.id == AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK.id
                    }
                    assertTrue(hasClickAction)
                } finally {
                    node.recycle()
                }
            }
        }
    }

    @Test
    fun operatorButtonsExposeExpectedTalkbackLabels() {
        scenarioRule.scenario.onActivity { activity ->
            val expectedLabels = listOf(
                R.id.op_div to R.string.desc_op_div,
                R.id.op_mul to R.string.desc_op_mul,
                R.id.op_sub to R.string.desc_op_sub,
                R.id.op_add to R.string.desc_op_add,
                R.id.eq to R.string.desc_eq,
                R.id.del to R.string.desc_del
            )

            expectedLabels.forEach { (viewId, labelId) ->
                val button = activity.findViewById<android.view.View>(viewId)
                assertEquals(
                    activity.getString(labelId),
                    button.contentDescription?.toString()
                )
            }
        }
    }

    private fun tap(vararg viewIds: Int) {
        scenarioRule.scenario.onActivity { activity ->
            viewIds.forEach { viewId ->
                activity.findViewById<android.view.View>(viewId).performClick()
            }
        }
    }
}
