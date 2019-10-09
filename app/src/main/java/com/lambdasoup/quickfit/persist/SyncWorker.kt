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
import android.os.Bundle
import android.os.RemoteException
import androidx.concurrent.futures.CallbackToFutureAdapter
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.SessionsClient
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.SessionInsertRequest
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.SuccessContinuation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.common.util.concurrent.ListenableFuture
import timber.log.Timber
import java.util.concurrent.Callable
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
        val fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_ACTIVITY_SEGMENT, FitnessOptions.ACCESS_WRITE)
                .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_WRITE)
                .build()

        fun onFailure() {
            Timber.d("Sign-in required, getting intent")
            val signInIntent = GoogleSignIn.getClient(
                    appContext,
                    GoogleSignInOptions.Builder().addExtension(fitnessOptions).build()
            ).signInIntent

            // TODO: cannot start service from background. there goes the architecture.
            Timber.d("I'd like to get this intent resolved appropriately, but I can't yet. ${signInIntent}")
            //appContext.startService(FitApiFailureResolutionService.getFailureResolutionIntent(appContext, signInIntent))
            completer.set(Result.failure()) // TODO or retry?
        }

        val account = GoogleSignIn.getLastSignedInAccount(appContext) ?: run {
            onFailure()
            return@getFuture "Sign-in required immediately"
        }

        sessionsClient = Fitness.getSessionsClient(appContext, account)

        Tasks.call(SYNC_EXECUTOR, Callable {
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
                .onSuccessTask (SYNC_EXECUTOR, SuccessContinuation<Cursor?, Void> { sessionsCursor ->
                    if (sessionsCursor == null) {
                        Tasks.forResult(null)
                    } else {
                        cursor = sessionsCursor
                        insertNextSession()
                    }
                })
                .addOnCanceledListener { Timber.d("work got cancelled") }
                .addOnCompleteListener(SYNC_EXECUTOR, OnCompleteListener {
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
        // TODO completer.addCancellationListener(Runnable {  })
    }

//    override fun startWork(): ListenableFuture<Result> =
//            CallbackToFutureAdapter.getFuture { completer ->
//                val connectionCallbacks = object : GoogleApiClient.ConnectionCallbacks {
//                    override fun onConnected(bundle: Bundle?) {
//                        try {
//                            val sessionsCursor = contentResolver.query(
//                                    QuickFitContentProvider.getUriSessionsList(),
//                                    QuickFitContract.SessionEntry.COLUMNS,
//                                    QuickFitContract.SessionEntry.STATUS + "=?",
//                                    arrayOf(QuickFitContract.SessionEntry.SessionStatus.NEW.name),
//                                    null
//                            )
//                            Timber.d("Found %s sessions to sync", sessionsCursor?.count ?: "no")
//                            insertNextSession(sessionsCursor, completer, SyncStatus(hasFailedInsertions = false))
//                        } catch (e: RemoteException) {
//                            completer.set(Result.retry())
//                        }
//                    }
//
//                    override fun onConnectionSuspended(cause: Int) {
//                        Timber.d("connection suspended")
//                        if (cause == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
//                            Timber.d("Connection lost.  Cause: Network Lost.")
//                        } else if (cause == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
//                            Timber.d("Connection lost.  Reason: Service Disconnected")
//                        }
//                        completer.set(Result.retry())
//                    }
//                }
//
//                val failureCallback = GoogleApiClient.OnConnectionFailedListener { result ->
//                    Timber.d("connection failed: %s", result.errorMessage)
//                    appContext.startService(FitApiFailureResolutionService.getFailureResolutionIntent(appContext, result))
//                    completer.set(Result.failure())
//                }
//
//                googleApiClient = GoogleApiClient.Builder(appContext)
//                        .addApi(Fitness.SESSIONS_API)
//                        .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
//                        .addConnectionCallbacks(connectionCallbacks)
//                        .addOnConnectionFailedListener(failureCallback)
//                        .build()
//
//                completer.addCancellationListener(Runnable { googleApiClient?.disconnect() }, Executors.newSingleThreadExecutor())
//
//                Timber.d("starting sync work")
//                googleApiClient!!.connect()
//            }

    private fun insertNextSession(): Task<Void> {
        if (!cursor.moveToNext()) {
            // done with sessions
            Timber.d("Done; disconnecting.")
            // TODO googleApiClient!!.disconnect()
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
                            .setTimestamp(startTime, TimeUnit.MILLISECONDS)
                            .setFloatValues(cursor.getInt(cursor.getColumnIndex(QuickFitContract.SessionEntry.CALORIES)).toFloat())
                            .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                            .build()
            )
        }

        return sessionsClient.insertSession(insertRequest.build())
                .addOnSuccessListener(SYNC_EXECUTOR, OnSuccessListener {
                    contentResolver.update(
                            QuickFitContentProvider.getUriSessionsId(cursor.getLong(cursor.getColumnIndex(QuickFitContract.SessionEntry._ID))),
                            STATUS_TRANSMITTED, null, null
                    )
                    Timber.d("insertion successful")
                })
                .addOnFailureListener(SYNC_EXECUTOR, OnFailureListener { e ->
                    Timber.w(e, "insertion failed")
                    //syncStatus.hasFailedInsertion = true
                })
                .continueWithTask(SYNC_EXECUTOR, Continuation { insertNextSession() })
    }
}
