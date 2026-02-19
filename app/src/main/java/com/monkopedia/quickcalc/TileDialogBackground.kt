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

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.WindowManager
import androidx.annotation.ColorInt

fun isDialogBlurSupported(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
        return false
    }
    val windowManager = context.getSystemService(WindowManager::class.java) ?: return false
    return windowManager.isCrossWindowBlurEnabled
}

@ColorInt
fun dialogWindowOverlayColorArgb(mode: TileDialogBackgroundMode, darkTheme: Boolean): Int =
    when (mode) {
        TileDialogBackgroundMode.CLEAR -> Color.TRANSPARENT
        TileDialogBackgroundMode.LIGHT -> Color.argb(92, 0, 0, 0)
        TileDialogBackgroundMode.DARK -> Color.argb(148, 0, 0, 0)
        TileDialogBackgroundMode.BLUR_SUBTLE ->
            if (darkTheme) Color.argb(10, 0, 0, 0) else Color.argb(8, 0, 0, 0)
        TileDialogBackgroundMode.BLUR_LIGHT ->
            if (darkTheme) Color.argb(28, 0, 0, 0) else Color.argb(22, 0, 0, 0)
        TileDialogBackgroundMode.BLUR_HEAVY ->
            if (darkTheme) Color.argb(44, 0, 0, 0) else Color.argb(36, 0, 0, 0)
        TileDialogBackgroundMode.BLUR_DEBUG ->
            if (darkTheme) Color.argb(44, 0, 0, 0) else Color.argb(36, 0, 0, 0)
    }

fun dialogWindowBackgroundBlurRadiusPx(mode: TileDialogBackgroundMode): Int = when (mode) {
    TileDialogBackgroundMode.BLUR_SUBTLE -> 12
    TileDialogBackgroundMode.BLUR_LIGHT -> 72
    TileDialogBackgroundMode.BLUR_HEAVY -> 132
    TileDialogBackgroundMode.BLUR_DEBUG -> 132
    else -> 0
}
