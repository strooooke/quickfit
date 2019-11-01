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

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.annotation.AnyThread
import androidx.annotation.WorkerThread
import androidx.core.app.AlarmManagerCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.core.database.getLongOrNull
import androidx.preference.PreferenceManager
import com.lambdasoup.quickfit.Constants.*
import com.lambdasoup.quickfit.FitActivityService
import com.lambdasoup.quickfit.R
import com.lambdasoup.quickfit.model.DayOfWeek
import com.lambdasoup.quickfit.model.FitActivity
import com.lambdasoup.quickfit.persist.QuickFitContentProvider
import com.lambdasoup.quickfit.persist.QuickFitContract.ScheduleEntry
import com.lambdasoup.quickfit.persist.QuickFitContract.WorkoutEntry
import com.lambdasoup.quickfit.ui.WorkoutListActivity
import com.lambdasoup.quickfit.util.DateTimes
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Actual logic for workout reminder alarm tasks. Called by different android components ([AlarmService], [AlarmReceiver],
 * [ResetAlarmsReceiver]) according to the needs of the Android framework.
 *
 * Responsible for database interaction as far as the reminder-related columns are concerned and for the related notifications.
 */
class Alarms(private val context: Context) {
    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private val alarmManager by lazy { context.getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    @WorkerThread
    fun onSnoozed(scheduleId: Long) {
        notificationManager.cancel(scheduleId.toString(), NOTIFICATION_ALARM)
        prepareNextAlert(scheduleId, ScheduleEntry.CURRENT_STATE_SNOOZED) { nowPlusSnoozeTime() }
    }

    @WorkerThread
    fun onDidIt(scheduleId: Long, workoutId: Long) {
        notificationManager.cancel(scheduleId.toString(), NOTIFICATION_ALARM)
        FitActivityService.enqueueInsertSession(context, workoutId)
        context.contentResolver.update(
                QuickFitContentProvider.getUriSchedulesId(scheduleId),
                ContentValues(1).apply {
                    put(ScheduleEntry.COL_CURRENT_STATE, ScheduleEntry.CURRENT_STATE_ACKNOWLEDGED)
                },
                null,
                null
        )
    }

    @AnyThread
    fun notify(scheduleId: Long, workoutData: WorkoutNotificationData) {
        val fitActivity = FitActivity.fromKey(workoutData.activityType, context.resources)

        val dismissIntent = getForegroundServicePendingIntentCompat(
                context,
                PENDING_INTENT_DISMISS_ALARM,
                AlarmService.getOnNotificationDismissedIntent(context, scheduleId),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val showWorkoutPendingIntent = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(
                        Intent(context, WorkoutListActivity::class.java)
                                // for intent disambiguation
                                // and for WorkoutListActivity to show correct workout
                                .setData(QuickFitContentProvider.getUriWorkoutsId(workoutData.workoutId))
                                // dismiss intent not fired by autocancel
                                .putExtra(WorkoutListActivity.EXTRA_NOTIFICATIONS_CANCEL_INTENT, dismissIntent)
                )
                .getPendingIntent(PENDING_INTENT_WORKOUT_LIST, PendingIntent.FLAG_UPDATE_CURRENT)

        val didItIntent = getForegroundServicePendingIntentCompat(
                context,
                PENDING_INTENT_DID_IT,
                AlarmService.getDidItIntent(context, workoutData.workoutId, scheduleId),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val snoozeIntent = getForegroundServicePendingIntentCompat(
                context,
                PENDING_INTENT_SNOOZE,
                AlarmService.getSnoozeIntent(context, scheduleId),
                PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_ALARM)
                .setContentTitle(context.getString(R.string.notification_alarm_title_single, fitActivity.displayName))
                .setContentText(context.getString(
                        R.string.notification_alarm_content_single,
                        context.resources.getQuantityString(
                                R.plurals.duration_mins_format,
                                workoutData.durationMinutes,
                                workoutData.durationMinutes
                        ),
                        workoutData.label.orEmpty()
                ))
                .setContentIntent(showWorkoutPendingIntent)
                .addAction(
                        R.drawable.ic_done_white_24dp,
                        context.getString(R.string.notification_action_did_it),
                        didItIntent
                )
                .addAction(
                        R.drawable.ic_alarm_white_24dp,
                        context.getString(R.string.notification_action_remind_me_later),
                        snoozeIntent
                )
                .setDeleteIntent(dismissIntent)
                // Only effect is on content-click. cancelIntent is not fired, despite the name. Actions need to cancel the notification
                // themselves.
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSmallIcon(R.drawable.ic_stat_quickfit_icon)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setCategory(NotificationCompat.CATEGORY_ALARM)

        // Starting with O, those properties are set on the notification channel
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            val ringtoneUriStr = preferences.getString(context.getString(R.string.pref_key_notification_ringtone), null)
            notificationBuilder.setSound(
                    when {
                        ringtoneUriStr == null -> RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_NOTIFICATION)
                        ringtoneUriStr.isNotEmpty() -> Uri.parse(ringtoneUriStr)
                        else -> null
                    }
            )

            val ledOn = preferences.getBoolean(context.getString(R.string.pref_key_notification_led), true)
            val vibrationOn = preferences.getBoolean(context.getString(R.string.pref_key_notification_vibrate), true)
            notificationBuilder.setDefaults(
                    (if (ledOn) Notification.DEFAULT_LIGHTS else 0) or if (vibrationOn) Notification.DEFAULT_VIBRATE else 0
            )
        }

        // Deliberately not using groups, for the time being, because we'd have to create a summary notification, so we would need to
        // know about all currently displaying workout schedule notifications.
        // On API levels >= 24, we could do this by querying the NotificationManager for all those, and construct an inbox-style
        // summary from the snippets. But on exactly those API levels, the system already does this for us, as long as we do *not*
        // specify a group.
        // On API levels < 24, we cannot query the NotificationManager for this information. We could query our own database - and on those
        // API levels, we can reliably do I/O work on an AlarmManager broadcast, in principle - but this would mean maintaining a separate
        // implementation for those API levels. Might happen at a later date - or we might raise minSdk to 24 instead.
        // While it is not nice of us to potentially generate loads of notifications for the same id on those API levels (and was advised
        // against in the documentation earlier), we can live with this for the time being.

        notificationManager.notify(scheduleId.toString(), NOTIFICATION_ALARM, notificationBuilder.build())
    }

    @WorkerThread
    fun onScheduleChanged(scheduleId: Long) {
        Timber.d("onScheduleChanged: $scheduleId")

        notificationManager.cancel(scheduleId.toString(), NOTIFICATION_ALARM)

        prepareNextAlert(scheduleId, ScheduleEntry.CURRENT_STATE_ACKNOWLEDGED, this::nextOccurence)
    }

    @WorkerThread
    fun onScheduleDeleted(scheduleId: Long) {
        Timber.d("onScheduleDeleted: $scheduleId")

        notificationManager.cancel(scheduleId.toString(), NOTIFICATION_ALARM)
        alarmManager.cancel(buildAlarmReceiverPendingIntent(scheduleId, null))
    }

    @WorkerThread
    fun onNotificationShown(scheduleId: Long) {
        prepareNextAlert(scheduleId, ScheduleEntry.CURRENT_STATE_DISPLAYING, this::nextOccurence)
    }

    @WorkerThread
    fun onNotificationDismissed(scheduleId: Long) {
        context.contentResolver.update(
                QuickFitContentProvider.getUriSchedulesId(scheduleId),
                ContentValues(1).apply {
                    put(ScheduleEntry.COL_CURRENT_STATE, ScheduleEntry.CURRENT_STATE_ACKNOWLEDGED)
                },
                null,
                null
        )
    }

    @WorkerThread
    fun onBootCompleted() {
        val now = System.currentTimeMillis()

        context.contentResolver.query(
                QuickFitContentProvider.getUriWorkoutsList(),
                arrayOf(
                        WorkoutEntry.SCHEDULE_ID,
                        WorkoutEntry.WORKOUT_ID,
                        WorkoutEntry.ACTIVITY_TYPE,
                        WorkoutEntry.LABEL,
                        WorkoutEntry.DURATION_MINUTES,
                        WorkoutEntry.DAY_OF_WEEK,
                        WorkoutEntry.HOUR,
                        WorkoutEntry.MINUTE,
                        WorkoutEntry.NEXT_ALARM_MILLIS,
                        WorkoutEntry.CURRENT_STATE
                ),
                "${ScheduleEntry.TABLE_NAME}.${ScheduleEntry.COL_ID} IS NOT NULL",
                null,
                null
        ).use { cursor ->
            if (cursor == null) {
                return@onBootCompleted
            }

            while (cursor.moveToNext()) {
                val scheduleId = cursor.getLong(cursor.getColumnIndexOrThrow(WorkoutEntry.SCHEDULE_ID))
                val nextAlarmMillis = cursor.getLongOrNull(cursor.getColumnIndexOrThrow(WorkoutEntry.NEXT_ALARM_MILLIS))

                val currentState = cursor.getString(cursor.getColumnIndexOrThrow(WorkoutEntry.CURRENT_STATE))
                val workoutNotificationData = WorkoutNotificationData.fromRow(cursor)
                if (nextAlarmMillis == null) {
                    // system had no chance yet to compute next alert time, so let's do that now.
                    prepareNextAlert(scheduleId, ScheduleEntry.CURRENT_STATE_ACKNOWLEDGED, this::nextOccurence)
                } else if (nextAlarmMillis <= now || currentState == ScheduleEntry.CURRENT_STATE_DISPLAYING) {
                    notify(scheduleId, workoutNotificationData)
                    prepareNextAlert(scheduleId, ScheduleEntry.CURRENT_STATE_DISPLAYING, this::nextOccurence)
                } else {
                    enqueueWithAlarmManager(scheduleId, nextAlarmMillis, workoutNotificationData)
                }
            }
        }
    }

    private fun nowPlusSnoozeTime(): Long {
        val durationMinutes = PreferenceManager.getDefaultSharedPreferences(context).getString(
                context.getString(R.string.pref_key_snooze_duration_mins),
                "60"
        )!!.toLong() // Why not an Int pref? Because of the string array resource. There are no Int array resources.

        return System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(durationMinutes)
    }

    private fun nextOccurence(schedule: Schedule) =
            DateTimes.getNextOccurrence(System.currentTimeMillis(), schedule.dayOfWeek, schedule.hour, schedule.minute)

    @WorkerThread
    private fun prepareNextAlert(scheduleId: Long, newCurrentState: String, getNextAlarmMillis: (Schedule) -> Long) {
        Timber.d("prepareNextAlert: $scheduleId")
        val (schedule, workoutData) = context.contentResolver.query(
                QuickFitContentProvider.getUriWorkoutsList(),
                arrayOf(
                        WorkoutEntry.SCHEDULE_ID,
                        WorkoutEntry.WORKOUT_ID,
                        WorkoutEntry.ACTIVITY_TYPE,
                        WorkoutEntry.LABEL,
                        WorkoutEntry.DURATION_MINUTES,
                        WorkoutEntry.DAY_OF_WEEK,
                        WorkoutEntry.HOUR,
                        WorkoutEntry.MINUTE
                ),
                "${ScheduleEntry.TABLE_NAME}.${ScheduleEntry.COL_ID}=?",
                arrayOf(scheduleId.toString()),
                null
        ).use { cursor ->
            if (cursor?.moveToNext() == true) {
                Pair(
                        Schedule.fromRow(cursor),
                        WorkoutNotificationData.fromRow(cursor)
                )
            } else {
                Timber.w("Schedule $scheduleId does not exist, aborting prepareNextAlert")
                return@prepareNextAlert
            }
        }

        val nextAlarmMillis = getNextAlarmMillis(schedule)

        enqueueWithAlarmManager(scheduleId, nextAlarmMillis, workoutData)

        context.contentResolver.update(
                QuickFitContentProvider.getUriSchedulesId(scheduleId),
                ContentValues(2).apply {
                    put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, nextAlarmMillis)
                    put(ScheduleEntry.COL_CURRENT_STATE, newCurrentState)
                },
                null,
                null
        )
    }

    private fun enqueueWithAlarmManager(scheduleId: Long, nextAlarmMillis: Long, workoutData: WorkoutNotificationData) {
        val alarmReceiverPendingIntent = buildAlarmReceiverPendingIntent(scheduleId, workoutData)

        AlarmManagerCompat.setExactAndAllowWhileIdle(
                alarmManager,
                AlarmManager.RTC_WAKEUP,
                nextAlarmMillis,
                alarmReceiverPendingIntent
        )
    }

    private fun buildAlarmReceiverPendingIntent(scheduleId: Long, workoutData: WorkoutNotificationData?) =
            PendingIntent.getBroadcast(
                    context,
                    PENDING_INTENT_ALARM_RECEIVER, // disambiguation is via Intent data
                    AlarmReceiver.getNotifyIntent(context, scheduleId, workoutData),
                    PendingIntent.FLAG_UPDATE_CURRENT
            )
}

@Suppress("SameParameterValue")
private fun getForegroundServicePendingIntentCompat(context: Context, requestCode: Int, intent: Intent, flags: Int) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }

private data class Schedule constructor(
        val id: Long,
        val dayOfWeek: DayOfWeek,
        val hour: Int,
        val minute: Int
) {
    companion object {
        internal fun fromRow(cursor: Cursor): Schedule {
            val dayOfWeek = DayOfWeek.valueOf(cursor.getString(cursor.getColumnIndexOrThrow(WorkoutEntry.DAY_OF_WEEK)))
            val hour = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutEntry.HOUR))
            val minute = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutEntry.MINUTE))
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(WorkoutEntry.SCHEDULE_ID))

            return Schedule(id, dayOfWeek, hour, minute)
        }
    }
}

data class WorkoutNotificationData constructor(
        val workoutId: Long,
        val activityType: String,
        val label: String?,
        val durationMinutes: Int
) {

    /*
     *  Passing custom Parcelables through other processes (e.g. as a PendingIntent extra) fails in general - see
     * https://commonsware.com/blog/2016/07/22/be-careful-where-you-use-custom-parcelables.html
     *
     * In particular, when used as an extra in a PendingIntent for AlarmManager, this never works.
     *
     * Tried using the workaround suggested in
     * https://stackoverflow.com/questions/18000093/how-to-marshall-and-unmarshall-a-parcelable-to-a-byte-array-with-help-of-parcel/18000094#18000094
     * and gave that avenue up again, because I don't see a way of making properly generic `Bundle.putSafeExtra` and
     * `<T : Parcelable> Bundle.getSafeParcelableExtra` extension methods - the latter fails, due to the fantastic
     * design of the CREATOR object which is not part of the Parcelable interface (and cannot be, due to the Java type system).
     * So there's no way of obtaining it from the (reified) type parameter, so we'd have to pass it in at each use.
     *
     * And then we notice that when using @Parcelize for auto-generated Parcelable implementation, this CREATOR object
     * does not exist (at least not enough for the IDE to see it at compile time).
     *
     * So if we have to hand-generate the serialization anyway, going through Bundle is the more readable approach.
     */
    fun asBundle(): Bundle = Bundle(4).apply {
        putLong(KEY_WORKOUT_ID, workoutId)
        putString(KEY_ACTIVITY_TYPE, activityType)
        putString(KEY_LABEL, label)
        putInt(KEY_DURATION_MINUTES, durationMinutes)
    }

    companion object {
        private const val KEY_WORKOUT_ID = "com.lambdasoup.quickfit.alarm.WorkoutNotificationData.KEY_WORKOUT_ID"
        private const val KEY_ACTIVITY_TYPE = "com.lambdasoup.quickfit.alarm.WorkoutNotificationData.KEY_ACTIVITY_TYPE"
        private const val KEY_LABEL = "com.lambdasoup.quickfit.alarm.WorkoutNotificationData.KEY_LABEL"
        private const val KEY_DURATION_MINUTES = "com.lambdasoup.quickfit.alarm.WorkoutNotificationData.KEY_DURATION_MINUTES"

        internal fun fromBundle(bundle: Bundle): WorkoutNotificationData =
                with(bundle) {
                    WorkoutNotificationData(
                            getLong(KEY_WORKOUT_ID),
                            getString(KEY_ACTIVITY_TYPE)!!,
                            getString(KEY_LABEL),
                            getInt(KEY_DURATION_MINUTES)
                    )
                }

        internal fun fromRow(cursor: Cursor): WorkoutNotificationData {
            val workoutId = cursor.getLong(cursor.getColumnIndexOrThrow(WorkoutEntry.WORKOUT_ID))
            val activityType = cursor.getString(cursor.getColumnIndexOrThrow(WorkoutEntry.ACTIVITY_TYPE))
            val label = cursor.getColumnIndexOrThrow(WorkoutEntry.LABEL).let {
                if (!cursor.isNull(it)) cursor.getString(it) else ""
            }
            val durationMinutes = cursor.getInt(cursor.getColumnIndexOrThrow(WorkoutEntry.DURATION_MINUTES))

            return WorkoutNotificationData(workoutId, activityType, label, durationMinutes)
        }

    }
}
