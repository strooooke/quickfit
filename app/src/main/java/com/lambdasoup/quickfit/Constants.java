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

public class Constants {
    public static final int NOTIFICATION_PLAY_INTERACTION = 0;
    public static final int NOTIFICATION_ALARM = 1;
    public static final int PENDING_INTENT_ALARM_RECEIVER = 0;
    public static final int PENDING_INTENT_WORKOUT_LIST = 1;
    public static final int PENDING_INTENT_DID_IT = 2;
    public static final int PENDING_INTENT_SNOOZE = 3;

    public static final String PREF_NOTIFICATION_RINGTONE = "pref_key_notification_ringtone";
    public static final String PREF_NOTIFICATION_LED_ON = "pref_key_notification_led";
    public static final String PREF_NOTIFICATION_VIBRATION_ON = "pref_key_notification_vibrate";
    public static final String PREF_SNOOZE_DURATION_MINS = "pref_key_snooze_duration_mins";

    private Constants() {
        // do not instantiate
    }
}
