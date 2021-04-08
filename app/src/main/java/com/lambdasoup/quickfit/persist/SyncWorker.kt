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

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.SessionsClient
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.SessionInsertRequest
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.common.util.concurrent.ListenableFuture
import com.lambdasoup.quickfit.Constants.FITNESS_API_OPTIONS
import timber.log.Timber
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

private val SYNC_EXECUTOR = Executors.newSingleThreadExecutor { r -> Thread(r, "sync worker thread") }
private val STATUS_TRANSMITTED = ContentValues().apply {
    put(QuickFitContract.SessionEntry.STATUS, QuickFitContract.SessionEntry.SessionStatus.SYNCED.name)
}

class SyncWorker(private val appContext: Context, workerParams: WorkerParameters) : ListenableWorker(appContext, workerParams) {

    private val contentResolver: ContentResolver by lazy { appContext.contentResolver }

    // Exactly one worker gets instantiated per unit of work, so we use the instance to keep our state around.
    // Does not apply to cursor, because that is nullable...
    private lateinit var sessionsClient: SessionsClient
    private lateinit var cursor: Cursor
    private var hasFailedInsertions = false

    override fun startWork(): ListenableFuture<Result> = CallbackToFutureAdapter.getFuture { completer ->
        Tasks.call(SYNC_EXECUTOR, {
                    val sessionsCursor = contentResolver.query(
                            QuickFitContentProvider.getUriSessionsList(),
                            QuickFitContract.SessionEntry.COLUMNS,
                            QuickFitContract.SessionEntry.STATUS + "=?",
                            arrayOf(QuickFitContract.SessionEntry.SessionStatus.NEW.name),
                            null
                    )
                    Timber.d("Found %s sessions to sync", sessionsCursor?.count ?: "no")
                    sessionsCursor
                })
                .onSuccessTask (SYNC_EXECUTOR, { sessionsCursor ->
                    if (sessionsCursor == null || sessionsCursor.isAfterLast) {
                        sessionsCursor?.close()
                        Tasks.forResult(null)
                    } else {
                        val account = GoogleSignIn.getAccountForExtension(appContext, FITNESS_API_OPTIONS)

                        if (!GoogleSignIn.hasPermissions(account, FITNESS_API_OPTIONS)) {
                            Timber.d("Sign-in required")
                            FitApiFailureResolution.requestFitPermissions(appContext, account)

                            // will be retried after sign in resolution
                            completer.set(Result.failure())
                            return@onSuccessTask Tasks.forCanceled()
                        }
                        sessionsClient = Fitness.getSessionsClient(appContext, account)
                        cursor = sessionsCursor
                        insertNextSession()
                    }
                })
                .addOnCanceledListener { Timber.d("work got cancelled") }
                .addOnCompleteListener(SYNC_EXECUTOR, {
                    completer.set(
                            it.exception.let { t ->
                                if (t == null) {
                                    // task is successful.
                                    Timber.d("Sync complete")
                                    if (hasFailedInsertions) {
                                        Result.retry()
                                    } else {
                                        Result.success()
                                    }
                                } else {
                                    completer.setException(t)
                                    Result.failure() // TODO: or retry? depend on exception? might need to do the signIn thing?
                                }
                            }
                    )
                })
    }

    private fun insertNextSession(): Task<Void> {
        if (!cursor.moveToNext()) {
            // done with sessions
            Timber.d("Done.")
            cursor.close()
            // sync finished
            return Tasks.forResult(null)
        }

        val startTime = cursor.getLong(cursor.getColumnIndex(QuickFitContract.SessionEntry.START_TIME))
        val endTime = cursor.getLong(cursor.getColumnIndex(QuickFitContract.SessionEntry.END_TIME))
        val sessionName = cursor.getString(cursor.getColumnIndex(QuickFitContract.SessionEntry.NAME))
        val sessionBuilder = Session.Builder()
                .setActivity(cursor.getString(cursor.getColumnIndex(QuickFitContract.SessionEntry.ACTIVITY_TYPE)))
                .setStartTime(startTime, TimeUnit.MILLISECONDS)
                .setEndTime(endTime, TimeUnit.MILLISECONDS)
        if (sessionName != null) {
            sessionBuilder.setName(sessionName)
        }

        val insertRequest = SessionInsertRequest.Builder()
                .setSession(sessionBuilder.build())

        if (cursor.getType(cursor.getColumnIndex(QuickFitContract.SessionEntry.CALORIES)) != Cursor.FIELD_TYPE_NULL) {
            val datasource = DataSource.Builder()
                    .setAppPackageName(appContext)
                    .setDataType(DataType.AGGREGATE_CALORIES_EXPENDED)
                    .setType(DataSource.TYPE_RAW)
                    .setDevice(Device.getLocalDevice(appContext))
                    .build()

            insertRequest.addAggregateDataPoint(
                    DataPoint.builder(datasource)
                            .setField(
                                    Field.FIELD_CALORIES,
                                    cursor.getInt(cursor.getColumnIndex(QuickFitContract.SessionEntry.CALORIES)).toFloat()
                            )
                            // remove 1 ms from end time, so the fit api will accept this data point as nested inside the session
                            .setTimeInterval(startTime, endTime - 1, TimeUnit.MILLISECONDS)
                            .build()
            )
        }

        return sessionsClient.insertSession(insertRequest.build())
                .addOnSuccessListener(SYNC_EXECUTOR, {
                    contentResolver.update(
                            QuickFitContentProvider.getUriSessionsId(cursor.getLong(cursor.getColumnIndex(QuickFitContract.SessionEntry._ID))),
                            STATUS_TRANSMITTED, null, null
                    )
                    Timber.d("insertion successful")
                })
                .addOnFailureListener(SYNC_EXECUTOR, { e ->
                    Timber.w(e, "insertion failed")
                    hasFailedInsertions = true
                })
                .continueWithTask(SYNC_EXECUTOR, { insertNextSession() })
    }
}
