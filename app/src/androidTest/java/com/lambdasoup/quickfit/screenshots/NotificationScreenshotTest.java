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
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;

import com.lambdasoup.quickfit.Constants;
import com.lambdasoup.quickfit.alarm.AlarmService;
import com.lambdasoup.quickfit.persist.QuickFitContract.ScheduleEntry;
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

import java.util.Locale;


import tools.fastlane.screengrab.locale.LocaleTestRule;

import static org.junit.Assert.assertTrue;

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
        Context targetContext = InstrumentationRegistry.getTargetContext();
        QuickFitDbHelper dbHelper = new QuickFitDbHelper(targetContext);
        try (SQLiteDatabase conn = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, DatabasePreparationTestRule.NEXT_ALARM_MILLIS_PAST);
            conn.update(ScheduleEntry.TABLE_NAME, values, ScheduleEntry.COL_ID + "=" + DatabasePreparationTestRule.s11.get(ScheduleEntry.COL_ID), null);
        }
        targetContext.startService(AlarmService.getIntentOnAlarmReceived(targetContext));
        Thread.sleep(500); // wait for intent service to finish processing
        deviceInstance.openNotification();
    }

    @AfterClass
    public static void tearDown() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        ((NotificationManager) targetContext.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Constants.NOTIFICATION_ALARM);
    }

    @Test
    public void takeScreenshot() throws Exception {
        SystemScreengrab.takeScreenshot("notification");
    }

    @Test
    public void didItButtonPresent() throws Exception {
        UiObject didItButton = deviceInstance.findObject(new UiSelector()
                .className(android.widget.Button.class)
                .description("Did it!")
                .clickable(true));
        assertTrue("Missing DidIt button", didItButton.exists());
    }

    @Test
    public void snoozeButtonPresent() throws Exception {
        UiObject snoozeButton = deviceInstance.findObject(new UiSelector()
                .className(android.widget.Button.class)
                .description("Snooze")
                .clickable(true));
        assertTrue("Missing snooze button", snoozeButton.exists());
    }

}
