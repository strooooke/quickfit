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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


public class QuickFitDbHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "quickfit.db";
    static final int DATABASE_VERSION = 6;
    public static final String TAG = QuickFitDbHelper.class.getSimpleName();

    public QuickFitDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    public void onCreate(SQLiteDatabase database) {
        for (String stmt : QuickFitContract.WorkoutEntry.CREATE_STATEMENTS) {
            database.execSQL(stmt);
        }
        for (String stmt : QuickFitContract.ScheduleEntry.CREATE_STATEMENTS) {
            database.execSQL(stmt);
        }
        for (String stmt : QuickFitContract.SessionEntry.CREATE_STATEMENTS) {
            database.execSQL(stmt);
        }
    }

    public void onUpgrade(SQLiteDatabase database, int oldVersion,
                          int newVersion) {
        Log.w(TAG, "Upgrading database from version "
                + oldVersion + " to " + newVersion);

        if (newVersion > 6) {
            Log.e(TAG, "No upgrading procedure for version " + newVersion + " implemented yet!");
            return;
        }

        if (oldVersion < 5) {
            database.execSQL("DROP TABLE IF EXISTS " + QuickFitContract.WorkoutEntry.TABLE_NAME);
            database.execSQL("DROP TABLE IF EXISTS " + QuickFitContract.SessionEntry.TABLE_NAME);

            for (String stmt : QuickFitContract.WorkoutEntry.CREATE_STATEMENTS) {
                database.execSQL(stmt);
            }
            for (String stmt : QuickFitContract.SessionEntry.CREATE_STATEMENTS) {
                database.execSQL(stmt);
            }
        }


        if (newVersion == 6) {
            for (String stmt : QuickFitContract.ScheduleEntry.CREATE_STATEMENTS) {
                database.execSQL(stmt);
            }
        }


    }
}
