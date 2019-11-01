/*
 * Copyright 2016-2019 Juliane Lehmann <jl@lambdasoup.com>
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
 * limitations under the License.
 *
 */

package com.lambdasoup.quickfit.persist

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.lambdasoup.quickfit.persist.QuickFitContract.*
import timber.log.Timber

private const val DATABASE_NAME = "quickfit.db"
private const val DATABASE_VERSION = 10

class QuickFitDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(database: SQLiteDatabase) {
        onUpgrade(database, 0, DATABASE_VERSION)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int,
                           newVersion: Int) {
        Timber.w("Upgrading database from version %d to %d", oldVersion, newVersion)

        for (versionStep in oldVersion + 1..newVersion) {
            upgradeStep(database, versionStep)
        }
    }

    private fun upgradeStep(database: SQLiteDatabase, newVersion: Int) {
        if (newVersion <= 5) {
            // version 5 was the first version in the wild
            database.execSQL("DROP TABLE IF EXISTS ${WorkoutEntry.TABLE_NAME}")
            database.execSQL("DROP TABLE IF EXISTS ${SessionEntry.TABLE_NAME}")

            val createWorkout = """
                CREATE TABLE ${WorkoutEntry.TABLE_NAME} ( 
                    ${WorkoutEntry.COL_ID} INTEGER PRIMARY KEY, 
                    ${WorkoutEntry.COL_ACTIVITY_TYPE} TEXT NOT NULL, 
                    ${WorkoutEntry.COL_DURATION_MINUTES} INTEGER NOT NULL, 
                    ${WorkoutEntry.COL_LABEL} TEXT NULL, 
                    ${WorkoutEntry.COL_CALORIES} INTEGER NULL 
                )
            """.trimIndent()
            database.execSQL(createWorkout)

            val createSession = """
                CREATE TABLE ${SessionEntry.TABLE_NAME} ( 
                    ${SessionEntry._ID} INTEGER PRIMARY KEY, 
                    ${SessionEntry.ACTIVITY_TYPE} TEXT NOT NULL, 
                    ${SessionEntry.START_TIME} INTEGER NOT NULL, 
                    ${SessionEntry.END_TIME} INTEGER NOT NULL, 
                    ${SessionEntry.STATUS} TEXT NOT NULL, 
                    ${SessionEntry.NAME} TEXT NULL, 
                    ${SessionEntry.CALORIES} INTEGER NULL 
                )
            """.trimIndent()
            database.execSQL(createSession)
            return
        }
        if (newVersion == 6) {
            val createSchedule = """
                CREATE TABLE ${ScheduleEntry.TABLE_NAME} ( 
                    ${ScheduleEntry.COL_ID} INTEGER PRIMARY KEY, 
                    ${ScheduleEntry.COL_WORKOUT_ID} INTEGER NOT NULL 
                        REFERENCES ${WorkoutEntry.TABLE_NAME}(${WorkoutEntry.COL_ID}) ON DELETE CASCADE, 
                    ${ScheduleEntry.COL_DAY_OF_WEEK} TEXT NOT NULL, 
                    ${ScheduleEntry.COL_HOUR} INTEGER NOT NULL, 
                    ${ScheduleEntry.COL_MINUTE} INTEGER NOT NULL 
                )
            """.trimIndent()
            database.execSQL(createSchedule)
            return
        }
        if (newVersion == 7) {
            // version 6 was never in the wild
            database.execSQL("DROP TABLE ${ScheduleEntry.TABLE_NAME}")
            val createSchedule = """
                CREATE TABLE ${ScheduleEntry.TABLE_NAME} (
                    ${ScheduleEntry.COL_ID} INTEGER PRIMARY KEY, 
                    ${ScheduleEntry.COL_WORKOUT_ID} INTEGER NOT NULL 
                        REFERENCES ${WorkoutEntry.TABLE_NAME}(${WorkoutEntry.COL_ID}) ON DELETE CASCADE, 
                    ${ScheduleEntry.COL_DAY_OF_WEEK} TEXT NOT NULL, 
                    ${ScheduleEntry.COL_HOUR} INTEGER NOT NULL, 
                    ${ScheduleEntry.COL_MINUTE} INTEGER NOT NULL, 
                    ${ScheduleEntry.COL_NEXT_ALARM_MILLIS} INTEGER NOT NULL 
                )
            """.trimIndent()
            database.execSQL(createSchedule)
            return
        }
        if (newVersion == 8) {
            // version 7 was never in the wild
            database.execSQL("DROP TABLE ${ScheduleEntry.TABLE_NAME}")
            val createSchedule = """
                CREATE TABLE ${ScheduleEntry.TABLE_NAME} ( 
                    ${ScheduleEntry.COL_ID} INTEGER PRIMARY KEY, 
                    ${ScheduleEntry.COL_WORKOUT_ID} INTEGER NOT NULL 
                        REFERENCES ${WorkoutEntry.TABLE_NAME}(${WorkoutEntry.COL_ID}) ON DELETE CASCADE, 
                    ${ScheduleEntry.COL_DAY_OF_WEEK} TEXT NOT NULL, 
                    ${ScheduleEntry.COL_HOUR} INTEGER NOT NULL, 
                    ${ScheduleEntry.COL_MINUTE} INTEGER NOT NULL, 
                    ${ScheduleEntry.COL_NEXT_ALARM_MILLIS} INTEGER NOT NULL, 
                    show_notification INTEGER NOT NULL 
                )
            """.trimIndent()
            database.execSQL(createSchedule)
            return
        }
        if (newVersion == 9) {
            // version 9 was never in the wild
            database.execSQL("DROP TABLE ${ScheduleEntry.TABLE_NAME}")
            val createSchedule = """
                CREATE TABLE ${ScheduleEntry.TABLE_NAME} ( 
                    ${ScheduleEntry.COL_ID} INTEGER PRIMARY KEY, 
                    ${ScheduleEntry.COL_WORKOUT_ID} INTEGER NOT NULL 
                        REFERENCES ${WorkoutEntry.TABLE_NAME}(${WorkoutEntry.COL_ID}) ON DELETE CASCADE, 
                    ${ScheduleEntry.COL_DAY_OF_WEEK} TEXT NOT NULL, 
                    ${ScheduleEntry.COL_HOUR} INTEGER NOT NULL, 
                    ${ScheduleEntry.COL_MINUTE} INTEGER NOT NULL, 
                    ${ScheduleEntry.COL_NEXT_ALARM_MILLIS} INTEGER NOT NULL, 
                    show_notification INTEGER NOT NULL DEFAULT ${ScheduleEntry.SHOW_NOTIFICATION_NO}
                )
            """.trimIndent()
            database.execSQL(createSchedule)
            return
        }
        if (newVersion == 10) {
            database.execSQL("PRAGMA defer_foreign_keys = true") // until end of transaction - controlled by SQLiteOpenHelper
            database.execSQL("""
                CREATE TABLE TEMPORARY_SCHEDULES ( 
                    ${ScheduleEntry.COL_ID} INTEGER PRIMARY KEY, 
                    ${ScheduleEntry.COL_WORKOUT_ID} INTEGER NOT NULL 
                        REFERENCES ${WorkoutEntry.TABLE_NAME}(${WorkoutEntry.COL_ID}) ON DELETE CASCADE, 
                    ${ScheduleEntry.COL_DAY_OF_WEEK} TEXT NOT NULL, 
                    ${ScheduleEntry.COL_HOUR} INTEGER NOT NULL, 
                    ${ScheduleEntry.COL_MINUTE} INTEGER NOT NULL, 
                    ${ScheduleEntry.COL_NEXT_ALARM_MILLIS} INTEGER NULL, 
                    ${ScheduleEntry.COL_CURRENT_STATE} TEXT NOT NULL DEFAULT "${ScheduleEntry.CURRENT_STATE_ACKNOWLEDGED}"
                )
            """.trimIndent())
            database.execSQL("""
                INSERT INTO TEMPORARY_SCHEDULES 
                    SELECT
                        ${ScheduleEntry.COL_ID}, 
                        ${ScheduleEntry.COL_WORKOUT_ID}, 
                        ${ScheduleEntry.COL_DAY_OF_WEEK}, 
                        ${ScheduleEntry.COL_HOUR}, 
                        ${ScheduleEntry.COL_MINUTE}, 
                        ${ScheduleEntry.COL_NEXT_ALARM_MILLIS}, 
                        CASE show_notification
                            WHEN ${ScheduleEntry.SHOW_NOTIFICATION_YES}
                                THEN "${ScheduleEntry.CURRENT_STATE_DISPLAYING}"
                            ELSE 
                                "${ScheduleEntry.CURRENT_STATE_ACKNOWLEDGED}"
                        END
                    FROM ${ScheduleEntry.TABLE_NAME}                
            """.trimIndent())
            database.execSQL("DROP TABLE ${ScheduleEntry.TABLE_NAME}")
            database.execSQL("ALTER TABLE TEMPORARY_SCHEDULES RENAME TO ${ScheduleEntry.TABLE_NAME}")
            return
        }
    }
}
