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

package com.lambdasoup.quickfit.model;

import com.lambdasoup.quickfit.model.DayOfWeek;

import org.junit.Assert;
import org.junit.Test;

import java.util.Calendar;

import static com.lambdasoup.quickfit.model.DayOfWeek.FRIDAY;
import static com.lambdasoup.quickfit.model.DayOfWeek.MONDAY;
import static com.lambdasoup.quickfit.model.DayOfWeek.SATURDAY;
import static com.lambdasoup.quickfit.model.DayOfWeek.SUNDAY;
import static com.lambdasoup.quickfit.model.DayOfWeek.THURSDAY;
import static com.lambdasoup.quickfit.model.DayOfWeek.TUESDAY;
import static com.lambdasoup.quickfit.model.DayOfWeek.WEDNESDAY;

/**
 * Created by jl on 07.03.16.
 */
public class DayOfWeekTest {

    @Test
    public void week_startsMonday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        Assert.assertArrayEquals(new DayOfWeek[]{MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY}, DayOfWeek.getWeek(calendar));
    }

    @Test
    public void week_startsTuesday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.TUESDAY);
        Assert.assertArrayEquals(new DayOfWeek[]{TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, MONDAY}, DayOfWeek.getWeek(calendar));
    }

    @Test
    public void week_startsWednesday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.WEDNESDAY);
        Assert.assertArrayEquals(new DayOfWeek[]{ WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY, MONDAY, TUESDAY}, DayOfWeek.getWeek(calendar));
    }

    @Test
    public void week_startsThursday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.THURSDAY);
        Assert.assertArrayEquals(new DayOfWeek[]{THURSDAY, FRIDAY, SATURDAY, SUNDAY, MONDAY, TUESDAY, WEDNESDAY}, DayOfWeek.getWeek(calendar));
    }

    @Test
    public void week_startsFriday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.FRIDAY);
        Assert.assertArrayEquals(new DayOfWeek[]{FRIDAY, SATURDAY, SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY}, DayOfWeek.getWeek(calendar));
    }

    @Test
    public void week_startsSaturday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SATURDAY);
        Assert.assertArrayEquals(new DayOfWeek[]{SATURDAY, SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY}, DayOfWeek.getWeek(calendar));
    }

    @Test
    public void week_startsSunday() {
        Calendar calendar = Calendar.getInstance();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        Assert.assertArrayEquals(new DayOfWeek[]{SUNDAY, MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY}, DayOfWeek.getWeek(calendar));
    }
}
