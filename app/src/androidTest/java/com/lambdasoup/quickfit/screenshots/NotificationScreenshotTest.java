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
import android.support.test.uiautomator.UiDevice;

import com.lambdasoup.quickfit.Constants;
import com.lambdasoup.quickfit.alarm.AlarmService;
import com.lambdasoup.quickfit.persist.QuickFitContract.ScheduleEntry;
import com.lambdasoup.quickfit.persist.QuickFitDbHelper;
import com.lambdasoup.quickfit.ui.WorkoutListActivity;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import java.util.Locale;

import tools.fastlane.screengrab.locale.LocaleTestRule;

/**
 * Created by jl on 07.04.16.
 */
public class NotificationScreenshotTest {
    @ClassRule
    public static final RuleChain classRules = RuleChain.outerRule(new FixedLocaleTestRule(Locale.US))
            .around(new LocaleTestRule())
            .around(new DatabasePreparationTestRule());

    @Rule
    public final ActivityTestRule<WorkoutListActivity> workoutListActivityActivityTestRule = new ActivityTestRule<>(WorkoutListActivity.class);

    @Before
    public void setUp() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        QuickFitDbHelper dbHelper = new QuickFitDbHelper(targetContext);
        try (SQLiteDatabase conn = dbHelper.getWritableDatabase()) {
            ContentValues values = new ContentValues();
            values.put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, DatabasePreparationTestRule.NEXT_ALARM_MILLIS_PAST);
            conn.update(ScheduleEntry.TABLE_NAME, values, ScheduleEntry.COL_ID + "=" + DatabasePreparationTestRule.s11.get(ScheduleEntry.COL_ID), null);
        }
        targetContext.startService(AlarmService.getIntentOnAlarmReceived(targetContext));
    }

    @Test
    public void takeNotificationScreenshot() throws Exception {
        swipeDownNotificationBar();
        Thread.sleep(500); // wait for notification area to settle
        SystemScreengrab.takeScreenshot("notification");
    }

    @After
    public void tearDown() {
        Context targetContext = InstrumentationRegistry.getTargetContext();
        ((NotificationManager)targetContext.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(Constants.NOTIFICATION_ALARM);
    }

    public static void swipeDownNotificationBar() {
        UiDevice deviceInstance = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        int dHeight = deviceInstance.getDisplayHeight();
        int dWidth = deviceInstance.getDisplayWidth();
        int xScrollPosition = dWidth / 2;
        int yScrollStop = dHeight / 2;
        deviceInstance.swipe(
                xScrollPosition,
                0,
                xScrollPosition,
                yScrollStop,
                5
        );
    }
}
