package com.lambdasoup.quickfit;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import android.widget.Toast;

import com.lambdasoup.quickfit.persist.QuickFitContentProvider;
import com.lambdasoup.quickfit.persist.QuickFitContract;

import java.util.concurrent.TimeUnit;

import androidx.core.app.JobIntentService;
import timber.log.Timber;

import static com.lambdasoup.quickfit.Constants.JOB_ID_FIT_ACTIVITY_SERVICE;


public class FitActivityService extends JobIntentService {
    private static final String ACTION_INSERT_SESSION = "com.lambdasoup.quickfit.action.INSERT_SESSION";
    private static final String ACTION_SESSION_SYNC = "com.lambdasoup.quickfit.action.SESSION_SYNC";
    private static final String ACTION_SET_PERIODIC_SYNC = "com.lambdasoup.quickfit.action.SET_PERIODIC_SYNC";

    private static final String EXTRA_WORKOUT_ID = "com.lambdasoup.quickfit.alarm.WORKOUT_ID";

    private static final String ACCOUNT_TYPE = "com.lambdasoup.quickfit";
    private final Account account = new Account("QuickFit", ACCOUNT_TYPE);

    public static void enqueueInsertSession(Context context, long workoutId) {
        Intent intent = new Intent(ACTION_INSERT_SESSION).putExtra(EXTRA_WORKOUT_ID, workoutId);
        JobIntentService.enqueueWork(context, FitActivityService.class, JOB_ID_FIT_ACTIVITY_SERVICE, intent);
    }

    public static void enqueueSyncSession(Context context) {
        Intent intent = new Intent(ACTION_SESSION_SYNC);
        JobIntentService.enqueueWork(context, FitActivityService.class, JOB_ID_FIT_ACTIVITY_SERVICE, intent);
    }

    public static void enqueueSetPeriodicSync(Context context) {
        Intent intent = new Intent(ACTION_SET_PERIODIC_SYNC);
        JobIntentService.enqueueWork(context, FitActivityService.class, JOB_ID_FIT_ACTIVITY_SERVICE, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String action = intent.getAction();
        if (ACTION_INSERT_SESSION.equals(action)) {
            long workoutId = intent.getLongExtra(EXTRA_WORKOUT_ID, -1);
            handleInsertSession(workoutId);
        } else if (ACTION_SESSION_SYNC.equals(action)) {
            requestSync();
        } else if (ACTION_SET_PERIODIC_SYNC.equals(action)) {
            setPeriodicSync();
        } else {
            throw new IllegalArgumentException("Action " + action + " not supported.");
        }
    }

    @WorkerThread
    private void handleInsertSession(long workoutId) {
        Cursor cursor = getContentResolver().query(
                QuickFitContentProvider.getUriWorkoutsId(workoutId),
                QuickFitContract.WorkoutEntry.COLUMNS_WORKOUT_ONLY,
                null,
                null,
                null);
        if (cursor == null || !cursor.moveToFirst()) {
            Timber.w("Workout missing with id: %d", workoutId);
            return;
        }

        int durationInMinutes = cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.DURATION_MINUTES));
        long endTime = System.currentTimeMillis();
        long startTime = endTime - TimeUnit.MINUTES.toMillis(durationInMinutes);

        ContentValues values = new ContentValues();
        values.put(QuickFitContract.SessionEntry.ACTIVITY_TYPE, cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.ACTIVITY_TYPE)));
        values.put(QuickFitContract.SessionEntry.START_TIME, startTime);
        values.put(QuickFitContract.SessionEntry.END_TIME, endTime);
        values.put(QuickFitContract.SessionEntry.STATUS, QuickFitContract.SessionEntry.SessionStatus.NEW.name());
        values.put(QuickFitContract.SessionEntry.NAME, cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.LABEL)));
        values.put(QuickFitContract.SessionEntry.CALORIES, cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.CALORIES)));

        cursor.close();

        getContentResolver().insert(QuickFitContentProvider.getUriSessionsList(), values);
        requestSync();
        showToast(R.string.success_session_insert);
    }

    @WorkerThread
    private void showToast(@SuppressWarnings("SameParameterValue") @StringRes int resId) {
        new Handler(getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_SHORT).show());
    }


    @WorkerThread
    private void requestSync() {
        ensureAccountExistsAndIsSyncable();
        Bundle syncOptions = new Bundle();
        syncOptions.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(account, QuickFitContentProvider.AUTHORITY, syncOptions);
    }

    @WorkerThread
    private void setPeriodicSync() {
        ensureAccountExistsAndIsSyncable();
        ContentResolver.addPeriodicSync(account, QuickFitContentProvider.AUTHORITY, Bundle.EMPTY, TimeUnit.HOURS.toSeconds(3));
    }

    private void ensureAccountExistsAndIsSyncable() {
        if (AccountManager.get(getApplicationContext()).addAccountExplicitly(account, null, null)) {
            ContentResolver.setIsSyncable(account, QuickFitContentProvider.AUTHORITY, 1);
        }
    }

}
