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

import android.app.LoaderManager
import android.content.Loader
import android.database.Cursor
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import android.view.View

import com.lambdasoup.quickfit.R
import com.lambdasoup.quickfit.databinding.ActivitySchedulesBinding
import com.lambdasoup.quickfit.persist.QuickFitContract
import com.lambdasoup.quickfit.util.ui.systemWindowInsetsRelative
import com.lambdasoup.quickfit.viewmodel.WorkoutItem

import com.lambdasoup.quickfit.util.ui.updateHeight
import com.lambdasoup.quickfit.util.ui.updateMargins
import com.lambdasoup.quickfit.util.ui.updatePadding

class SchedulesActivity : FitFailureResolutionActivity(),
        LoaderManager.LoaderCallbacks<Cursor>, TimeDialogFragment.OnFragmentInteractionListenerProvider,
        DayOfWeekDialogFragment.OnFragmentInteractionListenerProvider
{

    private lateinit var schedulesFragment: SchedulesFragment
    private lateinit var workoutBinding: ActivitySchedulesBinding

    private var workoutId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // if orientation has changed and now width is large enough for two-pane mode, finish and let
        // WorkoutListActivity show the schedules
        if (resources.getBoolean(R.bool.isTwoPane)) {
            finish()
        }

        workoutBinding = DataBindingUtil.setContentView(this, R.layout.activity_schedules)

        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                // Tells the system that the window wishes the content to
                // be laid out as if the navigation bar was hidden
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        if (intent.hasExtra(EXTRA_WORKOUT_ID)) {
            workoutId = intent.getLongExtra(EXTRA_WORKOUT_ID, -1)
        } else {
            throw IllegalArgumentException("Intent is missing workoutId extra")
        }

        setSupportActionBar(workoutBinding.toolbar)

        this.schedulesFragment = supportFragmentManager.findFragmentById(R.id.schedules_container) as SchedulesFragment? ?:
            SchedulesFragment.create(workoutId).also {
                supportFragmentManager.beginTransaction()
                        .add(R.id.schedules_container, it)
                        .commit()
            }

        workoutBinding.fab.setOnClickListener { this.schedulesFragment.onAddNewSchedule() }

        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        workoutBinding.root.setOnApplyWindowInsetsListener { view, windowInsets ->
            view.setOnApplyWindowInsetsListener(null)
            val relativeSystemWindowInsets = windowInsets.systemWindowInsetsRelative(view)

            with(workoutBinding.appBar) {
                updatePadding { oldPadding ->
                    oldPadding + relativeSystemWindowInsets.copy(bottom = 0)
                }
                updateHeight { oldHeight -> oldHeight + windowInsets.systemWindowInsetTop }
            }

            workoutBinding.fab.updateMargins { oldMargins ->
                oldMargins.copy(end = oldMargins.end + relativeSystemWindowInsets.end)
            }

            windowInsets
        }

        loaderManager.initLoader(LOADER_WORKOUT, null, this)
    }

    override fun getOnTimeDialogFragmentInteractionListener(): TimeDialogFragment.OnFragmentInteractionListener? {
        return schedulesFragment
    }

    override fun getOnDayOfWeekDialogFragmentInteractionListener(): DayOfWeekDialogFragment.OnFragmentInteractionListener? {
        return schedulesFragment
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        when (id) {
            LOADER_WORKOUT -> return WorkoutLoader(this, workoutId)
        }
        throw IllegalArgumentException("Not a loader id: $id")
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor) {
        when (loader.id) {
            LOADER_WORKOUT -> {
                bindHeader(data)
                return
            }
        }
        throw IllegalArgumentException("Not a loader id: " + loader.id)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        when (loader.id) {
            LOADER_WORKOUT ->
                // nothing to do
                return
        }
        throw IllegalArgumentException("Not a loader id: " + loader.id)
    }

    private fun bindHeader(cursor: Cursor) {
        cursor.moveToFirst()

        val workoutId = cursor.getLong(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.WORKOUT_ID))
        val workoutItem = WorkoutItem.Builder(this)
                .withWorkoutId(workoutId)
                .withActivityTypeKey(cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.ACTIVITY_TYPE)))
                .withDurationInMinutes(cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.DURATION_MINUTES)))
                .withCalories(cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.CALORIES)))
                .withLabel(cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.LABEL)))
                .build(null)
        workoutBinding.workout = workoutItem

        workoutBinding.toolbarLayout.title = workoutItem.activityType.displayName
    }

    companion object {

        const val EXTRA_WORKOUT_ID = "com.lambdasoup.quickfit_workoutId"
        private const val LOADER_WORKOUT = 0
    }


}
