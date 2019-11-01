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

import android.app.NotificationManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.lambdasoup.quickfit.Constants;
import com.lambdasoup.quickfit.alarm.Alarms;
import com.lambdasoup.quickfit.alarm.WorkoutNotificationData;
import com.lambdasoup.quickfit.persist.QuickFitContract;
import com.lambdasoup.quickfit.persist.QuickFitDbHelper;
import com.lambdasoup.quickfit.ui.WorkoutListActivity;
import com.lambdasoup.quickfit.util.DatabasePreparationTestRule;
import com.lambdasoup.quickfit.util.SystemScreengrab;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import tools.fastlane.screengrab.locale.LocaleTestRule;

/**
 * Tests correct appearance of a single notification, taking a screenshot in the process
 */
@RunWith(AndroidJUnit4.class)
public class NotificationScreenshotTest {
    @ClassRule
    public static final RuleChain classRules = RuleChain.outerRule(new LocaleTestRule())
            .around(new DatabasePreparationTestRule());

    private static UiDevice deviceInstance;

    @Rule
    public final ActivityTestRule<WorkoutListActivity> workoutListActivityActivityTestRule = new ActivityTestRule<>(WorkoutListActivity.class);

    @BeforeClass
    public static void setUp() throws Exception {
        deviceInstance = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        Alarms alarms = new Alarms(targetContext);
        long scheduleId = DatabasePreparationTestRule.s11.getAsLong(QuickFitContract.ScheduleEntry.COL_ID);
        WorkoutNotificationData notificationData = new WorkoutNotificationData(
                DatabasePreparationTestRule.w1.getAsLong(QuickFitContract.WorkoutEntry.COL_ID),
                DatabasePreparationTestRule.w1.getAsString(QuickFitContract.WorkoutEntry.COL_ACTIVITY_TYPE),
                DatabasePreparationTestRule.w1.getAsString(QuickFitContract.WorkoutEntry.COL_LABEL),
                DatabasePreparationTestRule.w1.getAsInteger(QuickFitContract.WorkoutEntry.COL_DURATION_MINUTES)
        );
        alarms.notify(
                scheduleId,
                notificationData
        );
        Thread.sleep(500); // wait for intent service to finish processing
        deviceInstance.openNotification();
    }

    @AfterClass
    public static void tearDown() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ((NotificationManager) targetContext.getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
    }

    @Test
    public void takeScreenshot() throws Exception {
        SystemScreengrab.takeScreenshot("notification");
    }


}
