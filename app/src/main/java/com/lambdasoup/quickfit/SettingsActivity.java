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

import android.content.Context;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREF_KEY_DISCONNECT_G_FIT = "pref_key_disconnect_g_fit";
    private static final String TAG = SettingsActivity.class.getSimpleName();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

    }

    public static class SettingsFragment extends PreferenceFragment {

        private static final String TAG = SettingsFragment.class.getSimpleName();
        private GoogleApiClient googleApiClient;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            findPreference(PREF_KEY_DISCONNECT_G_FIT).setOnPreferenceClickListener(preference -> {
                        disconnectGoogleFit();
                        return true;
                    }
            );
        }

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            addPreferencesFromResource(R.xml.preferences_google_account);
        }


        private void disconnectGoogleFit() {
            if (googleApiClient != null && (googleApiClient.isConnecting() || googleApiClient.isConnected())) {
                // disconnect already in progress
                return;
            }
            Context context = getActivity().getApplicationContext();
            googleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Fitness.CONFIG_API)
                    .addConnectionCallbacks(
                            new GoogleApiClient.ConnectionCallbacks() {
                                @Override
                                public void onConnected(Bundle bundle) {
                                    PendingResult<Status> result = Fitness.ConfigApi.disableFit(googleApiClient);
                                    result.setResultCallback(status -> {
                                        if (status.isSuccess()) {
                                            Toast.makeText(context, R.string.msg_fit_disconnect_success, Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(context, R.string.msg_fit_disconnect_failure, Toast.LENGTH_SHORT).show();
                                        }
                                        googleApiClient.disconnect();
                                    });

                                }

                                @Override
                                public void onConnectionSuspended(int i) {
                                    Log.d(TAG, "connection suspended");
                                }
                            }
                    )
                    .addOnConnectionFailedListener(
                            result -> {
                                Toast.makeText(context, R.string.msg_fit_disconnect_no_connection, Toast.LENGTH_SHORT).show();
                            }
                    )
                    .build();
            googleApiClient.connect();
        }
    }
}
