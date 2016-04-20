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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Created by jl on 17.03.16.
 */
public class ScheduleListTest {
    private static final ScheduleItem ID_2_OLD = new ScheduleItem(2000L, DayOfWeek.MONDAY, "10:00", 10, 0);
    private static final ScheduleItem ID_3_OLD = new ScheduleItem(3000L, DayOfWeek.MONDAY, "12:15", 12, 15);
    private static final ScheduleItem ID_4 = new ScheduleItem(4000L, DayOfWeek.TUESDAY, "10:00", 10, 0);
    private static final ScheduleItem ID_1_OLD = new ScheduleItem(1000L, DayOfWeek.TUESDAY, "11:30", 11, 30);

    private static final ScheduleItem ID_3_UP = new ScheduleItem(3000L, DayOfWeek.MONDAY, "09:00", 9, 0);
    private static final ScheduleItem ID_3_DOWN = new ScheduleItem(3000L, DayOfWeek.WEDNESDAY, "09:00", 9, 0);
    private static final ScheduleItem ID_3_STAY = new ScheduleItem(3000L, DayOfWeek.MONDAY, "18:00", 18, 0);

    private static final ScheduleItem[] INITIAL_ITEMS = {
            ID_1_OLD,
            ID_2_OLD,
            ID_3_OLD
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
        scheduleList.swapData(Arrays.asList(INITIAL_ITEMS));

        reset(itemChangeCallback);
    }

    @After
    public void tearDown() throws Exception {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testSize() throws Exception {
        assertEquals(3, scheduleList.size());
    }

    @Test
    public void testSwapData_insert() throws Exception {
        List<ScheduleItem> newItems = new ArrayList<>(4);
        newItems.add(ID_1_OLD);
        newItems.add(ID_2_OLD);
        newItems.add(ID_3_OLD);
        newItems.add(ID_4);

        scheduleList.swapData(newItems);
        assertEquals(ID_2_OLD, scheduleList.get(0));
        assertEquals(ID_3_OLD, scheduleList.get(1));
        assertEquals(ID_4, scheduleList.get(2));
        assertEquals(ID_1_OLD, scheduleList.get(3));
        verify(itemChangeCallback).onInserted(2);
        verifyNoMoreInteractions(itemChangeCallback);
    }

    @Test
    public void testSwapData_remove() throws Exception {
        List<ScheduleItem> newItems = new ArrayList<>(2);
        newItems.add(ID_1_OLD);
        newItems.add(ID_2_OLD);

        scheduleList.swapData(newItems);
        assertEquals(ID_2_OLD, scheduleList.get(0));
        assertEquals(ID_1_OLD, scheduleList.get(1));
        verify(itemChangeCallback).onRemoved(1);
        verifyNoMoreInteractions(itemChangeCallback);
    }

    @Test
    public void testSwapData_updateDown() throws Exception {
        List<ScheduleItem> newItems = new ArrayList<>(3);
        newItems.add(ID_1_OLD);
        newItems.add(ID_2_OLD);
        newItems.add(ID_3_DOWN);

        scheduleList.swapData(newItems);
        assertEquals(ID_2_OLD, scheduleList.get(0));
        assertEquals(ID_1_OLD, scheduleList.get(1));
        assertEquals(ID_3_DOWN, scheduleList.get(2));
        verify(itemChangeCallback).onUpdated(1);
        verify(itemChangeCallback).onMoved(1, 2);
        verifyNoMoreInteractions(itemChangeCallback);
    }

    @Test
    public void testSwapData_updateUp() throws Exception {
        List<ScheduleItem> newItems = new ArrayList<>(3);
        newItems.add(ID_1_OLD);
        newItems.add(ID_2_OLD);
        newItems.add(ID_3_UP);

        scheduleList.swapData(newItems);
        assertEquals(ID_3_UP, scheduleList.get(0));
        assertEquals(ID_2_OLD, scheduleList.get(1));
        assertEquals(ID_1_OLD, scheduleList.get(2));
        verify(itemChangeCallback).onUpdated(1);
        verify(itemChangeCallback).onMoved(1, 0);
        verifyNoMoreInteractions(itemChangeCallback);
    }

    @Test
    public void testSwapData_updateStay() throws Exception {
        List<ScheduleItem> newItems = new ArrayList<>(3);
        newItems.add(ID_1_OLD);
        newItems.add(ID_2_OLD);
        newItems.add(ID_3_STAY);

        scheduleList.swapData(newItems);
        assertEquals(ID_2_OLD, scheduleList.get(0));
        assertEquals(ID_3_STAY, scheduleList.get(1));
        assertEquals(ID_1_OLD, scheduleList.get(2));
        verify(itemChangeCallback).onUpdated(1);
        verifyNoMoreInteractions(itemChangeCallback);
    }

    @Test
    public void testGet() throws Exception {
        assertEquals(ID_2_OLD, scheduleList.get(0));
        assertEquals(ID_3_OLD, scheduleList.get(1));
        assertEquals(ID_1_OLD, scheduleList.get(2));
    }

    @Test
    public void testClear() throws Exception {
        scheduleList.clear();
        assertEquals(0, scheduleList.size());
        verify(itemChangeCallback).onCleared();
        verifyNoMoreInteractions(itemChangeCallback);
    }
}