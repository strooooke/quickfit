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

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.lambdasoup.quickfit.Constants;
import com.lambdasoup.quickfit.R;

public class SettingsActivity extends AppCompatActivity {

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
        private Preference disconnectGoogleFitPref;
        private RingtonePreference notificationRingtonePref;
        private GoogleApiClient googleApiClient;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);

            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());

            String keyNotificationRingtone = getString(R.string.pref_key_notification_ringtone);
            notificationRingtonePref = (RingtonePreference) findPreference(keyNotificationRingtone);
            disconnectGoogleFitPref = findPreference(getString(R.string.pref_key_disconnect_g_fit));


            updateRingtoneSummary(notificationRingtonePref, prefs.getString(keyNotificationRingtone, Settings.System.NOTIFICATION_SOUND));
        }

        @Override
        public void onStart() {
            super.onStart();
            notificationRingtonePref.setOnPreferenceChangeListener((preference, newValue) -> {
                updateRingtoneSummary(preference, (String) newValue);
                return true;
            });

            disconnectGoogleFitPref.setOnPreferenceClickListener(preference -> {
                disconnectGoogleFit();
                return true;
            });

        }

        @Override
        public void onStop() {
            super.onStop();
            notificationRingtonePref.setOnPreferenceChangeListener(null);
            disconnectGoogleFitPref.setOnPreferenceClickListener(null);
        }


        private void updateRingtoneSummary(Preference preference, String strUri) {
            String name;
            if (strUri.isEmpty()) {
                name = getString(R.string.pref_notification_ringtone_silent);
            } else {
                Uri ringtoneUri = Uri.parse(strUri);
                Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), ringtoneUri);
                name = ringtone.getTitle(getActivity());
            }
            preference.setSummary(name);
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
