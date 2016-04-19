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

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import java.util.HashSet;
import java.util.Set;

/**
 * WakefulBroadcastReceiver that runs
 * 1) on boot
 * 2) on alarm
 * and
 * a) checks the database for what schedules' next occurrence is now in the past
 * b) updates the 'show notification' flag in the database
 * c) applies the current state of the 'show notifications' flag to the actual notification
 */
public class AlarmReceiver extends WakefulBroadcastReceiver {
    private static final Set<String> ALLOWED_ACTIONS = new HashSet<String>() {
        {
            add(Intent.ACTION_BOOT_COMPLETED);
            add("android.intent.action.QUICKBOOT_POWERON");
            add("com.htc.intent.action.QUICKBOOT_POWERON"); // see http://stackoverflow.com/a/14866346/1428514
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (!ALLOWED_ACTIONS.contains(action)) {
            return;
        }
        startWakefulService(context, AlarmService.getIntentOnAlarmReceived(context));
    }
}
