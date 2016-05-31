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
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;

import com.lambdasoup.quickfit.R;

import static com.lambdasoup.quickfit.util.Nullables.coalesce;

/**
 * Behavior for FABs that does not support anchoring to AppBarLayout, but instead translates the FAB
 * out of the bottom in sync with the AppBarLayout collapsing towards the top.
 * <p>
 * Extends FloatingActionButton.Behavior to keep using the pre-Lollipop shadow padding offset, and
 * hopefully also the Snackbar displacement (this is untested).
 */
public class AppBarBoundFabBehavior extends FloatingActionButton.Behavior {

    public AppBarBoundFabBehavior(Context context, AttributeSet attrs) {
        super();
    }


    @Override
    public boolean layoutDependsOn(CoordinatorLayout parent, FloatingActionButton child, View dependency) {
        if (dependency instanceof AppBarLayout) {
            ((AppBarLayout) dependency).addOnOffsetChangedListener(new OnOffsetChangedListener(parent, child));
        }
        return dependency instanceof AppBarLayout || super.layoutDependsOn(parent, child, dependency);
    }

    @Override
    public boolean onDependentViewChanged(CoordinatorLayout parent, FloatingActionButton fab, View dependency) {
        //noinspection SimplifiableIfStatement
        if (dependency instanceof AppBarLayout) {
            // if the dependency is an AppBarLayout, do not allow super to react on that
            // we don't want that behavior
            return true;
        }
        return super.onDependentViewChanged(parent, fab, dependency);
    }

    private void updateTranslationForAppBar(CoordinatorLayout parent, FloatingActionButton fab, AppBarLayout appBarLayout, int verticalOffset) {
        // fab should scroll out down in sync with the appBarLayout scrolling out up.
        // let's see how far along the way the appBarLayout is
        // (if displacementFraction == 0.0f then no displacement, appBar is fully expanded;
        //  if displacementFraction == 1.0f then full displacement, appBar is totally collapsed)
        float displacementFraction = -verticalOffset / (float) appBarLayout.getHeight();

        // need to separate translationY on the fab that comes from this behavior
        // and one that comes from other sources
        // translationY from this behavior is stored in a tag on the fab
        float translationYFromThis = coalesce((Float) fab.getTag(R.id.fab_translationY_from_AppBarBoundFabBehavior), 0f);

        // top position, accounting for translation not coming from this behavior
        float topUntranslatedFromThis = fab.getTop() + fab.getTranslationY() - translationYFromThis;

        // total length to displace by (from position uninfluenced by this behavior) for a full appBar collapse
        float fullDisplacement = parent.getBottom() - topUntranslatedFromThis;

        // calculate and store new value for displacement coming from this behavior
        float newTranslationYFromThis = fullDisplacement * displacementFraction;
        fab.setTag(R.id.fab_translationY_from_AppBarBoundFabBehavior, newTranslationYFromThis);

        // update translation value by difference found in this step
        fab.setTranslationY(newTranslationYFromThis - translationYFromThis + fab.getTranslationY());
    }

    private class OnOffsetChangedListener implements AppBarLayout.OnOffsetChangedListener {
        private final CoordinatorLayout parent;
        private final FloatingActionButton fab;

        public OnOffsetChangedListener(CoordinatorLayout parent, FloatingActionButton child) {
            this.parent = parent;
            this.fab = child;
        }

        @Override
        public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
            updateTranslationForAppBar(parent, fab, appBarLayout, verticalOffset);
        }
    }

}
