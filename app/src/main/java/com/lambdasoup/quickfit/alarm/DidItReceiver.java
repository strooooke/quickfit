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

package com.lambdasoup.quickfit.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.lambdasoup.quickfit.FitActivityService;

public class DidItReceiver extends BroadcastReceiver {
    private static final String EXTRA_WORKOUT_ID = "com.lambdasoup.quickfit.alarm.WORKOUT_ID";
    private static final String EXTRA_SCHEDULE_ID = "com.lambdasoup.quickfit.alarm.SCHEDULE_ID";
    private static final String TAG = DidItReceiver.class.getSimpleName();

    public DidItReceiver() {
    }

    public static Intent getIntentDidIt(Context context, long workoutId, long scheduleId) {
        Intent intent = new Intent(context.getApplicationContext(), DidItReceiver.class);
        intent.putExtra(EXTRA_WORKOUT_ID, workoutId);
        intent.putExtra(EXTRA_SCHEDULE_ID, scheduleId);
        return intent;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive() called with: " + "context = [" + context + "], intent = [" + intent + "]");
        long workoutId = intent.getLongExtra(EXTRA_WORKOUT_ID, -1);
        long scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1);
        context.startService(AlarmService.getIntentCancelNotification(context, scheduleId));
        context.startService(FitActivityService.getIntentInsertSession(context, workoutId));
    }
}
