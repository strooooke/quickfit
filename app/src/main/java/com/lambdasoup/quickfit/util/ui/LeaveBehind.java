package com.lambdasoup.quickfit.util.ui;

import android.graphics.Canvas;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

/**
 *
 */
public abstract class LeaveBehind extends ItemTouchHelper.SimpleCallback {

    private static final String TAG = LeaveBehind.class.getSimpleName();

    public LeaveBehind() {
        super(0, ItemTouchHelper.START | ItemTouchHelper.END);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        // no dragging
        return false;
    }

    @Override
    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        LeaveBehindViewHolder leaveBehindViewHolder = (LeaveBehindViewHolder) viewHolder;
        final boolean rtl = ViewCompat.getLayoutDirection(recyclerView) == ViewCompat.LAYOUT_DIRECTION_RTL;
        final boolean start = dX < 0 == rtl;

        if (start) {
            leaveBehindViewHolder.showLeaveBehindStart();
        } else {
            leaveBehindViewHolder.showLeaveBehindEnd();
        }

        getDefaultUIUtil().onDraw(c, recyclerView, leaveBehindViewHolder.swipeableItemView, dX, dY, actionState, isCurrentlyActive);
    }

    @Override
    public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        LeaveBehindViewHolder leaveBehindViewHolder = (LeaveBehindViewHolder) viewHolder;
        getDefaultUIUtil().clearView(leaveBehindViewHolder.swipeableItemView);
        leaveBehindViewHolder.hideLeaveBehinds();
    }

    public static class LeaveBehindViewHolder extends RecyclerView.ViewHolder {
        public final View swipeableItemView;
        public final View leaveBehindEnd;
        public final View leaveBehindStart;

        public LeaveBehindViewHolder(View itemView, View swipeableItemView, View leaveBehindEnd, View leaveBehindStart) {
            super(itemView);
            this.swipeableItemView = swipeableItemView;
            this.leaveBehindEnd = leaveBehindEnd;
            this.leaveBehindStart = leaveBehindStart;
            hideLeaveBehinds();
        }

        void showLeaveBehindStart() {
            leaveBehindStart.setVisibility(View.VISIBLE);
            leaveBehindEnd.setVisibility(View.GONE);
        }

        void showLeaveBehindEnd() {
            leaveBehindEnd.setVisibility(View.VISIBLE);
            leaveBehindStart.setVisibility(View.GONE);
        }

        void hideLeaveBehinds() {
            leaveBehindEnd.setVisibility(View.GONE);
            leaveBehindStart.setVisibility(View.GONE);
        }
    }
}
