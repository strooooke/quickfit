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
package com.lambdasoup.quickfit

import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataType

object Constants {
    const val NOTIFICATION_PLAY_INTERACTION = 0
    const val NOTIFICATION_CHANNEL_ID_PLAY_INTERACTION = "play_interaction"
    const val NOTIFICATION_ALARM = 1
    const val NOTIFICATION_CHANNEL_ID_ALARM = "alarm"
    const val NOTIFICATION_ALARM_BG_IO_WORK = 2
    const val NOTIFICATION_CHANNEL_ID_BG_IO = "bg_io"
    const val PENDING_INTENT_ALARM_RECEIVER = 0
    const val PENDING_INTENT_WORKOUT_LIST = 1
    const val PENDING_INTENT_DID_IT = 2
    const val PENDING_INTENT_SNOOZE = 3
    const val PENDING_INTENT_DISMISS_ALARM = 4
    const val JOB_ID_FIT_ACTIVITY_SERVICE = 100

    val FITNESS_API_OPTIONS = FitnessOptions.builder()
            .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_WRITE)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)
            .build()
}
