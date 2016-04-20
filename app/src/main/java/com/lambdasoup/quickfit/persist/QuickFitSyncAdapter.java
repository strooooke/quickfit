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

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SyncResult;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;

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

import timber.log.Timber;

class QuickFitSyncAdapter extends AbstractThreadedSyncAdapter {
    private static final ContentValues STATUS_TRANSMITTED = new ContentValues();

    static {
        STATUS_TRANSMITTED.put(QuickFitContract.SessionEntry.STATUS, QuickFitContract.SessionEntry.SessionStatus.SYNCED.name());
    }

    private final ContentResolver contentResolver;
    private GoogleApiClient googleApiClient;

    public QuickFitSyncAdapter(Context context) {
        super(context, true, false);
        contentResolver = context.getContentResolver();
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        googleApiClient = new GoogleApiClient.Builder(getContext())
                .addApi(Fitness.SESSIONS_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                try {
                                    Cursor sessionsCursor = provider.query(
                                            QuickFitContentProvider.getUriSessionsList(),
                                            QuickFitContract.SessionEntry.COLUMNS,
                                            QuickFitContract.SessionEntry.STATUS + "=?",
                                            new String[]{QuickFitContract.SessionEntry.SessionStatus.NEW.name()},
                                            null);
                                    Timber.d("Found %s sessions to sync", (sessionsCursor == null ? "no" : sessionsCursor.getCount()));
                                    insertNextSession(sessionsCursor, syncResult);
                                } catch (RemoteException e) {
                                    syncResult.stats.numParseExceptions++;
                                }
                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                Timber.d("connection suspended");
                                syncResult.stats.numIoExceptions++;
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Timber.d("Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Timber.d("Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        result -> {
                            Timber.d("connection failed");
                            getContext().startService(FitApiFailureResolutionService.getFailureResolutionIntent(getContext(), result));
                        }
                )
                .build();
        googleApiClient.connect();
    }

    private void insertNextSession(Cursor cursor, SyncResult syncResult) {
        if (!cursor.moveToNext()) {
            // done with sessions
            Timber.d("Done; disconnecting.");
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
                        QuickFitContentProvider.getUriSessionsId(cursor.getLong(cursor.getColumnIndex(QuickFitContract.SessionEntry._ID))),
                        STATUS_TRANSMITTED,
                        null,
                        null
                );
                syncResult.stats.numInserts++;
                Timber.d("insertion successful");
            } else {
                Timber.d("insertion failed: %s", status.getStatusMessage());
                syncResult.stats.numIoExceptions++;
            }
            Timber.d("Looking at the next session");
            insertNextSession(cursor, syncResult);
        });
    }
}
