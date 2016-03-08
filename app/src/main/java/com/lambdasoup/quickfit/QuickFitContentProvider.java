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
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.lambdasoup.quickfit.QuickFitContract.ScheduleEntry;
import com.lambdasoup.quickfit.QuickFitContract.SessionEntry;
import com.lambdasoup.quickfit.QuickFitContract.WorkoutEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class QuickFitContentProvider extends ContentProvider {

    private static final String TAG = QuickFitContentProvider.class.getSimpleName();
    private QuickFitDbHelper database;

    public static final String AUTHORITY = "com.lambdasoup.quickfit.provider";

    private static final String PATH_WORKOUTS = "workouts";
    private static final String PATH_SESSIONS = "sessions";
    private static final String PATH_SCHEDULES = "schedules";
    private static final String PATH_WORKOUT_SCHEDULES = "workoutSchedules";

    public static final Uri URI_WORKOUTS = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path(PATH_WORKOUTS).build();
    public static final Uri URI_SESSIONS = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path(PATH_SESSIONS).build();
    public static final Uri URI_SCHEDULES = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path(PATH_SCHEDULES).build();
    public static final Uri URI_WORKOUT_SCHEDULES = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path(PATH_WORKOUT_SCHEDULES).build();

    private static final int TYPE_WORKOUTS_LIST = 1;
    private static final int TYPE_WORKOUT_SINGLE_ROW = 2;
    private static final int TYPE_SESSIONS_LIST = 3;
    private static final int TYPE_SESSION_SINGLE_ROW = 4;
    private static final int TYPE_WORKOUT_SCHEDULES_LIST = 5;
    private static final int TYPE_SCHEDULES_LIST = 6;
    private static final int TYPE_SCHEDULE_SINGLE_ROW = 7;


    private static final UriMatcher uriMatcher = new UriMatcher(0);

    static {
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS, TYPE_WORKOUTS_LIST);
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS + "/#", TYPE_WORKOUT_SINGLE_ROW);
        uriMatcher.addURI(AUTHORITY, PATH_SESSIONS, TYPE_SESSIONS_LIST);
        uriMatcher.addURI(AUTHORITY, PATH_SESSIONS + "/#", TYPE_SESSION_SINGLE_ROW);
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUT_SCHEDULES, TYPE_WORKOUT_SCHEDULES_LIST);
        uriMatcher.addURI(AUTHORITY, PATH_SCHEDULES + "/#", TYPE_SCHEDULE_SINGLE_ROW);
        uriMatcher.addURI(AUTHORITY, PATH_SCHEDULES, TYPE_SCHEDULES_LIST);
    }

    @Override
    public boolean onCreate() {
        database = new QuickFitDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        List<String> moreSelectionArgs = new ArrayList<>();
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUT_SINGLE_ROW:
                queryBuilder.appendWhere(WorkoutEntry._ID + "=?");
                moreSelectionArgs.add(uri.getLastPathSegment());
            case TYPE_WORKOUTS_LIST:
                queryBuilder.setTables(WorkoutEntry.TABLE_NAME);
                break;
            case TYPE_SESSION_SINGLE_ROW:
                queryBuilder.appendWhere(SessionEntry._ID + "=?");
                moreSelectionArgs.add(uri.getLastPathSegment());
            case TYPE_SESSIONS_LIST:
                queryBuilder.setTables(SessionEntry.TABLE_NAME);
                break;
            case TYPE_SCHEDULE_SINGLE_ROW:
                queryBuilder.appendWhere(ScheduleEntry._ID + "=?");
                moreSelectionArgs.add(uri.getLastPathSegment());
                queryBuilder.setTables(ScheduleEntry.TABLE_NAME);
                break;
            case TYPE_WORKOUT_SCHEDULES_LIST:
                queryBuilder.setTables(WorkoutEntry.TABLE_NAME + " left outer join " + ScheduleEntry.TABLE_NAME
                        + " on (" + WorkoutEntry.TABLE_NAME + "." + WorkoutEntry._ID + "=" + ScheduleEntry.TABLE_NAME + "." + ScheduleEntry.WORKOUT_ID +")" );
                break;
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }

        String[] expandedSelectionArgs = expandSelectionArgs(selectionArgs, moreSelectionArgs);

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection,
                expandedSelectionArgs, null, null, sortOrder);
        //noinspection ConstantConditions
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUT_SINGLE_ROW:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".workout";
            case TYPE_WORKOUTS_LIST:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".workout";
            case TYPE_SESSION_SINGLE_ROW:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".session";
            case TYPE_SESSIONS_LIST:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".session";
            case TYPE_WORKOUT_SCHEDULES_LIST:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".workoutSchedules";
            case TYPE_SCHEDULES_LIST:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".schedule";
            case TYPE_SCHEDULE_SINGLE_ROW:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".schedule";
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        long id;
        String basePath;
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUTS_LIST:
                id = sqlDB.insert(WorkoutEntry.TABLE_NAME, null, values);
                basePath = PATH_WORKOUTS;
                break;
            case TYPE_SESSIONS_LIST:
                id = sqlDB.insert(SessionEntry.TABLE_NAME, null, values);
                basePath = PATH_SESSIONS;
                break;
            case TYPE_SCHEDULES_LIST:
                id = sqlDB.insert(ScheduleEntry.TABLE_NAME, null, values);
                basePath = PATH_SCHEDULES;
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

        int rowsDeleted;
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUTS_LIST: {
                rowsDeleted = sqlDB.delete(WorkoutEntry.TABLE_NAME, selection,
                        selectionArgs);
                break;
            }
            case TYPE_WORKOUT_SINGLE_ROW: {
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(WorkoutEntry.TABLE_NAME,
                            WorkoutEntry._ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsDeleted = sqlDB.delete(WorkoutEntry.TABLE_NAME,
                            WorkoutEntry._ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            }
            case TYPE_SESSIONS_LIST: {
                rowsDeleted = sqlDB.delete(SessionEntry.TABLE_NAME, selection,
                        selectionArgs);
                break;
            }
            case TYPE_SESSION_SINGLE_ROW: {
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(SessionEntry.TABLE_NAME,
                            SessionEntry._ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsDeleted = sqlDB.delete(SessionEntry.TABLE_NAME,
                            SessionEntry._ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            }
            case TYPE_SCHEDULE_SINGLE_ROW: {
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(ScheduleEntry.TABLE_NAME,
                            ScheduleEntry._ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsDeleted = sqlDB.delete(ScheduleEntry.TABLE_NAME,
                            ScheduleEntry._ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }
        //noinspection ConstantConditions
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.d(TAG, "ContentProvider update for uri " + uri + ", with content values " + values);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated;
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUTS_LIST:
                rowsUpdated = sqlDB.update(WorkoutEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case TYPE_WORKOUT_SINGLE_ROW:
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(WorkoutEntry.TABLE_NAME,
                            values,
                            WorkoutEntry._ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsUpdated = sqlDB.update(WorkoutEntry.TABLE_NAME,
                            values,
                            WorkoutEntry._ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            case TYPE_SESSIONS_LIST:
                rowsUpdated = sqlDB.update(SessionEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case TYPE_SESSION_SINGLE_ROW:
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(SessionEntry.TABLE_NAME,
                            values,
                            SessionEntry._ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsUpdated = sqlDB.update(SessionEntry.TABLE_NAME,
                            values,
                            SessionEntry._ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            case TYPE_SCHEDULE_SINGLE_ROW:
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(ScheduleEntry.TABLE_NAME,
                            values,
                            ScheduleEntry._ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsUpdated = sqlDB.update(ScheduleEntry.TABLE_NAME,
                            values,
                            ScheduleEntry._ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }
        //noinspection ConstantConditions
        getContext().getContentResolver().notifyChange(uri, null);
        Log.d(TAG, rowsUpdated + " rows updated.");
        return rowsUpdated;
    }

    private String[] expandSelectionArgs(String[] selectionArgs, List<String> moreSelectionArgs) {
        String[] expandedSelectionArgs = selectionArgs;
        if (!moreSelectionArgs.isEmpty()) {
            int originalArgsLength = selectionArgs == null ? 0 : selectionArgs.length;
            expandedSelectionArgs = new String[originalArgsLength + moreSelectionArgs.size()];
            for (int i = 0; i < originalArgsLength; i++) {
                expandedSelectionArgs[i] = selectionArgs[i];
            }
            for (int i = 0; i < moreSelectionArgs.size(); i++) {
                expandedSelectionArgs[originalArgsLength + i] = moreSelectionArgs.get(i);
            }
        }
        return expandedSelectionArgs;
    }

}
