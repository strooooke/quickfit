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

/* based on https://gist.githubusercontent.com/mobiRic/963a814d51259c730467/raw/d52c09e11abf68aba300ed05171d8ae99cd92c22/EmptyRecyclerView.java
 * Original copyright notice follows.
 *
 * Changes:
 *   - package name
 *   - added isEmpty method
 */


/*
 * Copyright (C) 2015 Glowworm Software
 * Copyright (C) 2014 Nizamutdinov Adel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// based on https://gist.github.com/adelnizamutdinov/31c8f054d1af4588dc5c

package com.lambdasoup.quickfit.util.ui;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;

public class EmptyRecyclerView extends RecyclerView {

    @Nullable
    private View emptyView;

    // stores the current state
    private boolean isEmpty;
    @NonNull
    private final AdapterDataObserver observer = new AdapterDataObserver() {
        @Override
        public void onChanged() {
            super.onChanged();
            checkIfEmpty();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            super.onItemRangeInserted(positionStart, itemCount);
            checkIfEmpty();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            super.onItemRangeRemoved(positionStart, itemCount);
            checkIfEmpty();
        }
    };

    public EmptyRecyclerView(Context context) {
        super(context);
    }

    public EmptyRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EmptyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(observer);
        }

        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer);
        }
        super.setAdapter(adapter);
        checkIfEmpty();
    }

    @Override
    public void swapAdapter(Adapter adapter, boolean removeAndRecycleExistingViews) {
        final Adapter oldAdapter = getAdapter();
        if (oldAdapter != null) {
            oldAdapter.unregisterAdapterDataObserver(observer);
        }

        if (adapter != null) {
            adapter.registerAdapterDataObserver(observer);
        }
        super.swapAdapter(adapter, removeAndRecycleExistingViews);
        checkIfEmpty();
    }

    /**
     * Indicates the view to be shown when the adapter for this object is empty
     *
     * @param emptyView the empty view
     */
    public void setEmptyView(@Nullable View emptyView) {
        if (this.emptyView != null) {
            this.emptyView.setVisibility(GONE);
        }

        this.emptyView = emptyView;
        checkIfEmpty();
    }

    public boolean isEmpty() {
        return isEmpty;
    }

    /**
     * Check adapter item count and toggle visibility of empty view if the adapter is empty
     */
    private void checkIfEmpty() {
        Adapter adapter = getAdapter();
        boolean isEmptyNow = (adapter == null || adapter.getItemCount() == 0);

        if (emptyView == null || isEmptyNow == isEmpty) {
            return;
        }

        if (isEmptyNow) {
            emptyView.setVisibility(VISIBLE);
            isEmpty = true;
        } else {
            isEmpty = false;
            emptyView.setVisibility(GONE);
        }
    }

}