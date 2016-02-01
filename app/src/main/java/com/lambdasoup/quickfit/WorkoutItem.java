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

package com.lambdasoup.quickfit;


public class WorkoutItem {
    final public long id;
    final public int activityTypeIndex;
    final public String activityTypeDisplayName;
    final public int durationInMinutes;
    final public int calories;
    final public String label;

    public WorkoutItem(long id, int activityTypeIndex, String activityTypeDisplayName, int durationInMinutes, int calories, String label) {
        this.id = id;
        this.activityTypeIndex = activityTypeIndex;
        this.activityTypeDisplayName = activityTypeDisplayName;
        this.durationInMinutes = durationInMinutes;
        this.calories = calories;
        this.label = label;
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
        sb.append('}');
        return sb.toString();
    }
}