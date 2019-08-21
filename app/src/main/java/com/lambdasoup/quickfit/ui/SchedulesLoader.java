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

package com.lambdasoup.quickfit.ui;

import android.content.Context;
import androidx.loader.content.CursorLoader;

import com.lambdasoup.quickfit.persist.QuickFitContentProvider;
import com.lambdasoup.quickfit.persist.QuickFitContract;

/**
 * Loader for the schedule items belonging to a single workout
 */
public class SchedulesLoader extends CursorLoader {
    public SchedulesLoader(Context context, long workoutId) {
        super(context,
                QuickFitContentProvider.getUriWorkoutsId(workoutId),
                QuickFitContract.WorkoutEntry.COLUMNS_SCHEDULE_ONLY,
                null,
                null,
                null);
    }
}
