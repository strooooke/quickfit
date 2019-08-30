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

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.InstrumentationRegistry;

import com.google.android.gms.fitness.FitnessActivities;
import com.lambdasoup.quickfit.model.DayOfWeek;
import com.lambdasoup.quickfit.persist.QuickFitContract.ScheduleEntry;
import com.lambdasoup.quickfit.persist.QuickFitContract.WorkoutEntry;
import com.lambdasoup.quickfit.persist.QuickFitDbHelper;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static com.lambdasoup.quickfit.persist.QuickFitContract.SessionEntry.TABLE_NAME;


/**
 * TestRule that prepares the database with fixed values. Values depend on the test locale stored
 * in the instrumentation arguments (accessed via LocaleUtil.getTestLocale()) by the screengrabScript
 * or, if running inside Android Studio, the one set statically in @{link LocaleUtil}.
 */
public class DatabasePreparationTestRule implements TestRule {
    public static final long NEXT_ALARM_MILLIS_PAST = 946681200000L; // year 2000
    static final long NEXT_ALARM_MILLIS_FUTURE = 2524604400000L; // year 2050
    public static final ContentValues w1 = new ContentValues();
    public static final ContentValues w2 = new ContentValues();
    public static final ContentValues w3 = new ContentValues();
    public static final Map<Locale, Map<ContentValues, String>> labels = new HashMap<>();
    public static final ContentValues s11 = new ContentValues();
    public static final ContentValues s12 = new ContentValues();
    public static final ContentValues s21 = new ContentValues();
    public static final ContentValues s22 = new ContentValues();
    public static final ContentValues s23 = new ContentValues();

    static {
        w1.put(WorkoutEntry.COL_ID, 1L);
        w1.put(WorkoutEntry.COL_ACTIVITY_TYPE, FitnessActivities.DANCING);
        w1.put(WorkoutEntry.COL_DURATION_MINUTES, 90);

        w2.put(WorkoutEntry.COL_ID, 2L);
        w2.put(WorkoutEntry.COL_ACTIVITY_TYPE, FitnessActivities.HIGH_INTENSITY_INTERVAL_TRAINING);
        w2.put(WorkoutEntry.COL_DURATION_MINUTES, 15);
        w2.put(WorkoutEntry.COL_CALORIES, 120);

        w3.put(WorkoutEntry.COL_ID, 3L);
        w3.put(WorkoutEntry.COL_ACTIVITY_TYPE, FitnessActivities.BADMINTON);
        w3.put(WorkoutEntry.COL_DURATION_MINUTES, 30);

        Map<ContentValues, String> labelsEn = new HashMap<>();
        labelsEn.put(w1, "Latin dance group");
        labelsEn.put(w2, "Jenny's vid");

        Map<ContentValues, String> labelsDe = new HashMap<>();
        labelsDe.put(w1, "Lateinamerika-Tanzgruppe");
        labelsDe.put(w2, "Jennys Video");

        labels.put(Locale.US, labelsEn);
        labels.put(Locale.GERMANY, labelsDe);

        s11.put(ScheduleEntry.COL_ID, 1L);
        s11.put(ScheduleEntry.COL_WORKOUT_ID, 1L);
        s11.put(ScheduleEntry.COL_DAY_OF_WEEK, DayOfWeek.TUESDAY.name());
        s11.put(ScheduleEntry.COL_HOUR, 18);
        s11.put(ScheduleEntry.COL_MINUTE, 30);
        s11.put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, NEXT_ALARM_MILLIS_FUTURE);
        s11.put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_NO);

        s12.put(ScheduleEntry.COL_ID, 5L);
        s12.put(ScheduleEntry.COL_WORKOUT_ID, 1L);
        s12.put(ScheduleEntry.COL_DAY_OF_WEEK, DayOfWeek.SATURDAY.name());
        s12.put(ScheduleEntry.COL_HOUR, 15);
        s12.put(ScheduleEntry.COL_MINUTE, 0);
        s12.put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, NEXT_ALARM_MILLIS_FUTURE);
        s12.put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_NO);

        s21.put(ScheduleEntry.COL_ID, 2L);
        s21.put(ScheduleEntry.COL_WORKOUT_ID, 2L);
        s21.put(ScheduleEntry.COL_DAY_OF_WEEK, DayOfWeek.MONDAY.name());
        s21.put(ScheduleEntry.COL_HOUR, 12);
        s21.put(ScheduleEntry.COL_MINUTE, 0);
        s21.put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, NEXT_ALARM_MILLIS_FUTURE);
        s21.put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_NO);

        s22.put(ScheduleEntry.COL_ID, 3L);
        s22.put(ScheduleEntry.COL_WORKOUT_ID, 2L);
        s22.put(ScheduleEntry.COL_DAY_OF_WEEK, DayOfWeek.WEDNESDAY.name());
        s22.put(ScheduleEntry.COL_HOUR, 13);
        s22.put(ScheduleEntry.COL_MINUTE, 0);
        s22.put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, NEXT_ALARM_MILLIS_FUTURE);
        s22.put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_NO);

        s23.put(ScheduleEntry.COL_ID, 4L);
        s23.put(ScheduleEntry.COL_WORKOUT_ID, 2L);
        s23.put(ScheduleEntry.COL_DAY_OF_WEEK, DayOfWeek.FRIDAY.name());
        s23.put(ScheduleEntry.COL_HOUR, 17);
        s23.put(ScheduleEntry.COL_MINUTE, 0);
        s23.put(ScheduleEntry.COL_NEXT_ALARM_MILLIS, NEXT_ALARM_MILLIS_FUTURE);
        s23.put(ScheduleEntry.COL_SHOW_NOTIFICATION, ScheduleEntry.SHOW_NOTIFICATION_NO);
    }

    private final QuickFitDbHelper dbHelper;

    public DatabasePreparationTestRule() {
        this.dbHelper = new QuickFitDbHelper(InstrumentationRegistry.getTargetContext());
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try (SQLiteDatabase conn = dbHelper.getWritableDatabase()) {
                    Locale testLocale = LocaleUtil.getTestLocale();

                    conn.delete(TABLE_NAME, null, null);

                    conn.delete(WorkoutEntry.TABLE_NAME, null, null);

                    conn.insert(WorkoutEntry.TABLE_NAME, null, w1);
                    conn.insert(WorkoutEntry.TABLE_NAME, null, w2);
                    conn.insert(WorkoutEntry.TABLE_NAME, null, w3);

                    for (Map.Entry<ContentValues, String> label : labels.get(testLocale).entrySet()) {
                        ContentValues v = new ContentValues();
                        v.put(WorkoutEntry.COL_LABEL, label.getValue());
                        conn.update(WorkoutEntry.TABLE_NAME, v, WorkoutEntry.COL_ID + "=" + label.getKey().get(WorkoutEntry.COL_ID), null);
                    }

                    conn.insert(ScheduleEntry.TABLE_NAME, null, s11);
                    conn.insert(ScheduleEntry.TABLE_NAME, null, s12);
                    conn.insert(ScheduleEntry.TABLE_NAME, null, s21);
                    conn.insert(ScheduleEntry.TABLE_NAME, null, s22);
                    conn.insert(ScheduleEntry.TABLE_NAME, null, s23);
                }

                base.evaluate();
            }
        };
    }
}
