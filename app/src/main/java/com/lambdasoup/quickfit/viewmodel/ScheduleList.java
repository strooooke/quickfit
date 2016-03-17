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

package com.lambdasoup.quickfit.viewmodel;

import com.lambdasoup.quickfit.model.DayOfWeek;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jl on 15.03.16.
 */
public class ScheduleList {
    private static final int POSITION_INVALID = -1;

    private List<ScheduleItem> dataset = new ArrayList<>();
    private Map<Long, Integer> positionForId = new HashMap<>();

    private ItemChangeCallback callback;
    private ScheduleItem.ByCalendar ordering = new ScheduleItem.ByCalendar(DayOfWeek.getWeek());

    public ScheduleList(ItemChangeCallback callback) {
        this.callback = callback;
    }

    public int size() {
        return dataset.size();
    }

    public void swapData(Iterable<ScheduleItem> newDataSet) { // TODO: async?
        Set<Long> newIds = new HashSet<>();
        for (ScheduleItem newItem : newDataSet) {
            newIds.add(newItem.id);

            int oldPosition = getPositionForId(newItem.id);
            int newPosition = findPositionFor(newItem);

            if (oldPosition == POSITION_INVALID) {
                // entering item
                dataset.add(newPosition, newItem);
                refreshPositions();
                callback.onInserted(newPosition);
            } else {
                // persistent item
                ScheduleItem oldItem = dataset.get(oldPosition);
                if (ordering.compare(newItem, oldItem) != 0) {
                    // items do not yield identical view and ordering
                    callback.onUpdated(oldPosition);
                    if (newPosition == oldPosition) {
                        // but position in the list did not actually change
                        dataset.set(oldPosition, newItem);
                        continue;
                    }

                    if (newPosition > oldPosition) {
                        // new position was found with the item at oldPosition
                        // still in place
                        // but that item will leave now
                        newPosition--;
                    }
                    dataset.remove(oldPosition);
                    dataset.add(newPosition, newItem);
                    refreshPositions();
                    callback.onMoved(oldPosition, newPosition);
                }
            }
        }
        for (Long oldId : positionForId.keySet()) {
            if (!newIds.contains(oldId)) {
                // leaving item
                dataset.remove(getPositionForId(oldId));
                int oldPosition = getPositionForId(oldId);
                refreshPositions();
                callback.onRemoved(oldPosition);
            }
        }
    }

    private int getPositionForId(long id) {
        Integer pos = positionForId.get(id);
        return pos == null ? POSITION_INVALID : pos;
    }

    private int findPositionFor(ScheduleItem item) {
        // TODO: improve performance - bisect instead of linear
        for (int i = 0; i < dataset.size(); i++) {
            ScheduleItem currentItem = dataset.get(i);
            if (ordering.compare(item, currentItem) <= 0) {
                return i;
            }
        }
        return dataset.size();
    }

    private void refreshPositions() {
        positionForId.clear();
        for (int i = 0; i < dataset.size(); i++) {
            positionForId.put(dataset.get(i).id, i);
        }
    }

    public ScheduleItem get(int position) {
        return dataset.get(position);
    }

    public void clear() {
        dataset.clear();
        positionForId.clear();
        callback.onCleared();
    }

    public interface ItemChangeCallback {
        void onInserted(int position);

        void onRemoved(int position);

        void onUpdated(int position);

        void onMoved(int from, int to);

        void onCleared();
    }
}
