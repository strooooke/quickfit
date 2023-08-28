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

package com.lambdasoup.quickfit.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.SparseArray
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * This helper is modeled after the old [androidx.legacy.content.WakefulBroadcastReceiver] - which is deprecated by now, and is indeed
 * useless now, as it does not start the service as a foreground service - so is in general not allowed to do so.
 *
 * The documentation there suggests enqueueing a JobScheduler Job instead. In this project, for the time being, we use ForegroundServices
 * instead, as it is unclear (especially on API levels below 26) whether there are any guarantees about somewhat-timely execution of
 * a restriction-free job. The work we're talking about here is not really deferrable, it's just IO work that happens in the background.
 */
object WakefulIntents {
    private const val EXTRA_WAKE_LOCK_ID = "com.lambdasoup.quickfit.util.WakefulBroadcastReceiver.WAKE_LOCK_ID"

    private val activeWakeLocks = SparseArray<PowerManager.WakeLock>()
    private var nextId = 1

    fun startWakefulForegroundService(context: Context, intent: Intent) {
        synchronized(activeWakeLocks) {
            val id = nextId
            nextId++
            if (nextId <= 0) {
                nextId = 1
            }

            intent.putExtra(EXTRA_WAKE_LOCK_ID, id)

            // Not using [ContextCompat.startForegroundService] here, because we need the ComponentName
            // because if null is returned, we should not acquire a wakelock - it will never be released.
            val componentName = if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(intent)
            } else {
                // Pre-O behavior.
                context.startService(intent)
            }

            if (componentName == null) {
                return
            }

            val wl = (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
                    .newWakeLock(
                            PowerManager.PARTIAL_WAKE_LOCK,
                            "com.lambdasoup.quickfit:wake:" + componentName.flattenToShortString()
                    )
            wl.setReferenceCounted(false)
            wl.acquire(TimeUnit.SECONDS.toMillis(60))
            activeWakeLocks.put(id, wl)
        }
    }

    fun completeWakefulIntent(intent: Intent) {
        val id = intent.getIntExtra(EXTRA_WAKE_LOCK_ID, 0)
        if (id == 0) {
            return
        }

        synchronized(activeWakeLocks) {
            val wl = activeWakeLocks[id]

            if (wl == null) {
                // We just log a warning here if there is no wake lock found, which could
                // happen for example if this function is called twice on the same
                // intent or the process is killed and restarted before processing the intent.
                Timber.w("No active wake lock id #$id")

                return
            }

            wl.release()
            activeWakeLocks.remove(id)
        }
    }
}
