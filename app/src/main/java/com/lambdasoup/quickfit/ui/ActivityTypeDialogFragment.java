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

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.lambdasoup.quickfit.R;
import com.lambdasoup.quickfit.model.FitActivity;
import com.lambdasoup.quickfit.util.Arrays;
import com.lambdasoup.quickfit.util.ConstantListAdapter;

public class ActivityTypeDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String KEY_WORKOUT_ID = "workoutId";

    private OnFragmentInteractionListener listener;
    private ConstantListAdapter<FitActivity> activityTypesAdapter;

    public ActivityTypeDialogFragment() {
        // It's a fragment, it needs a default constructor
    }

    public static ActivityTypeDialogFragment newInstance(long workoutId) {
        ActivityTypeDialogFragment fragment = new ActivityTypeDialogFragment();
        Bundle args = new Bundle();
        args.putLong(KEY_WORKOUT_ID, workoutId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) activity;
        } else {
            throw new RuntimeException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FitActivity[] fitActivities = FitActivity.all(getResources());
        java.util.Arrays.sort(fitActivities, (left, right) -> left.displayName.compareToIgnoreCase(right.displayName));
        activityTypesAdapter = new ConstantListAdapter<>(
                getContext(),
                android.R.layout.simple_list_item_1,
                android.R.layout.simple_spinner_dropdown_item,
                fitActivities,
                fitAct -> fitAct.displayName);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_workout_activityType)
                .setAdapter(activityTypesAdapter, this)
                .setOnDismissListener(this)
                .create();

        return dialog;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (listener != null) {
            listener.onActivityTypeChanged(getArguments().getLong(KEY_WORKOUT_ID), activityTypesAdapter.getItem(which).key);
        }
    }


    interface OnFragmentInteractionListener {
        void onActivityTypeChanged(long workoutId, String newActivityTypeKey);
    }
}
