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

import android.support.annotation.Nullable;

import com.lambdasoup.quickfit.model.DayOfWeek;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;

/**
 * Created by jl on 14.03.16.
 */
public class ScheduleItem {
    final public long id;
    final public DayOfWeek dayOfWeek;
    final public String time;
    final public int minute;
    final public int hour;

    public static class ByCalendar implements Comparator<ScheduleItem> {
        private DayOfWeek[] week;

        public ByCalendar(@Nullable DayOfWeek[] week) {
            this.week = week == null ? new DayOfWeek[0] : week;
        }

        @Override
        public int compare(ScheduleItem left, ScheduleItem right) {
            for (DayOfWeek dayOfWeek : week) {
                if (dayOfWeek == left.dayOfWeek) {
                    if (dayOfWeek == right.dayOfWeek) {
                        // compare times
                        if (left.hour < right.hour) {
                            return -1;
                        } else if (left.hour > right.hour) {
                            return 1;
                        } else {
                            return Integer.compare(left.minute, right.minute);
                        }
                    } else {
                        return -1;
                    }
                } else if (dayOfWeek == right.dayOfWeek) {
                    return 1;
                }
            }
            throw new RuntimeException("The week seems to be missing days. Week is: " + Arrays.toString(week) + ", trying to find " + left.dayOfWeek + " and " + right.dayOfWeek);
        }
    };


    // not private for testing
    protected ScheduleItem(long id, DayOfWeek dayOfWeek, String time, int minute, int hour) {
        this.id = id;
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.minute = minute;
        this.hour = hour;
    }

    public static class Builder {
        private long scheduleId;
        private String dayOfWeekName;
        private int minute;
        private int hour;


        public ScheduleItem build() {
            Calendar time = Calendar.getInstance();
            time.set(Calendar.HOUR_OF_DAY, hour);
            time.set(Calendar.MINUTE, minute);
            time.set(Calendar.SECOND, 0); // seconds should not be shown, but just in case
            String timeFormatted = SimpleDateFormat.getTimeInstance(DateFormat.SHORT).format(time.getTime());

            DayOfWeek dayOfWeek = DayOfWeek.valueOf(dayOfWeekName);

            return new ScheduleItem(
                    scheduleId,
                    dayOfWeek,
                    timeFormatted,
                    minute,
                    hour
            );
        }

        public Builder withScheduleId(long scheduleId) {
            this.scheduleId = scheduleId;
            return this;
        }

        public Builder withHour(int hour) {
            this.hour = hour;
            return this;
        }

        public Builder withMinute(int minute) {
            this.minute = minute;
            return this;
        }

        public Builder withDayOfWeekName(String dayOfWeekName) {
            this.dayOfWeekName = dayOfWeekName;
            return this;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ScheduleItem that = (ScheduleItem) o;

        if (id != that.id) return false;
        if (minute != that.minute) return false;
        if (hour != that.hour) return false;
        if (dayOfWeek != that.dayOfWeek) return false;
        return !(time != null ? !time.equals(that.time) : that.time != null);

    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (dayOfWeek != null ? dayOfWeek.hashCode() : 0);
        result = 31 * result + (time != null ? time.hashCode() : 0);
        result = 31 * result + minute;
        result = 31 * result + hour;
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ScheduleItem{");
        sb.append("id=").append(id);
        sb.append(", dayOfWeek=").append(dayOfWeek);
        sb.append(", time='").append(time).append('\'');
        sb.append(", minute=").append(minute);
        sb.append(", hour=").append(hour);
        sb.append('}');
        return sb.toString();
    }
}
