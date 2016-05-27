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

package com.lambdasoup.quickfit.viewmodel;

import com.lambdasoup.quickfit.model.DayOfWeek;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.verification.VerificationMode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests {@link ScheduleList}, in particular the swapping algorithm.
 */
public class ScheduleListTest2 {
    private static final ScheduleItem ID_1_OLD = new ScheduleItem(1L, DayOfWeek.TUESDAY, "18:30", 18, 30);
    private static final ScheduleItem ID_5_OLD = new ScheduleItem(5L, DayOfWeek.SATURDAY, "15:00", 15, 0);
    private static final ScheduleItem ID_8_OLD = new ScheduleItem(8L, DayOfWeek.FRIDAY, "16:58", 16, 58);

    private static final ScheduleItem ID_2_NEW = new ScheduleItem(2L, DayOfWeek.MONDAY, "12:00", 12, 0);
    private static final ScheduleItem ID_3_NEW = new ScheduleItem(3L, DayOfWeek.WEDNESDAY, "13:00", 13, 0);
    private static final ScheduleItem ID_4_NEW = new ScheduleItem(4L, DayOfWeek.FRIDAY, "16:55", 16, 55);

    private static final ScheduleItem[] OLD_LIST = {
            ID_1_OLD,
            ID_5_OLD,
            ID_8_OLD
    };

    private static final ScheduleItem[] NEW_LIST = {
            ID_2_NEW,
            ID_3_NEW,
            ID_4_NEW
    };

    private Locale defaultLocale;
    private ScheduleList scheduleList;
    private ScheduleList.ItemChangeCallback itemChangeCallback;

    @Before
    public void setUp() throws Exception {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMANY);

        itemChangeCallback = mock(ScheduleList.ItemChangeCallback.class);

        scheduleList = new ScheduleList(itemChangeCallback);
        scheduleList.swapData(Arrays.asList(OLD_LIST));

        reset(itemChangeCallback);
    }

    @After
    public void tearDown() throws Exception {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testSwapData() throws Exception {
        scheduleList.swapData(Arrays.asList(NEW_LIST));

        assertEquals(ID_2_NEW, scheduleList.get(0));
        assertEquals(ID_3_NEW, scheduleList.get(1));
        assertEquals(ID_4_NEW, scheduleList.get(2));

        ArgumentCaptor<Integer> insertionArgs = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> removalArgs = ArgumentCaptor.forClass(Integer.class);

        verify(itemChangeCallback, times(3)).onRemoved(removalArgs.capture());
        verify(itemChangeCallback, times(3)).onInserted(insertionArgs.capture());

        assertEquals(3, insertionArgs.getAllValues().size());
        assertEquals(3, removalArgs.getAllValues().size());
        verifyNoMoreInteractions(itemChangeCallback);
    }





}