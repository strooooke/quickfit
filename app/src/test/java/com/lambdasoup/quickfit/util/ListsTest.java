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

import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link Lists}
 */
public class ListsTest {
    private static final Comparator<String> ordering = String.CASE_INSENSITIVE_ORDER;

    @Test
    public void bisectLeft_singleton() {
        List<String> list = new ArrayList<>();
        list.add("b");
        assertEquals(0, Lists.bisectLeft(list, ordering, "a"));
        assertEquals(0, Lists.bisectLeft(list, ordering, "b"));
        assertEquals(1, Lists.bisectLeft(list, ordering, "c"));
    }

    @Test
    public void bisectLeft_withRuns() {
        List<String> list = new ArrayList<>();
        list.add("b");
        list.add("b");
        list.add("d");
        list.add("d");
        list.add("d");
        list.add("e");
        list.add("f");
        list.add("f");
        assertEquals(0, Lists.bisectLeft(list, ordering, "a"));
        assertEquals(0, Lists.bisectLeft(list, ordering, "b"));
        assertEquals(2, Lists.bisectLeft(list, ordering, "c"));
        assertEquals(2, Lists.bisectLeft(list, ordering, "d"));
        assertEquals(5, Lists.bisectLeft(list, ordering, "e"));
        assertEquals(6, Lists.bisectLeft(list, ordering, "f"));
        assertEquals(8, Lists.bisectLeft(list, ordering, "g"));
    }
}
