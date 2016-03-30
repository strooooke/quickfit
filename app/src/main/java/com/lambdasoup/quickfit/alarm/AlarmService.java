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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.support.v4.app.NotificationCompat.InboxStyle;
import android.support.v4.content.WakefulBroadcastReceiver;
import android.support.v7.app.NotificationCompat;
import android.text.format.DateUtils;
import android.util.Log;

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
import com.lambdasoup.quickfit.util.IntentServiceCompat;

import static com.lambdasoup.quickfit.Constants.PENDING_INTENT_ALARM_RECEIVER;

/**
 * A wakeful intent service that handles notifications due to alarms
 * and interaction with the AlarmManager.
 */
public class AlarmService extends IntentServiceCompat {
    private static final String TAG = AlarmService.class.getSimpleName();

    private static final String ACTION_SCHEDULE_NEXT_ALARMS = "com.lambdasoup.quickfit.alarm.action.SCHEDULE_NEXT_ALARMS";
    private static final String ACTION_SET_ALARM = "com.lambdasoup.quickfit.alarm.action.SET_ALARM";
    private static final String ACTION_CANCEL_NOTIFICATIONS = "com.lambdasoup.quickfit.alarm.action.CANCEL_NOTIFICATIONS";
    private static final String ACTION_SNOOZE = "com.lambdasoup.quickfit.alarm.action.SNOOZE";
    private static final String EXTRA_SCHEDULE_ID = "com.lambdasoup.quickfit.alarm.extra.SCHEDULE_ID";

    private static final String QUERY_SELECT_MIN_NEXT_ALERT = "SELECT " + ScheduleEntry.COL_NEXT_ALARM_MILLIS + " FROM " + ScheduleEntry.TABLE_NAME +
            " ORDER BY " + ScheduleEntry.COL_NEXT_ALARM_MILLIS + " ASC LIMIT 1";

    private QuickFitDbHelper dbHelper; // TODO: move into content provider

    public AlarmService() {
        super("AlarmService");
        dbHelper = new QuickFitDbHelper(this);
    }

    public static Intent getIntentScheduleNextAlarms(Context context) {
        Intent intent = new Intent(context, AlarmService.class);
        intent.setAction(ACTION_SCHEDULE_NEXT_ALARMS);
        return intent;
    }

    public static Intent getIntentSetAlarm(Context context) {
        Intent intent = new Intent(context, AlarmService.class);
        intent.setAction(ACTION_SET_ALARM);
        return intent;
    }

    public static Intent getIntentCancelNotifications(Context context) {
        Intent intent = new Intent(context, AlarmService.class);
        intent.setAction(ACTION_CANCEL_NOTIFICATIONS);
        return intent;
    }

    public static Intent getIntentCancelNotification(Context context, long scheduleId) {
        Intent intent = new Intent(context, AlarmService.class);
        intent.setAction(ACTION_CANCEL_NOTIFICATIONS);
        intent.putExtra(EXTRA_SCHEDULE_ID, scheduleId);
        return intent;
    }

    private static Intent getIntentSnooze(Context context, long scheduleId) {
        Intent intent = new Intent(context, AlarmService.class);
        intent.setAction(ACTION_SNOOZE);
        intent.putExtra(EXTRA_SCHEDULE_ID, scheduleId);
        return intent;
    }


    @Override
    @WorkerThread
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SET_ALARM.equals(action)) {
                handleSetAlarms(intent);
            } else if (ACTION_SCHEDULE_NEXT_ALARMS.equals(action)) {
                handleScheduleNextAlarms(intent);
            } else if (ACTION_CANCEL_NOTIFICATIONS.equals(action)) {
                if (intent.hasExtra(EXTRA_SCHEDULE_ID)) {
                    handleCancelNotification(intent.getLongExtra(EXTRA_SCHEDULE_ID, -1));
                } else {
                    handleCancelNotifications();
                }
            } else if (ACTION_SNOOZE.equals(action)) {
                long scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1);
                handleSnooze(scheduleId);
            }
        }
    }


    // TODO: proper names for the actions!

    /**
     * Apply database state to the AlarmManager:
     * Set the next Alarm as necessitated by the data in schedule.next_alarm. in the provided background thread,
     * releasing the wake lock associated with the intent if one exists.
     */
    @WorkerThread
    private void handleSetAlarms(Intent intent) {
        try {
            setAlarms();
        } finally {
            WakefulBroadcastReceiver.completeWakefulIntent(intent);
        }
    }

    @WorkerThread
    private void setAlarms() {
        Log.d(TAG, "setAlarms() called with: " + "");
        updateNotification();
        try (SQLiteDatabase db = dbHelper.getReadableDatabase();
             Cursor cursor = db.rawQuery(QUERY_SELECT_MIN_NEXT_ALERT, null)) {
            // if cursor is empty, no schedules exist, no alarms to set
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
            }
        }
        Log.d(TAG, "setAlarms() returned: ");
    }

    /**
     * Apply current system time to database:
     * Collect all events with next_alarm in the past, post notifications for each,
     * update next_alarm time for each.
     * Then perform set alarm action, releasing the wakelock carried by the intent, if any.
     *
     * @param intent Intent to release wakelock on
     */
    @WorkerThread
    private void handleScheduleNextAlarms(Intent intent) {
        Log.d(TAG, "handleScheduleNextAlarms() called with: " + "intent = [" + intent + "]");
        long now = System.currentTimeMillis();
        Schedule[] schedules;

        try {
            try (Cursor pastEvents = getContentResolver().query(
                    QuickFitContentProvider.getUriSchedulesList(),
                    new String[]{ScheduleEntry.COL_ID, ScheduleEntry.COL_DAY_OF_WEEK, ScheduleEntry.COL_HOUR, ScheduleEntry.COL_MINUTE},
                    ScheduleEntry.COL_NEXT_ALARM_MILLIS + "<=?",
                    new String[]{Long.toString(now)},
                    ScheduleEntry.COL_NEXT_ALARM_MILLIS + " ASC")) {
                if (pastEvents == null) {
                    schedules = new Schedule[0];
                } else {
                    schedules = new Schedule[pastEvents.getCount()];
                    int i = 0;
                    while (pastEvents.moveToNext()) {
                        schedules[i] = Schedule.fromRow(pastEvents);
                        i++;
                    }
                }
            }

            // set time for next alarm and flag that notification is needed in single pass
            try (SQLiteDatabase db = dbHelper.getWritableDatabase()) {
                db.beginTransactionNonExclusive();
                try {
                    for (Schedule schedule : schedules) {
                        long nextAlarmMillis = DateTimes.getNextOccurence(now, schedule.dayOfWeek, schedule.hour, schedule.minute);
                        ContentValues contentValues = new ContentValues(2);
                        contentValues.put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, nextAlarmMillis);
                        contentValues.put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_YES);
                        db.update(ScheduleEntry.TABLE_NAME, contentValues, ScheduleEntry.COL_ID + "=?", new String[]{Long.toString(schedule.id)});
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }

        } finally {
            handleSetAlarms(intent);
        }
        Log.d(TAG, "handleScheduleNextAlarms() returned: ");
    }


    @WorkerThread
    private void updateNotification() {
        Log.d(TAG, "updateNotification() called with: " + "");
        try (Cursor toNotify = getContentResolver().query(
                QuickFitContentProvider.getUriWorkoutsList(),
                new String[]{WorkoutEntry.SCHEDULE_ID, WorkoutEntry.WORKOUT_ID, WorkoutEntry.ACTIVITY_TYPE, WorkoutEntry.LABEL, WorkoutEntry.DURATION_MINUTES},
                ScheduleEntry.TABLE_NAME + "." + ScheduleEntry.COL_SHOW_NOTIFICATION + "=?",
                new String[]{Integer.toString(ScheduleEntry.SHOW_NOTIFICATION_YES)},
                null
        )) {
            int count = toNotify == null ? 0 : toNotify.getCount();
            if (count == 0) {
                Log.d(TAG, "updateNotification: 0 notifications");
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancel(Constants.NOTIFICATION_ALARM);
                return;
            }
            NotificationCompat.Builder notification;
            if (count == 1) {
                Log.d(TAG, "updateNotification: single notification");
                toNotify.moveToFirst();
                notification = notifySingleEvent(toNotify);
            } else {
                Log.d(TAG, "updateNotification: multiple notifications");
                toNotify.moveToPosition(-1);
                notification = notifyMultipleEvents(toNotify);
            }

            PendingIntent cancelIntent = PendingIntent.getService(getApplicationContext(), 0, getIntentCancelNotifications(this), PendingIntent.FLAG_UPDATE_CURRENT);

            notification.setAutoCancel(true);
            notification.setPriority(Notification.PRIORITY_HIGH);
            notification.setSmallIcon(R.drawable.ic_stat_quickfit_icon);
            notification.setColor(getColorCompat(R.color.colorPrimary));
            notification.setDefaults(Notification.DEFAULT_ALL);
            notification.setDeleteIntent(cancelIntent);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                notification.setCategory(Notification.CATEGORY_ALARM);
            }

            NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            notificationManager.notify(Constants.NOTIFICATION_ALARM, notification.build());
        }
    }

    /**
     * Replaces the notification for alarms with a notification about a single workout.
     * <p>
     * Relies on the caller to position the cursor on the desired row and to close the cursor.
     *
     * @param cursor Cursor to read the workout data from
     */
    @WorkerThread
    private
    @NonNull
    NotificationCompat.Builder notifySingleEvent(@NonNull Cursor cursor) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);


        FitActivity fitActivity = FitActivity.fromKey(cursor.getString(cursor.getColumnIndex(WorkoutEntry.ACTIVITY_TYPE)), getResources());
        String label = "";
        if (!cursor.isNull(cursor.getColumnIndex(WorkoutEntry.LABEL))) {
            label = cursor.getString(cursor.getColumnIndex(WorkoutEntry.LABEL));
        }
        int durationMinutes = cursor.getInt(cursor.getColumnIndex(WorkoutEntry.DURATION_MINUTES));

        String title = getString(R.string.notification_alarm_title_single, fitActivity.displayName);
        String formattedMinutes = String.format(getResources().getQuantityString(R.plurals.duration_mins_format, durationMinutes), durationMinutes);
        String content = getString(R.string.notification_alarm_content_single, formattedMinutes, label);

        notification.setContentTitle(title);
        notification.setContentText(content);


        long workoutId = cursor.getLong(cursor.getColumnIndex(WorkoutEntry.WORKOUT_ID));

        Intent workoutIntent = new Intent(getApplicationContext(), WorkoutListActivity.class);
        workoutIntent.putExtra(WorkoutListActivity.EXTRA_SHOW_WORKOUT_ID, workoutId);
        PendingIntent activityIntent = TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(workoutIntent)
                .getPendingIntent(Constants.PENDING_INTENT_WORKOUT_LIST, PendingIntent.FLAG_UPDATE_CURRENT);
        notification.setContentIntent(activityIntent);// open workout list, scroll to workout


        long scheduleId = cursor.getLong(cursor.getColumnIndex(WorkoutEntry.SCHEDULE_ID));

        PendingIntent didItIntent = PendingIntent.getBroadcast(
                getApplicationContext(),
                Constants.PENDING_INTENT_DID_IT,
                DidItReceiver.getIntentDidIt(getApplicationContext(), workoutId, scheduleId),
                PendingIntent.FLAG_UPDATE_CURRENT);
        notification.addAction(R.drawable.ic_done_white_24dp, getString(R.string.notification_action_did_it), didItIntent);

        PendingIntent snoozeIntent = PendingIntent.getService(
                getApplicationContext(),
                Constants.PENDING_INTENT_SNOOZE,
                AlarmService.getIntentSnooze(this, scheduleId),
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        notification.addAction(R.drawable.ic_alarm_white_24dp, getString(R.string.notification_action_remind_me_later), snoozeIntent);

        return notification;
    }


    /**
     * Replaces the notification for alarms with a notification about multiple workouts.
     * <p>
     * Relies on the caller to position the cursor before the first row and to close the cursor.
     *
     * @param cursor Cursor to read the workout data from
     */
    @WorkerThread
    private
    @NonNull
    NotificationCompat.Builder notifyMultipleEvents(@NonNull Cursor cursor) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(this);

        Intent workoutIntent = new Intent(getApplicationContext(), WorkoutListActivity.class);
        PendingIntent activityIntent = TaskStackBuilder.create(this)
                .addNextIntentWithParentStack(workoutIntent)
                .getPendingIntent(Constants.PENDING_INTENT_WORKOUT_LIST, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.setContentIntent(activityIntent);


        InboxStyle inboxStyle = new InboxStyle();
        while (cursor.moveToNext()) {
            FitActivity fitActivity = FitActivity.fromKey(cursor.getString(cursor.getColumnIndex(WorkoutEntry.ACTIVITY_TYPE)), getResources());
            String label = "";
            if (!cursor.isNull(cursor.getColumnIndex(WorkoutEntry.LABEL))) {
                label = cursor.getString(cursor.getColumnIndex(WorkoutEntry.LABEL));
            }
            int durationMinutes = cursor.getInt(cursor.getColumnIndex(WorkoutEntry.DURATION_MINUTES));

            String formattedMinutes = String.format(getResources().getQuantityString(R.plurals.duration_mins_format, durationMinutes), durationMinutes);
            String line = getString(R.string.notification_alarm_content_line_multi, fitActivity.displayName, formattedMinutes, label);
            inboxStyle.addLine(line);
        }

        notification.setContentTitle(getString(R.string.notification_alarm_title_multi));
        notification.setContentText(getString(R.string.notification_alarm_content_summary_multi, cursor.getCount()));

        notification.setStyle(inboxStyle);

        return notification;
    }

    @WorkerThread
    private void handleCancelNotification(long scheduleId) {
        Log.d(TAG, "handleCancelNotification() called with: " + "scheduleId = [" + scheduleId + "]");
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_NO);
        getContentResolver().update(
                QuickFitContentProvider.getUriSchedulesId(scheduleId),
                contentValues,
                null,
                null
        );
        Log.d(TAG, "handleCancelNotifications() returned: ");
    }

    @WorkerThread
    private void handleCancelNotifications() {
        Log.d(TAG, "handleCancelNotifications() called with: " + "");
        ContentValues contentValues = new ContentValues(1);
        contentValues.put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_NO);
        getContentResolver().update(
                QuickFitContentProvider.getUriSchedulesList(),
                contentValues,
                null,
                null
        );
        Log.d(TAG, "handleCancelNotifications() returned: ");
    }

    @WorkerThread
    private void handleSnooze(long scheduleId) {
        Log.d(TAG, "handleSnooze() called with: " + "scheduleId = [" + scheduleId + "]");
        long oldNextAlarmMillis;
        try (Cursor scheduleToSnooze =
                     getContentResolver().query(
                             QuickFitContentProvider.getUriSchedulesId(scheduleId),
                             new String[]{ScheduleEntry.COL_ID, ScheduleEntry.COL_NEXT_ALARM_MILLIS},
                             null,
                             null,
                             null)) {
            if (scheduleToSnooze == null || scheduleToSnooze.getCount() != 1) {
                Log.w(TAG, "Cannot snooze schedule with id " + scheduleId + " : missing.");
                return;
            }
            scheduleToSnooze.moveToFirst();
            oldNextAlarmMillis = scheduleToSnooze.getLong(scheduleToSnooze.getColumnIndex(ScheduleEntry.COL_NEXT_ALARM_MILLIS));
        }
        ContentValues values = new ContentValues(2);
        values.put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, oldNextAlarmMillis + Constants.SNOOZE_DELAY_MILLIS);
        values.put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_NO);
        getContentResolver().update(QuickFitContentProvider.getUriSchedulesId(scheduleId), values, null, null);
        setAlarms();
        Log.d(TAG, "handleSnooze() returned: ");
    }


    private static class Schedule {
        public final long id;
        public final DayOfWeek dayOfWeek;
        public final int hour;
        public final int minute;

        private Schedule(long id, DayOfWeek dayOfWeek, int hour, int minute) {
            this.id = id;
            this.dayOfWeek = dayOfWeek;
            this.hour = hour;
            this.minute = minute;
        }

        static Schedule fromRow(Cursor cursor) {
            DayOfWeek dayOfWeek = DayOfWeek.valueOf(cursor.getString(cursor.getColumnIndex(ScheduleEntry.COL_DAY_OF_WEEK)));
            int hour = cursor.getInt(cursor.getColumnIndex(ScheduleEntry.COL_HOUR));
            int minute = cursor.getInt(cursor.getColumnIndex(ScheduleEntry.COL_MINUTE));
            long id = cursor.getLong(cursor.getColumnIndex(ScheduleEntry.COL_ID));

            return new Schedule(id, dayOfWeek, hour, minute);
        }
    }

}
