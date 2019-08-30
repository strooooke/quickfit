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

import android.content.Context;
import android.database.Cursor;
import androidx.recyclerview.widget.SortedList;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lambdasoup.quickfit.databinding.WorkoutListContentBinding;
import com.lambdasoup.quickfit.model.DayOfWeek;
import com.lambdasoup.quickfit.model.FitActivity;
import com.lambdasoup.quickfit.persist.QuickFitContract.WorkoutEntry;
import com.lambdasoup.quickfit.viewmodel.ScheduleItem;
import com.lambdasoup.quickfit.viewmodel.WorkoutItem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import timber.log.Timber;

import static androidx.recyclerview.widget.RecyclerView.NO_ID;
import static com.lambdasoup.quickfit.util.Lists.map;

public class WorkoutItemRecyclerViewAdapter
        extends RecyclerView.Adapter<WorkoutItemRecyclerViewAdapter.ViewHolder> {
    private final Context context;

    private final SortedList<WorkoutItem> dataset;
    private final boolean isTwoPane;
    private OnWorkoutInteractionListener onWorkoutInteractionListener;
    private long selectedItemId = NO_ID;

    public WorkoutItemRecyclerViewAdapter(Context context, boolean isTwoPane) {
        this.context = context;
        this.isTwoPane = isTwoPane;
        dataset = new SortedList<>(WorkoutItem.class, new SortedList.Callback<WorkoutItem>() {
            @Override
            public int compare(WorkoutItem left, WorkoutItem right) {
                return Long.compare(left.id, right.id);
            }

            @Override
            public boolean areContentsTheSame(WorkoutItem oldItem, WorkoutItem newItem) {
                return Objects.equals(oldItem.activityType, newItem.activityType)
                        && oldItem.durationInMinutes == newItem.durationInMinutes
                        && Objects.equals(oldItem.label, newItem.label)
                        && oldItem.calories == newItem.calories
                        && oldItem.scheduleDisplay.equals(newItem.scheduleDisplay);
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


    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < dataset.size()) {
            return dataset.get(position).id;
        }
        return NO_ID;
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

        DayOfWeek[] week = DayOfWeek.getWeek();

        Set<Long> newIds = new HashSet<>();
        List<WorkoutItem.Builder> newItems = new ArrayList<>();
        long prevId = -1;
        while (cursor.moveToNext()) {
            long workoutId = cursor.getLong(cursor.getColumnIndex(WorkoutEntry.WORKOUT_ID));
            if (workoutId != prevId) {
                // next workout, start new item
                WorkoutItem.Builder newItem = new WorkoutItem.Builder(context)
                        .withWorkoutId(workoutId)
                        .withActivityTypeKey(cursor.getString(cursor.getColumnIndex(WorkoutEntry.ACTIVITY_TYPE)))
                        .withDurationInMinutes(cursor.getInt(cursor.getColumnIndex(WorkoutEntry.DURATION_MINUTES)))
                        .withCalories(cursor.getInt(cursor.getColumnIndex(WorkoutEntry.CALORIES)))
                        .withLabel(cursor.getString(cursor.getColumnIndex(WorkoutEntry.LABEL)));

                newIds.add(workoutId);
                newItems.add(newItem);
                prevId = workoutId;
            }

            if (!cursor.isNull(cursor.getColumnIndex(WorkoutEntry.SCHEDULE_ID))) {
                // more schedule data for current workout item
                WorkoutItem.Builder currentWorkout = newItems.get(newItems.size() - 1);

                ScheduleItem.Builder newScheduleItem = new ScheduleItem.Builder()
                        .withScheduleId(cursor.getLong(cursor.getColumnIndex(WorkoutEntry.SCHEDULE_ID)))
                        .withHour(cursor.getInt(cursor.getColumnIndex(WorkoutEntry.HOUR)))
                        .withMinute(cursor.getInt(cursor.getColumnIndex(WorkoutEntry.MINUTE)))
                        .withDayOfWeekName(cursor.getString(cursor.getColumnIndex(WorkoutEntry.DAY_OF_WEEK)));

                currentWorkout.addSchedule(newScheduleItem.build());
            }
        }

        dataset.beginBatchedUpdates();
        dataset.addAll(map(newItems, newItem -> newItem.build(week)));
        for (int i = dataset.size() - 1; i >= 0; i--) {
            if (!newIds.contains(dataset.get(i).id)) {
                dataset.removeItemAt(i);
            }
        }
        dataset.endBatchedUpdates();
    }

    public int getPosition(long id) {
        // hack to make SortedList find the position of the item with the given id
        // depends on dataset being ordered and equaled by id
        return dataset.indexOf(WorkoutItem.getForIdHack(id));
    }

    public void setSelectedItemId(long selectedItemId) {
        if (selectedItemId == this.selectedItemId) {
            Timber.d("reselecting already selected item, ignoring");
            return;
        }

        long previousSelectedItemId = this.selectedItemId;
        this.selectedItemId = selectedItemId;

        Timber.d("setSelectedItemId previous: %d new: %d", previousSelectedItemId, selectedItemId);
        notifyItemChanged(getPosition(previousSelectedItemId));
        notifyItemChanged(getPosition(selectedItemId));

        if (onWorkoutInteractionListener != null) {
            onWorkoutInteractionListener.onItemSelected(selectedItemId);
        }
    }


    public void setSelectedItemIdAfterDeletionOf(long itemIdToDelete) {
        setSelectedItemId(getSelectedItemIdAfterDeletion(itemIdToDelete));
    }

    private long getSelectedItemIdAfterDeletion(long itemIdToDelete) {
        if (itemIdToDelete != selectedItemId) {
            return selectedItemId;
        }
        int deletePos = getPosition(itemIdToDelete);
        if (deletePos == SortedList.INVALID_POSITION) {
            return NO_ID;
        }
        if (deletePos == 0) {
            if (getItemCount() == 1) {
                // list will be empty after this deletion
                return NO_ID;
            } else {
                return getItemId(1);
            }
        }
        return getItemId(deletePos - 1);
    }

    public long getSelectedItemId() {
        return selectedItemId;
    }


    public interface OnWorkoutInteractionListener {
        void onDoneItClick(long workoutId);

        void onDurationMinsEditRequested(long workoutId, int oldValue);

        void onLabelEditRequested(long workoutId, String oldValue);

        void onCaloriesEditRequested(long workoutId, int oldValue);

        void onActivityTypeEditRequested(long workoutId, FitActivity oldValue);

        void onDeleteClick(long workoutId);

        void onSchedulesEditRequested(long workoutId);

        void onItemSelected(long workoutId);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final WorkoutListContentBinding binding;
        private final EventHandler eventHandler;
        private WorkoutItem item;

        ViewHolder(WorkoutListContentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.eventHandler = new EventHandler(this);
            binding.setHandler(eventHandler);

            if (isTwoPane) {
                binding.schedules.setVisibility(View.GONE);
            }
        }

        void bindItem(WorkoutItem item) {
            Timber.d("binding item with id %d to viewholder", item.id);
            this.item = item;
            binding.setWorkout(item);
            binding.getRoot().setActivated(item.id == selectedItemId);
        }

        void onItemClicked() {
            Timber.d("viewholder onItemCLicked selectedItemId: %d, clicked item id: %d", selectedItemId, item.id);
            if (selectedItemId != item.id) {
                setSelectedItemId(item.id);
            }
        }

    }

    @SuppressWarnings("unused") // members get used by databinding expressions
    public class EventHandler {
        private final ViewHolder viewHolder;

        public final View.OnClickListener listItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Timber.d("listitemClickListener onclick");
                viewHolder.onItemClicked();
            }
        };

        public final View.OnClickListener activityTypeClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWorkoutInteractionListener != null) {
                    onWorkoutInteractionListener.onActivityTypeEditRequested(viewHolder.item.id, viewHolder.item.activityType);
                }
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

        public final View.OnClickListener schedulesClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onWorkoutInteractionListener != null) {
                    onWorkoutInteractionListener.onSchedulesEditRequested(viewHolder.item.id);
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


}
