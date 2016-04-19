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

import java.util.Calendar;

/**
 * Created by jl on 22.03.16.
 */
public class DateTimes {
    private DateTimes() {
        // do not instantiate
    }


    public static long getNextOccurrence(long now, DayOfWeek dayOfWeek, int hour, int minute) {
        Calendar nowCal = Calendar.getInstance();
        nowCal.setTimeInMillis(now);

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(now);

        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);

        if (calendar.getTimeInMillis() <= now) {
            calendar.add(Calendar.DATE, 1);
        }

        // Am I glad that there are only 7 week days.
        while (calendar.get(Calendar.DAY_OF_WEEK) != dayOfWeek.calendarConst) {
            calendar.add(Calendar.DATE, 1);
        }

        return calendar.getTimeInMillis();
    }
}
