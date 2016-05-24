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

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.RemoteViews;

import com.bartoszlipinski.viewpropertyobjectanimator.ViewPropertyObjectAnimator;
import com.lambdasoup.quickfit.R;

import timber.log.Timber;

/**
 * Created by jl on 19.05.16.
 */
@RemoteViews.RemoteView
public class MasterDetailLayout extends LinearLayout {
    // duration of collapse/expand animation in milliseconds
    private int animationDuration;

    // width of masterPane in collapsed state in pixels
    private int masterPaneWidth;

    private Runnable afterCollapse;

    private boolean showDetail = false;
    private View masterView;
    private View detailView;

    public MasterDetailLayout(Context context) {
        this(context, null);
    }

    public MasterDetailLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MasterDetailLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MasterDetailLayout, defStyle, 0);

        try {
            masterPaneWidth = a.getDimensionPixelSize(R.styleable.MasterDetailLayout_masterPaneWidth, 0);
            animationDuration = a.getInt(R.styleable.MasterDetailLayout_animationDuration, 200);
        } finally {
            a.recycle();
        }


        getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                getViewTreeObserver().removeOnGlobalLayoutListener(this);

                int dividerWidth = getDividerDrawable() != null ? getDividerDrawable().getIntrinsicWidth() + 2 * getDividerPadding() : 0;
                int innerWidth = getInnerWidth();

                ViewGroup.LayoutParams detailViewLayoutParams = detailView.getLayoutParams();
                Timber.d("adjusting layout width for detail pane to %d", innerWidth - dividerWidth - masterPaneWidth);
                detailViewLayoutParams.width = innerWidth - dividerWidth - masterPaneWidth;
                detailView.setLayoutParams(detailViewLayoutParams);

                ViewGroup.LayoutParams masterLayoutParams = masterView.getLayoutParams();
                masterLayoutParams.width = innerWidth;
                masterView.setLayoutParams(masterLayoutParams);
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() != 2) {
            throw new IllegalArgumentException("MasterDetailLayout should have exactly two children");
        }
        masterView = getChildAt(0);
        detailView = getChildAt(1);

        detailView.setVisibility(GONE);
    }

    public void showDetail() {
        Timber.d("showing detail pane");
        showDetail = true;

        detailView.setVisibility(VISIBLE);

        ObjectAnimator animator = ViewPropertyObjectAnimator
                .animate(masterView)
                .width(masterPaneWidth)
                .setDuration(animationDuration)
                .get();
        animator.start();
    }

    public void hideDetail() {
        Timber.d("hiding detail pane");
        showDetail = false;

        ObjectAnimator animator = ViewPropertyObjectAnimator
                .animate(masterView)
                .width(getInnerWidth())
                .withEndAction(() -> {
                    detailView.setVisibility(View.GONE);

                    if (afterCollapse != null) {
                        afterCollapse.run();
                    }
                })
                .setDuration(animationDuration)
                .get();
        animator.start();
    }

    /**
     * gets width available for children in pixels
     */
    private int getInnerWidth() {
       return  getWidth() - getPaddingLeft() - getPaddingRight();
    }


    /**
     * gets width of collapsed master pane in pixels
     *
     * @return width of master pane, in pixels
     */
    public int getMasterPaneWidth() {
        return masterPaneWidth;
    }

    /**
     * sets width of collapsed master pane in pixels
     *
     * @param masterPaneWidth pixels
     */
    public void setMasterPaneWidth(int masterPaneWidth) {
        this.masterPaneWidth = masterPaneWidth;
    }

    /**
     * gets duration of expand and collapse animations in milliseconds
     */
    public int getAnimationDuration() {
        return animationDuration;
    }

    /**
     * sets duration of expand and collapse animations in milliseconds
     */
    public void setAnimationDuration(int animationDuration) {
        this.animationDuration = animationDuration;
    }

    public boolean isShowDetailsPane() {
        return showDetail;
    }

    public Runnable getAfterCollapse() {
        return afterCollapse;
    }

    public void setAfterCollapse(Runnable afterCollapse) {
        this.afterCollapse = afterCollapse;
    }
}
