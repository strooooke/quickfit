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

package com.lambdasoup.quickfit.persist;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
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
import android.util.Pair;

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

    private static final Uri URI_WORKOUTS = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path(PATH_WORKOUTS).build();
    private static final Uri URI_SESSIONS = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path(PATH_SESSIONS).build();

    public static Uri getUriWorkoutsList() {
        return URI_WORKOUTS;
    }

    public static Uri getUriWorkoutsId(long workoutId) {
        return ContentUris.withAppendedId(getUriWorkoutsList(), workoutId);
    }

    public static Uri getUriWorkoutsSchedulesId(long workoutId, long scheduleId) {
        Uri uri = getUriWorkoutsId(workoutId);
        return uri.buildUpon().appendEncodedPath(PATH_SCHEDULES).appendEncodedPath(String.valueOf(scheduleId)).build();
    }

    public static Uri getUriSessionsList() {
        return URI_SESSIONS;
    }

    public static Uri getUriSessionsId(long sessionId) {
        return ContentUris.withAppendedId(getUriSessionsList(), sessionId);
    }

    private static final int TYPE_WORKOUTS = 1;
    private static final int TYPE_WORKOUT_ID = 2;
    private static final int TYPE_SESSIONS = 3;
    private static final int TYPE_SESSION_ID = 4;
    private static final int TYPE_WORKOUT_ID_SCHEDULES = 5;
    private static final int TYPE_WORKOUT_ID_SCHEDULE_ID = 6;


    private static final UriMatcher uriMatcher = new UriMatcher(0);

    static {
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS, TYPE_WORKOUTS);
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS + "/#", TYPE_WORKOUT_ID);
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS + "/#" + PATH_SCHEDULES, TYPE_WORKOUT_ID_SCHEDULES);
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS + "/#" + PATH_SCHEDULES + "/#", TYPE_WORKOUT_ID_SCHEDULE_ID);
        uriMatcher.addURI(AUTHORITY, PATH_SESSIONS, TYPE_SESSIONS);
        uriMatcher.addURI(AUTHORITY, PATH_SESSIONS + "/#", TYPE_SESSION_ID);
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
        String[] aliasedProjection = projection;

        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        int type = uriMatcher.match(uri);
        switch (type) {
            case TYPE_WORKOUT_ID:
            case TYPE_WORKOUT_ID_SCHEDULE_ID:
            case TYPE_WORKOUT_ID_SCHEDULES:
            case TYPE_WORKOUTS:
                Pair<String, String[]> tableAndProjection = QuickFitContract.WorkoutEntry.toAlias(projection);
                aliasedProjection = tableAndProjection.second;
                queryBuilder.setTables(tableAndProjection.first);
                break;
            case TYPE_SESSION_ID:
            case TYPE_SESSIONS:
                queryBuilder.setTables(QuickFitContract.SessionEntry.TABLE_NAME);
                break;
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }
        switch (type) {
            case TYPE_WORKOUT_ID:
            case TYPE_WORKOUT_ID_SCHEDULES:
                queryBuilder.appendWhere(QuickFitContract.WorkoutEntry.WORKOUT_ID + "=?");
                moreSelectionArgs.add(uri.getPathSegments().get(1));
                break;
            case TYPE_WORKOUT_ID_SCHEDULE_ID:
                queryBuilder.appendWhere(QuickFitContract.WorkoutEntry.SCHEDULE_ID + "=?");
                moreSelectionArgs.add(uri.getLastPathSegment());
                break;
            case TYPE_SESSION_ID:
                queryBuilder.appendWhere(QuickFitContract.SessionEntry._ID + "=?");
                moreSelectionArgs.add(uri.getLastPathSegment());
                break;
            case TYPE_WORKOUTS:
            case TYPE_SESSIONS:
                break;
        }

        String[] expandedSelectionArgs = expandSelectionArgs(selectionArgs, moreSelectionArgs);

        SQLiteDatabase db = database.getWritableDatabase();
        Cursor cursor = queryBuilder.query(db, aliasedProjection, selection,
                expandedSelectionArgs, null, null, sortOrder);
        //noinspection ConstantConditions
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUT_ID:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".workout";
            case TYPE_WORKOUTS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".workout";
            case TYPE_SESSION_ID:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".session";
            case TYPE_SESSIONS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".session";
            case TYPE_WORKOUT_ID_SCHEDULES:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd." + AUTHORITY + ".schedule";
            case TYPE_WORKOUT_ID_SCHEDULE_ID:
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
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUTS:
                id = sqlDB.insert(QuickFitContract.WorkoutEntry.TABLE_NAME, null, values);
                break;
            case TYPE_SESSIONS:
                id = sqlDB.insert(QuickFitContract.SessionEntry.TABLE_NAME, null, values);
                break;
            case TYPE_WORKOUT_ID_SCHEDULES:
                id = sqlDB.insert(QuickFitContract.ScheduleEntry.TABLE_NAME, null, values);
                break;
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }
        //noinspection ConstantConditions
        getContext().getContentResolver().notifyChange(uri, null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase sqlDB = database.getWritableDatabase();

        int rowsDeleted;
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUTS: {
                rowsDeleted = sqlDB.delete(QuickFitContract.WorkoutEntry.TABLE_NAME, selection,
                        selectionArgs);
                break;
            }
            case TYPE_WORKOUT_ID: {
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(QuickFitContract.WorkoutEntry.TABLE_NAME,
                            QuickFitContract.WorkoutEntry.COL_ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsDeleted = sqlDB.delete(QuickFitContract.WorkoutEntry.TABLE_NAME,
                            QuickFitContract.WorkoutEntry.COL_ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            }
            case TYPE_SESSIONS: {
                rowsDeleted = sqlDB.delete(QuickFitContract.SessionEntry.TABLE_NAME, selection,
                        selectionArgs);
                break;
            }
            case TYPE_SESSION_ID: {
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(QuickFitContract.SessionEntry.TABLE_NAME,
                            QuickFitContract.SessionEntry._ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsDeleted = sqlDB.delete(QuickFitContract.SessionEntry.TABLE_NAME,
                            QuickFitContract.SessionEntry._ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            }
            case TYPE_WORKOUT_ID_SCHEDULES: {
                rowsDeleted = sqlDB.delete(QuickFitContract.ScheduleEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case TYPE_WORKOUT_ID_SCHEDULE_ID: {
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(QuickFitContract.ScheduleEntry.TABLE_NAME,
                            QuickFitContract.ScheduleEntry.COL_ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsDeleted = sqlDB.delete(QuickFitContract.ScheduleEntry.TABLE_NAME,
                            QuickFitContract.ScheduleEntry.COL_ID + "=? and " + selection,
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
            case TYPE_WORKOUTS:
                rowsUpdated = sqlDB.update(QuickFitContract.WorkoutEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case TYPE_WORKOUT_ID:
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(QuickFitContract.WorkoutEntry.TABLE_NAME,
                            values,
                            QuickFitContract.WorkoutEntry.COL_ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsUpdated = sqlDB.update(QuickFitContract.WorkoutEntry.TABLE_NAME,
                            values,
                            QuickFitContract.WorkoutEntry.COL_ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            case TYPE_SESSIONS:
                rowsUpdated = sqlDB.update(QuickFitContract.SessionEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case TYPE_SESSION_ID:
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(QuickFitContract.SessionEntry.TABLE_NAME,
                            values,
                            QuickFitContract.SessionEntry._ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsUpdated = sqlDB.update(QuickFitContract.SessionEntry.TABLE_NAME,
                            values,
                            QuickFitContract.SessionEntry._ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            case TYPE_WORKOUT_ID_SCHEDULES:
                rowsUpdated = sqlDB.update(QuickFitContract.ScheduleEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case TYPE_WORKOUT_ID_SCHEDULE_ID:
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(QuickFitContract.ScheduleEntry.TABLE_NAME,
                            values,
                            QuickFitContract.ScheduleEntry.COL_ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsUpdated = sqlDB.update(QuickFitContract.ScheduleEntry.TABLE_NAME,
                            values,
                            QuickFitContract.ScheduleEntry.COL_ID + "=? and " + selection,
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
            if (selectionArgs != null) {
                System.arraycopy(selectionArgs, 0, expandedSelectionArgs, 0, originalArgsLength);
            }
            for (int i = 0; i < moreSelectionArgs.size(); i++) {
                expandedSelectionArgs[originalArgsLength + i] = moreSelectionArgs.get(i);
            }
        }
        return expandedSelectionArgs;
    }

}
