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

package com.lambdasoup.quickfit.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.lambdasoup.quickfit.FitActivityService;
import com.lambdasoup.quickfit.persist.FitApiFailureResolution;
import com.lambdasoup.quickfit.persist.FitApiFailureResolver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import timber.log.Timber;

/**
 * Base class for activities that bind to {@link FitApiFailureResolution}; which allows to interrupt the
 * user with Fit Api connection failure resolution while this activity is in the foreground. Can also be started with an
 * intent with a failure connection result as extra to start the resolution process.
 */
public abstract class FitFailureResolutionActivity extends DialogActivity implements FitApiFailureResolver {
    public static final String EXTRA_PLAY_API_CONNECT_RESULT = "com.lambdasoup.quickfit.play_api_connect_result";
    private static final int REQUEST_FAILURE_RESOLUTION = 0;
    private static final String KEY_FAILURE_RESOLUTION_IN_PROGRESS = "com.lambdasoup.quickfit.failure_resolution_in_progress";
    private static final String TAG_ERROR_DIALOG = "error_dialog";
    private static final String ARG_ERROR_CODE = "com.lambdasoup.quickfit.play_api_error_code";

    private boolean failureResolutionInProgress = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            failureResolutionInProgress = savedInstanceState.getBoolean(KEY_FAILURE_RESOLUTION_IN_PROGRESS);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_FAILURE_RESOLUTION_IN_PROGRESS, failureResolutionInProgress);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FitApiFailureResolution.INSTANCE.registerAsCurrentForeground(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        FitApiFailureResolution.INSTANCE.unregisterAsCurrentForeground(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // if started from notification (failure occurred while no activity was bound to FitApiFailureResolution)
        ConnectionResult connectionResult = getIntent().getParcelableExtra(EXTRA_PLAY_API_CONNECT_RESULT);
        if (connectionResult != null) {
            onFitApiFailure(connectionResult);
        }
    }

    @Override
    public void onFitApiFailure(@NonNull ConnectionResult connectionResult) {
        if (failureResolutionInProgress) {
            // nothing to do
            return;
        }
        if (!connectionResult.hasResolution()) {
            failureResolutionInProgress = true;
            showErrorDialog(connectionResult);
        } else {
            failureResolutionInProgress = true;
            try {
                connectionResult.startResolutionForResult(this, REQUEST_FAILURE_RESOLUTION);
            } catch (IntentSender.SendIntentException e) {
                Timber.e(e, "Exception while starting resolution activity");
            }
        }
    }

    private void showErrorDialog(ConnectionResult connectionResult) {
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ERROR_CODE, connectionResult.getErrorCode());
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), TAG_ERROR_DIALOG);
    }

    private void onDialogDismissed() {
        failureResolutionInProgress = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FAILURE_RESOLUTION) {
            failureResolutionInProgress = false;
            getIntent().removeExtra(EXTRA_PLAY_API_CONNECT_RESULT);
            if (resultCode == RESULT_OK) {
                FitActivityService.Companion.enqueueSyncSession(getApplicationContext());
            }
        }
    }

    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() {
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int errorCode = this.getArguments().getInt(ARG_ERROR_CODE);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_FAILURE_RESOLUTION);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((FitFailureResolutionActivity) getActivity()).onDialogDismissed();
        }
    }


}
