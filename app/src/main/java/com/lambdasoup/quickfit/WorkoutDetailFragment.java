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
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.databinding.BaseObservable;
import android.databinding.Bindable;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.lambdasoup.quickfit.databinding.WorkoutDetailBinding;

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


    private WorkoutViewModel item;
    private WorkoutDetailBinding binding;

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
        item = new WorkoutViewModel();
        binding = WorkoutDetailBinding.inflate(inflater, container, false);
        binding.setWorkout(item);
        View rootView = binding.getRoot();


        ArrayAdapter<FitActivity> activitiesAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, FitActivity.all(getResources()));
        activitiesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitiesAdapter.sort((left, right) -> left.displayName.compareToIgnoreCase(right.displayName));
        binding.activityTypeSpinner.setAdapter(activitiesAdapter);

        binding.activityTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                item.setActivityTypeIndex(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

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
        item.setActivityTypeIndex(
                ((ArrayAdapter<FitActivity>) binding.activityTypeSpinner.getAdapter())
                        .getPosition(
                                FitActivity.fromKey(
                                        cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.ACTIVITY_TYPE)),
                                        getResources()
                                )
                        )
        );
        item.setDurationInMinutes(cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.DURATION_MINUTES)));

        // TODO: label?
        Activity activity = this.getActivity();
        CollapsingToolbarLayout appBarLayout = (CollapsingToolbarLayout) activity.findViewById(R.id.toolbar_layout);
        if (appBarLayout != null) {
            //appBarLayout.setTitle(mItem.content);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    public void save() {
        long id = getArguments().getLong(ARG_ITEM_ID);
        ContentValues contentValues = new ContentValues();
        contentValues.put(QuickFitContract.WorkoutEntry.ACTIVITY_TYPE,((FitActivity) binding.activityTypeSpinner.getSelectedItem()).key);
        contentValues.put(QuickFitContract.WorkoutEntry.DURATION_MINUTES, binding.durationMins.getText().toString());
        getActivity().getContentResolver().update(ContentUris.withAppendedId(QuickFitContentProvider.URI_WORKOUTS, id), contentValues, null, null);
    }

    public static class WorkoutViewModel extends BaseObservable {
        private int durationInMinutes;
        private int activityTypeIndex;

        @Bindable
        public int getActivityTypeIndex() {
            return activityTypeIndex;
        }

        public void setActivityTypeIndex(int activityTypeIndex) {
            this.activityTypeIndex = activityTypeIndex;
            //notifyPropertyChanged(com.lambdasoup.quickfit.BR.activityTypeIndex);
        }

        @Bindable
        public String getDurationInMinutes() {
            return Integer.toString(durationInMinutes);
        }

        public void setDurationInMinutes(int durationInMinutes) {
            this.durationInMinutes = durationInMinutes;
            //notifyPropertyChanged(com.lambdasoup.quickfit.BR.durationInMinutes);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("WorkoutViewModel{");
            sb.append("durationInMinutes=").append(durationInMinutes);
            sb.append(", activityTypeIndex=").append(activityTypeIndex);
            sb.append('}');
            return sb.toString();
        }
    }
}
