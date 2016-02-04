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

import android.content.Context;
import android.database.Cursor;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import com.lambdasoup.quickfit.databinding.WorkoutListContentBinding;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class WorkoutItemRecyclerViewAdapter
        extends RecyclerView.Adapter<WorkoutItemRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = WorkoutItemRecyclerViewAdapter.class.getSimpleName();
    private final Context context;

    private final SortedList<WorkoutItem> dataset;

    private OnWorkoutInteractionListener onWorkoutInteractionListener;

    private final ArrayAdapter<FitActivity> activityTypesAdapter;

    public WorkoutItemRecyclerViewAdapter(Context context) {
        this.context = context;
        dataset = new SortedList<>(WorkoutItem.class, new SortedList.Callback<WorkoutItem>() {
            @Override
            public int compare(WorkoutItem left, WorkoutItem right) {
                return Long.compare(left.id, right.id);
            }

            @Override
            public boolean areContentsTheSame(WorkoutItem oldItem, WorkoutItem newItem) {
                return oldItem.activityTypeDisplayName.equals(newItem.activityTypeDisplayName)
                        && oldItem.durationInMinutes == newItem.durationInMinutes
                        && Objects.equals(oldItem.label, newItem.label)
                        && oldItem.calories == newItem.calories;
            }

            @Override
            public boolean areItemsTheSame(WorkoutItem item1, WorkoutItem item2) {
                return item1.id == item2.id;
            }

            @Override
            public void onInserted(int position, int count) {
                notifyItemRangeInserted(position, count);
            }

            @Override
            public void onRemoved(int position, int count) {
                notifyItemRangeRemoved(position, count);
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                notifyItemMoved(fromPosition, toPosition);
            }

            @Override
            public void onChanged(int position, int count) {
                notifyItemRangeChanged(position, count);
            }
        });
        setHasStableIds(true);
        activityTypesAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, FitActivity.all(context.getResources()));
        activityTypesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activityTypesAdapter.sort((left, right) -> left.displayName.compareToIgnoreCase(right.displayName));
    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < dataset.size()) {
            return dataset.get(position).id;
        }
        return RecyclerView.NO_ID;
    }

    public void setOnWorkoutInteractionListener(OnWorkoutInteractionListener onWorkoutInteractionListener) {
        this.onWorkoutInteractionListener = onWorkoutInteractionListener;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        WorkoutListContentBinding binding = WorkoutListContentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        holder.bindItem(dataset.get(position));
    }

    @Override
    public int getItemCount() {
        return dataset.size();
    }

    public void swapCursor(Cursor cursor) {
        if (cursor == null) {
            dataset.clear();
            return;
        }

        cursor.moveToPosition(-1);

        Set<Long> newIds = new HashSet<>();
        WorkoutItem[] newItems = new WorkoutItem[cursor.getCount()];
        while (cursor.moveToNext()) {
            FitActivity fitActivity = FitActivity.fromKey(cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.ACTIVITY_TYPE)),
                    context.getResources());
            WorkoutItem newItem = new WorkoutItem(
                    cursor.getLong(cursor.getColumnIndex(QuickFitContract.WorkoutEntry._ID)),
                    activityTypesAdapter.getPosition(fitActivity),
                    fitActivity.displayName, cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.DURATION_MINUTES)),
                    cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.CALORIES)),
                    cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.LABEL))
            );
            newIds.add(newItem.id);
            newItems[cursor.getPosition()] = newItem;
        }

        dataset.beginBatchedUpdates();
        dataset.addAll(newItems, true);
        for (int i = dataset.size() - 1; i >= 0; i--) {
            if (!newIds.contains(dataset.get(i).id)) {
                dataset.removeItemAt(i);
            }
        }
        dataset.endBatchedUpdates();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {

        private WorkoutItem item;
        private final WorkoutListContentBinding binding;
        private final EventHandler activeEventHandler;

        public ViewHolder(WorkoutListContentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.activeEventHandler = new EventHandler(this);
            binding.activityTypeSpinner.setAdapter(activityTypesAdapter);
            binding.setHandler(activeEventHandler);
        }

        void bindItem(WorkoutItem item) {
            this.item = item;
            binding.setWorkout(item);
        }

    }

    @SuppressWarnings("unused") // members get used by databinding expressions
    public class EventHandler {
        private final ViewHolder viewHolder;

        public final AdapterView.OnItemSelectedListener activityTypeSpinnerItemSelected = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "ActivityType item selected at position " + position);
                if (viewHolder.item.activityTypeIndex == position) {
                    // do not update database if view state is already identical to model
                    // e.g. if selection event originates from data bind
                    Log.d(TAG, "ignoring itemSelection event where new position identical to model");
                    return;
                }
                if (onWorkoutInteractionListener != null) {
                    FitActivity activityType = activityTypesAdapter.getItem(position);
                    onWorkoutInteractionListener.onActivityTypeChanged(viewHolder.item.id, activityType.key);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        public final View.OnClickListener doneItButtonClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWorkoutInteractionListener != null) {
                    onWorkoutInteractionListener.onDoneItClick(viewHolder.item.id);
                }
            }
        };

        public final View.OnClickListener durationMinsClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWorkoutInteractionListener != null) {
                    onWorkoutInteractionListener.onDurationMinsEditRequested(viewHolder.item.id, viewHolder.item.durationInMinutes);
                }
            }
        };

        public final View.OnClickListener labelClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWorkoutInteractionListener != null) {
                    onWorkoutInteractionListener.onLabelEditRequested(viewHolder.item.id, viewHolder.item.label);
                }
            }
        };

        public final View.OnClickListener caloriesClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWorkoutInteractionListener != null) {
                    onWorkoutInteractionListener.onCaloriesEditRequested(viewHolder.item.id, viewHolder.item.calories);
                }
            }
        };

        public final View.OnClickListener deleteButtonClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWorkoutInteractionListener != null) {
                    onWorkoutInteractionListener.onDeleteClick(viewHolder.item.id);
                }
            }
        };

        EventHandler(ViewHolder viewHolder) {
            this.viewHolder = viewHolder;
        }
    }

    public interface OnWorkoutInteractionListener {
        void onDoneItClick(long workoutId);

        void onActivityTypeChanged(long workoutId, String newActivityTypeKey);

        void onDurationMinsEditRequested(long workoutId, int oldValue);

        void onLabelEditRequested(long workoutId, String oldValue);

        void onCaloriesEditRequested(long workoutId, int oldValue);

        void onDeleteClick(long workoutId);
    }


}
