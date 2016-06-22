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

import com.lambdasoup.quickfit.persist.QuickFitContract.ScheduleEntry;
import com.lambdasoup.quickfit.persist.QuickFitContract.SessionEntry;
import com.lambdasoup.quickfit.persist.QuickFitContract.TablesAndAliases;
import com.lambdasoup.quickfit.persist.QuickFitContract.WorkoutEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import timber.log.Timber;


public class QuickFitContentProvider extends ContentProvider {

    public static final String AUTHORITY = "com.lambdasoup.quickfit.provider";
    private static final String PATH_WORKOUTS = "workouts";
    private static final String PATH_SESSIONS = "sessions";
    private static final String PATH_SCHEDULES = "schedules";
    private static final Uri URI_WORKOUTS = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path(PATH_WORKOUTS).build();
    private static final Uri URI_SESSIONS = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path(PATH_SESSIONS).build();
    private static final Uri URI_SCHEDULES = new Uri.Builder().scheme(ContentResolver.SCHEME_CONTENT).authority(AUTHORITY).path(PATH_SCHEDULES).build();
    private static final int TYPE_WORKOUTS = 1;
    private static final int TYPE_WORKOUT_ID = 2;
    private static final int TYPE_SESSIONS = 3;
    private static final int TYPE_SESSION_ID = 4;
    private static final int TYPE_WORKOUT_ID_SCHEDULES = 5;
    private static final int TYPE_WORKOUT_ID_SCHEDULE_ID = 6;
    private static final int TYPE_SCHEDULES = 7;
    private static final int TYPE_SCHEDULE_ID = 8;
    private static final UriMatcher uriMatcher = new UriMatcher(0);
    public static final String VND_PREFIX = "vnd";

    static {
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS, TYPE_WORKOUTS);
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS + "/#", TYPE_WORKOUT_ID);
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS + "/#/" + PATH_SCHEDULES, TYPE_WORKOUT_ID_SCHEDULES);
        uriMatcher.addURI(AUTHORITY, PATH_WORKOUTS + "/#/" + PATH_SCHEDULES + "/#", TYPE_WORKOUT_ID_SCHEDULE_ID);
        uriMatcher.addURI(AUTHORITY, PATH_SESSIONS, TYPE_SESSIONS);
        uriMatcher.addURI(AUTHORITY, PATH_SESSIONS + "/#", TYPE_SESSION_ID);
        uriMatcher.addURI(AUTHORITY, PATH_SCHEDULES, TYPE_SCHEDULES);
        uriMatcher.addURI(AUTHORITY, PATH_SCHEDULES + "/#", TYPE_SCHEDULE_ID);
    }

    private QuickFitDbHelper database;

    public static Uri getUriWorkoutsList() {
        return URI_WORKOUTS;
    }

    public static Uri getUriWorkoutsId(long workoutId) {
        return ContentUris.withAppendedId(getUriWorkoutsList(), workoutId);
    }

    public static Uri getUriWorkoutsIdSchedules(long workoutId) {
        return getUriWorkoutsId(workoutId).buildUpon().appendEncodedPath(PATH_SCHEDULES).build();
    }

    public static Uri getUriWorkoutsIdSchedulesId(long workoutId, long scheduleId) {
        return ContentUris.withAppendedId(getUriWorkoutsIdSchedules(workoutId), scheduleId);
    }

    public static Uri getUriSchedulesList() {
        return URI_SCHEDULES;
    }

    public static Uri getUriSchedulesId(long scheduleId) {
        return ContentUris.withAppendedId(getUriSchedulesList(), scheduleId);
    }

    public static Uri getUriSessionsList() {
        return URI_SESSIONS;
    }

    public static Uri getUriSessionsId(long sessionId) {
        return ContentUris.withAppendedId(getUriSessionsList(), sessionId);
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
                TablesAndAliases tableAndProjection = WorkoutEntry.toAlias(projection);
                aliasedProjection = tableAndProjection.aliases;
                queryBuilder.setTables(tableAndProjection.tableExpression);
                switch (type) {
                    case TYPE_WORKOUT_ID:
                    case TYPE_WORKOUT_ID_SCHEDULES:
                        if (tableAndProjection.tables.contains(WorkoutEntry.TABLE_NAME)) {
                            queryBuilder.appendWhere(WorkoutEntry.TABLE_NAME + "." + WorkoutEntry.COL_ID + "=?");
                        } else {
                            queryBuilder.appendWhere(ScheduleEntry.TABLE_NAME + "." + ScheduleEntry.COL_WORKOUT_ID + "=?");
                        }
                        moreSelectionArgs.add(uri.getPathSegments().get(1));
                        break;
                    case TYPE_WORKOUT_ID_SCHEDULE_ID:
                        queryBuilder.appendWhere(ScheduleEntry.TABLE_NAME + "." + ScheduleEntry.COL_ID + "=?");
                        moreSelectionArgs.add(uri.getLastPathSegment());
                        break;
                    case TYPE_WORKOUTS:
                        break;
                }
                break;
            case TYPE_SCHEDULE_ID:
                queryBuilder.appendWhere(ScheduleEntry.COL_ID + "=?");
                moreSelectionArgs.add(uri.getLastPathSegment());
            case TYPE_SCHEDULES:
                queryBuilder.setTables(ScheduleEntry.TABLE_NAME);
                break;
            case TYPE_SESSION_ID:
                queryBuilder.appendWhere(SessionEntry._ID + "=?");
                moreSelectionArgs.add(uri.getLastPathSegment());
            case TYPE_SESSIONS:
                queryBuilder.setTables(SessionEntry.TABLE_NAME);
                break;
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }

        String[] expandedSelectionArgs = expandSelectionArgs(selectionArgs, moreSelectionArgs);

        SQLiteDatabase db = database.getReadableDatabase();
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
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND_PREFIX + "." + AUTHORITY + ".workout";
            case TYPE_WORKOUTS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND_PREFIX + "." + AUTHORITY + ".workout";
            case TYPE_SESSION_ID:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND_PREFIX + "." + AUTHORITY + ".session";
            case TYPE_SESSIONS:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND_PREFIX + "." + AUTHORITY + ".session";
            case TYPE_WORKOUT_ID_SCHEDULES:
            case TYPE_SCHEDULES:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + VND_PREFIX + "." + AUTHORITY + ".schedule";
            case TYPE_WORKOUT_ID_SCHEDULE_ID:
            case TYPE_SCHEDULE_ID:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + VND_PREFIX + "." + AUTHORITY + ".schedule";
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
                id = sqlDB.insert(WorkoutEntry.TABLE_NAME, null, values);
                break;
            case TYPE_SESSIONS:
                id = sqlDB.insert(SessionEntry.TABLE_NAME, null, values);
                break;
            case TYPE_WORKOUT_ID_SCHEDULES:
                ContentValues expandedValues = new ContentValues(values);
                expandedValues.put(ScheduleEntry.COL_WORKOUT_ID, uri.getPathSegments().get(1));
                id = sqlDB.insert(ScheduleEntry.TABLE_NAME, null, expandedValues);
                break;
            case TYPE_SCHEDULES:
                id = sqlDB.insert(ScheduleEntry.TABLE_NAME, null, values);
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
                rowsDeleted = sqlDB.delete(WorkoutEntry.TABLE_NAME, selection,
                        selectionArgs);
                break;
            }
            case TYPE_WORKOUT_ID: {
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(WorkoutEntry.TABLE_NAME,
                            WorkoutEntry.COL_ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsDeleted = sqlDB.delete(WorkoutEntry.TABLE_NAME,
                            WorkoutEntry.COL_ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            }
            case TYPE_SESSIONS: {
                rowsDeleted = sqlDB.delete(SessionEntry.TABLE_NAME, selection,
                        selectionArgs);
                break;
            }
            case TYPE_SESSION_ID: {
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
            case TYPE_SCHEDULES:
            case TYPE_WORKOUT_ID_SCHEDULES: {
                rowsDeleted = sqlDB.delete(ScheduleEntry.TABLE_NAME, selection, selectionArgs);
                break;
            }
            case TYPE_SCHEDULE_ID:
            case TYPE_WORKOUT_ID_SCHEDULE_ID: {
                if (TextUtils.isEmpty(selection)) {
                    rowsDeleted = sqlDB.delete(ScheduleEntry.TABLE_NAME,
                            ScheduleEntry.COL_ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsDeleted = sqlDB.delete(ScheduleEntry.TABLE_NAME,
                            ScheduleEntry.COL_ID + "=? and " + selection,
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
        Timber.d("ContentProvider update for uri %s, with content values %s", uri, values);
        SQLiteDatabase sqlDB = database.getWritableDatabase();
        int rowsUpdated;
        switch (uriMatcher.match(uri)) {
            case TYPE_WORKOUTS:
                rowsUpdated = sqlDB.update(WorkoutEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case TYPE_WORKOUT_ID:
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(WorkoutEntry.TABLE_NAME,
                            values,
                            WorkoutEntry.COL_ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsUpdated = sqlDB.update(WorkoutEntry.TABLE_NAME,
                            values,
                            WorkoutEntry.COL_ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            case TYPE_SESSIONS:
                rowsUpdated = sqlDB.update(SessionEntry.TABLE_NAME,
                        values,
                        selection,
                        selectionArgs);
                break;
            case TYPE_SESSION_ID:
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
            case TYPE_SCHEDULES:
            case TYPE_WORKOUT_ID_SCHEDULES:
                rowsUpdated = sqlDB.update(ScheduleEntry.TABLE_NAME, values, selection, selectionArgs);
                break;
            case TYPE_SCHEDULE_ID:
            case TYPE_WORKOUT_ID_SCHEDULE_ID:
                if (TextUtils.isEmpty(selection)) {
                    rowsUpdated = sqlDB.update(ScheduleEntry.TABLE_NAME,
                            values,
                            ScheduleEntry.COL_ID + "=?",
                            new String[]{uri.getLastPathSegment()});
                } else {
                    rowsUpdated = sqlDB.update(ScheduleEntry.TABLE_NAME,
                            values,
                            ScheduleEntry.COL_ID + "=? and " + selection,
                            expandSelectionArgs(selectionArgs, Collections.singletonList(uri.getLastPathSegment())));
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid content URI:" + uri);
        }
        //noinspection ConstantConditions
        getContext().getContentResolver().notifyChange(uri, null);
        Timber.d("%d rows updated.", rowsUpdated);
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
