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

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import androidx.recyclerview.widget.SortedList
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.lambdasoup.quickfit.databinding.WorkoutListContentBinding
import com.lambdasoup.quickfit.model.DayOfWeek
import com.lambdasoup.quickfit.model.FitActivity
import com.lambdasoup.quickfit.persist.QuickFitContract.WorkoutEntry
import com.lambdasoup.quickfit.viewmodel.ScheduleItem
import com.lambdasoup.quickfit.viewmodel.WorkoutItem

import java.util.ArrayList
import java.util.HashSet
import java.util.Objects

import timber.log.Timber

import androidx.recyclerview.widget.RecyclerView.NO_ID
import com.lambdasoup.quickfit.util.Lists.map

class WorkoutItemRecyclerViewAdapter(
        private val context: Context,
        private val isTwoPane: Boolean
) : RecyclerView.Adapter<WorkoutItemRecyclerViewAdapter.ViewHolder>() {

    private val dataset: SortedList<WorkoutItem> = SortedList(WorkoutItem::class.java, object : SortedList.Callback<WorkoutItem>() {
        override fun compare(left: WorkoutItem, right: WorkoutItem): Int {
            return left.id.compareTo(right.id)
        }

        override fun areContentsTheSame(oldItem: WorkoutItem, newItem: WorkoutItem): Boolean {
            return (oldItem.activityType == newItem.activityType
                    && oldItem.durationInMinutes == newItem.durationInMinutes
                    && oldItem.label == newItem.label
                    && oldItem.calories == newItem.calories
                    && oldItem.scheduleDisplay == newItem.scheduleDisplay)
        }

        override fun areItemsTheSame(item1: WorkoutItem, item2: WorkoutItem): Boolean {
            return item1.id == item2.id
        }

        override fun onInserted(position: Int, count: Int) {
            notifyItemRangeInserted(position, count)
        }

        override fun onRemoved(position: Int, count: Int) {
            notifyItemRangeRemoved(position, count)
        }

        override fun onMoved(fromPosition: Int, toPosition: Int) {
            notifyItemMoved(fromPosition, toPosition)
        }

        override fun onChanged(position: Int, count: Int) {
            notifyItemRangeChanged(position, count)
        }
    })

    private var onWorkoutInteractionListener: OnWorkoutInteractionListener? = null
    internal var selectedItemId = NO_ID
        set(selectedItemId) {
            if (selectedItemId == this.selectedItemId) {
                Timber.d("reselecting already selected item, ignoring")
                return
            }

            val previousSelectedItemId = this.selectedItemId
            field = selectedItemId

            Timber.d("setSelectedItemId previous: %d new: %d", previousSelectedItemId, selectedItemId)
            notifyItemChanged(getPosition(previousSelectedItemId))
            notifyItemChanged(getPosition(selectedItemId))

            if (onWorkoutInteractionListener != null) {
                onWorkoutInteractionListener!!.onItemSelected(selectedItemId)
            }
        }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return if (position >= 0 && position < dataset.size()) {
            dataset.get(position).id
        } else NO_ID
    }

    fun setOnWorkoutInteractionListener(onWorkoutInteractionListener: OnWorkoutInteractionListener) {
        this.onWorkoutInteractionListener = onWorkoutInteractionListener
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = WorkoutListContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(dataset.get(position))
    }

    override fun getItemCount(): Int {
        return dataset.size()
    }

    @SuppressLint("Range")
    fun swapCursor(cursor: Cursor?) {
        if (cursor == null) {
            dataset.clear()
            return
        }

        cursor.moveToPosition(-1)

        val week = DayOfWeek.getWeek()

        val newIds = HashSet<Long>()
        val newItems = ArrayList<WorkoutItem.Builder>()
        var prevId: Long = -1
        while (cursor.moveToNext()) {
            val workoutId = cursor.getLong(cursor.getColumnIndex(WorkoutEntry.WORKOUT_ID))
            if (workoutId != prevId) {
                // next workout, start new item
                val newItem = WorkoutItem.Builder(context)
                        .withWorkoutId(workoutId)
                        .withActivityTypeKey(cursor.getString(cursor.getColumnIndex(WorkoutEntry.ACTIVITY_TYPE)))
                        .withDurationInMinutes(cursor.getInt(cursor.getColumnIndex(WorkoutEntry.DURATION_MINUTES)))
                        .withCalories(cursor.getInt(cursor.getColumnIndex(WorkoutEntry.CALORIES)))
                        .withLabel(cursor.getString(cursor.getColumnIndex(WorkoutEntry.LABEL)))

                newIds.add(workoutId)
                newItems.add(newItem)
                prevId = workoutId
            }

            if (!cursor.isNull(cursor.getColumnIndex(WorkoutEntry.SCHEDULE_ID))) {
                // more schedule data for current workout item
                val currentWorkout = newItems[newItems.size - 1]

                val newScheduleItem = ScheduleItem.Builder()
                        .withScheduleId(cursor.getLong(cursor.getColumnIndex(WorkoutEntry.SCHEDULE_ID)))
                        .withHour(cursor.getInt(cursor.getColumnIndex(WorkoutEntry.HOUR)))
                        .withMinute(cursor.getInt(cursor.getColumnIndex(WorkoutEntry.MINUTE)))
                        .withDayOfWeekName(cursor.getString(cursor.getColumnIndex(WorkoutEntry.DAY_OF_WEEK)))

                currentWorkout.addSchedule(newScheduleItem.build())
            }
        }

        dataset.beginBatchedUpdates()
        dataset.addAll(map<WorkoutItem.Builder, WorkoutItem>(newItems) { newItem -> newItem.build(week) })
        for (i in dataset.size() - 1 downTo 0) {
            if (!newIds.contains(dataset.get(i).id)) {
                dataset.removeItemAt(i)
            }
        }
        dataset.endBatchedUpdates()
    }

    fun getPosition(id: Long): Int {
        // hack to make SortedList find the position of the item with the given id
        // depends on dataset being ordered and equaled by id
        return dataset.indexOf(WorkoutItem.getForIdHack(id))
    }

    fun setSelectedItemIdAfterDeletionOf(itemIdToDelete: Long) {
        selectedItemId = getSelectedItemIdAfterDeletion(itemIdToDelete)
    }

    private fun getSelectedItemIdAfterDeletion(itemIdToDelete: Long): Long {
        if (itemIdToDelete != this.selectedItemId) {
            return this.selectedItemId
        }
        val deletePos = getPosition(itemIdToDelete)
        if (deletePos == SortedList.INVALID_POSITION) {
            return NO_ID
        }
        return if (deletePos == 0) {
            if (itemCount == 1) {
                // list will be empty after this deletion
                NO_ID
            } else {
                getItemId(1)
            }
        } else getItemId(deletePos - 1)
    }


    interface OnWorkoutInteractionListener {
        fun onDoneItClick(workoutId: Long)

        fun onDurationMinsEditRequested(workoutId: Long, oldValue: Int)

        fun onLabelEditRequested(workoutId: Long, oldValue: String?)

        fun onCaloriesEditRequested(workoutId: Long, oldValue: Int)

        fun onActivityTypeEditRequested(workoutId: Long, oldValue: FitActivity)

        fun onDeleteClick(workoutId: Long)

        fun onSchedulesEditRequested(workoutId: Long)

        fun onItemSelected(workoutId: Long)
    }

    inner class ViewHolder internal constructor(private val binding: WorkoutListContentBinding) : RecyclerView.ViewHolder(binding.root) {
        private val eventHandler: EventHandler = EventHandler(this)
        internal var item: WorkoutItem? = null

        init {
            binding.handler = eventHandler

            if (isTwoPane) {
                binding.schedules.visibility = View.GONE
            }
        }

        internal fun bindItem(item: WorkoutItem) {
            Timber.d("binding item with id %d to viewholder", item.id)
            this.item = item
            binding.workout = item
            binding.root.isActivated = item.id == selectedItemId
        }

        internal fun onItemClicked() {
            Timber.d("viewholder onItemCLicked selectedItemId: %d, clicked item id: %d", selectedItemId, item!!.id)
            if (selectedItemId != item!!.id) {
                selectedItemId = item!!.id
            }
        }

    }

    // members get used by databinding expressions
    inner class EventHandler internal constructor(private val viewHolder: ViewHolder) {

        val listItemClickListener: View.OnClickListener = View.OnClickListener {
            viewHolder.onItemClicked()
        }

        val activityTypeClicked: View.OnClickListener = View.OnClickListener {
            onWorkoutInteractionListener?.onActivityTypeEditRequested(viewHolder.item!!.id, viewHolder.item!!.activityType)
        }

        val doneItButtonClicked: View.OnClickListener = View.OnClickListener {
            onWorkoutInteractionListener?.onDoneItClick(viewHolder.item!!.id)
        }

        val durationMinsClicked: View.OnClickListener = View.OnClickListener {
            onWorkoutInteractionListener?.onDurationMinsEditRequested(viewHolder.item!!.id, viewHolder.item!!.durationInMinutes)
        }

        val labelClicked: View.OnClickListener = View.OnClickListener {
            onWorkoutInteractionListener?.onLabelEditRequested(viewHolder.item!!.id, viewHolder.item!!.label)
        }

        val schedulesClicked: View.OnClickListener = View.OnClickListener {
            onWorkoutInteractionListener?.onSchedulesEditRequested(viewHolder.item!!.id)
        }

        val caloriesClicked: View.OnClickListener = View.OnClickListener {
            onWorkoutInteractionListener?.onCaloriesEditRequested(viewHolder.item!!.id, viewHolder.item!!.calories)
        }

        val deleteButtonClicked: View.OnClickListener = View.OnClickListener {
            onWorkoutInteractionListener?.onDeleteClick(viewHolder.item!!.id)
        }
    }


}
