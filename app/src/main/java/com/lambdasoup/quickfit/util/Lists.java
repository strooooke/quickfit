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

import java.util.AbstractList;
import java.util.Comparator;
import java.util.List;

/**
 * Helper methods for {@link java.util.List} instances.
 */
public class Lists {
    private Lists() {
        // do not instantiate
    }


    @NonNull
    public static <S, T> List<T> map(@NonNull  List<S> source, @NonNull Function<S, T> transformation) {
        return new AbstractList<T>() {
            @Override
            public T get(int location) {
                return transformation.apply(source.get(location));
            }

            @Override
            public int size() {
                return source.size();
            }
        };
    }

    /**
     * Assumes that list is ordered according to ordering. If not, results
     * are undefined.
     * Finds index i, such that list.get(j) for all j < i is less than item
     * according to ordering, and list.get(j) for all j>= i is at least item
     * according to ordering. In other words, the left-most possible insertion
     * point for item that keeps list ordered is found.
     */
    public static <T> int bisectLeft(@NonNull List<T> list, @NonNull Comparator<T> ordering, @NonNull T item) {
        int low = 0;
        int high = list.size();
        int mid;

        while (low < high) {
            mid = (low + high) / 2;
            if (ordering.compare(item, list.get(mid)) <= 0) {
                high = mid;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }
}
