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
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;


public class DurationMinutesDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String KEY_WORKOUT_ID = "workoutId";
    private static final String KEY_OLD_VALUE = "oldValue";

    private OnFragmentInteractionListener listener;

    public DurationMinutesDialogFragment(){
        // It's a fragment, it needs a default constructor
    }

    public static DurationMinutesDialogFragment newInstance(long workoutId, int oldValue) {
        DurationMinutesDialogFragment fragment = new DurationMinutesDialogFragment();
        Bundle args = new Bundle();
        args.putLong(KEY_WORKOUT_ID, workoutId);
        args.putInt(KEY_OLD_VALUE, oldValue);
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
        View dialogContent = LayoutInflater.from(getContext()).inflate(R.layout.dialog_duration_minutes, null);
        NumberPicker numberPicker = ((NumberPicker) dialogContent.findViewById(R.id.duration_mins_picker));
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(9999);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setValue(getArguments().getInt(KEY_OLD_VALUE));

        return new AlertDialog.Builder(getContext())
                .setView(dialogContent)
                .setTitle(R.string.title_workout_duration)
                .setPositiveButton(R.string.button_done_workout_duration, this)
                .create();
    }


    interface OnFragmentInteractionListener {
        void onDurationChanged(long workoutId, int newValue);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                NumberPicker numberPicker = ((NumberPicker) getDialog().findViewById(R.id.duration_mins_picker));
                numberPicker.clearFocus();
                int newVal = numberPicker.getValue();
                listener.onDurationChanged(getArguments().getLong(KEY_WORKOUT_ID), newVal);
                break;
            default:
                throw new IllegalStateException("No such button.");
        }
    }
}
