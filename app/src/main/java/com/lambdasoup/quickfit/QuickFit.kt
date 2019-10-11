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

package com.lambdasoup.quickfit

import android.app.Application

import com.lambdasoup.quickfit.alarm.AlarmService
import com.lambdasoup.quickfit.persist.FitApiFailureResolution

import timber.log.Timber


class QuickFit : Application() {

    override fun onCreate() {
        super.onCreate()

        // init logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        AlarmService.initNotificationChannels(this)
        FitApiFailureResolution.initNotificationChannels(this)

        // ensure that our we sync periodically, to catch any problems with
        // missing manual sync requests eventually
        FitActivityService.enqueueSetPeriodicSync(applicationContext)
    }
}
