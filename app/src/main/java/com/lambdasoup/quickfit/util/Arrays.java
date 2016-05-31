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

package com.lambdasoup.quickfit.util;

import android.support.annotation.NonNull;

import java.lang.reflect.Array;
import java.util.NoSuchElementException;

/**
 * Helper methods for arrays
 */
public class Arrays {
    public static <S, T> T[] map(S[] source, Class<T[]> targetClass, Function<S, T> transformation) {
        T[] target = targetClass.cast(Array.newInstance(targetClass.getComponentType(), source.length));
        for (int i = 0; i < source.length; i++) {
            target[i] = transformation.apply(source[i]);
        }
        return target;
    }

    public static <T> int firstIndexOf(@NonNull T[] array, @NonNull T item) {
        for (int i = 0; i < array.length; i++) {
            if (item.equals(array[i])) {
                return i;
            }
        }
        throw new NoSuchElementException();
    }
}
