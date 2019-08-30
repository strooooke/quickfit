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

import android.app.Application;
import android.os.Build;

import com.lambdasoup.quickfit.alarm.AlarmService;
import com.lambdasoup.quickfit.persist.FitApiFailureResolutionService;

import timber.log.Timber;


public class QuickFit extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // ensure that our sync adapter is set to sync periodically, to catch any problems with
        // missing manual sync requests eventually
        FitActivityService.enqueueSetPeriodicSync(getApplicationContext());

        // init logging
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }

        AlarmService.initNotificationChannels(this);
        FitApiFailureResolutionService.initNotificationChannels(this);
    }
}
