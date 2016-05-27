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
import com.lambdasoup.quickfit.util.Lists;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

/**
 * List model for {@link ScheduleItem} items. Inspired by {@link android.support.v7.util.SortedList},
 * but does not depend on compare, itemsTheSame and contentsTheSame being compatible.
 *
 * Assumes that item.id is a unique key for items; assumes that there are no duplicate ids in swapped
 * in item iterables.
 *
 * Not threadsafe; current usage is always on the main thread. Detects attempts for concurrent updates
 * and warns by throwing.
 */
public class ScheduleList {
    private static final int POSITION_INVALID = -1;

    private final AtomicBoolean writing = new AtomicBoolean(false);

    private final List<ScheduleItem> dataset = new ArrayList<>();
    private final Map<Long, Integer> positionForId = new HashMap<>();

    private final ItemChangeCallback callback;
    private final ScheduleItem.ByCalendar ordering = new ScheduleItem.ByCalendar(DayOfWeek.getWeek());

    public ScheduleList(ItemChangeCallback callback) {
        this.callback = callback;
    }

    public int size() {
        return dataset.size();
    }

    public void swapData(Iterable<ScheduleItem> newDataSet) {
        obtainWriteLock();
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

        List<Long> leavingIds = new ArrayList<>();
        //noinspection Convert2streamapi
        for (Long oldId : positionForId.keySet()) {
            if (!newIds.contains(oldId)) {
                leavingIds.add(oldId);
            }
        }
        for (Long leavingId : leavingIds) {
            dataset.remove(getPositionForId(leavingId));
            int oldPosition = getPositionForId(leavingId);
            callback.onRemoved(oldPosition);
            refreshPositions();
        }

        releaseWriteLock();

    }

    private int getPositionForId(long id) {
        Integer pos = positionForId.get(id);
        return pos == null ? POSITION_INVALID : pos;
    }

    private int findPositionFor(ScheduleItem item) {
        return Lists.bisectLeft(dataset, ordering, item);
    }

    private void refreshPositions() {
        positionForId.clear();
        for (int i = 0; i < dataset.size(); i++) {
            positionForId.put(dataset.get(i).id, i);
        }
    }

    public ScheduleItem get(int position) {
        throwIfWriting();
        return dataset.get(position);
    }

    public ScheduleItem getById(long scheduleId) {
        throwIfWriting();
        return dataset.get(positionForId.get(scheduleId));
    }

    public void clear() {
        obtainWriteLock();
        dataset.clear();
        positionForId.clear();
        callback.onCleared();
        releaseWriteLock();
    }

    private void obtainWriteLock() throws ConcurrentModificationException {
        if (!writing.compareAndSet(false, true)) {
            throw new ConcurrentModificationException("ScheduleList is already currently being modified");
        }
    }

    private void releaseWriteLock() throws ConcurrentModificationException {
        if (!writing.compareAndSet(true, false)) {
            throw new ConcurrentModificationException("Something is wrong with the locking logic.");
        }
    }

    private void throwIfWriting() throws ConcurrentModificationException {
        if (writing.get()) {
            throw new ConcurrentModificationException("ScheduleList is currently being modified.");
        }
    }

    public interface ItemChangeCallback {
        void onInserted(int position);

        void onRemoved(int position);

        void onUpdated(int position);

        void onMoved(int from, int to);

        void onCleared();
    }
}
