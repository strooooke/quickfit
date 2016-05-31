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

import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.lambdasoup.quickfit.databinding.ScheduleListContentBinding;
import com.lambdasoup.quickfit.model.DayOfWeek;
import com.lambdasoup.quickfit.persist.QuickFitContract;
import com.lambdasoup.quickfit.util.ui.LeaveBehind;
import com.lambdasoup.quickfit.viewmodel.ScheduleItem;
import com.lambdasoup.quickfit.viewmodel.ScheduleList;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerViewAdapter for {@link ScheduleItem}; with item set change animations and
 * swipe-dismiss support for items.
 */
public class SchedulesRecyclerViewAdapter extends RecyclerView.Adapter<SchedulesRecyclerViewAdapter.ViewHolder> {

    private ScheduleList dataset;
    private OnScheduleInteractionListener onScheduleInteractionListener;

    public SchedulesRecyclerViewAdapter() {
        dataset = new ScheduleList(new ScheduleList.ItemChangeCallback() {
            @Override
            public void onInserted(int position) {
                notifyItemInserted(position);
            }

            @Override
            public void onRemoved(int position) {
                notifyItemRemoved(position);
            }

            @Override
            public void onUpdated(int position) {
                notifyItemChanged(position);
            }

            @Override
            public void onMoved(int from, int to) {
                notifyItemMoved(from, to);
            }

            @Override
            public void onCleared() {
                notifyItemRangeRemoved(0, dataset.size());
            }
        });
        setHasStableIds(true);


    }

    @Override
    public long getItemId(int position) {
        if (position >= 0 && position < dataset.size()) {
            return dataset.get(position).id;
        }
        return RecyclerView.NO_ID;
    }

    public void setOnScheduleInteractionListener(OnScheduleInteractionListener onScheduleInteractionListener) {
        this.onScheduleInteractionListener = onScheduleInteractionListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ScheduleListContentBinding binding = ScheduleListContentBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
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
        List<ScheduleItem> newItems = new ArrayList<>(cursor.getCount());
        while (cursor.moveToNext()) {
            ScheduleItem newScheduleItem = new ScheduleItem.Builder()
                    .withScheduleId(cursor.getLong(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.SCHEDULE_ID)))
                    .withHour(cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.HOUR)))
                    .withMinute(cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.MINUTE)))
                    .withDayOfWeekName(cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.DAY_OF_WEEK)))
                    .build();

            newItems.add(newScheduleItem);
        }
        dataset.swapData(newItems);
    }

    public ScheduleItem getById(long scheduleId) {
        return dataset.getById(scheduleId);
    }

    public interface OnScheduleInteractionListener {

        void onTimeEditRequested(long scheduleId, int oldHour, int oldMinute);

        void onDayOfWeekEditRequested(long scheduleId, DayOfWeek dayOfWeek);
    }

    public class ViewHolder extends LeaveBehind.LeaveBehindViewHolder {
        private final ScheduleListContentBinding binding;
        private final EventHandler eventHandler;
        private ScheduleItem item;


        ViewHolder(ScheduleListContentBinding binding) {
            super(binding.getRoot(), binding.listItem, binding.leaveBehindEnd, binding.leaveBehindStart);
            this.binding = binding;
            this.eventHandler = new EventHandler(this);
            binding.setHandler(eventHandler);
        }

        void bindItem(ScheduleItem item) {
            this.item = item;
            binding.setSchedule(item);
        }
    }

    @SuppressWarnings("unused") // members get used by databinding expressions
    public class EventHandler {
        private final ViewHolder viewHolder;

        public final View.OnClickListener dayOfWeekClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onScheduleInteractionListener != null) {
                    onScheduleInteractionListener.onDayOfWeekEditRequested(viewHolder.item.id, viewHolder.item.dayOfWeek);
                }
            }
        };

        public final View.OnClickListener timeClicked = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onScheduleInteractionListener != null) {
                    onScheduleInteractionListener.onTimeEditRequested(viewHolder.item.id, viewHolder.item.hour, viewHolder.item.minute);
                }
            }
        };

        EventHandler(ViewHolder viewHolder) {
            this.viewHolder = viewHolder;
        }
    }
}
