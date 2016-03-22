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

package com.lambdasoup.quickfit.ui;

import android.accounts.Account;
import android.accounts.AccountManager;
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
import android.support.v7.util.SortedList;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.fitness.FitnessActivities;
import com.lambdasoup.quickfit.R;
import com.lambdasoup.quickfit.persist.QuickFitContentProvider;
import com.lambdasoup.quickfit.persist.QuickFitContract.SessionEntry;
import com.lambdasoup.quickfit.persist.QuickFitContract.SessionEntry.SessionStatus;
import com.lambdasoup.quickfit.persist.QuickFitContract.WorkoutEntry;
import com.lambdasoup.quickfit.util.ui.DividerItemDecoration;
import com.lambdasoup.quickfit.util.ui.EmptyRecyclerView;

import java.util.concurrent.TimeUnit;


public class WorkoutListActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        WorkoutItemRecyclerViewAdapter.OnWorkoutInteractionListener, DurationMinutesDialogFragment.OnFragmentInteractionListener,
        LabelDialogFragment.OnFragmentInteractionListener, CaloriesDialogFragment.OnFragmentInteractionListener {

    public static final String EXTRA_PLAY_API_CONNECT_RESULT = "play_api_connect_result";
    private static final String TAG = WorkoutListActivity.class.getSimpleName();
    private static final int REQUEST_OAUTH = 0;
    private static final String ACCOUNT_TYPE = "com.lambdasoup.quickfit";
    private static final String KEY_AUTH_IN_PROGRESS = "auth_in_progress";
    private static final long NO_FRESH_INSERTION = -1;

    private final Account account = new Account("QuickFit", ACCOUNT_TYPE);
    AuthProgress authProgress = AuthProgress.NONE;
    private WorkoutItemRecyclerViewAdapter workoutsAdapter;
    private EmptyRecyclerView workoutsRecyclerView;
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

        workoutsRecyclerView = (EmptyRecyclerView) findViewById(R.id.workout_list);
        workoutsAdapter = new WorkoutItemRecyclerViewAdapter(this);
        workoutsAdapter.setOnWorkoutInteractionListener(this);

        workoutsRecyclerView.setAdapter(workoutsAdapter);
        workoutsRecyclerView.addItemDecoration(new DividerItemDecoration(this, false));
        workoutsRecyclerView.setEmptyView(findViewById(R.id.workout_list_empty));

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
        contentValues.put(WorkoutEntry.COL_ACTIVITY_TYPE, FitnessActivities.AEROBICS);
        contentValues.put(WorkoutEntry.COL_DURATION_MINUTES, 30);
        Uri newWorkoutUri = getContentResolver().insert(QuickFitContentProvider.getUriWorkoutsList(), contentValues);
        idFreshlyInserted = ContentUris.parseId(newWorkoutUri);
    }

    @Override
    public void onDoneItClick(long workoutId) {
        new InsertSessionTask().execute(workoutId);
    }

    @Override
    public void onDeleteClick(long workoutId) {
        getContentResolver().delete(QuickFitContentProvider.getUriWorkoutsId(workoutId), null, null);
    }

    @Override
    public void onSchedulesEditRequested(long workoutId) {
        Intent intent = new Intent(getApplicationContext(), SchedulesActivity.class);
        intent.putExtra(SchedulesActivity.EXTRA_WORKOUT_ID, workoutId);
        startActivity(intent);
    }

    @Override
    public void onActivityTypeChanged(long workoutId, String newActivityTypeKey) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(WorkoutEntry.COL_ACTIVITY_TYPE, newActivityTypeKey);
        getContentResolver().update(QuickFitContentProvider.getUriWorkoutsId(workoutId), contentValues, null, null);
    }

    @Override
    public void onDurationMinsEditRequested(long workoutId, int oldValue) {
        showDialog(DurationMinutesDialogFragment.newInstance(workoutId, oldValue));
    }

    @Override
    public void onDurationChanged(long workoutId, int newValue) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(WorkoutEntry.COL_DURATION_MINUTES, newValue);
        getContentResolver().update(QuickFitContentProvider.getUriWorkoutsId(workoutId), contentValues, null, null);
    }

    @Override
    public void onLabelEditRequested(long workoutId, String oldValue) {
        showDialog(LabelDialogFragment.newInstance(workoutId, oldValue));
    }

    @Override
    public void onLabelChanged(long workoutId, String newValue) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(WorkoutEntry.COL_LABEL, newValue);
        getContentResolver().update(QuickFitContentProvider.getUriWorkoutsId(workoutId), contentValues, null, null);
    }

    @Override
    public void onCaloriesEditRequested(long workoutId, int oldValue) {
        showDialog(CaloriesDialogFragment.newInstance(workoutId, oldValue));
    }

    @Override
    public void onCaloriesChanged(long workoutId, int newValue) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(WorkoutEntry.COL_CALORIES, newValue);
        getContentResolver().update(QuickFitContentProvider.getUriWorkoutsId(workoutId), contentValues, null, null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authProgress = AuthProgress.DONE;
            if (resultCode == RESULT_OK) {
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

    private class InsertSessionTask extends AsyncTask<Long, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Long... params) {
            long workoutId = params[0];

            Cursor cursor = getContentResolver().query(
                    QuickFitContentProvider.getUriWorkoutsId(workoutId),
                    WorkoutEntry.COLUMNS_WORKOUT_ONLY,
                    null,
                    null,
                    null);
            if (cursor == null || !cursor.moveToFirst()) {
                Log.w(TAG, "Workout missing with id: " + workoutId);
                return false;
            }

            int durationInMinutes = cursor.getInt(cursor.getColumnIndex(WorkoutEntry.DURATION_MINUTES));
            long endTime = System.currentTimeMillis();
            long startTime = endTime - TimeUnit.MINUTES.toMillis(durationInMinutes);

            ContentValues values = new ContentValues();
            values.put(SessionEntry.ACTIVITY_TYPE, cursor.getString(cursor.getColumnIndex(WorkoutEntry.ACTIVITY_TYPE)));
            values.put(SessionEntry.START_TIME, startTime);
            values.put(SessionEntry.END_TIME, endTime);
            values.put(SessionEntry.STATUS, SessionStatus.NEW.name());
            values.put(SessionEntry.NAME, cursor.getString(cursor.getColumnIndex(WorkoutEntry.LABEL)));
            values.put(SessionEntry.CALORIES, cursor.getInt(cursor.getColumnIndex(WorkoutEntry.CALORIES)));

            cursor.close();

            getContentResolver().insert(QuickFitContentProvider.getUriSessionsList(), values);
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

}
