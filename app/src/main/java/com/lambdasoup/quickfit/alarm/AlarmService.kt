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

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.lambdasoup.quickfit.Constants
import com.lambdasoup.quickfit.Constants.PENDING_INTENT_WORKOUT_LIST
import com.lambdasoup.quickfit.R
import com.lambdasoup.quickfit.persist.QuickFitContentProvider
import com.lambdasoup.quickfit.ui.WorkoutListActivity
import com.lambdasoup.quickfit.util.WakefulIntents
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * ForegroundService, handling alarm-related background I/O work. Could also have been solved as a [androidx.core.app.JobIntentService],
 * or as a collection of jobs for [androidx.work.WorkManager], but (a) reading the documentation says that JobScheduler is for _deferrable_
 * I/O work, which this is not - and also no promises are given as to the timeliness of the execution of JobScheduler jobs that are free
 * of restrictions. Although it seems to be fine in practice (on non-background crippling devices). Also (b) - just for trying it out.
 * Currently, this project serves as an exhibition of different ways of performing background work.
 */
class AlarmService : Service() {
    private val alarms by lazy { Alarms(this.applicationContext) }
    private val runWakeLock by lazy {
        (getSystemService(Context.POWER_SERVICE) as PowerManager)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, AlarmService::class.java.canonicalName)
    }

    private lateinit var executor: ExecutorService

    override fun onCreate() {
        super.onCreate()
        executor = Executors.newFixedThreadPool(2)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        @Suppress("ReplaceGuardClauseWithFunctionCall") // IDE fails then, demands wrong type. New type inference at fault?
        if (intent == null) throw IllegalArgumentException("Should never receive null intents because of START_REDELIVER_INTENT")

        startForeground(Constants.NOTIFICATION_ALARM_BG_IO_WORK, buildForegroundNotification())

        runWakeLock.acquire(TimeUnit.SECONDS.toMillis(30))
        WakefulIntents.completeWakefulIntent(intent)

        Timber.d("Done with wakelock handover and foreground start, about to enqueue intent processing. intent=$intent")

        executor.submit {
            try {
                fun getScheduleId() = QuickFitContentProvider.getScheduleIdFromUriOrThrow(intent.data!!)

                when (intent.action) {
                    ACTION_SNOOZE -> alarms.onSnoozed(getScheduleId())
                    ACTION_DIDIT -> {
                        val workoutId = QuickFitContentProvider.getWorkoutIdFromUriOrThrow(intent.data!!)
                        alarms.onDidIt(getScheduleId(), workoutId)
                    }
                    ACTION_ON_NOTIFICATION_SHOWN -> alarms.onNotificationShown(getScheduleId())
                    ACTION_ON_NOTIFICATION_DISMISSED -> alarms.onNotificationDismissed(getScheduleId())
                    ACTION_ON_SCHEDULE_CHANGED -> alarms.onScheduleChanged(getScheduleId())
                    ACTION_ON_SCHEDULE_DELETED -> alarms.onScheduleDeleted(getScheduleId())
                    ACTION_TIME_DISCONTINUITY -> alarms.resetAlarms()
                    else -> throw IllegalArgumentException("Unknown action: ${intent.action}")
                }
            } catch (e: Throwable) {
                Timber.e(e, "Failed to execute $intent")
            } finally {
                runWakeLock.release()
                stopSelf(startId)
            }
        }

        return START_REDELIVER_INTENT
    }

    private fun buildForegroundNotification(): Notification =
            NotificationCompat.Builder(this, Constants.NOTIFICATION_CHANNEL_ID_BG_IO)
                    .setContentTitle(getString(R.string.notification_alarm_bg_io_title))
                    .setContentText(getString(R.string.notification_alarm_bg_io_content))
                    .setContentIntent(
                            PendingIntent.getActivity(
                                    this,
                                    PENDING_INTENT_WORKOUT_LIST,
                                    Intent(this, WorkoutListActivity::class.java),
                                    PendingIntent.FLAG_IMMUTABLE xor PendingIntent.FLAG_UPDATE_CURRENT
                            )
                    )
                    .build()

    companion object {
        private const val ACTION_SNOOZE = "com.lambdasoup.quickfit.alarm.ACTION_SNOOZE"
        private const val ACTION_DIDIT = "com.lambdasoup.quickfit.alarm.ACTION_DIDIT"
        private const val ACTION_ON_NOTIFICATION_SHOWN = "com.lambdasoup.quickfit.alarm.ACTION_ON_NOTIFICATION_SHOWN"
        private const val ACTION_ON_NOTIFICATION_DISMISSED = "com.lambdasoup.quickfit.alarm.ACTION_ON_NOTIFICATION_DISMISSED"
        private const val ACTION_ON_SCHEDULE_CHANGED = "com.lambdasoup.quickfit.alarm.ACTION_ON_SCHEDULE_CHANGED"
        private const val ACTION_ON_SCHEDULE_DELETED = "com.lambdasoup.quickfit.alarm.ACTION_ON_SCHEDULE_DELETED"
        private const val ACTION_TIME_DISCONTINUITY = "com.lambdasoup.quickfit.alarm.ACTION_TIME_DISCONTINUITY"

        fun getSnoozeIntent(context: Context, scheduleId: Long) =
                Intent(context, AlarmService::class.java)
                        .setData(QuickFitContentProvider.getUriSchedulesId(scheduleId))
                        .setAction(ACTION_SNOOZE)

        fun getDidItIntent(context: Context, workoutId: Long, scheduleId: Long) =
                Intent(context, AlarmService::class.java)
                        .setData(QuickFitContentProvider.getUriWorkoutsIdSchedulesId(workoutId, scheduleId))
                        .setAction(ACTION_DIDIT)

        fun getOnNotificationShownIntent(context: Context, scheduleId: Long) =
                Intent(context, AlarmService::class.java)
                        .setData(QuickFitContentProvider.getUriSchedulesId(scheduleId))
                        .setAction(ACTION_ON_NOTIFICATION_SHOWN)

        fun getOnNotificationDismissedIntent(context: Context, scheduleId: Long) =
                Intent(context, AlarmService::class.java)
                        .setData(QuickFitContentProvider.getUriSchedulesId(scheduleId))
                        .setAction(ACTION_ON_NOTIFICATION_DISMISSED)

        fun getOnScheduleChangedIntent(context: Context, scheduleId: Long) =
                Intent(context, AlarmService::class.java)
                        .setData(QuickFitContentProvider.getUriSchedulesId(scheduleId))
                        .setAction(ACTION_ON_SCHEDULE_CHANGED)

        fun getOnScheduleDeletedIntent(context: Context, scheduleId: Long) =
                Intent(context, AlarmService::class.java)
                        .setData(QuickFitContentProvider.getUriSchedulesId(scheduleId))
                        .setAction(ACTION_ON_SCHEDULE_DELETED)

        fun getOnBootCompletedIntent(context: Context) =
                Intent(context, AlarmService::class.java)
                        .setAction(ACTION_TIME_DISCONTINUITY)

        fun initNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val bgIoChannel = NotificationChannel(
                        Constants.NOTIFICATION_CHANNEL_ID_BG_IO,
                        context.getString(R.string.notification_channel_bg_io_name),
                        NotificationManager.IMPORTANCE_LOW
                )

                context.getSystemService(NotificationManager::class.java)!!.createNotificationChannel(bgIoChannel)
            }
        }
    }
}
