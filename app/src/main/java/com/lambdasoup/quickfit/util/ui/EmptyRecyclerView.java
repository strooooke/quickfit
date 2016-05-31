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


package com.lambdasoup.quickfit.util.ui;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

/**
 * Does not toggle visibility of an external View (like https://gist.github.com/adelnizamutdinov/31c8f054d1af4588dc5c ),
 * because the RecyclerViews should be part of the view of a fragment, which should be shown as content in a CoordinatorLayout.
 * Then, there must a single child of CoordinatorLayout that the fragment attaches into. That child, as direct child
 * of CoordinatorLayout, must be carrier of {@link android.support.design.widget.AppBarLayout.ScrollingViewBehavior}. Nesting the
 * empty view into that child makes the empty view scroll too, which is undesirable.
 *
 * Thus, the empty view is not part of the fragment, but must be supplied by the containing activity layout and displayed
 * underneath this RecyclerView. This RecyclerView should be initialized with an opaque background, that gets switched to transparent while
 * the RecyclerView is empty, thus revealing the empty view.
 *
 * Toggling the visibility of the RecyclerView instead does not work - event sequencing with {@link LeaveBehind} fails,
 * as the item gets removed from the adapter before the swipe animation completes and the
 * {@link com.lambdasoup.quickfit.util.ui.LeaveBehind.LeaveBehindViewHolder} gets reset.
 */
public class EmptyRecyclerView extends RecyclerView {

    // stores the current state
    private boolean isEmpty;

    private final Drawable solidBackground;
    private final Drawable transparentBackground;

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
        this(context, null);
    }

    public EmptyRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EmptyRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        solidBackground = getBackground();
        //noinspection deprecation
        transparentBackground = new ColorDrawable(getResources().getColor(android.R.color.transparent));
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


    public boolean isEmpty() {
        return isEmpty;
    }

    /**
     * Check adapter item count and toggle visibility of empty view if the adapter is empty
     */
    private void checkIfEmpty() {
        Adapter adapter = getAdapter();
        boolean isEmptyNow = (adapter == null || adapter.getItemCount() == 0);

        if (isEmptyNow == isEmpty) {
            return;
        }

        if (isEmptyNow) {
            setBackground(transparentBackground);
            isEmpty = true;
        } else {
            isEmpty = false;
            setBackground(solidBackground);
        }
    }

}