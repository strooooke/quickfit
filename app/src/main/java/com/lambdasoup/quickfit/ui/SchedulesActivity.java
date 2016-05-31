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
import android.content.Loader;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.os.Bundle;

import com.lambdasoup.quickfit.R;
import com.lambdasoup.quickfit.databinding.ActivitySchedulesBinding;
import com.lambdasoup.quickfit.persist.QuickFitContract;
import com.lambdasoup.quickfit.viewmodel.WorkoutItem;

public class SchedulesActivity extends BaseActivity implements LoaderManager.LoaderCallbacks<Cursor>, TimeDialogFragment.OnFragmentInteractionListenerProvider, DayOfWeekDialogFragment.OnFragmentInteractionListenerProvider {

    public static final String EXTRA_WORKOUT_ID = "com.lambdasoup.quickfit_workoutId";
    private static final int LOADER_WORKOUT = 0;


    private ActivitySchedulesBinding workoutBinding;

    private long workoutId;
    private SchedulesFragment schedulesFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // if orientation has changed and now width is large enough for two-pane mode, finish and let
        // WorkoutListActivity show the schedules
        if (getResources().getBoolean(R.bool.isTwoPane)) {
            finish();
        }

        workoutBinding = DataBindingUtil.setContentView(this, R.layout.activity_schedules);

        if (getIntent().hasExtra(EXTRA_WORKOUT_ID)) {
            workoutId = getIntent().getLongExtra(EXTRA_WORKOUT_ID, -1);
        } else {
            throw new IllegalArgumentException("Intent is missing workoutId extra");
        }

        setSupportActionBar(workoutBinding.toolbar);

        SchedulesFragment schedulesFragment = (SchedulesFragment) getSupportFragmentManager().findFragmentById(R.id.schedules_container);
        if (schedulesFragment == null) {
            schedulesFragment = SchedulesFragment.create(workoutId);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.schedules_container, schedulesFragment)
                    .commit();
        }
        this.schedulesFragment = schedulesFragment;

        workoutBinding.fab.setOnClickListener(v -> this.schedulesFragment.onAddNewSchedule());
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getLoaderManager().initLoader(LOADER_WORKOUT, null, this);
    }

    @Override
    public TimeDialogFragment.OnFragmentInteractionListener getOnTimeDialogFragmentInteractionListener() {
        return schedulesFragment;
    }

    @Override
    public DayOfWeekDialogFragment.OnFragmentInteractionListener getOnDayOfWeekDialogFragmentInteractionListener() {
        return schedulesFragment;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_WORKOUT:
                return new WorkoutLoader(this, workoutId);
        }
        throw new IllegalArgumentException("Not a loader id: " + id);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_WORKOUT:
                bindHeader(data);
                return;
        }
        throw new IllegalArgumentException("Not a loader id: " + loader.getId());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_WORKOUT:
                // nothing to do
                return;
        }
        throw new IllegalArgumentException("Not a loader id: " + loader.getId());
    }

    private void bindHeader(Cursor cursor) {
        cursor.moveToFirst();

        long workoutId = cursor.getLong(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.WORKOUT_ID));
        WorkoutItem workoutItem = new WorkoutItem.Builder(this)
                .withWorkoutId(workoutId)
                .withActivityTypeKey(cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.ACTIVITY_TYPE)))
                .withDurationInMinutes(cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.DURATION_MINUTES)))
                .withCalories(cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.CALORIES)))
                .withLabel(cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.LABEL)))
                .build(null);
        workoutBinding.setWorkout(workoutItem);

        workoutBinding.toolbarLayout.setTitle(workoutItem.activityType.displayName);
    }


}
