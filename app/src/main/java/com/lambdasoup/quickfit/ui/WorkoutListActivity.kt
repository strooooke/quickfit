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

import android.animation.ObjectAnimator
import android.app.LoaderManager
import android.app.PendingIntent
import android.content.ContentUris
import android.content.ContentValues
import android.content.Intent
import android.content.Loader
import android.database.Cursor
import android.net.Uri.parse
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import androidx.recyclerview.widget.SortedList
import com.google.android.gms.fitness.FitnessActivities
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.lambdasoup.quickfit.FitActivityService
import com.lambdasoup.quickfit.R
import com.lambdasoup.quickfit.model.FitActivity
import com.lambdasoup.quickfit.persist.QuickFitContentProvider
import com.lambdasoup.quickfit.persist.QuickFitContract.WorkoutEntry
import com.lambdasoup.quickfit.util.ui.*
import timber.log.Timber


class WorkoutListActivity : FitFailureResolutionActivity(), LoaderManager.LoaderCallbacks<Cursor>,
        WorkoutItemRecyclerViewAdapter.OnWorkoutInteractionListener, DurationMinutesDialogFragment.OnFragmentInteractionListener,
        LabelDialogFragment.OnFragmentInteractionListener, CaloriesDialogFragment.OnFragmentInteractionListener,
        TimeDialogFragment.OnFragmentInteractionListenerProvider, DayOfWeekDialogFragment.OnFragmentInteractionListenerProvider,
        ActivityTypeDialogFragment.OnFragmentInteractionListener
{
    private val twoPanes by lazy { findViewById<MasterDetailLayout>(R.id.two_panes) }
    private val fabAddSchedule by lazy { findViewById<FloatingActionButton>(R.id.fab_add_schedule) }
    private val fabAddWorkout by lazy { findViewById<FloatingActionButton>(R.id.fab_add_workout) }
    private val workoutList by lazy { findViewById<RecyclerView>(R.id.workout_list) }
    private val fab by lazy { findViewById<FloatingActionButton>(R.id.fab) }


    private val fabAnimationDuration: Int by lazy { resources.getInteger(R.integer.fab_animation_duration) }
    private val fabBackgroundToActivated: ObjectAnimator by lazy {
        BackgroundTintListAnimator.create(
            this,
            fab,
            R.color.colorAccent,
            R.color.colorPrimaryMediumLight,
            fabAnimationDuration.toLong()
        )
    }
    private val fabBackgroundToNotActivated: ObjectAnimator by lazy {
        BackgroundTintListAnimator.create(
            this,
            fab,
            R.color.colorPrimaryMediumLight,
            R.color.colorAccent,
            fabAnimationDuration.toLong()
        )
    }

    private var idToSelect = NO_ID
    private var offsetFabAddWorkout: Float = 0f
    private var offsetFabAddSchedule: Float = 0f

    private lateinit var workoutsAdapter: WorkoutItemRecyclerViewAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.d("onCreate before inflate")
        setContentView(R.layout.activity_workout_list)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                // Tells the system that the window wishes the content to
                // be laid out as if the navigation bar was hidden
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.title = title
        fab.setOnClickListener { addNewWorkout() }

        if (twoPanes != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            fabAddSchedule!!.setOnClickListener { addNewSchedule() }
            fabAddWorkout!!.setOnClickListener { addNewWorkout() }
            setMiniFabOffsets()

            twoPanes!!.setAfterCollapse {
                fab.layoutParams = (fab.layoutParams as CoordinatorLayout.LayoutParams).apply {
                    anchorId = View.NO_ID
                    gravity = Gravity.BOTTOM or Gravity.END
                }

                val schedulesFragment = supportFragmentManager.findFragmentById(R.id.schedules_container)
                supportFragmentManager.beginTransaction()
                        .remove(schedulesFragment!!)
                        .commit()
            }
        }

        workoutsAdapter = WorkoutItemRecyclerViewAdapter(this, twoPanes != null).apply {
            setOnWorkoutInteractionListener(this@WorkoutListActivity)
        }

        with(workoutList) {
            adapter = workoutsAdapter
            addItemDecoration(DividerItemDecoration(this@WorkoutListActivity, false))
        }

        // Theoretically, setting fitsSystemWindows on the CoordinatorLayout and appropriate children should work.
        // In practice, it does not apply correctly to its RecyclerView children, breaking the scrolling AppBar behavior too.
        // So we do things by hand.
        findViewById<View>(R.id.root).setOnApplyWindowInsetsListener { v, windowInsets ->
            v.setOnApplyWindowInsetsListener(null)

            val systemWindowInsetsRelative = windowInsets.systemWindowInsetsRelative(v)

            toolbar.updatePadding { oldPadding -> oldPadding + systemWindowInsetsRelative.copy(bottom = 0) }
            toolbar.updateHeight { oldHeight -> oldHeight + windowInsets.systemWindowInsetTop }

            fab.updateMargins { oldMargins -> oldMargins + systemWindowInsetsRelative.copy(top = 0) }

            workoutList.updatePadding { oldPadding -> oldPadding + systemWindowInsetsRelative.copy(top = 0) }

            windowInsets
        }

        if (savedInstanceState != null) {
            idToSelect = savedInstanceState.getLong(KEY_SHOW_WORKOUT_ID, NO_ID)
            if (idToSelect == NO_ID) {
                idToSelect = savedInstanceState.getLong(KEY_SELECTED_ITEM_ID, NO_ID)
            }
        } else {
            idToSelect = FIRST_ITEM_IF_EXISTS
        }

        applyIntentContents()

        loaderManager.initLoader(0, null, this)
        Timber.d("onCreate finished")
        // TODO: check/request notification permission
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntentContents()
        Timber.d("onNewIntent: intent read, idToSelect=$idToSelect")
    }

    private fun Intent.idToSelect(): Long? =
            data?.let {
                try {
                    QuickFitContentProvider.getWorkoutIdFromUriOrThrow(it)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }

    private fun applyIntentContents() {
        intent.idToSelect().let { idFromIntent ->
            if (idFromIntent != null) {
                idToSelect = idFromIntent
            }
        }

        if (intent.hasExtra(EXTRA_NOTIFICATIONS_CANCEL_INTENT)) {
            val cancelIntent = intent.getParcelableExtra<PendingIntent>(EXTRA_NOTIFICATIONS_CANCEL_INTENT)
            try {
                cancelIntent!!.send()
            } catch (e: PendingIntent.CanceledException) {
                // intent is already canceled, do nothing
                Timber.d("sending notification cancel intent failed: pending intent already canceled")
            }

        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        with(outState) {
            putLong(KEY_SHOW_WORKOUT_ID, idToSelect)
            putLong(KEY_SELECTED_ITEM_ID, workoutsAdapter.selectedItemId)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_workout_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(baseContext, SettingsActivity::class.java))
                return true
            }
            R.id.action_about -> {
                startActivity(Intent(baseContext, AboutActivity::class.java))
                return true
            }
            R.id.action_privacy -> {
                startActivity(Intent(Intent.ACTION_VIEW, parse("https://lambdasoup.com/privacypolicy-quickfit/")))
                return true
            }
            else ->
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        Timber.d("creating loader")
        return WorkoutListLoader(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        Timber.d("onLoadFinished, idToSelect=$idToSelect")
        workoutsAdapter.swapCursor(data)

        if (idToSelect == FIRST_ITEM_IF_EXISTS) {
            idToSelect = workoutsAdapter.getItemId(0)
        }

        if (idToSelect != NO_ID) {
            val pos = workoutsAdapter.getPosition(idToSelect)
            if (pos != SortedList.INVALID_POSITION) {
                workoutsAdapter.selectedItemId = idToSelect
                Timber.d("going to scroll to pos $pos - recyclerView.paddingBottom = ${workoutList.paddingBottom}")
                workoutList.smoothScrollToPosition(pos)
                Timber.d("scrolled")
            }
            idToSelect = NO_ID
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onLoaderReset(loader: Loader<Cursor>) {
        Timber.d("onLoaderReset")
        workoutsAdapter.swapCursor(null)
    }

    private fun setMiniFabOffsets() {
        val fabSize = resources.getDimensionPixelSize(R.dimen.design_lib_fab_size_normal)
        val miniFabSize = resources.getDimensionPixelSize(R.dimen.design_lib_fab_size_mini)
        val separation = resources.getDimensionPixelSize(R.dimen.list_item_height) - miniFabSize

        offsetFabAddWorkout = fabSize.toFloat() / 2.0f + separation.toFloat() + miniFabSize.toFloat() / 2.0f
        offsetFabAddSchedule = offsetFabAddWorkout + miniFabSize.toFloat() + separation.toFloat()
    }

    private fun showMiniFabs() {
        if (twoPanes == null) {
            return
        }

        if (fab.isActivated) {
            Timber.d("fab is already in activated state: ignoring.")
            return
        }
        fab.isActivated = true

        with(fabAddSchedule) {
            (this as View).visibility = View.VISIBLE
            animate()
                    .setDuration(fabAnimationDuration.toLong())
                    .translationY(-offsetFabAddSchedule)
        }
        with(fabAddWorkout) {
            (this as View).visibility = View.VISIBLE
            animate()
                    .setDuration(fabAnimationDuration.toLong())
                    .translationY(-offsetFabAddWorkout)
        }
        fabBackgroundToActivated.start()
        fab.setOnClickListener { hideMiniFabs() }
    }


    private fun hideMiniFabs() {
        if (twoPanes == null) {
            return
        }

        if (!fab.isActivated) {
            Timber.d("fab is already in deactivated state: ignoring.")
            return
        }

        fab.isActivated = false

        fabAddSchedule!!.animate()
                .setDuration(fabAnimationDuration.toLong())
                .translationY(0f)
                .withEndAction { (fabAddSchedule!! as View).visibility = View.GONE }
        fabAddWorkout!!.animate()
                .setDuration(fabAnimationDuration.toLong())
                .translationY(0f)
                .withEndAction { (fabAddWorkout!! as View).visibility = View.GONE }
        fabBackgroundToNotActivated.start()
        fab.setOnClickListener { showMiniFabs() }
    }

    private fun addNewWorkout() {
        val newWorkoutUri = contentResolver.insert(
                QuickFitContentProvider.getUriWorkoutsList(),
                ContentValues().apply {
                    put(WorkoutEntry.COL_ACTIVITY_TYPE, FitnessActivities.AEROBICS)
                    put(WorkoutEntry.COL_DURATION_MINUTES, 30)
                }
        )
        idToSelect = ContentUris.parseId(newWorkoutUri!!)
        hideMiniFabs()
    }

    private fun addNewSchedule() {
        val schedulesFragment = supportFragmentManager.findFragmentById(R.id.schedules_container) as SchedulesFragment?
        if (schedulesFragment != null) {
            schedulesFragment.onAddNewSchedule()
        } else {
            Timber.d("cannot add new schedule: no schedulesFragment attached")
        }
        hideMiniFabs()
    }

    private fun ensureSchedulesPaneShown(workoutId: Long) {
        if (twoPanes == null) {
            return
        }

        Timber.d("ensuring schedules pane is shown for workoutId %d", workoutId)
        if (!twoPanes!!.isShowDetailsPane) {
            showSchedulesPane(workoutId)
        }

    }

    private fun showSchedulesPane(workoutId: Long) {
        if (twoPanes == null) {
            Timber.wtf("showSchedulesPane called despite not in two-pane layout mode")
            return
        }

        fab.layoutParams = (fab.layoutParams as CoordinatorLayout.LayoutParams).apply {
            anchorId = R.id.list_pane
            anchorGravity = Gravity.BOTTOM or Gravity.END
            gravity = Gravity.CENTER_HORIZONTAL
        }

        fab.setOnClickListener { showMiniFabs() }

        twoPanes!!.requestShowDetail()

        val newFragment = SchedulesFragment.create(workoutId)
        supportFragmentManager
                .beginTransaction()
                .add(R.id.schedules_container, newFragment)
                .commit()
    }

    private fun hideSchedulesPane() {
        if (twoPanes == null) {
            Timber.wtf("hideSchedulesPane called despite not in two-pane layout mode")
            return
        }

        hideMiniFabs()
        fab.setOnClickListener { addNewWorkout() }

        twoPanes!!.requestHideDetail()

    }

    override fun onItemSelected(workoutId: Long) {
        Timber.d("item selected. Id is %d", workoutId)
        if (workoutId == NO_ID) {
            hideSchedulesPane()
        } else {
            ensureSchedulesPaneShown(workoutId)
            updateSchedulesPane(workoutId)
        }
    }

    private fun updateSchedulesPane(workoutId: Long) {
        if (twoPanes == null) {
            return
        }
        val fragment = supportFragmentManager.findFragmentById(R.id.schedules_container) as SchedulesFragment?
        if (workoutId != NO_ID && (fragment == null || workoutId != fragment.workoutId)) {
            val newFragment = SchedulesFragment.create(workoutId)
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.schedules_container, newFragment)
                    .commit()
        }
    }

    override fun onDoneItClick(workoutId: Long) {
        FitActivityService.enqueueInsertSession(applicationContext, workoutId)
    }

    override fun onDeleteClick(workoutId: Long) {
        workoutsAdapter.setSelectedItemIdAfterDeletionOf(workoutId)
        contentResolver.delete(QuickFitContentProvider.getUriWorkoutsId(workoutId), null, null)
    }

    override fun onSchedulesEditRequested(workoutId: Long) {
        if (twoPanes != null) {
            Timber.wtf("onSchedulesEditRequested despite in two-pane layout mode")
            return
        }
        // track item selection so that if we return from SchedulesActivity, this item is
        // selected, and in case we are in a two-pane layout then, this item's schedule is shown
        workoutsAdapter.selectedItemId = workoutId

        startActivity(
                Intent(applicationContext, SchedulesActivity::class.java)
                        .putExtra(SchedulesActivity.EXTRA_WORKOUT_ID, workoutId)
        )
    }

    override fun onActivityTypeEditRequested(workoutId: Long, oldValue: FitActivity) {
        showDialog(ActivityTypeDialogFragment.newInstance(workoutId, oldValue))
    }

    override fun onActivityTypeChanged(workoutId: Long, newActivityTypeKey: String) {
        contentResolver.update(
                QuickFitContentProvider.getUriWorkoutsId(workoutId),
                ContentValues().apply {
                    put(WorkoutEntry.COL_ACTIVITY_TYPE, newActivityTypeKey)
                },
                null,
                null
        )
    }

    override fun onDurationMinsEditRequested(workoutId: Long, oldValue: Int) {
        showDialog(DurationMinutesDialogFragment.newInstance(workoutId, oldValue))
    }

    override fun onDurationChanged(workoutId: Long, newValue: Int) {
        contentResolver.update(
                QuickFitContentProvider.getUriWorkoutsId(workoutId),
                ContentValues().apply {
                    put(WorkoutEntry.COL_DURATION_MINUTES, newValue)
                },
                null,
                null
        )
    }

    override fun onLabelEditRequested(workoutId: Long, oldValue: String?) {
        showDialog(LabelDialogFragment.newInstance(workoutId, oldValue))
    }

    override fun onLabelChanged(workoutId: Long, newValue: String?) {
        contentResolver.update(
                QuickFitContentProvider.getUriWorkoutsId(workoutId),
                ContentValues().apply {
                    put(WorkoutEntry.COL_LABEL, newValue)
                },
                null,
                null
        )
    }

    override fun onCaloriesEditRequested(workoutId: Long, oldValue: Int) {
        showDialog(CaloriesDialogFragment.newInstance(workoutId, oldValue))
    }

    override fun onCaloriesChanged(workoutId: Long, newValue: Int) {
        contentResolver.update(
                QuickFitContentProvider.getUriWorkoutsId(workoutId),
                ContentValues().apply {
                    put(WorkoutEntry.COL_CALORIES, newValue)
                },
                null,
                null
        )
    }

    override fun getOnTimeDialogFragmentInteractionListener(): TimeDialogFragment.OnFragmentInteractionListener {
        return supportFragmentManager.findFragmentById(R.id.schedules_container) as TimeDialogFragment.OnFragmentInteractionListener
    }

    override fun getOnDayOfWeekDialogFragmentInteractionListener(): DayOfWeekDialogFragment.OnFragmentInteractionListener {
        return supportFragmentManager.findFragmentById(R.id.schedules_container) as DayOfWeekDialogFragment.OnFragmentInteractionListener
    }

    companion object {

        const val EXTRA_NOTIFICATIONS_CANCEL_INTENT = "com.lambdasoup.quickfit.cancel_intent"
        const val EXTRA_SHOW_WORKOUT_ID = "com.lambdasoup.quickfit.show_workout_id"

        private const val KEY_SHOW_WORKOUT_ID = "com.lambdasoup.quickfit.show_workout_id"
        private const val KEY_SELECTED_ITEM_ID = "com.lambdasoup.quickfit.WorkoutListActivity_selected_item_id"
        private const val FIRST_ITEM_IF_EXISTS: Long = -2
    }
}
