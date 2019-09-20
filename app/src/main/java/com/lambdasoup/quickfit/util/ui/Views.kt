/*
 * Copyright 2016-2019 Juliane Lehmann <jl@lambdasoup.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.lambdasoup.quickfit.util.ui

import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets

data class RelativeInsets(val start: Int, val top: Int, val end: Int, val bottom: Int) {
    operator fun plus(other: RelativeInsets) =
            RelativeInsets(
                    start = start + other.start,
                    top = top + other.top,
                    end = end + other.end,
                    bottom = bottom + other.bottom
            )
}

fun View.updateHeight(update: (Int) -> Int) {
    val lp = layoutParams
    lp.height = update(lp.height)
    layoutParams = lp
}

fun View.updateMargins(update: (RelativeInsets) -> RelativeInsets) {
    with(layoutParams as ViewGroup.MarginLayoutParams) {
        val newMargins = update(RelativeInsets(marginStart, topMargin, marginEnd, bottomMargin))
        marginStart = newMargins.start
        marginEnd = newMargins.end
        topMargin = newMargins.top
        bottomMargin = newMargins.bottom
        layoutParams = this
    }
}

fun View.updatePadding(update: (RelativeInsets) -> RelativeInsets) {
    val newPadding = update(RelativeInsets(paddingStart, paddingTop, paddingBottom, paddingEnd))
    setPaddingRelative(
            newPadding.start,
            newPadding.top,
            newPadding.end,
            newPadding.bottom
    )
}

fun WindowInsets.systemWindowInsetsRelative(v: View): RelativeInsets =
        RelativeInsets(
                if (v.layoutDirection == View.LAYOUT_DIRECTION_LTR) systemWindowInsetLeft else systemWindowInsetRight,
                systemWindowInsetTop,
                if (v.layoutDirection == View.LAYOUT_DIRECTION_LTR) systemWindowInsetRight else systemWindowInsetLeft,
                systemWindowInsetBottom
        )
