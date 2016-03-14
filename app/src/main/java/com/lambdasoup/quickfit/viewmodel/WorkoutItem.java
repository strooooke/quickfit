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

import com.lambdasoup.quickfit.R;
import com.lambdasoup.quickfit.model.FitActivity;
import com.lambdasoup.quickfit.util.Function;
import com.lambdasoup.quickfit.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.lambdasoup.quickfit.util.Lists.map;

public class WorkoutItem {
    final public long id;
    final public int activityTypeIndex;
    final public String activityTypeDisplayName;
    final public int durationInMinutes;
    final public int calories;
    final public String label;
    final public String scheduleDisplay;

    private WorkoutItem(long id, int activityTypeIndex, String activityTypeDisplayName, int durationInMinutes, int calories, String label, String scheduleDisplay) {
        this.id = id;
        this.activityTypeIndex = activityTypeIndex;
        this.activityTypeDisplayName = activityTypeDisplayName;
        this.durationInMinutes = durationInMinutes;
        this.calories = calories;
        this.label = label;
        this.scheduleDisplay = scheduleDisplay;
    }

    public static WorkoutItem getForIdHack(long id) {
        return new WorkoutItem(id, 0, "", 0, 0, "", "");
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("WorkoutItem{");
        sb.append("id=").append(id);
        sb.append(", activityTypeIndex=").append(activityTypeIndex);
        sb.append(", activityTypeDisplayName='").append(activityTypeDisplayName).append('\'');
        sb.append(", durationInMinutes=").append(durationInMinutes);
        sb.append(", calories=").append(calories);
        sb.append(", label='").append(label).append('\'');
        sb.append(", scheduleDisplay='").append(scheduleDisplay).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static class Builder {
        private final Context context;
        private final Function<FitActivity, Integer> fitActPositionSupplier;

        private long workoutId;
        private String activityTypeKey;
        private int durationInMinutes;
        private int calories;
        private String label;
        private List<ScheduleItem> scheduleItems = new ArrayList<>();

        public Builder(Context context, Function<FitActivity, Integer> fitActPositionSupplier) {
            this.context = context;
            this.fitActPositionSupplier = fitActPositionSupplier;
        }


        public WorkoutItem build() {
            FitActivity fitActivity = FitActivity.fromKey(activityTypeKey, context.getResources());
            int activityTypeIndex = fitActPositionSupplier.apply(fitActivity);

            Collections.sort(scheduleItems, ScheduleItem.BY_CALENDAR);

            String schedulesDisplay = Strings.join(", ", map(scheduleItems, this::formatScheduleShort));
            return new WorkoutItem(workoutId, activityTypeIndex, fitActivity.displayName, durationInMinutes, calories, label, schedulesDisplay);
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
