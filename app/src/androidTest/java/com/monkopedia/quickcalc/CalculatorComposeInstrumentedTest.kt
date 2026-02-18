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

package com.monkopedia.quickcalc

import android.content.ClipboardManager
import android.content.pm.ActivityInfo
import android.view.KeyEvent
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class CalculatorComposeInstrumentedTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<CalculatorComposeActivity>()

    @Test
    fun launchRendersComposeNodes() {
        composeRule.onNodeWithTag(TEST_TAG_DISPLAY).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_FORMULA).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_RESULT).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_DIGIT_1).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_ADD).assertIsDisplayed()
    }

    @Test
    fun additionFlowRendersExpectedResult() {
        tap(TEST_TAG_DIGIT_1, TEST_TAG_ADD, TEST_TAG_DIGIT_2, TEST_TAG_EQUALS)
        composeRule.onNodeWithTag(TEST_TAG_RESULT).assertTextContains("3")
    }

    @Test
    fun hardwareEnterEvaluatesCurrentExpression() {
        tap(TEST_TAG_DIGIT_1, TEST_TAG_ADD, TEST_TAG_DIGIT_2)
        pressHardwareKey(KeyEvent.KEYCODE_ENTER)

        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TEST_TAG_RESULT).assertTextContains("3")
    }

    @Test
    fun ctrlClipboardShortcutsPasteAndCopyFormulaText() {
        val clipboard = composeRule.activity.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("expr", "45+6"))

        pressHardwareKey(
            keyCode = KeyEvent.KEYCODE_V,
            metaState = KeyEvent.META_CTRL_ON
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(TEST_TAG_FORMULA).assertTextContains("45+6")

        pressHardwareKey(
            keyCode = KeyEvent.KEYCODE_C,
            metaState = KeyEvent.META_CTRL_ON
        )
        composeRule.waitForIdle()

        val copiedText = clipboard.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(composeRule.activity)
            ?.toString()
            .orEmpty()
        assertTrue("Expected copied formula text in clipboard", copiedText.contains("45+6"))
    }

    @Test
    fun equalsTransitionSettlesWithoutDuplicateDisplayRows() {
        tap(TEST_TAG_DIGIT_1, TEST_TAG_ADD, TEST_TAG_DIGIT_2)
        composeRule.onNodeWithTag(TEST_TAG_RESULT).assertTextContains("3")

        composeRule.mainClock.autoAdvance = false
        try {
            composeRule.onNodeWithTag(TEST_TAG_EQUALS).performClick()
            composeRule.mainClock.advanceTimeByFrame()

            composeRule.mainClock.advanceTimeBy(700L)
            composeRule.waitForIdle()

            composeRule.onAllNodesWithTag(TEST_TAG_FORMULA).assertCountEquals(0)
            composeRule.onAllNodesWithTag(TEST_TAG_RESULT).assertCountEquals(1)
            composeRule.onNodeWithTag(TEST_TAG_RESULT).assertTextContains("3")
            composeRule.onNodeWithTag(TEST_TAG_RESULT).assertIsDisplayed()
            composeRule.onAllNodesWithText("1+2").assertCountEquals(0)
        } finally {
            composeRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun swipeLeftShowsAdvancedPadAndAppendsFunctionToken() {
        composeRule.onNodeWithTag(TEST_TAG_PAD_PAGER).performTouchInput {
            swipeLeft()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag(TEST_TAG_FUN_SIN) and hasClickAction())
                .fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNode(hasTestTag(TEST_TAG_FUN_SIN) and hasClickAction())
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithTag(TEST_TAG_FORMULA).assertTextContains("sin(")
    }

    @Test
    fun backFromAdvancedPadReturnsToPrimaryPad() {
        composeRule.onNodeWithTag(TEST_TAG_PAD_PAGER).performTouchInput {
            swipeLeft()
        }
        composeRule.onNodeWithTag(TEST_TAG_ADVANCED_PAD).assertIsDisplayed()

        composeRule.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(TEST_TAG_DIGIT_1).assertIsDisplayed()
    }

    @Test
    fun landscapeShowsAdvancedPadWithoutPagerSwipe() {
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(TEST_TAG_PAD_PAGER).assertCountEquals(0)
        composeRule.onNodeWithTag(TEST_TAG_ADVANCED_PAD).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_FUN_SIN).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_EQUALS).assertIsDisplayed()
    }

    @Test
    @Suppress("DEPRECATION")
    fun errorTransitionUpdatesStatusBarColor() {
        composeRule.waitForIdle()
        val initialStatusBarColor = composeRule.activity.window.statusBarColor

        tap(TEST_TAG_DIGIT_0, TEST_TAG_DIVIDE, TEST_TAG_DIGIT_0, TEST_TAG_EQUALS)
        composeRule.waitForIdle()
        val errorStatusBarColor = composeRule.activity.window.statusBarColor
        val expectedErrorColor = composeRule.activity.getColor(R.color.calculator_error_color)

        assertTrue(
            "Unexpected status bar error color transition: " +
                "initial=$initialStatusBarColor, error=$errorStatusBarColor",
            errorStatusBarColor == expectedErrorColor ||
                (initialStatusBarColor == 0 && errorStatusBarColor == 0)
        )
    }

    @Test
    fun primaryButtonsExposeTalkbackLabelAndTouchTarget() {
        val minTouchTargetPx = with(composeRule.density) { 48.dp.roundToPx() }
        val tags = listOf(
            TEST_TAG_DIGIT_0,
            TEST_TAG_DIGIT_1,
            TEST_TAG_DIGIT_2,
            TEST_TAG_DIGIT_3,
            TEST_TAG_DIGIT_4,
            TEST_TAG_DIGIT_5,
            TEST_TAG_DIGIT_6,
            TEST_TAG_DIGIT_7,
            TEST_TAG_DIGIT_8,
            TEST_TAG_DIGIT_9,
            TEST_TAG_DECIMAL,
            TEST_TAG_DIVIDE,
            TEST_TAG_MULTIPLY,
            TEST_TAG_SUBTRACT,
            TEST_TAG_ADD,
            TEST_TAG_EQUALS
        )

        tags.forEach { tag ->
            val node = composeRule.onNodeWithTag(tag).fetchSemanticsNode()
            assertTrue(
                "Missing click semantics for $tag",
                node.config.contains(SemanticsActions.OnClick)
            )

            val hasContentDescription =
                node.config.contains(SemanticsProperties.ContentDescription) &&
                    node.config[SemanticsProperties.ContentDescription]
                        .any { contentDescription -> contentDescription.isNotEmpty() }
            assertTrue(
                "Missing TalkBack label for $tag",
                hasContentDescription
            )

            assertTrue(
                "Touch target for $tag is below 48dp minimum",
                node.size.width >= minTouchTargetPx && node.size.height >= minTouchTargetPx
            )
        }
    }

    private fun tap(vararg tags: String) {
        tags.forEach { tag ->
            composeRule.onNodeWithTag(tag).performClick()
        }
    }

    private fun pressHardwareKey(keyCode: Int, metaState: Int = 0) {
        composeRule.runOnUiThread {
            composeRule.activity.dispatchKeyEvent(
                KeyEvent(0L, 0L, KeyEvent.ACTION_DOWN, keyCode, 0, metaState)
            )
            composeRule.activity.dispatchKeyEvent(
                KeyEvent(0L, 0L, KeyEvent.ACTION_UP, keyCode, 0, metaState)
            )
        }
    }
}
