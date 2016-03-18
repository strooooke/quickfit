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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.lambdasoup.quickfit.databinding.ScheduleListContentBinding;
import com.lambdasoup.quickfit.model.DayOfWeek;
import com.lambdasoup.quickfit.persist.QuickFitContract;
import com.lambdasoup.quickfit.util.ConstantListAdapter;
import com.lambdasoup.quickfit.viewmodel.ScheduleItem;
import com.lambdasoup.quickfit.viewmodel.ScheduleList;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by jl on 15.03.16.
 */
public class SchedulesRecyclerViewAdapter extends RecyclerView.Adapter<SchedulesRecyclerViewAdapter.ViewHolder> {
    private final Context context;
    private final ConstantListAdapter<DayOfWeek> dayOfWeekAdapter;
    private ScheduleList dataset;
    private OnScheduleInteractionListener onScheduleInteractionListener;

    public SchedulesRecyclerViewAdapter(Context context) {
        this.context = context;
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

        dayOfWeekAdapter = new ConstantListAdapter<>(
                context,
                android.R.layout.simple_spinner_item,
                android.R.layout.simple_spinner_dropdown_item,
                DayOfWeek.getWeek(),
                dayOfWeek -> context.getResources().getString(dayOfWeek.fullNameResId));
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
            ScheduleItem newScheduleItem = new ScheduleItem.Builder(dayOfWeekAdapter::getPosition)
                    .withScheduleId(cursor.getLong(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.SCHEDULE_ID)))
                    .withHour(cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.HOUR)))
                    .withMinute(cursor.getInt(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.MINUTE)))
                    .withDayOfWeekName(cursor.getString(cursor.getColumnIndex(QuickFitContract.WorkoutEntry.DAY_OF_WEEK)))
                    .build();

            newItems.add(newScheduleItem);
        }
        dataset.swapData(newItems);
    }

    public interface OnScheduleInteractionListener {
        void onDayOfWeekChanged(long scheduleId, DayOfWeek newDayOfWeek);

        void onTimeEditRequested(long scheduleId, int oldHour, int oldMinute);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ScheduleListContentBinding binding;
        private final EventHandler eventHandler;
        private ScheduleItem item;

        ViewHolder(ScheduleListContentBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            this.eventHandler = new EventHandler(this);
            binding.dayOfWeek.setAdapter(dayOfWeekAdapter);
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

        public final AdapterView.OnItemSelectedListener dayOfWeekSpinnerItemSelected = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (viewHolder.item.dayOfWeekIndex == position) {
                    // do not update database if view state is already identical to model
                    // e.g. if selection event originates from data bind
                    return;
                }
                if (onScheduleInteractionListener != null) {
                    DayOfWeek dayOfWeek = dayOfWeekAdapter.getItem(position);
                    onScheduleInteractionListener.onDayOfWeekChanged(viewHolder.item.id, dayOfWeek);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
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
