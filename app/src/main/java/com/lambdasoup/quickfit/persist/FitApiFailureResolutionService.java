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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.lambdasoup.quickfit.Constants;
import com.lambdasoup.quickfit.R;
import com.lambdasoup.quickfit.ui.WorkoutListActivity;

/**
 * Service that handles fit api connection failures: by notification if no suitable activity is
 * in the foreground, or by delegating to the activity bound to it.
 *
 * All operations of this service are executed on the UI thread.
 */
public class FitApiFailureResolutionService extends Service {
    private static final String EXTRA_FAILURE_RESULT = "com.lambdasoup.quickfit.persist.EXTRA_FAILURE_RESULT";

    private final Binder binder = new Binder();
    private FitApiFailureResolver currentForegroundResolver = null;
    private boolean isBound = false;

    public static Intent getFailureResolutionIntent(Context context, ConnectionResult connectionResult) {
        if (connectionResult.isSuccess()) {
            throw new IllegalArgumentException("connectionResult was a success; would not be handled by " + FitApiFailureResolutionService.class.getSimpleName());
        }
        Intent failureResultIntent = new Intent(context, FitApiFailureResolutionService.class);
        failureResultIntent.putExtra(EXTRA_FAILURE_RESULT, connectionResult);
        return  failureResultIntent;
    }

    public FitApiFailureResolutionService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!intent.hasExtra(EXTRA_FAILURE_RESULT)) {
            throw new IllegalArgumentException("Required extra " + EXTRA_FAILURE_RESULT + " missing.");
        }
        ConnectionResult connectionResult = intent.getParcelableExtra(EXTRA_FAILURE_RESULT);
        if (isBound && currentForegroundResolver != null) {
            handleErrorInForeground(connectionResult, startId);
        } else {
            handleErrorInBackground(connectionResult, startId);
        }
        return START_REDELIVER_INTENT;
    }

    @Override
    public IBinder onBind(Intent intent) {
        isBound = true;
        return binder;
    }

    @Override
    public void onRebind(Intent intent) {
        isBound = true;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        isBound = false;
        return true;
    }

    private void handleErrorInForeground(ConnectionResult connectionResult, int startId) {
        currentForegroundResolver.onFitApiFailure(connectionResult);
        stopSelfResult(startId);
    }

    private void handleErrorInBackground(ConnectionResult connectionResult, int startId) {
        if (!connectionResult.hasResolution()) {
            // Show the localized error notification
            GoogleApiAvailability.getInstance().showErrorNotification(this, connectionResult.getErrorCode());
            return;
        }
        // The failure has a resolution. Resolve it.
        // Called typically when the app is not yet authorized, and an
        // authorization dialog is displayed to the user.
        Intent resultIntent = new Intent(this, WorkoutListActivity.class);
        resultIntent.putExtra(WorkoutListActivity.EXTRA_PLAY_API_CONNECT_RESULT, connectionResult);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(WorkoutListActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentTitle(getResources().getString(R.string.permission_needed_play_service_title))
                .setContentText(getResources().getString(R.string.permission_needed_play_service))
                .setSmallIcon(R.drawable.common_ic_googleplayservices)
                .setContentIntent(resultPendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            notificationBuilder.setCategory(Notification.CATEGORY_ERROR);
        }

        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(Constants.NOTIFICATION_PLAY_INTERACTION, notificationBuilder.build());
        stopSelfResult(startId);
    }

    public class Binder extends android.os.Binder {
        public void registerAsCurrentForeground(FitApiFailureResolver fitApiFailureResolver) {
            FitApiFailureResolutionService.this.currentForegroundResolver = fitApiFailureResolver;
        }
    }

    public interface FitApiFailureResolver {
        void onFitApiFailure(ConnectionResult connectionResult);
    }
}
