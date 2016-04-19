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
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.lambdasoup.quickfit.FitActivityService;
import com.lambdasoup.quickfit.persist.FitApiFailureResolutionService;

/**
 * Base class for activities that bind to {@link FitApiFailureResolutionService}; which allows to interrupt the
 * user with Fit Api connection failure resolution while this activity is in the foreground. Can also be started with an
 * intent with a failure connection result as extra to start the resolution process.
 */
public abstract class BaseActivity extends DialogActivity implements FitApiFailureResolutionService.FitApiFailureResolver {
    private static final String TAG = BaseActivity.class.getSimpleName();

    public static final String EXTRA_PLAY_API_CONNECT_RESULT = "com.lambdasoup.quickfit.play_api_connect_result";
    private static final int REQUEST_FAILURE_RESOLUTION = 0;
    private static final String KEY_FAILURE_RESOLUTION_IN_PROGRESS = "com.lambdasoup.quickfit.failure_resolution_in_progress";
    private static final String TAG_ERROR_DIALOG = "error_dialog";
    private static final String ARG_ERROR_CODE = "com.lambdasoup.quickfit.play_api_error_code";

    boolean failureResolutionInProgress = false;

    private ServiceConnection fitApiFailureResolutionServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            FitApiFailureResolutionService.Binder binder = (FitApiFailureResolutionService.Binder) service;
            binder.registerAsCurrentForeground(BaseActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

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
        bindService(new Intent(this, FitApiFailureResolutionService.class), fitApiFailureResolutionServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(fitApiFailureResolutionServiceConnection);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // if started from notification (failure occured while no activity was bound to the FitApiFailureResolutionService)
        ConnectionResult connectionResult = getIntent().getParcelableExtra(EXTRA_PLAY_API_CONNECT_RESULT);
        if (connectionResult != null) {
            onFitApiFailure(connectionResult);
        }
    }

    @Override
    public void onFitApiFailure(ConnectionResult connectionResult) {
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
                Log.e(TAG, "Exception while starting resolution activity", e);
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

    public void onDialogDismissed() {
        failureResolutionInProgress = false;
    }

    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int errorCode = this.getArguments().getInt(ARG_ERROR_CODE);
            return GoogleApiAvailability.getInstance().getErrorDialog(
                    this.getActivity(), errorCode, REQUEST_FAILURE_RESOLUTION);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((BaseActivity) getActivity()).onDialogDismissed();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_FAILURE_RESOLUTION) {
            failureResolutionInProgress = false;
            getIntent().removeExtra(EXTRA_PLAY_API_CONNECT_RESULT);
            if (resultCode == RESULT_OK) {
                startService(FitActivityService.getIntentSyncSession(getApplicationContext()));
            }
        }
    }


}
