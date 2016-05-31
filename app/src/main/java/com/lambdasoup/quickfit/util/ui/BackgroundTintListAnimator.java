/*
 * Copyright 2016 Juliane Lehmann <jl@lambdasoup.com>
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
 *    limitations under the License.
 */

package com.lambdasoup.quickfit.util.ui;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;

/**
 * Provides an object animator suitable for animating the background color of a {@link android.support.design.widget.FloatingActionButton}.
 * As this can only be done by setting its backgroundTintList to singleton-valued ColorStateLists, the dirty parts necessary are
 * stored here.
 */
public class BackgroundTintListAnimator {
    public static ObjectAnimator create(@NonNull Context context, @NonNull Object target, @ColorRes int startColor, @ColorRes int endColor, long duration) {
        ColorStateList startColorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, startColor));
        ColorStateList endColorStateList = ColorStateList.valueOf(ContextCompat.getColor(context, endColor));
        ObjectAnimator animator = ObjectAnimator.ofObject(target, "backgroundTintList", ColorStateListEvaluator.INSTANCE, startColorStateList, endColorStateList);
        animator.setDuration(duration);
        return animator;
    }

    private static class ColorStateListEvaluator implements TypeEvaluator<ColorStateList> {
        static final ColorStateListEvaluator INSTANCE = new ColorStateListEvaluator();

        private final ArgbEvaluator argbEvaluator = new ArgbEvaluator();

        @Override
        public ColorStateList evaluate(float fraction, ColorStateList startValue, ColorStateList endValue) {
            return ColorStateList.valueOf((Integer) argbEvaluator.evaluate(fraction, startValue.getDefaultColor(), endValue.getDefaultColor()));
        }
    }
}
