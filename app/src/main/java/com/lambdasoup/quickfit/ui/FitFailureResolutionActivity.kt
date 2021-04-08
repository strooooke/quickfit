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
package com.lambdasoup.quickfit.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.lambdasoup.quickfit.Constants.FITNESS_API_OPTIONS
import com.lambdasoup.quickfit.FitActivityService.Companion.enqueueSyncSession
import com.lambdasoup.quickfit.persist.FitApiFailureResolution
import com.lambdasoup.quickfit.persist.FitApiFailureResolution.registerAsCurrentForeground
import com.lambdasoup.quickfit.persist.FitApiFailureResolution.unregisterAsCurrentForeground
import com.lambdasoup.quickfit.persist.FitApiFailureResolver
import timber.log.Timber

/**
 * Base class for activities that bind to [FitApiFailureResolution]; which allows to interrupt the
 * user with Fit Api connection failure resolution while this activity is in the foreground. Can also be started with an
 * intent with a failure connection result as extra to start the resolution process.
 */
abstract class FitFailureResolutionActivity : DialogActivity(), FitApiFailureResolver {
    private var failureResolutionInProgress = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            failureResolutionInProgress = savedInstanceState.getBoolean(KEY_FAILURE_RESOLUTION_IN_PROGRESS)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_FAILURE_RESOLUTION_IN_PROGRESS, failureResolutionInProgress)
    }

    override fun onStart() {
        super.onStart()
        registerAsCurrentForeground(this)
    }

    override fun onStop() {
        super.onStop()
        unregisterAsCurrentForeground(this)
    }

    override fun onResume() {
        super.onResume()

        // if started from notification (failure occurred while no activity was bound to FitApiFailureResolution)
        val account: GoogleSignInAccount? = intent.getParcelableExtra(EXTRA_PLAY_API_SIGNIN_ACCOUNT)
        if (account != null) {
            requestFitPermissions(account)
        }
    }

    override fun requestFitPermissions(account: GoogleSignInAccount) {
        if (failureResolutionInProgress) {
            // nothing to do
            return
        }
        failureResolutionInProgress = true
        try {
            GoogleSignIn.requestPermissions(
                    this,
                    REQUEST_FAILURE_RESOLUTION,
                    account,
                    FITNESS_API_OPTIONS
            )
        } catch (e: ActivityNotFoundException) {
            Timber.e(e, "Exception while starting resolution activity")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_FAILURE_RESOLUTION) {
            failureResolutionInProgress = false
            intent.removeExtra(EXTRA_PLAY_API_SIGNIN_ACCOUNT)
            if (resultCode == RESULT_OK) {
                enqueueSyncSession(applicationContext)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    companion object {
        const val EXTRA_PLAY_API_SIGNIN_ACCOUNT = "com.lambdasoup.quickfit.play_api_connect_result"
        private const val REQUEST_FAILURE_RESOLUTION = 0
        private const val KEY_FAILURE_RESOLUTION_IN_PROGRESS = "com.lambdasoup.quickfit.failure_resolution_in_progress"
    }
}
