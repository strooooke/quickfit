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

package com.lambdasoup.quickfit;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessActivities;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Device;
import com.google.android.gms.fitness.data.Session;
import com.google.android.gms.fitness.request.SessionInsertRequest;

import java.util.concurrent.TimeUnit;

/**
 * A placeholder fragment containing a simple view.
 */
public class OldWorkoutListActivityFragment extends Fragment {

    private static final String TAG = OldWorkoutListActivityFragment.class.getSimpleName();
    private GoogleApiClient mClient;
    private static final int REQUEST_OAUTH = 1;

    /**
     *  Track whether an authorization activity is stacking over the current activity, i.e. when
     *  a known auth error is being resolved, such as showing the account chooser or presenting a
     *  consent dialog. This avoids common duplications as might happen on screen rotations, etc.
     */
    private static final String AUTH_PENDING = "auth_state_pending";
    private boolean authInProgress = false;


    public OldWorkoutListActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.old_fragment_workout_list, container, false);
        view.findViewById(R.id.button_add_workout).setOnClickListener(v -> onAddWorkout());
        return view;
    }

    private void onAddWorkout() {
        buildFitnessClient();
        mClient.connect();
    }

    private void buildFitnessClient() {
        // Create the Google API Client
        mClient = new GoogleApiClient.Builder(getActivity())
                .addApi(Fitness.SESSIONS_API)
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(
                        new GoogleApiClient.ConnectionCallbacks() {
                            @Override
                            public void onConnected(Bundle bundle) {
                                Log.i(TAG, "Connected!!!");


                                long now = System.currentTimeMillis();
                                long startTime = now - 8 * 3_600_000;
                                long endTime = now - 7 * 3_600_000;
                                Session session = new Session.Builder()
                                        .setActivity(FitnessActivities.AEROBICS)
                                        .setName("Test old aerobics with disco")
                                        .setStartTime(startTime, TimeUnit.MILLISECONDS)
                                        .setEndTime(endTime, TimeUnit.MILLISECONDS)
                                        .build();

                                DataSource datasource = new DataSource.Builder()
                                        .setAppPackageName(getActivity())
                                        .setDataType(DataType.AGGREGATE_CALORIES_EXPENDED)
                                        .setType(DataSource.TYPE_RAW)
                                        .setDevice(Device.getLocalDevice(getActivity()))
                                        .build();

                                SessionInsertRequest insertRequest = new SessionInsertRequest.Builder()
                                        .setSession(session)
                                        .addAggregateDataPoint(
                                                DataPoint.create(datasource)
                                                        .setTimestamp(endTime, TimeUnit.MILLISECONDS)
                                                        .setFloatValues(333.3f)
                                                        .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                                        )
                                        .build();

                                PendingResult<Status> res = Fitness.SessionsApi.insertSession(mClient, insertRequest);
                                res.setResultCallback(status -> {
                                    Log.i(TAG, "insertion result: " + status.toString());
                                    mClient.disconnect();
                                });


                            }

                            @Override
                            public void onConnectionSuspended(int i) {
                                // If your connection to the sensor gets lost at some point,
                                // you'll be able to determine the reason and react to it here.
                                if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                    Log.i(TAG, "Connection lost.  Cause: Network Lost.");
                                } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                                    Log.i(TAG, "Connection lost.  Reason: Service Disconnected");
                                }
                            }
                        }
                )
                .addOnConnectionFailedListener(
                        result -> {
                            Log.i(TAG, "Connection failed. Cause: " + result.toString());
                            if (!result.hasResolution()) {
                                // Show the localized error notification
                                GoogleApiAvailability.getInstance().showErrorNotification(getContext(), result.getErrorCode());
                                return;
                            }
                            // The failure has a resolution. Resolve it.
                            // Called typically when the app is not yet authorized, and an
                            // authorization dialog is displayed to the user.
                            if (!authInProgress) {
                                try {
                                    Log.i(TAG, "Attempting to resolve failed connection");
                                    authInProgress = true;

                                    result.startResolutionForResult(getActivity(),
                                            REQUEST_OAUTH);
                                } catch (IntentSender.SendIntentException e) {
                                    Log.e(TAG,
                                            "Exception while starting resolution activity", e);
                                }
                            }
                        }
                )
                .build();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_OAUTH) {
            authInProgress = false;
            if (resultCode == Activity.RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!mClient.isConnecting() && !mClient.isConnected()) {
                    mClient.connect();
                }
            }
        }
    }

}
