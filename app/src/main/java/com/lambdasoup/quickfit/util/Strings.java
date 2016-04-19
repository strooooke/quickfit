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

import java.util.Iterator;

/**
 * Created by jl on 14.03.16.
 */
public class Strings {
    private Strings() {
        // do not instantiate
    }

    public static String join(@SuppressWarnings("SameParameterValue") String separator, Iterable<String> parts) {
        Iterator<String> iterator = parts.iterator();
        if (!iterator.hasNext()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(iterator.next());
        while (iterator.hasNext()) {
            sb.append(separator).append(iterator.next());
        }
        return sb.toString();
    }
}
