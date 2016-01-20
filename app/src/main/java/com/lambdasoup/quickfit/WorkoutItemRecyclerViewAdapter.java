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

import android.database.Cursor;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by jl on 06.01.16.
 */
public class WorkoutItemRecyclerViewAdapter
        extends RecyclerView.Adapter<WorkoutItemRecyclerViewAdapter.ViewHolder> {

    private static final String TAG = WorkoutItemRecyclerViewAdapter.class.getSimpleName();
    private WorkoutListActivity workoutListActivity;

    private final SortedList<WorkoutItem> dataset;
    private OnItemClickListener editClickListener;
    private OnItemClickListener insertSessionClickListener;


    public WorkoutItemRecyclerViewAdapter(WorkoutListActivity workoutListActivity) {
        this.workoutListActivity = workoutListActivity;
        dataset = new SortedList<>(WorkoutItem.class, new SortedList.Callback<WorkoutItem>() {
            @Override
            public int compare(WorkoutItem left, WorkoutItem right) {
                Log.d(TAG, "compare: " + left + ", " + right);
                int res = String.CASE_INSENSITIVE_ORDER.compare(left.activityName, right.activityName);
                if (res != 0) {
                    return res;
                }
                res = Integer.compare(left.durationMins, right.durationMins);
                if (res != 0) {
                    return res;
                }
                res = Long.compare(left.id, right.id);
                return res;
            }

            @Override
            public boolean areContentsTheSame(WorkoutItem oldItem, WorkoutItem newItem) {
                Log.d(TAG, "areContentsTheSame: " + oldItem + ", " + newItem);
                return oldItem.activityName.equals(newItem.activityName)
                        && oldItem.durationMins == newItem.durationMins;
            }

            @Override
            public boolean areItemsTheSame(WorkoutItem item1, WorkoutItem item2) {
                Log.d(TAG, "areItemsTheSame: " + item1 + ", " + item2);
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

    public void setOnEditClickListener(final OnItemClickListener listener) {
        this.editClickListener = listener;
    }

    public void setInsertSessionClickListener(final OnItemClickListener listener) {
        this.insertSessionClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.workout_list_content, parent, false);
        return new ViewHolder(view);
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

        Map<Long, Integer> oldIds = new HashMap<>();
        for (int i = 0; i < dataset.size(); i++) {
            oldIds.put(dataset.get(i).id, i);
        }

        Set<Long> newIds = new HashSet<>();
        dataset.beginBatchedUpdates();
        while (cursor.moveToNext()) {
            WorkoutItem newItem = new WorkoutItem(
                    cursor.getLong(cursor.getColumnIndex(QuickFitContract.WorkoutEntry._ID)),
                    FitActivity.fromKey(cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.ACTIVITY_TYPE)),
                            workoutListActivity.getResources()).displayName,
                    cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.DURATION_MINUTES)));
            newIds.add(newItem.id);
            if (oldIds.containsKey(newItem.id)) {
                dataset.updateItemAt(oldIds.get(newItem.id), newItem);
            } else {
                dataset.add(newItem);
            }
        }

        for (int i = dataset.size() - 1; i >= 0; i--) {
            if (!newIds.contains(dataset.get(i).id)) {
                dataset.removeItemAt(i);
            }
        }
        dataset.endBatchedUpdates();
    }

    private static <T> String sortedSetToString(SortedList<T> dataset) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < dataset.size(); i++) {
            sb.append("\n\t");
            sb.append(dataset.get(i).toString());
        }
        return sb.toString();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final View view;
        public final TextView idView;
        public final TextView activityTypeView;
        public final TextView durationView;
        public final Button editButton;
        public final Button insertSessionButton;
        public WorkoutItem item;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            idView = (TextView) view.findViewById(R.id.id);
            activityTypeView = (TextView) view.findViewById(R.id.activity_type);
            durationView = (TextView) view.findViewById(R.id.duration_mins);
            editButton = (Button) view.findViewById(R.id.edit_workout_button);
            insertSessionButton = (Button) view.findViewById(R.id.insert_session_button);
        }

        void bindItem(WorkoutItem item) {
            this.item = item;
            idView.setText(item.id + "");
            activityTypeView.setText(item.activityName);
            durationView.setText(workoutListActivity.getResources().getString(R.string.duration_mins_compact, item.durationMins));

            editButton.setOnClickListener(this);
            insertSessionButton.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v == editButton && editClickListener != null) {
                int pos = getAdapterPosition();
                editClickListener.onItemClick(dataset.get(pos).id);
            }
            if (v == insertSessionButton && insertSessionClickListener != null) {
                int pos = getAdapterPosition();
                insertSessionClickListener.onItemClick(dataset.get(pos).id);
            }
        }

        @Override
        public String toString() {
            return super.toString() + " '" + activityTypeView.getText() + "'";
        }

    }


    public interface OnItemClickListener {
        void onItemClick(long workoutId);
    }

    private static class WorkoutItem {

        final long id;
        final String activityName;
        final int durationMins;

        public WorkoutItem(long id, String activityName, int durationMins) {
            this.id = id;
            this.activityName = activityName;
            this.durationMins = durationMins;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("WorkoutItem{");
            sb.append("id=").append(id);
            sb.append(", activityName='").append(activityName).append('\'');
            sb.append(", durationMins=").append(durationMins);
            sb.append('}');
            return sb.toString();
        }
    }
}
