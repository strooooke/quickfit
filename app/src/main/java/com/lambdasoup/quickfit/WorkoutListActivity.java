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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.fitness.FitnessActivities;
import com.lambdasoup.quickfit.QuickFitContract.SessionEntry.SessionStatus;

import java.util.concurrent.TimeUnit;


public class WorkoutListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        WorkoutItemRecyclerViewAdapter.OnWorkoutInteractionListener, DurationMinutesDialogFragment.OnFragmentInteractionListener,
        LabelDialogFragment.OnFragmentInteractionListener, CaloriesDialogFragment.OnFragmentInteractionListener {

    private static final String TAG = WorkoutListActivity.class.getSimpleName();

    public static final String EXTRA_PLAY_API_CONNECT_RESULT = "play_api_connect_result";
    private static final int REQUEST_OAUTH = 0;
    private static final String ACCOUNT_TYPE = "com.lambdasoup.quickfit";
    private static final String KEY_AUTH_IN_PROGRESS = "auth_in_progress";
    public static final String TAG_DIALOG = "dialog";
    private static final long NO_FRESH_INSERTION = -1;

    private final Account account = new Account("QuickFit", ACCOUNT_TYPE);

    private WorkoutItemRecyclerViewAdapter workoutsAdapter;
    AuthProgress authProgress = AuthProgress.NONE;
    private RecyclerView workoutsRecyclerView;
    private long idFreshlyInserted = NO_FRESH_INSERTION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> addNewWorkout());

        workoutsRecyclerView = (RecyclerView) findViewById(R.id.workout_list);
        workoutsAdapter = new WorkoutItemRecyclerViewAdapter(this);
        workoutsAdapter.setOnWorkoutInteractionListener(this);

        workoutsRecyclerView.setAdapter(workoutsAdapter);
        workoutsRecyclerView.addItemDecoration(new DividerItemDecoration(this));

        if (savedInstanceState != null) {
            authProgress = AuthProgress.valueOf(savedInstanceState.getString(KEY_AUTH_IN_PROGRESS, AuthProgress.NONE.name()));
        }

        if (AccountManager.get(getApplicationContext()).addAccountExplicitly(account, null, null)) {
            ContentResolver.setIsSyncable(account, QuickFitContentProvider.AUTHORITY, 1);
        }

        getLoaderManager().initLoader(0, null, this);
    }


    @Override
    protected void onResume() {
        super.onResume();

        ConnectionResult connectionResult = getIntent().getParcelableExtra(EXTRA_PLAY_API_CONNECT_RESULT);
        if (connectionResult != null && authProgress == AuthProgress.NONE) {
            Log.d(TAG, "starting auth");

            authProgress = AuthProgress.IN_PROGRESS;
            try {
                connectionResult.startResolutionForResult(this, REQUEST_OAUTH);
            } catch (IntentSender.SendIntentException e) {
                Log.e(TAG,
                        "Exception while starting resolution activity", e);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_AUTH_IN_PROGRESS, authProgress.name());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_workout_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(getBaseContext(), SettingsActivity.class));
                return true;
            case R.id.action_about:
                startActivity(new Intent(getBaseContext(), AboutActivity.class));
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new WorkoutListLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        workoutsAdapter.swapCursor(data);
        if (idFreshlyInserted != NO_FRESH_INSERTION) {
            int pos = workoutsAdapter.getPosition(idFreshlyInserted);
            if (pos != SortedList.INVALID_POSITION) {
                workoutsRecyclerView.smoothScrollToPosition(pos);
            }
            idFreshlyInserted = NO_FRESH_INSERTION;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        workoutsAdapter.swapCursor(null);
    }

    private void addNewWorkout() {
        ContentValues contentValues = new ContentValues();
        contentValues.put(QuickFitContract.WorkoutEntry.ACTIVITY_TYPE, FitnessActivities.AEROBICS);
        contentValues.put(QuickFitContract.WorkoutEntry.DURATION_MINUTES, 30);
        Uri newWorkoutUri = getContentResolver().insert(QuickFitContentProvider.URI_WORKOUTS, contentValues);
        idFreshlyInserted = ContentUris.parseId(newWorkoutUri);
    }

    @Override
    public void onDoneItClick(long workoutId) {
        new InsertSessionTask().execute(workoutId);
    }

    @Override
    public void onDeleteClick(long workoutId) {
        getContentResolver().delete(ContentUris.withAppendedId(QuickFitContentProvider.URI_WORKOUTS, workoutId), null, null);
    }

    @Override
    public void onActivityTypeChanged(long workoutId, String newActivityTypeKey) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(QuickFitContract.WorkoutEntry.ACTIVITY_TYPE, newActivityTypeKey);
        getContentResolver().update(ContentUris.withAppendedId(QuickFitContentProvider.URI_WORKOUTS, workoutId), contentValues, null, null);
    }

    @Override
    public void onDurationMinsEditRequested(long workoutId, int oldValue) {
        showDialog(DurationMinutesDialogFragment.newInstance(workoutId, oldValue));
    }

    @Override
    public void onDurationChanged(long workoutId, int newValue) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(QuickFitContract.WorkoutEntry.DURATION_MINUTES, newValue);
        getContentResolver().update(ContentUris.withAppendedId(QuickFitContentProvider.URI_WORKOUTS, workoutId), contentValues, null, null);
    }

    @Override
    public void onLabelEditRequested(long workoutId, String oldValue) {
        showDialog(LabelDialogFragment.newInstance(workoutId, oldValue));
    }

    @Override
    public void onLabelChanged(long workoutId, String newValue) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(QuickFitContract.WorkoutEntry.LABEL, newValue);
        getContentResolver().update(ContentUris.withAppendedId(QuickFitContentProvider.URI_WORKOUTS, workoutId), contentValues, null, null);
    }

    @Override
    public void onCaloriesEditRequested(long workoutId, int oldValue) {
        showDialog(CaloriesDialogFragment.newInstance(workoutId, oldValue));
    }

    @Override
    public void onCaloriesChanged(long workoutId, int newValue) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(QuickFitContract.WorkoutEntry.CALORIES, newValue);
        getContentResolver().update(ContentUris.withAppendedId(QuickFitContentProvider.URI_WORKOUTS, workoutId), contentValues, null, null);
    }

    private void showDialog(DialogFragment dialogFragment) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag(TAG_DIALOG);
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        dialogFragment.show(ft, TAG_DIALOG);
    }

    private class InsertSessionTask extends AsyncTask<Long, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Long... params) {
            long workoutId = params[0];

            Cursor cursor = getContentResolver().query(
                    ContentUris.withAppendedId(QuickFitContentProvider.URI_WORKOUTS, workoutId),
                    QuickFitContract.WorkoutEntry.COLUMNS,
                    null,
                    null,
                    null);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.w(TAG, "Workout missing with id: " + workoutId);
                return false;
            }

            int durationInMinutes = cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.DURATION_MINUTES));
            long endTime = System.currentTimeMillis();
            long startTime = endTime - TimeUnit.MINUTES.toMillis(durationInMinutes);

            ContentValues values = new ContentValues();
            values.put(QuickFitContract.SessionEntry.ACTIVITY_TYPE, cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.ACTIVITY_TYPE)));
            values.put(QuickFitContract.SessionEntry.START_TIME, startTime);
            values.put(QuickFitContract.SessionEntry.END_TIME, endTime);
            values.put(QuickFitContract.SessionEntry.STATUS, SessionStatus.NEW.name());
            values.put(QuickFitContract.SessionEntry.NAME, cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.LABEL)));
            values.put(QuickFitContract.SessionEntry.CALORIES, cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.CALORIES)));

            cursor.close();

            getContentResolver().insert(QuickFitContentProvider.URI_SESSIONS, values);
            requestSync();
            return true;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(WorkoutListActivity.this, R.string.success_session_insert, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authProgress = AuthProgress.DONE;
            if (resultCode == Activity.RESULT_OK) {
                requestSync();
            }
        }
    }

    private void requestSync() {
        Bundle syncOptions = new Bundle();
        syncOptions.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        ContentResolver.requestSync(account, QuickFitContentProvider.AUTHORITY, syncOptions);
    }

    enum AuthProgress {
        NONE, IN_PROGRESS, DONE
    }

}
