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

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import java.util.concurrent.TimeUnit;

public class QuickFitSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = QuickFitSyncAdapter.class.getSimpleName();
    private static final ContentValues STATUS_TRANSMITTED = new ContentValues();
    static {
        STATUS_TRANSMITTED.put(QuickFitContract.SessionEntry.STATUS, QuickFitContract.SessionEntry.SessionStatus.SYNCED.name());
    }

    private final ContentResolver contentResolver;
    private GoogleApiClient googleApiClient;

    public QuickFitSyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        contentResolver = context.getContentResolver();
    }

    public QuickFitSyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        contentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "onPerformSync");
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .addApi(Fitness.SESSIONS_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!");
                                try {
                                    Cursor sessionsCursor = provider.query(
                                            QuickFitContentProvider.URI_SESSIONS,
                                            QuickFitContract.SessionEntry.COLUMNS,
                                            QuickFitContract.SessionEntry.STATUS + "=?",
                                            new String[]{QuickFitContract.SessionEntry.SessionStatus.NEW.name()},
                                            null);
                                    Log.i(TAG, "Found " + sessionsCursor.getCount() + " sessions to sync");
                                    insertNextSession(sessionsCursor, syncResult);
                                } catch (RemoteException e) {
                                    syncResult.stats.numParseExceptions++;
                                }
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                Log.i(TAG, "connection suspended");
                                syncResult.stats.numIoExceptions++;
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        result -> {
                            Log.i(TAG, "connection failed");
                            if (!result.hasResolution()) {
                                // Show the localized error notification
                                GoogleApiAvailability.getInstance().showErrorNotification(getContext(), result.getErrorCode());
                                return;
                            }
                            // The failure has a resolution. Resolve it.
                            // Called typically when the app is not yet authorized, and an
                            // authorization dialog is displayed to the user.
                            Intent resultIntent = new Intent(getContext(), WorkoutListActivity.class);
                            resultIntent.putExtra(WorkoutListActivity.EXTRA_PLAY_API_CONNECT_RESULT, result);
                            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getContext());
                            stackBuilder.addParentStack(WorkoutListActivity.class);
                            stackBuilder.addNextIntent(resultIntent);
                            PendingIntent resultPendingIntent =
                                    stackBuilder.getPendingIntent(
                                            0,
                                            PendingIntent.FLAG_UPDATE_CURRENT
                                    );

                            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getContext())
                                    .setContentTitle(getContext().getResources().getString(R.string.permission_needed_play_service_title))
                                    .setContentText(getContext().getResources().getString(R.string.permission_needed_play_service))
                                    .setSmallIcon(R.drawable.common_ic_googleplayservices)
                                    .setContentIntent(resultPendingIntent)
                                    .setAutoCancel(true);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                notificationBuilder.setCategory(Notification.CATEGORY_ERROR);
                            }

                            ((NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE)).notify(Constants.NOTIFICATION_PLAY_INTERACTION, notificationBuilder.build());
                        }
                )
                .build();
        googleApiClient.connect();
    }

    private void insertNextSession(Cursor cursor, SyncResult syncResult) {
        if (!cursor.moveToNext()) {
            // done with sessions
            Log.i(TAG, "Done; disconnecting.");
            googleApiClient.disconnect();
            // sync finished
            return;
        }

        long startTime = cursor.getLong(cursor.getColumnIndex(QuickFitContract.SessionEntry.START_TIME));
        long endTime = cursor.getLong(cursor.getColumnIndex(QuickFitContract.SessionEntry.END_TIME));
        String sessionName = cursor.getString(cursor.getColumnIndex(QuickFitContract.SessionEntry.NAME));
        Session.Builder sessionBuilder = new Session.Builder()
                .setActivity(cursor.getString(cursor.getColumnIndex(QuickFitContract.SessionEntry.ACTIVITY_TYPE)))
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setEndTime(endTime, TimeUnit.MILLISECONDS);
        if (sessionName != null) {
            sessionBuilder.setName(sessionName);
        }


        SessionInsertRequest.Builder insertRequest = new SessionInsertRequest.Builder()
                .setSession(sessionBuilder.build());

        if (cursor.getType(cursor.getColumnIndex(QuickFitContract.SessionEntry.CALORIES)) != Cursor.FIELD_TYPE_NULL) {
            DataSource datasource = new DataSource.Builder()
                    .setAppPackageName(getContext())
                    .setDataType(DataType.AGGREGATE_CALORIES_EXPENDED)
                    .setType(DataSource.TYPE_RAW)
                    .setDevice(Device.getLocalDevice(getContext()))
                    .build();

            insertRequest.addAggregateDataPoint(
                    DataPoint.create(datasource)
                            .setTimestamp(startTime, TimeUnit.MILLISECONDS)
                            .setFloatValues((float) cursor.getInt(cursor.getColumnIndex(QuickFitContract.SessionEntry.CALORIES)))
                            .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS));
        }

        PendingResult<Status> res = Fitness.SessionsApi.insertSession(googleApiClient, insertRequest.build());
        res.setResultCallback(status -> {
            if (status.isSuccess()) {
                contentResolver.update(
                        ContentUris.withAppendedId(QuickFitContentProvider.URI_SESSIONS, cursor.getLong(cursor.getColumnIndex(QuickFitContract.SessionEntry._ID))),
                        STATUS_TRANSMITTED,
                        null,
                        null
                );
                syncResult.stats.numInserts++;
                Log.i(TAG, "insertion successfull");
            } else {
                Log.i(TAG, "insertion failed: " + status.getStatusMessage());
                syncResult.stats.numIoExceptions++;
            }
            Log.i(TAG, "Looking at the next session");
            insertNextSession(cursor, syncResult);
        });
    }
}
