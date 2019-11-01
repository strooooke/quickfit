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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.preference.PreferenceManager
import com.lambdasoup.quickfit.Constants
import com.lambdasoup.quickfit.R
import com.lambdasoup.quickfit.persist.QuickFitContentProvider
import com.lambdasoup.quickfit.util.WakefulIntents

/**
 * Receives broadcasts for PendingIntents scheduled via [android.app.AlarmManager]. Uses the contained info to immediately show the
 * notification, and kicks off the necessary bookkeeping I/O work to (a) persist the notification state, so that it can be restored
 * after boot and (b) register the next alarm.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_NOTIFY) {
            return
        }

        val scheduleId = QuickFitContentProvider.getScheduleIdFromUriOrThrow(intent.data)
        val workoutData = WorkoutNotificationData.fromBundle(intent.getBundleExtra(EXTRA_WORKOUT_NOTIFICATION_DATA_BUNDLE)!!)

        Alarms(context).notify(scheduleId, workoutData)
        WakefulIntents.startWakefulForegroundService(context, AlarmService.getOnNotificationShownIntent(context, scheduleId))
    }

    companion object {
        private const val EXTRA_WORKOUT_NOTIFICATION_DATA_BUNDLE = "com.lambdasoup.quickfit.alarm.EXTRA_NOTIFICATION_DATA"
        private const val ACTION_NOTIFY = "com.lambdasoup.quickfit.alarm.ACTION_NOTIFY"

        private val AUDIO_ATTRS_NOTIFICATION = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).build()

        internal fun getNotifyIntent(context: Context, scheduleId: Long, workoutData: WorkoutNotificationData?): Intent =
                Intent(context, AlarmReceiver::class.java)
                        .setData(QuickFitContentProvider.getUriSchedulesId(scheduleId)) // for intent disambiguation
                        .setAction(ACTION_NOTIFY)
                        .apply {
                            if (workoutData != null) {
                                // see there for why as a Bundle
                                putExtra(EXTRA_WORKOUT_NOTIFICATION_DATA_BUNDLE, workoutData.asBundle())
                            }
                        }

        fun initNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val alarmsChannel = NotificationChannel(
                        Constants.NOTIFICATION_CHANNEL_ID_ALARM,
                        context.getString(R.string.notification_channel_alarm_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                ).apply {

                    val preferences = PreferenceManager.getDefaultSharedPreferences(context)

                    // If this is the first creation of the channel (after system upgrade to Oreo), use the previous explicit user
                    // preferences.
                    val (soundUri, audioAttrs) = when (val ringtoneUriStr = preferences.getString(context.getString(R.string.pref_key_notification_ringtone), null)) {
                        null -> Pair(Settings.System.DEFAULT_NOTIFICATION_URI, AUDIO_ATTRS_NOTIFICATION)
                        "" -> Pair(null, null)
                        else -> Pair(Uri.parse(ringtoneUriStr), AUDIO_ATTRS_NOTIFICATION)
                    }

                    setSound(soundUri, audioAttrs)
                    enableLights(preferences.getBoolean(context.getString(R.string.pref_key_notification_led), true))
                    enableVibration(preferences.getBoolean(context.getString(R.string.pref_key_notification_vibrate), true))
                }

                context.getSystemService(NotificationManager::class.java)!!.createNotificationChannel(alarmsChannel)
            }
        }
    }
}
