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
import android.provider.Settings
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import androidx.test.platform.app.InstrumentationRegistry
import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziRule
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import java.util.TimeZone
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CalculatorLegacyScreenshotTest {

    @get:Rule
    val roborazziRule = RoborazziRule(
        options = RoborazziRule.Options(
            outputDirectoryPath = "src/test/screenshots/legacy",
            roborazziOptions = RoborazziOptions(
                compareOptions = RoborazziOptions.CompareOptions(
                    changeThreshold = 0.01f,
                    imageComparator = SimpleImageComparator(
                        maxDistance = 0.007f,
                        hShift = 1,
                        vShift = 1
                    )
                )
            )
        )
    )

    @Before
    fun configureDeterministicRendering() {
        Locale.setDefault(Locale.US)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
        disableAnimations()
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT_LIGHT_QUALIFIERS)
    fun phonePortraitLight() {
        captureCalculator("phone_portrait_light")
    }

    @Test
    @Config(qualifiers = PHONE_LANDSCAPE_LIGHT_QUALIFIERS)
    fun phoneLandscapeLight() {
        captureCalculator("phone_landscape_light")
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT_LIGHT_QUALIFIERS)
    fun tabletPortraitLight() {
        captureCalculator("tablet_portrait_light")
    }

    @Test
    @Config(qualifiers = WINDOWED_LIGHT_QUALIFIERS)
    fun windowedLight() {
        captureCalculator("windowed_light")
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT_DARK_QUALIFIERS)
    fun phonePortraitDark() {
        captureCalculator("phone_portrait_dark")
    }

    @Test
    @Config(qualifiers = TABLET_PORTRAIT_DARK_QUALIFIERS)
    fun tabletPortraitDark() {
        captureCalculator("tablet_portrait_dark")
    }

    @Test
    @Config(qualifiers = PHONE_PORTRAIT_LIGHT_QUALIFIERS)
    fun phonePortraitLargeFont() {
        withFontScale(1.3f) {
            captureCalculator("phone_portrait_large_font")
        }
    }

    private fun captureCalculator(snapshotName: String) {
        val scenario = ActivityScenario.launch(Calculator::class.java)
        try {
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            onView(isRoot()).captureRoboImage(filePath = "$snapshotName.png")
        } finally {
            scenario.close()
        }
    }

    @Suppress("DEPRECATION")
    private fun withFontScale(fontScale: Float, block: () -> Unit) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val resources = context.resources
        val originalConfiguration = Configuration(resources.configuration)
        val updatedConfiguration = Configuration(resources.configuration)
        updatedConfiguration.fontScale = fontScale
        resources.updateConfiguration(updatedConfiguration, resources.displayMetrics)
        try {
            block()
        } finally {
            resources.updateConfiguration(originalConfiguration, resources.displayMetrics)
        }
    }

    private fun disableAnimations() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val contentResolver = context.contentResolver
        Settings.Global.putFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
        Settings.Global.putFloat(contentResolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 0f)
        Settings.Global.putFloat(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 0f)
    }

    companion object {
        private const val PHONE_PORTRAIT_LIGHT_QUALIFIERS =
            "en-rUS-w411dp-h891dp-port-notnight"
        private const val PHONE_LANDSCAPE_LIGHT_QUALIFIERS =
            "en-rUS-w891dp-h411dp-land-notnight"
        private const val TABLET_PORTRAIT_LIGHT_QUALIFIERS =
            "en-rUS-w800dp-h1280dp-port-notnight"
        private const val WINDOWED_LIGHT_QUALIFIERS =
            "en-rUS-w540dp-h380dp-land-notnight"
        private const val PHONE_PORTRAIT_DARK_QUALIFIERS =
            "en-rUS-w411dp-h891dp-port-night"
        private const val TABLET_PORTRAIT_DARK_QUALIFIERS =
            "en-rUS-w800dp-h1280dp-port-night"
    }
}
