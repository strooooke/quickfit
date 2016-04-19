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

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentSender;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.util.SortedList;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.fitness.FitnessActivities;
import com.lambdasoup.quickfit.FitActivityService;
import com.lambdasoup.quickfit.R;
import com.lambdasoup.quickfit.persist.QuickFitContentProvider;
import com.lambdasoup.quickfit.persist.QuickFitContract.WorkoutEntry;
import com.lambdasoup.quickfit.util.ui.DividerItemDecoration;
import com.lambdasoup.quickfit.util.ui.EmptyRecyclerView;


public class WorkoutListActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>,
        WorkoutItemRecyclerViewAdapter.OnWorkoutInteractionListener, DurationMinutesDialogFragment.OnFragmentInteractionListener,
        LabelDialogFragment.OnFragmentInteractionListener, CaloriesDialogFragment.OnFragmentInteractionListener {


    public static final String EXTRA_SHOW_WORKOUT_ID = "com.lambdasoup.quickfit.show_workout_id";

    private static final String TAG = WorkoutListActivity.class.getSimpleName();


    private static final String KEY_SHOW_WORKOUT_ID = "com.lambdasoup.quickfit.show_workout_id";
    private static final long NO_ID = -1;


    private WorkoutItemRecyclerViewAdapter workoutsAdapter;
    private EmptyRecyclerView workoutsRecyclerView;
    private long idToScrollTo = NO_ID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        //noinspection ConstantConditions
        fab.setOnClickListener(view -> addNewWorkout());

        workoutsRecyclerView = (EmptyRecyclerView) findViewById(R.id.workout_list);
        workoutsAdapter = new WorkoutItemRecyclerViewAdapter(this);
        workoutsAdapter.setOnWorkoutInteractionListener(this);

        workoutsRecyclerView.setAdapter(workoutsAdapter);
        workoutsRecyclerView.addItemDecoration(new DividerItemDecoration(this, false));
        workoutsRecyclerView.setEmptyView(findViewById(R.id.workout_list_empty));

        if (getIntent().hasExtra(EXTRA_SHOW_WORKOUT_ID)) {
            idToScrollTo = getIntent().getLongExtra(EXTRA_SHOW_WORKOUT_ID, NO_ID);
        }

        if (savedInstanceState != null) {
            idToScrollTo = savedInstanceState.getLong(KEY_SHOW_WORKOUT_ID, NO_ID);
        }

        getLoaderManager().initLoader(0, null, this);
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(KEY_SHOW_WORKOUT_ID, idToScrollTo);
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
        if (idToScrollTo != NO_ID) {
            int pos = workoutsAdapter.getPosition(idToScrollTo);
            if (pos != SortedList.INVALID_POSITION) {
                workoutsRecyclerView.smoothScrollToPosition(pos);
            }
            idToScrollTo = NO_ID;
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
        idToScrollTo = ContentUris.parseId(newWorkoutUri);
    }

    @Override
    public void onDoneItClick(long workoutId) {
        startService(FitActivityService.getIntentInsertSession(getApplicationContext(), workoutId));
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




}
