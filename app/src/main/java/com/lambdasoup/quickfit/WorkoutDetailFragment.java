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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

/**
 * A fragment representing a single Workout detail screen.
 * This fragment is either contained in a {@link WorkoutListActivity}
 * in two-pane mode (on tablets) or a {@link WorkoutDetailActivity}
 * on handsets.
 */
public class WorkoutDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {
    /**
     * The fragment argument representing the item ID that this fragment
     * represents.
     */
    public static final String ARG_ITEM_ID = "item_id";


    private WorkoutDetail item;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WorkoutDetailFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            getLoaderManager().initLoader(0, getArguments(), this);


        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.workout_detail, container, false);

        ArrayAdapter<FitActivity> activitiesAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, FitActivity.all(getResources()));
        activitiesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitiesAdapter.sort((left, right) -> left.displayName.compareToIgnoreCase(right.displayName));

        ((Spinner) rootView.findViewById(R.id.activity_type_spinner)).setAdapter(activitiesAdapter);

        return rootView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        long workoutId = args.getLong(ARG_ITEM_ID, -1);
        if (workoutId != -1) {
            return new WorkoutLoader(getActivity(), workoutId);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        cursor.moveToFirst();
        item = new WorkoutDetail(
                cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.ACTIVITY_TYPE)),
                cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.DURATION_MINUTES))
        );
        // TODO: binding

        Activity activity = this.getActivity();
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
            //appBarLayout.setTitle(mItem.content);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // TODO: ???
    }

    static class WorkoutDetail {
        private String activityType;
        private int durationInMinutes;

        public WorkoutDetail(String activityType, int durationInMinutes) {
            this.activityType = activityType;
            this.durationInMinutes = durationInMinutes;
        }

        public String getActivityType() {
            return activityType;
        }

        public void setActivityType(String activityType) {
            this.activityType = activityType;
        }

        public int getDurationInMinutes() {
            return durationInMinutes;
        }

        public void setDurationInMinutes(int durationInMinutes) {
            this.durationInMinutes = durationInMinutes;
        }
    }
}
