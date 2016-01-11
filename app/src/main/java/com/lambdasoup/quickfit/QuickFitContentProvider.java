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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

/**
 * Created by jl on 06.01.16.
 */
public class QuickFitContentProvider extends ContentProvider {

    private QuickFitDbHelper database;

    public static final String AUTHORITY = "com.lambdasoup.quickfit.provider";

    public static final String PATH_WORKOUTS = "workouts";

    private static final int TYPE_WORKOUTS_LIST = 1;
    private static final int TYPE_WORKOUT_SINGLE_ROW = 2;


    private static final UriMatcher uriMatcher = new UriMatcher(0);
    static {
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS, TYPE_WORKOUTS_LIST);
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS + "/#", TYPE_WORKOUT_SINGLE_ROW);
    }

    @Override
    public boolean onCreate() {
        database = new QuickFitDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUT_SINGLE_ROW:
                queryBuilder.appendWhere(QuickFitContract.WorkoutEntry._ID + "="
                        + uri.getLastPathSegment());
            case TYPE_WORKOUTS_LIST:
                queryBuilder.setTables(QuickFitContract.WorkoutEntry.TABLE_NAME);
                break;
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder);
        //noinspection ConstantConditions
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUT_SINGLE_ROW:
                return "vnd.android.cursor.item/vnd." + AUTHORITY + ".workout";
            case TYPE_WORKOUTS_LIST:
                return "vnd.android.cursor.dir/vnd." + AUTHORITY + ".workout";
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id = 0;
        String basePath;
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUTS_LIST:
                id = sqlDB.insert(QuickFitContract.WorkoutEntry.TABLE_NAME, null, values);
                basePath = PATH_WORKOUTS;
                break;
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }
        //noinspection ConstantConditions
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(basePath + "/" + id);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsDeleted = 0;
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUTS_LIST:
                rowsDeleted = sqlDB.delete(QuickFitContract.WorkoutEntry.TABLE_NAME, selection,
                        selectionArgs);
                break;
            case TYPE_WORKOUT_SINGLE_ROW:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(QuickFitContract.WorkoutEntry.TABLE_NAME,
                            QuickFitContract.WorkoutEntry._ID + "=" + id,
                            null);
                } else {
                    rowsDeleted = sqlDB.delete(QuickFitContract.WorkoutEntry.TABLE_NAME,
                            QuickFitContract.WorkoutEntry._ID + "=" + id
                                    + " and " + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }
        //noinspection ConstantConditions
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated = 0;
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUTS_LIST:
                rowsUpdated = sqlDB.update(QuickFitContract.WorkoutEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case TYPE_WORKOUT_SINGLE_ROW:
                String id = uri.getLastPathSegment();
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(QuickFitContract.WorkoutEntry.TABLE_NAME,
                            values,
                            QuickFitContract.WorkoutEntry._ID + "=" + id,
                            null);
                } else {
                    rowsUpdated = sqlDB.update(QuickFitContract.WorkoutEntry.TABLE_NAME,
                            values,
                            QuickFitContract.WorkoutEntry._ID + "=" + id
                                    + " and "
                                    + selection,
                            selectionArgs);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }
        //noinspection ConstantConditions
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }
}
