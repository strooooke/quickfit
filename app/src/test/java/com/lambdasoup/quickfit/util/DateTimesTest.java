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

import com.lambdasoup.quickfit.model.DayOfWeek;

import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.TimeZone;

import static org.junit.Assert.*;

/**
 * Created by jl on 22.03.16.
 */
public class DateTimesTest {

    private static long JULY_FIRST_2016; // 2016-07-01T13:00Z+02:00, Friday

    static {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        cal.set(2016, Calendar.JULY, 1, 13, 0, 0);
        cal.set(Calendar.MILLISECOND, 0);
        JULY_FIRST_2016 = cal.getTimeInMillis();
    }


    @Test
    public void testGetNextOccurence_laterToday() throws Exception {
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        expected.set(2016, Calendar.JULY, 1, 14, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertEquals(expected.getTimeInMillis(), DateTimes.getNextOccurence(JULY_FIRST_2016, DayOfWeek.FRIDAY, 14, 0));
    }

    @Test
    public void testGetNextOccurence_tomorrow() throws Exception {
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        expected.set(2016, Calendar.JULY, 2, 9, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertEquals(expected.getTimeInMillis(), DateTimes.getNextOccurence(JULY_FIRST_2016, DayOfWeek.SATURDAY, 9, 0));
    }

    @Test
    public void testGetNextOccurence_overmorrow() throws Exception {
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        expected.set(2016, Calendar.JULY, 3, 9, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertEquals(expected.getTimeInMillis(), DateTimes.getNextOccurence(JULY_FIRST_2016, DayOfWeek.SUNDAY, 9, 0));
    }

    @Test
    public void testGetNextOccurence_earlierToday() throws Exception {
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        expected.set(2016, Calendar.JULY, 8, 9, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertEquals(expected.getTimeInMillis(), DateTimes.getNextOccurence(JULY_FIRST_2016, DayOfWeek.FRIDAY, 9, 0));
    }

    @Test
    public void testGetNextOccurence_now() throws Exception {
        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        expected.set(2016, Calendar.JULY, 8, 13, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertEquals(expected.getTimeInMillis(), DateTimes.getNextOccurence(JULY_FIRST_2016, DayOfWeek.FRIDAY, 13, 0));
    }

    @Test
    public void testGetNextOccurence_springForward() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        cal.set(2016, Calendar.MARCH, 26, 13, 0, 0); // Saturday
        cal.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        expected.set(2016, Calendar.MARCH, 28, 13, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertEquals(expected.getTimeInMillis(), DateTimes.getNextOccurence(cal.getTimeInMillis(), DayOfWeek.MONDAY, 13, 0));
    }

    @Test
    public void testGetNextOccurence_nextMonth() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        cal.set(2016, Calendar.JULY, 29, 13, 0, 0); // Friday
        cal.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        expected.set(2016, Calendar.AUGUST, 1, 13, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertEquals(expected.getTimeInMillis(), DateTimes.getNextOccurence(cal.getTimeInMillis(), DayOfWeek.MONDAY, 13, 0));
    }

    @Test
    public void testGetNextOccurence_fallBack() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        cal.set(2016, Calendar.OCTOBER, 29, 13, 0, 0); // Saturday
        cal.set(Calendar.MILLISECOND, 0);

        Calendar expected = Calendar.getInstance(TimeZone.getTimeZone("Europe/Berlin"));
        expected.set(2016, Calendar.NOVEMBER, 1, 13, 0, 0);
        expected.set(Calendar.MILLISECOND, 0);
        assertEquals(expected.getTimeInMillis(), DateTimes.getNextOccurence(cal.getTimeInMillis(), DayOfWeek.TUESDAY, 13, 0));
    }
}