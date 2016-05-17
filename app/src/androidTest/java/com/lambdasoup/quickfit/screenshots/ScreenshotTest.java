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

package com.lambdasoup.quickfit.screenshots;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.lambdasoup.quickfit.R;
import com.lambdasoup.quickfit.ui.WorkoutListActivity;
import com.lambdasoup.quickfit.util.DatabasePreparationTestRule;
import com.lambdasoup.quickfit.util.LocaleUtil;
import com.lambdasoup.quickfit.util.SystemScreengrab;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;


import tools.fastlane.screengrab.locale.LocaleTestRule;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.assertion.ViewAssertions.selectedDescendantsMatch;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.lambdasoup.quickfit.util.ConfigurationMatchers.isWideScreen;
import static com.lambdasoup.quickfit.util.RecyclerViewMatcher.withRecyclerView;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeThat;

/**
 * Created by jl on 06.04.16.
 */
@RunWith(AndroidJUnit4.class)
public class ScreenshotTest {
    @ClassRule
    public static final RuleChain classRules = RuleChain.outerRule(new LocaleTestRule())
            .around(new DatabasePreparationTestRule());

    @Rule
    public ActivityTestRule<WorkoutListActivity> activityTestRule = new ActivityTestRule<>(WorkoutListActivity.class);

    @Test
    public void workoutList() throws Exception {
        String label1 = DatabasePreparationTestRule.labels.get(LocaleUtil.getTestLocale()).get(DatabasePreparationTestRule.w1);
        onView(withId(R.id.workout_list)).check(selectedDescendantsMatch(withText(label1), isDisplayed()));

        // still need to give the recyclerview some time to stop animating
        Thread.sleep(200);
        SystemScreengrab.takeScreenshot("main_activity");
    }

    @Test
    public void viewScheduleNormal() throws Exception {
        assumeThat(InstrumentationRegistry.getTargetContext(), not(isWideScreen()));

        onView(withRecyclerView(R.id.workout_list).atPosition(0, withId(R.id.schedules))).perform(click());

        // withSpinnerText does not work - uses toString on spinner items; ignores custom layout
        onView(withRecyclerView(R.id.schedule_list).atPosition(1, withId(R.id.day_of_week))).check(matches(hasDescendant(withText(R.string.saturday))));

        SystemScreengrab.takeScreenshot("schedule_activity");
    }

    @Test
    public void viewScheduleWideScreen() throws Exception {
        assumeThat(InstrumentationRegistry.getTargetContext(), isWideScreen());
        onView(withRecyclerView(R.id.workout_list).atPosition(0)).perform(click());

        // withSpinnerText does not work - uses toString on spinner items; ignores custom layout
        onView(withRecyclerView(R.id.schedule_list).atPosition(1, withId(R.id.day_of_week))).check(matches(hasDescendant(withText(R.string.saturday))));

        SystemScreengrab.takeScreenshot("wide_list_with_fab");
    }

}
