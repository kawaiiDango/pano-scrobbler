/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.arn.scrobble.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val PanoIcons.RectFilledTranslucent: ImageVector
    get() {
        if (_RectFilledTranslucent != null) {
            return _RectFilledTranslucent!!
        }
        _RectFilledTranslucent = ImageVector.Builder(
            name = "RectFilledTranslucent",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // draw a solid rectangle as the bg first
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 0.15f,
                stroke = null,
            ) {
                moveTo(0f, 0f)
                lineTo(24f, 0f)
                lineTo(24f, 24f)
                lineTo(0f, 24f)
                close()
            }
        }.build()
        return _RectFilledTranslucent!!
    }

private var _RectFilledTranslucent: ImageVector? = null
