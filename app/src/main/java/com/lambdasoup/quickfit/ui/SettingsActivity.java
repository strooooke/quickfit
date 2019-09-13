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
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.lambdasoup.quickfit.R;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import timber.log.Timber;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Display the fragment as the main content.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private static final int REQUEST_CODE_ALERT_RINGTONE = 1;

        private Preference disconnectGoogleFitPref;
        private Preference notificationRingtonePref;
        private GoogleApiClient googleApiClient;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.preferences, rootKey);
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            disconnectGoogleFitPref =  findPreference(getString(R.string.pref_key_disconnect_g_fit));

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                findPreference(getString(R.string.pref_key_notifications)).setVisible(false);
            } else {
                String keyNotificationRingtone = getString(R.string.pref_key_notification_ringtone);
                notificationRingtonePref = findPreference(keyNotificationRingtone);
                updateRingtoneSummary(notificationRingtonePref, getRingtonePreferenceValue(Settings.System.NOTIFICATION_SOUND));
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            if (notificationRingtonePref != null) {
                notificationRingtonePref.setOnPreferenceClickListener(preference -> {
                    Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
                    intent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI);

                    String existingValue = getRingtonePreferenceValue(null);
                    if (existingValue != null) {
                        if (existingValue.length() == 0) {
                            // Select "Silent"
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, (Uri) null);
                        } else {
                            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingValue));
                        }
                    } else {
                        // No ringtone has been selected, set to the default
                        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Settings.System.DEFAULT_NOTIFICATION_URI);
                    }

                    startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE);
                    return true;
                });

                notificationRingtonePref.setOnPreferenceChangeListener((preference, newValue) -> {
                    updateRingtoneSummary(preference, (String) newValue);
                    return true;
                });
            }

            disconnectGoogleFitPref.setOnPreferenceClickListener(preference -> {
                disconnectGoogleFit();
                return true;
            });
        }

        @Override
        public void onStop() {
            super.onStop();
            if (notificationRingtonePref != null) {
                notificationRingtonePref.setOnPreferenceChangeListener(null);
                notificationRingtonePref.setOnPreferenceClickListener(null);
            }
            disconnectGoogleFitPref.setOnPreferenceClickListener(null);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            switch (requestCode) {
                case REQUEST_CODE_ALERT_RINGTONE: {
                    if (data != null) {
                        Uri ringtone = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                        String ringtonePrefValue;
                        if (ringtone != null) {
                            ringtonePrefValue = ringtone.toString();
                        } else {
                            // "Silent" was selected
                            ringtonePrefValue = "";
                        }
                        setRingtonPreferenceValue(ringtonePrefValue);
                        updateRingtoneSummary(notificationRingtonePref, ringtonePrefValue);
                    }
                    break;
                }
                default:
                    super.onActivityResult(requestCode, resultCode, data);
            }
        }

        private String getRingtonePreferenceValue(@Nullable String defaultValue) {
            String keyNotificationRingtone = getString(R.string.pref_key_notification_ringtone);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().getApplicationContext());
            return prefs.getString(keyNotificationRingtone, defaultValue);
        }

        private void setRingtonPreferenceValue(String newValue) {
            String keyNotificationRingtone = getString(R.string.pref_key_notification_ringtone);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireActivity().getApplicationContext());
            prefs.edit().putString(keyNotificationRingtone, newValue).apply();
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
            Context context = requireActivity().getApplicationContext();

            //noinspection CodeBlock2Expr,CodeBlock2Expr
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
                                    Timber.d("connection suspended");
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
