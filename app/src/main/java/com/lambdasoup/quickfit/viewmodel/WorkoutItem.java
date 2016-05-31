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


import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.lambdasoup.quickfit.R;
import com.lambdasoup.quickfit.model.DayOfWeek;
import com.lambdasoup.quickfit.model.FitActivity;
import com.lambdasoup.quickfit.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.lambdasoup.quickfit.util.Lists.map;

public class WorkoutItem {
    final public long id;
    final public int durationInMinutes;
    final public int calories;
    final public String label;
    final public String scheduleDisplay;
    public final FitActivity activityType;

    private WorkoutItem(long id, FitActivity activityType, int durationInMinutes, int calories, @Nullable String label, @NonNull String scheduleDisplay) {
        this.id = id;
        this.activityType = activityType;
        this.durationInMinutes = durationInMinutes;
        this.calories = calories;
        this.label = label;
        this.scheduleDisplay = scheduleDisplay;
    }

    public static WorkoutItem getForIdHack(long id) {
        return new WorkoutItem(id, null, 0, 0, "", "");
    }

    @Override
    public String toString() {
        return "WorkoutItem{" + "id=" + id +
                ", activityType='" + activityType + '\'' +
                ", durationInMinutes=" + durationInMinutes +
                ", calories=" + calories +
                ", label='" + label + '\'' +
                ", scheduleDisplay='" + scheduleDisplay + '\'' +
                '}';
    }

    public static class Builder {
        private final Context context;
        private final List<ScheduleItem> scheduleItems = new ArrayList<>();
        private long workoutId;
        private String activityTypeKey;
        private int durationInMinutes;
        private int calories;
        private String label;

        public Builder(Context context) {
            this.context = context;
        }


        public WorkoutItem build(DayOfWeek[] week) {
            FitActivity fitActivity = FitActivity.fromKey(activityTypeKey, context.getResources());

            Collections.sort(scheduleItems, new ScheduleItem.ByCalendar(week));

            String schedulesDisplay = Strings.join(", ", map(scheduleItems, this::formatScheduleShort));
            return new WorkoutItem(workoutId, fitActivity, durationInMinutes, calories, label, schedulesDisplay);
        }

        private String formatScheduleShort(ScheduleItem scheduleItem) {
            Resources resources = context.getResources();
            String dayOfWeek = resources.getString(scheduleItem.dayOfWeek.fullNameResId);
            return resources.getString(R.string.weekday_time_format, dayOfWeek, scheduleItem.time);
        }

        public void addSchedule(ScheduleItem scheduleItem) {
            this.scheduleItems.add(scheduleItem);
        }

        public Builder withWorkoutId(long workoutId) {
            this.workoutId = workoutId;
            return this;
        }

        public Builder withActivityTypeKey(String activityTypeKey) {
            this.activityTypeKey = activityTypeKey;
            return this;
        }

        public Builder withDurationInMinutes(int durationInMinutes) {
            this.durationInMinutes = durationInMinutes;
            return this;
        }

        public Builder withCalories(int calories) {
            this.calories = calories;
            return this;
        }

        public Builder withLabel(String label) {
            this.label = label;
            return this;
        }
    }
}
