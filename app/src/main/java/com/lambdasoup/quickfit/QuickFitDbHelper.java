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

/**
 * Created by jl on 06.01.16.
 */
public class QuickFitDbHelper extends SQLiteOpenHelper {

    static final String DATABASE_NAME = "quickfit.db";
    static final int DATABASE_VERSION = 1;
    public static final String TAG = QuickFitDbHelper.class.getSimpleName();

    public QuickFitDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    public void onCreate(SQLiteDatabase database) {
        for (String stmt : QuickFitContract.WorkoutEntry.CREATE_STATEMENTS) {
            database.execSQL(stmt);
        }
    }

    public void onUpgrade(SQLiteDatabase database, int oldVersion,
                          int newVersion) {
        Log.w(TAG, "Upgrading database from version "
                + oldVersion + " to " + newVersion);

        if (newVersion > 1) {
            Log.e(TAG, "No upgrading procedure for version " + newVersion + " implemented yet!");
        }
    }
}
