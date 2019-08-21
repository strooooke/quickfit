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

package com.lambdasoup.quickfit.util;

import android.content.res.Resources;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import static androidx.test.espresso.core.deps.guava.collect.Iterables.any;
import static androidx.test.espresso.util.TreeIterables.breadthFirstViewTraversal;

/**
 * Matcher for matching i-th item of a recycler view or children thereof.
 */
public class RecyclerViewMatcher {
    private final int recyclerViewId;

    public RecyclerViewMatcher(int recyclerViewId) {
        this.recyclerViewId = recyclerViewId;
    }

    public static RecyclerViewMatcher withRecyclerView(int recyclerViewId) {
        return new RecyclerViewMatcher(recyclerViewId);
    }

    public Matcher<View> atPosition(int position) {
        return atPosition(position, null);
    }

    public Matcher<View> atPosition(int position, Matcher<View> itemMatcher) {
        return new TypeSafeMatcher<View>() {
            Resources resources = null;
            View childView;

            public void describeTo(Description description) {
                String idDescription = Integer.toString(recyclerViewId);
                if (this.resources != null) {
                    try {
                        idDescription = this.resources.getResourceName(recyclerViewId);
                    } catch (Resources.NotFoundException e) {
                        idDescription = String.format("%s (resource name not found)", recyclerViewId);
                    }
                }
                description.appendText("with id: " + idDescription);
            }

            public boolean matchesSafely(View view) {
                this.resources = view.getResources();

                if (childView == null) {
                    RecyclerView recyclerView =
                            (RecyclerView) view.getRootView().findViewById(recyclerViewId);
                    if (recyclerView != null && recyclerView.getId() == recyclerViewId) {
                        childView = recyclerView.getChildAt(position);
                        if (childView == null) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }

                if (itemMatcher == null) {
                    return view == childView;
                } else {
                    return isDescendantOfChild(view) && itemMatcher.matches(view);
                }
            }

            private boolean isDescendantOfChild(View view) {
                return any(breadthFirstViewTraversal(childView), descendantView -> descendantView == view);
            }
        };
    }
}
