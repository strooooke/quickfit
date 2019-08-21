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
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;

import com.lambdasoup.quickfit.R;
import com.lambdasoup.quickfit.model.FitActivity;
import com.lambdasoup.quickfit.util.Arrays;

public class ActivityTypeDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String KEY_WORKOUT_ID = "workoutId";
    private static final String KEY_OLD_VALUE_KEY = "oldValue";

    private OnFragmentInteractionListener listener;
    private int checkedItemPosition;
    private FitActivity[] fitActivities;

    public ActivityTypeDialogFragment() {
        // It's a fragment, it needs a default constructor
    }

    public static ActivityTypeDialogFragment newInstance(long workoutId, FitActivity oldValue) {
        ActivityTypeDialogFragment fragment = new ActivityTypeDialogFragment();
        Bundle args = new Bundle();
        args.putLong(KEY_WORKOUT_ID, workoutId);
        args.putString(KEY_OLD_VALUE_KEY, oldValue.key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) activity;
        } else {
            throw new IllegalArgumentException(activity.toString() + " must implement OnFragmentInteractionListener");
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
        fitActivities = FitActivity.all(getResources());
        java.util.Arrays.sort(fitActivities, (left, right) -> left.displayName.compareToIgnoreCase(right.displayName));
        //noinspection ConstantConditions
        checkedItemPosition = Arrays.firstIndexOf(fitActivities, FitActivity.fromKey(getArguments().getString(KEY_OLD_VALUE_KEY), getResources()));


        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_workout_activityType)
                .setSingleChoiceItems(Arrays.map(fitActivities, String[].class, fitAct -> fitAct.displayName), checkedItemPosition, this)
                .setPositiveButton(R.string.button_done_workout_activityType, this)
                .setNegativeButton(R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (listener != null) {
                    listener.onActivityTypeChanged(getArguments().getLong(KEY_WORKOUT_ID), fitActivities[checkedItemPosition].key);
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
            default:
                checkedItemPosition = which;
                break;
        }

    }


    interface OnFragmentInteractionListener {
        void onActivityTypeChanged(long workoutId, String newActivityTypeKey);
    }
}
