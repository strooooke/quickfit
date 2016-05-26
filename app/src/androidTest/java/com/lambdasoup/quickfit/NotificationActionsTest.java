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

package com.lambdasoup.quickfit;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiSelector;

import com.lambdasoup.quickfit.alarm.AlarmService;
import com.lambdasoup.quickfit.persist.QuickFitContract;
import com.lambdasoup.quickfit.persist.QuickFitContract.ScheduleEntry;
import com.lambdasoup.quickfit.persist.QuickFitContract.SessionEntry;
import com.lambdasoup.quickfit.persist.QuickFitContract.WorkoutEntry;
import com.lambdasoup.quickfit.persist.QuickFitDbHelper;
import com.lambdasoup.quickfit.ui.WorkoutListActivity;
import com.lambdasoup.quickfit.util.DatabasePreparationTestRule;
import com.lambdasoup.quickfit.util.LocaleUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.selectedDescendantsMatch;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;

/**
 * Created by jl on 26.05.16.
 */

@RunWith(AndroidJUnit4.class)
public class NotificationActionsTest {

    private static QuickFitDbHelper dbHelper;
    private static Context targetContext;

    private UiDevice deviceInstance;

    @Rule
    public final TestRule testRule = new DatabasePreparationTestRule();

    @BeforeClass public static void setUpBeforeClass() throws Exception {
        targetContext = InstrumentationRegistry.getTargetContext();
        dbHelper = new QuickFitDbHelper(targetContext);
    }

    @Before
    public void setUp() throws Exception {
        deviceInstance = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        try (SQLiteDatabase conn = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, DatabasePreparationTestRule.NEXT_ALARM_MILLIS_PAST);
            conn.update(ScheduleEntry.TABLE_NAME, values, ScheduleEntry.COL_ID + "=" + DatabasePreparationTestRule.s11.get(ScheduleEntry.COL_ID), null);
        }
        targetContext.startService(AlarmService.getIntentOnAlarmReceived(targetContext));
        Thread.sleep(500); // wait for intent service to finish processing
        deviceInstance.openNotification();
    }

    @Test
    public void contentAction() throws Exception {
        UiObject icon = deviceInstance.findObject(new UiSelector()
                .packageName("com.lambdasoup.quickfit")
                .resourceId("android:id/icon"));
        icon.clickAndWaitForNewWindow();

        // check that workout list is displayed with correct item visible
        String label1 = DatabasePreparationTestRule.labels.get(LocaleUtil.getTestLocale()).get(DatabasePreparationTestRule.w1);
        onView(withId(R.id.workout_list)).check(selectedDescendantsMatch(withText(label1), isDisplayed()));

        // check that notification got cleared
        checkNotificationCleared();
    }

    @Test
    public void didItAction() throws Exception {
        long testStartMillis = System.currentTimeMillis();

        UiObject didItButton = deviceInstance.findObject(new UiSelector()
                .className(android.widget.Button.class)
                .description("Did it!"));
        didItButton.clickAndWaitForNewWindow();

        // check that a session got inserted
        try (SQLiteDatabase conn = dbHelper.getReadableDatabase()) {
            try (Cursor cur = conn.query(
                    SessionEntry.TABLE_NAME,
                    SessionEntry.COLUMNS,
                    null,
                    null,
                    null, null, null)) {

                assertEquals(1, cur.getCount());
                cur.moveToFirst();
                assertEquals(DatabasePreparationTestRule.w1.getAsString(WorkoutEntry.COL_ACTIVITY_TYPE), cur.getString(cur.getColumnIndex(SessionEntry.ACTIVITY_TYPE)));
            }
        }

        // check that the next alarm time got moved
        checkNextAlarmTimeMoved(testStartMillis);

        // check that the notification got cleared
        checkNotificationCleared();
    }
    @Test
    public void snoozeAction() throws Exception {
        long testStartMillis = System.currentTimeMillis();

        UiObject snoozeButton = deviceInstance.findObject(new UiSelector()
                .className(android.widget.Button.class)
                .description("Snooze"));
        snoozeButton.clickAndWaitForNewWindow();

        // check that no session got inserted
        try (SQLiteDatabase conn = dbHelper.getReadableDatabase()) {
            try (Cursor cur = conn.query(
                    SessionEntry.TABLE_NAME,
                    SessionEntry.COLUMNS,
                    null,
                    null,
                    null, null, null)) {

                assertEquals(0, cur.getCount());
            }
        }

        // check that the next alarm time got moved
        checkNextAlarmTimeMoved(testStartMillis);

        // check that the notification got cleared
        checkNotificationCleared();
    }

    private void checkNextAlarmTimeMoved(long testStartMillis) {
        try (SQLiteDatabase conn = dbHelper.getReadableDatabase()) {
            try (Cursor cur = conn.query(
                    ScheduleEntry.TABLE_NAME,
                    new String[]{ScheduleEntry.COL_NEXT_ALARM_MILLIS},
                    ScheduleEntry.COL_ID + "=?",
                    new String[]{DatabasePreparationTestRule.s11.getAsString(ScheduleEntry.COL_ID)},
                    null, null, null)) {

                assertEquals(1, cur.getCount());
                cur.moveToFirst();
                Assert.assertTrue("alarm time not advanced", testStartMillis < cur.getLong(cur.getColumnIndex(ScheduleEntry.COL_NEXT_ALARM_MILLIS)));
            }
        }
    }

    private void checkNotificationCleared() {
        try (SQLiteDatabase conn = dbHelper.getReadableDatabase()) {
            try (Cursor cur = conn.query(
                    ScheduleEntry.TABLE_NAME,
                    new String[]{ScheduleEntry.COL_SHOW_NOTIFICATION},
                    ScheduleEntry.COL_ID + "=?",
                    new String[]{DatabasePreparationTestRule.s11.getAsString(ScheduleEntry.COL_ID)},
                    null, null, null)) {

                assertEquals(1, cur.getCount());
                cur.moveToFirst();
                assertEquals("notification showing not cleared", ScheduleEntry.SHOW_NOTIFICATION_NO, cur.getInt(cur.getColumnIndex(ScheduleEntry.COL_SHOW_NOTIFICATION)));
            }
        }
    }
}
