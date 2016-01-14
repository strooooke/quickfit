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

import android.app.LoaderManager;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TimeUtils;
import android.widget.Toast;

import com.lambdasoup.quickfit.QuickFitContract.SessionEntry.SessionStatus;

import java.util.concurrent.TimeUnit;

/**
 * An activity representing a list of Workouts. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link WorkoutDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class WorkoutListActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = WorkoutListActivity.class.getSimpleName();
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    boolean isTwoPane;
    private WorkoutItemRecyclerViewAdapter workoutsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_workout_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.workout_list);
        assert recyclerView != null;
        workoutsAdapter = new WorkoutItemRecyclerViewAdapter(this);
        workoutsAdapter.setInsertSessionClickListener(this::onInsertSession);
        workoutsAdapter.setOnEditClickListener(workoutId -> {
            if (isTwoPane) {
                Bundle arguments = new Bundle();
                arguments.putLong(WorkoutDetailFragment.ARG_ITEM_ID, workoutId);
                WorkoutDetailFragment fragment = new WorkoutDetailFragment();
                fragment.setArguments(arguments);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.workout_detail_container, fragment)
                        .commit();

            } else {
                Intent intent = new Intent(this, WorkoutDetailActivity.class);
                intent.putExtra(WorkoutDetailFragment.ARG_ITEM_ID, workoutId);

                startActivity(intent);
            }
        });
        recyclerView.setAdapter(workoutsAdapter);

        if (findViewById(R.id.workout_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            isTwoPane = true;
        }
    }




    @Override
    protected void onStart() {
        super.onStart();
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new WorkoutListLoader(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        workoutsAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        workoutsAdapter.swapCursor(null);
    }

    private void onInsertSession(long workoutId) {
        new InsertSessionTask().execute(workoutId);
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

            getContentResolver().insert(QuickFitContentProvider.URI_SESSIONS, values);
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
