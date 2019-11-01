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

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.lambdasoup.quickfit.Constants
import com.lambdasoup.quickfit.Constants.NOTIFICATION_CHANNEL_ID_PLAY_INTERACTION
import com.lambdasoup.quickfit.R
import com.lambdasoup.quickfit.ui.FitFailureResolutionActivity.EXTRA_PLAY_API_CONNECT_RESULT
import com.lambdasoup.quickfit.ui.WorkoutListActivity
import timber.log.Timber

object FitApiFailureResolution {
    private var currentForegroundResolver: FitApiFailureResolver? = null

    fun registerAsCurrentForeground(fitApiFailureResolver: FitApiFailureResolver) {
        currentForegroundResolver = fitApiFailureResolver
    }

    fun unregisterAsCurrentForeground(fitApiFailureResolver: FitApiFailureResolver) {
        if (fitApiFailureResolver == currentForegroundResolver) {
            currentForegroundResolver = null
        }
    }

    fun resolveFailure(context: Context, connectionResult: ConnectionResult) {
        Timber.d("Trying to resolve Fit API error while application in foreground")
        currentForegroundResolver?.onFitApiFailure(connectionResult) ?: run {

            Timber.d("Resolving Fit API error while application in background")
            if (!connectionResult.hasResolution()) {
                // Show the localized error notification
                GoogleApiAvailability.getInstance().showErrorNotification(context, connectionResult.errorCode)
                return
            }
            // The failure has a resolution. Resolve it.
            // Called typically when the app is not yet authorized, and an
            // authorization dialog is displayed to the user.
            val resultIntent = Intent(context, WorkoutListActivity::class.java)
            resultIntent.putExtra(EXTRA_PLAY_API_CONNECT_RESULT, connectionResult)
            val stackBuilder = TaskStackBuilder.create(context)
            stackBuilder.addParentStack(WorkoutListActivity::class.java)
            stackBuilder.addNextIntent(resultIntent)
            val resultPendingIntent = stackBuilder.getPendingIntent(
                    0,
                    PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notificationBuilder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID_PLAY_INTERACTION)
                    .setContentTitle(context.resources.getString(R.string.permission_needed_play_service_title))
                    .setContentText(context.resources.getString(R.string.permission_needed_play_service))
                    .setSmallIcon(R.drawable.ic_fitness_center_white_24px)
                    .setContentIntent(resultPendingIntent)
                    .setAutoCancel(true)

            notificationBuilder.setCategory(Notification.CATEGORY_ERROR)

            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                    .notify(Constants.NOTIFICATION_PLAY_INTERACTION, notificationBuilder.build())
        }
    }

    fun initNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID_PLAY_INTERACTION,
                    context.getString(R.string.notification_channel_play_interaction_name),
                    NotificationManager.IMPORTANCE_DEFAULT
            )

            context.getSystemService(NotificationManager::class.java)!!.createNotificationChannel(channel)
        }
    }
}

interface FitApiFailureResolver {
    fun onFitApiFailure(connectionResult: ConnectionResult)
}
