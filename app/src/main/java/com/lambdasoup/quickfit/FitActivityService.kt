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

package com.lambdasoup.quickfit

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.core.app.JobIntentService
import androidx.work.*
import com.lambdasoup.quickfit.Constants.JOB_ID_FIT_ACTIVITY_SERVICE
import com.lambdasoup.quickfit.persist.QuickFitContentProvider
import com.lambdasoup.quickfit.persist.QuickFitContract.SessionEntry
import com.lambdasoup.quickfit.persist.QuickFitContract.WorkoutEntry
import com.lambdasoup.quickfit.persist.SyncWorker
import timber.log.Timber
import java.util.concurrent.TimeUnit

class FitActivityService : JobIntentService() {

    override fun onHandleWork(intent: Intent) {
        when(val action = intent.action) {
            ACTION_INSERT_SESSION -> {
                val workoutId = intent.getLongExtra(EXTRA_WORKOUT_ID, -1)
                handleInsertSession(workoutId)
            }
            ACTION_SESSION_SYNC -> requestSync()
            ACTION_SET_PERIODIC_SYNC -> setPeriodicSync()
            else -> throw IllegalArgumentException("Action $action not supported.")
        }
    }

    @WorkerThread
    private fun handleInsertSession(workoutId: Long) {
        val cursor = contentResolver.query(
                QuickFitContentProvider.getUriWorkoutsId(workoutId),
                WorkoutEntry.COLUMNS_WORKOUT_ONLY,
                null,
                null,
                null
        )
        if (cursor == null || !cursor.moveToFirst()) {
            Timber.w("Workout missing with id: %d", workoutId)
            return
        }

        val durationInMinutes = cursor.getInt(cursor.getColumnIndex(WorkoutEntry.DURATION_MINUTES))
        val endTime = System.currentTimeMillis()
        val startTime = endTime - TimeUnit.MINUTES.toMillis(durationInMinutes.toLong())

        val values = ContentValues(6).apply {
            put(SessionEntry.ACTIVITY_TYPE, cursor.getString(cursor.getColumnIndex(WorkoutEntry.ACTIVITY_TYPE)))
            put(SessionEntry.START_TIME, startTime)
            put(SessionEntry.END_TIME, endTime)
            put(SessionEntry.STATUS, SessionEntry.SessionStatus.NEW.name)
            put(SessionEntry.NAME, cursor.getString(cursor.getColumnIndex(WorkoutEntry.LABEL)))
            put(SessionEntry.CALORIES, cursor.getInt(cursor.getColumnIndex(WorkoutEntry.CALORIES)))
        }

        cursor.close()

        contentResolver.insert(QuickFitContentProvider.getUriSessionsList(), values)
        requestSync()
        showToast(R.string.success_session_insert)
    }

    @WorkerThread
    private fun showToast(@StringRes resId: Int) {
        Handler(mainLooper).post { Toast.makeText(applicationContext, resId, Toast.LENGTH_SHORT).show() }
    }

    @WorkerThread
    private fun requestSync() {
        Timber.d("About to enqueue one-time sync:")
        val workRequest = OneTimeWorkRequest.Builder(SyncWorker::class.java)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
        WorkManager.getInstance(applicationContext)
                .beginUniqueWork(
                        "sync",
                        ExistingWorkPolicy.REPLACE,
                        workRequest
                )
                .enqueue()
                .result
                .get()
        Timber.d("enqueue done")
    }

    @WorkerThread
    private fun setPeriodicSync() {
        val workRequest = PeriodicWorkRequest.Builder(
                SyncWorker::class.java,
                3, TimeUnit.HOURS,
                3, TimeUnit.HOURS
        )
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

        WorkManager.getInstance(applicationContext)
                .enqueueUniquePeriodicWork(
                        "periodicsync",
                        ExistingPeriodicWorkPolicy.KEEP,
                        workRequest
                )
    }

    companion object {
        private const val ACTION_INSERT_SESSION = "com.lambdasoup.quickfit.action.INSERT_SESSION"
        private const val ACTION_SESSION_SYNC = "com.lambdasoup.quickfit.action.SESSION_SYNC"
        private const val ACTION_SET_PERIODIC_SYNC = "com.lambdasoup.quickfit.action.SET_PERIODIC_SYNC"

        private const val EXTRA_WORKOUT_ID = "com.lambdasoup.quickfit.alarm.WORKOUT_ID"

        fun enqueueInsertSession(context: Context, workoutId: Long) {
            val intent = Intent(ACTION_INSERT_SESSION).putExtra(EXTRA_WORKOUT_ID, workoutId)
            enqueueWork(context, FitActivityService::class.java, JOB_ID_FIT_ACTIVITY_SERVICE, intent)
        }

        fun enqueueSyncSession(context: Context) {
            val intent = Intent(ACTION_SESSION_SYNC)
            enqueueWork(context, FitActivityService::class.java, JOB_ID_FIT_ACTIVITY_SERVICE, intent)
        }

        fun enqueueSetPeriodicSync(context: Context) {
            val intent = Intent(ACTION_SET_PERIODIC_SYNC)
            enqueueWork(context, FitActivityService::class.java, JOB_ID_FIT_ACTIVITY_SERVICE, intent)
        }
    }

}
