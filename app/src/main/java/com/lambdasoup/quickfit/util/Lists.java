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

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by jl on 14.03.16.
 */
public class Lists {
    private Lists() {
        // do not instantiate
    }

    public static <S, T> List<T> map(List<S> source, Function<S, T> transformation) {
        final Iterator<S> sourceIterator = source.iterator();
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
}
