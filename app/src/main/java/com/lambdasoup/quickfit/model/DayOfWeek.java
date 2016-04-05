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

import android.support.annotation.StringRes;

import com.lambdasoup.quickfit.R;

import java.util.Calendar;

/**
 * Created by jl on 07.03.16.
 */
public enum DayOfWeek {
    MONDAY(Calendar.MONDAY, R.string.monday),
    TUESDAY(Calendar.TUESDAY, R.string.tuesday),
    WEDNESDAY(Calendar.WEDNESDAY, R.string.wednesday),
    THURSDAY(Calendar.THURSDAY, R.string.thursday),
    FRIDAY(Calendar.FRIDAY, R.string.friday),
    SATURDAY(Calendar.SATURDAY, R.string.saturday),
    SUNDAY(Calendar.SUNDAY, R.string.sunday);

    public final int calendarConst;
    public final int fullNameResId;
    DayOfWeek(int calendarConst, @StringRes int fullNameResId) {
        this.calendarConst = calendarConst;
        this.fullNameResId = fullNameResId;
    }

    public static DayOfWeek[] getWeek() {
        return getWeek(Calendar.getInstance());
    }

    protected static DayOfWeek[] getWeek(Calendar calendar) {
        int firstDay = calendar.getFirstDayOfWeek();
        DayOfWeek[] week = new DayOfWeek[7];
        for (int i = 0; i < 7; i++) {
            int calendarConst = firstDay + i;
            if (calendarConst > 7) {
                calendarConst -= 7;
            }
            week[i] = getByCalendarConst(calendarConst);
        }
        return week;
    }

    public static DayOfWeek getByCalendarConst(int calendarConst) {
        switch (calendarConst) {
            case Calendar.MONDAY:
                return MONDAY;
            case Calendar.TUESDAY:
                return TUESDAY;
            case Calendar.WEDNESDAY:
                return WEDNESDAY;
            case Calendar.THURSDAY:
                return THURSDAY;
            case Calendar.FRIDAY:
                return FRIDAY;
            case Calendar.SATURDAY:
                return SATURDAY;
            case Calendar.SUNDAY:
                return SUNDAY;
            default:
                throw new IllegalArgumentException("Not a java.util.Calendar weekeday constant: " + calendarConst);
        }
    }

}
