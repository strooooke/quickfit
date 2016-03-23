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

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.NotificationCompat;
import android.text.format.DateUtils;

import com.lambdasoup.quickfit.Constants;
import com.lambdasoup.quickfit.R;
import com.lambdasoup.quickfit.model.DayOfWeek;
import com.lambdasoup.quickfit.model.FitActivity;
import com.lambdasoup.quickfit.persist.QuickFitContentProvider;
import com.lambdasoup.quickfit.persist.QuickFitContract.ScheduleEntry;
import com.lambdasoup.quickfit.persist.QuickFitContract.WorkoutEntry;
import com.lambdasoup.quickfit.persist.QuickFitDbHelper;
import com.lambdasoup.quickfit.ui.WorkoutListActivity;
import com.lambdasoup.quickfit.util.DateTimes;

import static com.lambdasoup.quickfit.Constants.PENDING_INTENT_ALARM_RECEIVER;

/**
 * A wakeful intent service that handles notifications due to alarms
 * and interaction with the AlarmManager.
 */
public class AlarmService extends IntentService {

    public static final String ACTION_NOTIFY_ALARMS = "com.lambdasoup.quickfit.alarm.action.NOTIFY_ALARMS";
    public static final String ACTION_SET_ALARM = "com.lambdasoup.quickfit.alarm.action.SET_ALARM";

    private static final int REQUEST_CODE_SHOW_WORKOUT_LIST = 0;

    private static final String QUERY_SELECT_MIN_NEXT_ALERT = "SELECT " + ScheduleEntry.COL_NEXT_ALARM_MILLIS + " FROM " + ScheduleEntry.TABLE_NAME +
            " ORDER BY " + ScheduleEntry.COL_NEXT_ALARM_MILLIS + " ASC LIMIT 1";
    private QuickFitDbHelper dbHelper;

    public AlarmService() {
        super("AlarmService");
        dbHelper = new QuickFitDbHelper(this);
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SET_ALARM.equals(action)) {
                handleActionSetAlarms(intent);
            } else if (ACTION_NOTIFY_ALARMS.equals(action)) {
                handleActionNotifyAlarms(intent);
            }
        }
    }

    /**
     * Apply database state to the AlarmManager:
     * Set the next Alarm as necessitated by the data in schedule.next_alarm. in the provided background thread,
     * releasing the wake lock associated with the intent if one exists.
     */
    private void handleActionSetAlarms(Intent intent) {
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(QUERY_SELECT_MIN_NEXT_ALERT, null)) {
            if (cursor.moveToFirst()) {
                long nextAlarmMillis = cursor.getLong(cursor.getColumnIndexOrThrow(ScheduleEntry.COL_NEXT_ALARM_MILLIS));

                AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
                PendingIntent alarmReceiverIntent = PendingIntent.getBroadcast(this, PENDING_INTENT_ALARM_RECEIVER, new Intent(getApplicationContext(), AlarmReceiver.class), PendingIntent.FLAG_UPDATE_CURRENT);
                alarmManager.cancel(alarmReceiverIntent);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    // TODO: test if setAndAllowWhileIdle is sufficient
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmMillis, alarmReceiverIntent);
                } else {
                    alarmManager.setWindow(AlarmManager.RTC_WAKEUP, nextAlarmMillis, DateUtils.MINUTE_IN_MILLIS, alarmReceiverIntent);
                }
            } else {
                // cursor empty, no schedules exist, no alarms to set
            }
        } finally {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    /**
     * Apply current system time to database:
     * Collect all events with next_alarm in the past, post notifications for each,
     * update next_alarm time for each.
     * Then perform set alarm action, releasing the wakelock carried by the intent, if any.
     *
     * @param intent
     */
    private void handleActionNotifyAlarms(Intent intent) {
        long now = System.currentTimeMillis();
        Schedule[] schedules;

        try {
            try (SQLiteDatabase db = dbHelper.getReadableDatabase();
                 Cursor pastEvents = db.query(
                         ScheduleEntry.TABLE_NAME,
                         ScheduleEntry.COLUMNS,
                         ScheduleEntry.COL_NEXT_ALARM_MILLIS + "<=?",
                         new String[]{Long.toString(now)},
                         null, null, ScheduleEntry.COL_NEXT_ALARM_MILLIS + " ASC")) {

                schedules = new Schedule[pastEvents.getCount()];
                int i = 0;
                while (pastEvents.moveToNext()) {
                    schedules[i] = Schedule.fromRow(pastEvents);
                    i++;
                }
            }

            if (schedules.length > 1) {
                notifyMultipleEvents(schedules);
            } else if (schedules.length == 1) {
                notifySingleEvent(schedules[0]);
            }


            // TODO: dispatch notification, query workout for schedule...
            try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
                db.beginTransactionNonExclusive();
                try {
                    for (Schedule schedule : schedules) {
                        long nextAlarmMillis = DateTimes.getNextOccurence(now, schedule.dayOfWeek, schedule.hour, schedule.minute);
                        ContentValues contentValues = new ContentValues(1);
                        contentValues.put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, nextAlarmMillis);
                        db.update(ScheduleEntry.TABLE_NAME, contentValues, ScheduleEntry.COL_ID + "=?", new String[]{Long.toString(schedule.id)});
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
        } finally {
            handleActionSetAlarms(intent);
        }
    }

    // TODO: how about existing notifications?
    private void notifySingleEvent(Schedule schedule) {
        FitActivity fitActivity;
        String label = null;
        int durationMinutes;

        try (Cursor workoutCursor = getContentResolver().query(
                QuickFitContentProvider.getUriWorkoutsId(schedule.workoutId),
                new String[]{WorkoutEntry.ACTIVITY_TYPE, WorkoutEntry.LABEL, WorkoutEntry.DURATION_MINUTES},
                null,
                null,
                null)) {
            if (workoutCursor == null || !workoutCursor.moveToFirst()) {
                // workout is missing; skip this
                return;
            }
            fitActivity = FitActivity.fromKey(workoutCursor.getString(workoutCursor.getColumnIndex(WorkoutEntry.ACTIVITY_TYPE)), getResources());
            if (!workoutCursor.isNull(workoutCursor.getColumnIndex(WorkoutEntry.LABEL))) {
                label = workoutCursor.getString(workoutCursor.getColumnIndex(WorkoutEntry.LABEL));
            }
            durationMinutes = workoutCursor.getInt(workoutCursor.getColumnIndex(WorkoutEntry.DURATION_MINUTES));
        }

        Intent workoutIntent = new Intent(getApplicationContext(), WorkoutListActivity.class);
        workoutIntent.putExtra(WorkoutListActivity.EXTRA_SHOW_WORKOUT_ID, schedule.workoutId);
        PendingIntent activityIntent = TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(workoutIntent)
                .getPendingIntent(Constants.PENDING_INTENT_WORKOUT_LIST, PendingIntent.FLAG_UPDATE_CURRENT);

        String title = getString(R.string.notification_alarm_title, fitActivity.displayName);
        String content = String.format(getResources().getQuantityString(R.plurals.duration_mins_format, durationMinutes), durationMinutes);
        if (label != null) {
            content += ("\n" + label); // TODO: no newline yet :(
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);
        notification.setAutoCancel(true);
        notification.setPriority(Notification.PRIORITY_HIGH);
        notification.setContentTitle(title);
        notification.setContentText(content);
        notification.setSmallIcon(R.mipmap.ic_launcher); // TODO: notification icon
        notification.setContentIntent(activityIntent);// open workout list, scroll to workout

        //notification.addAction(todo, "Did it!", intent); // intent for service?
        //notification.addAction(todo, "Remind me later", intent); // intent for service?

        // TODO: no need for compat? in any case, needs sound, style, vibrate, bridge etc...

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notification.setCategory(Notification.CATEGORY_ALARM);
        }

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(Constants.NOTIFICATION_ALARM, notification.build());
    }

    private void notifyMultipleEvents(Schedule[] schedules) {
        // TODO: implement
    }


    private static class Schedule {
        public final long id;
        public final long workoutId;
        public final DayOfWeek dayOfWeek;
        public final int hour;
        public final int minute;

        private Schedule(long id, long workoutId, DayOfWeek dayOfWeek, int hour, int minute) {
            this.id = id;
            this.workoutId = workoutId;
            this.dayOfWeek = dayOfWeek;
            this.hour = hour;
            this.minute = minute;
        }

        static Schedule fromRow(Cursor cursor) {
            DayOfWeek dayOfWeek = DayOfWeek.valueOf(cursor.getString(cursor.getColumnIndex(ScheduleEntry.COL_DAY_OF_WEEK)));
            int hour = cursor.getInt(cursor.getColumnIndex(ScheduleEntry.COL_HOUR));
            int minute = cursor.getInt(cursor.getColumnIndex(ScheduleEntry.COL_MINUTE));
            long id = cursor.getLong(cursor.getColumnIndex(ScheduleEntry.COL_ID));
            long workoutId = cursor.getLong(cursor.getColumnIndex(ScheduleEntry.COL_WORKOUT_ID));

            return new Schedule(id, workoutId, dayOfWeek, hour, minute);
        }
    }

}
