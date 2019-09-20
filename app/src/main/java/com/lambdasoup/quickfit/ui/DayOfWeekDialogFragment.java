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
import com.lambdasoup.quickfit.model.DayOfWeek;
import com.lambdasoup.quickfit.util.Arrays;


public class DayOfWeekDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    private static final String KEY_SCHEDULE_ID = "scheduleId";
    private static final String KEY_OLD_VALUE = "oldValue";

    private OnFragmentInteractionListener listener;
    private DayOfWeek[] week;

    private int checkedItemPosition;

    public DayOfWeekDialogFragment() {
        // It's a fragment, it needs a default constructor
    }

    public static DayOfWeekDialogFragment newInstance(long objectId, DayOfWeek oldValue) {
        DayOfWeekDialogFragment fragment = new DayOfWeekDialogFragment();
        Bundle args = new Bundle();
        args.putLong(KEY_SCHEDULE_ID, objectId);
        args.putParcelable(KEY_OLD_VALUE, oldValue);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof OnFragmentInteractionListenerProvider) {
            listener = ((OnFragmentInteractionListenerProvider) activity).getOnDayOfWeekDialogFragmentInteractionListener();
        } else {
            throw new IllegalArgumentException(activity.toString() + " must implement OnFragmentInteractionListenerProvider");
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
        week = DayOfWeek.getWeek();
        //noinspection ConstantConditions
        checkedItemPosition = Arrays.firstIndexOf(week, getArguments().getParcelable(KEY_OLD_VALUE));

        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.title_schedule_dayOfWeek)
                .setSingleChoiceItems(Arrays.map(week, String[].class, dayOfWeek -> getResources().getString(dayOfWeek.fullNameResId)), checkedItemPosition, this)
                .setPositiveButton(R.string.button_done_schedule_dayOfWeek, this)
                .setNegativeButton(R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                if (listener != null) {
                    listener.onListItemChanged(getArguments().getLong(KEY_SCHEDULE_ID), week[checkedItemPosition]);
                }
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                break;
            default:
                checkedItemPosition = which;
                break;
        }

    }

    interface OnFragmentInteractionListenerProvider {
        OnFragmentInteractionListener getOnDayOfWeekDialogFragmentInteractionListener();
    }

    public interface OnFragmentInteractionListener {
        void onListItemChanged(long objectId, DayOfWeek newValue);
    }
}
