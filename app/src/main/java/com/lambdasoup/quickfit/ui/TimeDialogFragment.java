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
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.format.DateFormat;
import android.widget.TimePicker;

/**
 * Created by jl on 18.03.16.
 */
public class TimeDialogFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    private static final String KEY_SCHEDULE_ID = "scheduleId";
    private static final String KEY_OLD_HOUR = "oldHour";
    private static final String KEY_OLD_MINUTE = "oldMinute";

    private OnFragmentInteractionListener listener;

    public TimeDialogFragment() {
        // It's a fragment, it needs a default constructor
    }

    public static TimeDialogFragment newInstance(long scheduleId, int oldHour, int oldMinute) {
        TimeDialogFragment fragment = new TimeDialogFragment();
        Bundle args = new Bundle();
        args.putLong(KEY_SCHEDULE_ID, scheduleId);
        args.putInt(KEY_OLD_HOUR, oldHour);
        args.putInt(KEY_OLD_MINUTE, oldMinute);
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

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int hour = getArguments().getInt(KEY_OLD_HOUR);
        int minute = getArguments().getInt(KEY_OLD_MINUTE);

        // Create a new instance of TimePickerDialog and return it
        return new TimePickerDialog(getActivity(), this, hour, minute,
                DateFormat.is24HourFormat(getActivity()));
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
       listener.onTimeChanged(getArguments().getLong(KEY_SCHEDULE_ID), hourOfDay, minute);
    }

    interface OnFragmentInteractionListener {
        void onTimeChanged(long scheduleId, int newHour, int newMinute);
    }

}
