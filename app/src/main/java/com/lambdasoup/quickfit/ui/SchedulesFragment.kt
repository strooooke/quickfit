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

package com.lambdasoup.quickfit.ui

import android.content.ContentValues
import android.database.Cursor
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.Loader
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.lambdasoup.quickfit.alarm.AlarmService
import com.lambdasoup.quickfit.databinding.FragmentSchedulesBinding
import com.lambdasoup.quickfit.model.DayOfWeek
import com.lambdasoup.quickfit.persist.QuickFitContentProvider
import com.lambdasoup.quickfit.persist.QuickFitContract.ScheduleEntry
import com.lambdasoup.quickfit.util.DateTimes
import com.lambdasoup.quickfit.util.ui.DividerItemDecoration
import com.lambdasoup.quickfit.util.ui.LeaveBehind
import com.lambdasoup.quickfit.util.ui.systemWindowInsetsRelative
import com.lambdasoup.quickfit.util.ui.updatePadding
import timber.log.Timber
import java.util.*


class SchedulesFragment : Fragment(),
        LoaderManager.LoaderCallbacks<Cursor>, SchedulesRecyclerViewAdapter.OnScheduleInteractionListener,
        TimeDialogFragment.OnFragmentInteractionListener, DayOfWeekDialogFragment.OnFragmentInteractionListener
{

    internal var workoutId: Long = 0
        private set
    private lateinit var schedulesAdapter: SchedulesRecyclerViewAdapter
    private lateinit var schedulesBinding: FragmentSchedulesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        workoutId = arguments?.let {
            if (it.containsKey(ARG_WORKOUT_ID)) {
                it.getLong(ARG_WORKOUT_ID)
            } else {
                null
            }
        } ?: throw IllegalArgumentException("Argument 'workoutId' is missing")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        schedulesBinding = FragmentSchedulesBinding.inflate(inflater, container, false)
        return schedulesBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        Timber.d("activity created, initializing view binding")
        schedulesAdapter = SchedulesRecyclerViewAdapter().apply {
            setOnScheduleInteractionListener(this@SchedulesFragment)
        }
        schedulesBinding.scheduleList.adapter = schedulesAdapter

        val swipeDismiss = ItemTouchHelper(object : LeaveBehind() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                onRemoveSchedule(viewHolder.itemId)
            }
        })
        swipeDismiss.attachToRecyclerView(schedulesBinding.scheduleList)

        schedulesBinding.scheduleList.addItemDecoration(DividerItemDecoration(requireContext(), true))

        val bundle = Bundle(1)
        bundle.putLong(ARG_WORKOUT_ID, workoutId)
        loaderManager.initLoader(LOADER_SCHEDULES, bundle, this)
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        Timber.d("onCreateLoader with args %s on schedules fragment %d", args, this.hashCode())
        when (id) {
            LOADER_SCHEDULES -> return SchedulesLoader(context, args!!.getLong(ARG_WORKOUT_ID))
        }
        throw IllegalArgumentException("Not a loader id: $id")
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        Timber.d("onLoadFinished, cursor is null? %s, cursor size is %d on schedules fragment %d",
                data == null, data?.count ?: 0, this.hashCode())

        when (loader.id) {
            LOADER_SCHEDULES -> {
                schedulesAdapter.swapCursor(data)
                return
            }
            else -> throw IllegalArgumentException("Not a loader id: " + loader.id)
        }
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        when (loader.id) {
            LOADER_SCHEDULES -> {
                schedulesAdapter.swapCursor(null)
                return
            }
            else -> throw IllegalArgumentException("Not a loader id: " + loader.id)
        }
    }

    override fun onDayOfWeekEditRequested(scheduleId: Long, oldValue: DayOfWeek) {
        (activity as DialogActivity).showDialog(DayOfWeekDialogFragment.newInstance(scheduleId, oldValue))
    }

    override fun onListItemChanged(scheduleId: Long, newDayOfWeek: DayOfWeek) {
        val oldScheduleItem = schedulesAdapter.getById(scheduleId)
        val nextAlarmMillis = DateTimes.getNextOccurrence(
                System.currentTimeMillis(),
                newDayOfWeek,
                oldScheduleItem.hour,
                oldScheduleItem.minute
        )

        requireContext().contentResolver.update(
                QuickFitContentProvider.getUriWorkoutsIdSchedulesId(workoutId, scheduleId),
                ContentValues(3).apply {
                    put(ScheduleEntry.COL_DAY_OF_WEEK, newDayOfWeek.name)
                    put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, nextAlarmMillis)
                    put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_NO)
                },
                null,
                null
        )

        refreshAlarm()
    }


    override fun onTimeEditRequested(scheduleId: Long, oldHour: Int, oldMinute: Int) {
        (activity as DialogActivity).showDialog(TimeDialogFragment.newInstance(scheduleId, oldHour, oldMinute))
    }

    override fun onTimeChanged(scheduleId: Long, newHour: Int, newMinute: Int) {
        val oldScheduleItem = schedulesAdapter.getById(scheduleId)
        val nextAlarmMillis = DateTimes.getNextOccurrence(System.currentTimeMillis(), oldScheduleItem.dayOfWeek, newHour, newMinute)

        requireContext().contentResolver.update(
                QuickFitContentProvider.getUriWorkoutsIdSchedulesId(workoutId, scheduleId),
                ContentValues(4).apply {
                    put(ScheduleEntry.COL_HOUR, newHour)
                    put(ScheduleEntry.COL_MINUTE, newMinute)
                    put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, nextAlarmMillis)
                    put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_NO)
                },
                null,
                null
        )

        refreshAlarm()
    }

    internal fun onAddNewSchedule() {
        // initialize with current day and time
        val calendar = Calendar.getInstance()
        val dayOfWeek = DayOfWeek.getByCalendarConst(calendar.get(Calendar.DAY_OF_WEEK))
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val nextAlarmMillis = DateTimes.getNextOccurrence(System.currentTimeMillis(), dayOfWeek, hour, minute)

        requireContext().contentResolver.insert(
                QuickFitContentProvider.getUriWorkoutsIdSchedules(workoutId),
                ContentValues(5).apply {
                    put(ScheduleEntry.COL_DAY_OF_WEEK, dayOfWeek.name)
                    put(ScheduleEntry.COL_HOUR, hour)
                    put(ScheduleEntry.COL_MINUTE, minute)
                    put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, nextAlarmMillis)
                    put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_NO)
                }
        )

        refreshAlarm()
    }

    private fun refreshAlarm() {
        requireContext().startService(AlarmService.getIntentOnNextOccChanged(context))
    }

    private fun onRemoveSchedule(scheduleId: Long) {
        requireContext().contentResolver.delete(
                QuickFitContentProvider.getUriWorkoutsIdSchedulesId(workoutId, scheduleId),
                null,
                null
        )
    }

    companion object {

        private const val ARG_WORKOUT_ID = "com.lambdasoup.quickfit_workoutId"
        private const val LOADER_SCHEDULES = 1


        fun create(workoutId: Long): SchedulesFragment {
            Timber.d("Creating schedules fragment with id %d", workoutId)
            val fragment = SchedulesFragment().apply {
                arguments = Bundle().apply {
                    putLong(ARG_WORKOUT_ID, workoutId)
                }
            }
            Timber.d("created schedules fragment: %d", fragment.hashCode())
            return fragment
        }
    }
}
