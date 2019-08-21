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

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lambdasoup.quickfit.alarm.AlarmService;
import com.lambdasoup.quickfit.databinding.FragmentSchedulesBinding;
import com.lambdasoup.quickfit.model.DayOfWeek;
import com.lambdasoup.quickfit.persist.QuickFitContentProvider;
import com.lambdasoup.quickfit.persist.QuickFitContract;
import com.lambdasoup.quickfit.util.DateTimes;
import com.lambdasoup.quickfit.util.ui.DividerItemDecoration;
import com.lambdasoup.quickfit.util.ui.LeaveBehind;
import com.lambdasoup.quickfit.viewmodel.ScheduleItem;

import java.util.Calendar;

import timber.log.Timber;


public class SchedulesFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>,
        SchedulesRecyclerViewAdapter.OnScheduleInteractionListener, TimeDialogFragment.OnFragmentInteractionListener, DayOfWeekDialogFragment.OnFragmentInteractionListener {

    private static final String ARG_WORKOUT_ID = "com.lambdasoup.quickfit_workoutId";
    private static final int LOADER_SCHEDULES = 1;

    private long workoutId;
    private SchedulesRecyclerViewAdapter schedulesAdapter;
    private FragmentSchedulesBinding schedulesBinding;


    public static SchedulesFragment create(long workoutId) {
        Timber.d("Creating schedules fragment with id %d", workoutId);
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_WORKOUT_ID, workoutId);
        SchedulesFragment fragment = new SchedulesFragment();
        fragment.setArguments(arguments);
        Timber.d("created schedules fragment: %d", fragment.hashCode());
        return fragment;
    }

    public SchedulesFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && getArguments().containsKey(ARG_WORKOUT_ID)) {
            workoutId = getArguments().getLong(ARG_WORKOUT_ID);
        } else {
            throw new IllegalArgumentException("Argument 'workoutId' is missing");
        }


    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        schedulesBinding = FragmentSchedulesBinding.inflate(inflater, container, false);
        return schedulesBinding.getRoot();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Timber.d("activity created, initializing view binding");
        schedulesAdapter = new SchedulesRecyclerViewAdapter();
        schedulesAdapter.setOnScheduleInteractionListener(this);
        schedulesBinding.scheduleList.setAdapter(schedulesAdapter);

        ItemTouchHelper swipeDismiss = new ItemTouchHelper(new LeaveBehind() {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                onRemoveSchedule(viewHolder.getItemId());
            }
        });
        swipeDismiss.attachToRecyclerView(schedulesBinding.scheduleList);

        schedulesBinding.scheduleList.addItemDecoration(new DividerItemDecoration(getContext(), true));

        Bundle bundle = new Bundle(1);
        bundle.putLong(ARG_WORKOUT_ID, workoutId);
        getLoaderManager().initLoader(LOADER_SCHEDULES, bundle, this);


    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Timber.d("onCreateLoader with args %s on schedules fragment %d", args, this.hashCode());
        switch (id) {
            case LOADER_SCHEDULES:
                return new SchedulesLoader(getContext(), args.getLong(ARG_WORKOUT_ID));
        }
        throw new IllegalArgumentException("Not a loader id: " + id);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        Timber.d("onLoadFinished, cursor is null? %s, cursor size is %d on schedules fragment %d", data == null, data == null ? 0 : data.getCount(), this.hashCode());
        switch (loader.getId()) {
            case LOADER_SCHEDULES:
                schedulesAdapter.swapCursor(data);
                return;
        }
        throw new IllegalArgumentException("Not a loader id: " + loader.getId());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_SCHEDULES:
                schedulesAdapter.swapCursor(null);
                return;
        }
        throw new IllegalArgumentException("Not a loader id: " + loader.getId());
    }

    @Override
    public void onDayOfWeekEditRequested(long scheduleId, DayOfWeek oldValue) {
        ((DialogActivity) getActivity()).showDialog(DayOfWeekDialogFragment.newInstance(scheduleId, oldValue));
    }

    @Override
    public void onListItemChanged(long scheduleId, DayOfWeek newDayOfWeek) {
        ScheduleItem oldScheduleItem = schedulesAdapter.getById(scheduleId);
        long nextAlarmMillis = DateTimes.getNextOccurrence(System.currentTimeMillis(), newDayOfWeek, oldScheduleItem.hour, oldScheduleItem.minute);

        ContentValues contentValues = new ContentValues(3);
        contentValues.put(QuickFitContract.ScheduleEntry.COL_DAY_OF_WEEK, newDayOfWeek.name());
        contentValues.put(QuickFitContract.ScheduleEntry.COL_NEXT_ALARM_MILLIS, nextAlarmMillis);
        contentValues.put(QuickFitContract.ScheduleEntry.COL_SHOW_NOTIFICATION, QuickFitContract.ScheduleEntry.SHOW_NOTIFICATION_NO);
        getContext().getContentResolver().update(QuickFitContentProvider.getUriWorkoutsIdSchedulesId(workoutId, scheduleId), contentValues, null, null);

        refreshAlarm();
    }


    @Override
    public void onTimeEditRequested(long scheduleId, int oldHour, int oldMinute) {
        ((DialogActivity) getActivity()).showDialog(TimeDialogFragment.newInstance(scheduleId, oldHour, oldMinute));
    }

    @Override
    public void onTimeChanged(long scheduleId, int newHour, int newMinute) {
        ScheduleItem oldScheduleItem = schedulesAdapter.getById(scheduleId);
        long nextAlarmMillis = DateTimes.getNextOccurrence(System.currentTimeMillis(), oldScheduleItem.dayOfWeek, newHour, newMinute);

        ContentValues contentValues = new ContentValues(4);
        contentValues.put(QuickFitContract.ScheduleEntry.COL_HOUR, newHour);
        contentValues.put(QuickFitContract.ScheduleEntry.COL_MINUTE, newMinute);
        contentValues.put(QuickFitContract.ScheduleEntry.COL_NEXT_ALARM_MILLIS, nextAlarmMillis);
        contentValues.put(QuickFitContract.ScheduleEntry.COL_SHOW_NOTIFICATION, QuickFitContract.ScheduleEntry.SHOW_NOTIFICATION_NO);
        getContext().getContentResolver().update(QuickFitContentProvider.getUriWorkoutsIdSchedulesId(workoutId, scheduleId), contentValues, null, null);

        refreshAlarm();
    }

    void onAddNewSchedule() {
        // initialize with current day and time
        Calendar calendar = Calendar.getInstance();
        DayOfWeek dayOfWeek = DayOfWeek.getByCalendarConst(calendar.get(Calendar.DAY_OF_WEEK));
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        long nextAlarmMillis = DateTimes.getNextOccurrence(System.currentTimeMillis(), dayOfWeek, hour, minute);

        ContentValues contentValues = new ContentValues(4);
        contentValues.put(QuickFitContract.ScheduleEntry.COL_DAY_OF_WEEK, dayOfWeek.name());
        contentValues.put(QuickFitContract.ScheduleEntry.COL_HOUR, hour);
        contentValues.put(QuickFitContract.ScheduleEntry.COL_MINUTE, minute);
        contentValues.put(QuickFitContract.ScheduleEntry.COL_NEXT_ALARM_MILLIS, nextAlarmMillis);
        contentValues.put(QuickFitContract.ScheduleEntry.COL_SHOW_NOTIFICATION, QuickFitContract.ScheduleEntry.SHOW_NOTIFICATION_NO);
        getContext().getContentResolver().insert(QuickFitContentProvider.getUriWorkoutsIdSchedules(workoutId), contentValues);

        refreshAlarm();
    }

    private void refreshAlarm() {
        getContext().startService(AlarmService.getIntentOnNextOccChanged(getContext()));
    }

    private void onRemoveSchedule(long scheduleId) {
        getContext().getContentResolver().delete(QuickFitContentProvider.getUriWorkoutsIdSchedulesId(workoutId, scheduleId), null, null);
    }

    long getWorkoutId() {
        return workoutId;
    }


}
