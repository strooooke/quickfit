package com.lambdasoup.quickfit;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.IntentService;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.widget.Toast;

import com.lambdasoup.quickfit.persist.QuickFitContentProvider;
import com.lambdasoup.quickfit.persist.QuickFitContract;

import java.util.concurrent.TimeUnit;


public class FitActivityService extends IntentService {
    private static final String ACTION_INSERT_SESSION = "com.lambdasoup.quickfit.action.INSERT_SESSION";
    private static final String ACTION_SESSION_SYNC = "com.lambdasoup.quickfit.action.SESSION_SYNC";

    private static final String EXTRA_WORKOUT_ID = "com.lambdasoup.quickfit.alarm.WORKOUT_ID";
    private static final String TAG = FitActivityService.class.getSimpleName();

    private static final String ACCOUNT_TYPE = "com.lambdasoup.quickfit";
    private final Account account = new Account("QuickFit", ACCOUNT_TYPE);

    public FitActivityService() {
        super("FitActivityService");
    }

    public static Intent getIntentInsertSession(Context context, long workoutId) {
        Intent intent = new Intent(context, FitActivityService.class);
        intent.setAction(ACTION_INSERT_SESSION);
        intent.putExtra(EXTRA_WORKOUT_ID, workoutId);
        return intent;
    }

    public static Intent getIntentSyncSession(Context context) {
        Intent intent = new Intent(context, FitActivityService.class);
        intent.setAction(ACTION_SESSION_SYNC);
        return intent;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_INSERT_SESSION.equals(action)) {
                long workoutId = intent.getLongExtra(EXTRA_WORKOUT_ID, -1);
                handleInsertSession(workoutId);
            }
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
            Log.w(TAG, "Workout missing with id: " + workoutId);
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
    private void showToast(@StringRes int resId) {
        new Handler(getMainLooper()).post(() -> Toast.makeText(getApplicationContext(), resId, Toast.LENGTH_SHORT).show());
    }


    @WorkerThread
    private void requestSync() {
        if (AccountManager.get(getApplicationContext()).addAccountExplicitly(account, null, null)) {
            ContentResolver.setIsSyncable(account, QuickFitContentProvider.AUTHORITY, 1);
        }
        Bundle syncOptions = new Bundle();
        syncOptions.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(account, QuickFitContentProvider.AUTHORITY, syncOptions);
    }

}
