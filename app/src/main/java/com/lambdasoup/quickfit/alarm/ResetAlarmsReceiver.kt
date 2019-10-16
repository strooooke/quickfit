/*
 * Copyright 2016-2019 Juliane Lehmann <jl@lambdasoup.com>
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
 * limitations under the License.
 *
 */

package com.lambdasoup.quickfit.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.lambdasoup.quickfit.util.WakefulIntents
import timber.log.Timber

class ResetAlarmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!ALLOWED_ACTIONS.contains(intent.action)) {
            return
        }

        Timber.d("About to wakeful start foreground AlarmService")
        WakefulIntents.startWakefulForegroundService(context, AlarmService.getOnBootCompletedIntent(context))
        Timber.d("AlarmService started.")
    }

    companion object {
        private val ALLOWED_ACTIONS = setOf(
                Intent.ACTION_BOOT_COMPLETED,
                "android.intent.action.QUICKBOOT_POWERON",
                "com.htc.intent.action.QUICKBOOT_POWERON",
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
