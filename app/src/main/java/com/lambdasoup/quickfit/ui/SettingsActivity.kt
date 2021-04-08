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

import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.fitness.Fitness
import com.lambdasoup.quickfit.Constants.FITNESS_API_OPTIONS
import com.lambdasoup.quickfit.R
import com.lambdasoup.quickfit.util.ui.systemWindowInsetsRelative
import com.lambdasoup.quickfit.util.ui.updateMargins
import com.lambdasoup.quickfit.util.ui.updatePadding
import timber.log.Timber

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        // Tells the system that the window wishes the content to
                        // be laid out as if the navigation bar was hidden
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        // Display the fragment as the main content.
        supportFragmentManager
                .beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()

        findViewById<View>(android.R.id.content).setOnApplyWindowInsetsListener { v, insets ->
            //v.setOnApplyWindowInsetsListener(null)
            Timber.d("got insets: ${insets.systemWindowInsetsRelative(v)} raw Right=${insets.systemWindowInsetRight}")
            v.updatePadding { oldPadding -> oldPadding + insets.systemWindowInsetsRelative(v) }

            insets
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private val disconnectGoogleFitPref by lazy { findPreference(getString(R.string.pref_key_disconnect_g_fit)) }
        private var notificationRingtonePref: Preference? = null
        private var googleApiClient: GoogleApiClient? = null

        private val prefs: SharedPreferences by lazy {
            PreferenceManager.getDefaultSharedPreferences(requireActivity().applicationContext)
        }
        private val keyNotificationRingtone by lazy { getString(R.string.pref_key_notification_ringtone) }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preferences, rootKey)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                findPreference(getString(R.string.pref_key_notifications)).isVisible = false
            } else {
                notificationRingtonePref = findPreference(getString(R.string.pref_key_notification_ringtone))
                updateRingtoneSummary(getRingtonePreferenceValue())
            }
        }

        override fun onStart() {
            super.onStart()
            notificationRingtonePref?.setOnPreferenceClickListener { _ ->
                val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, Settings.System.DEFAULT_NOTIFICATION_URI)
                        .putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, getRingtonePreferenceValue())

                startActivityForResult(intent, REQUEST_CODE_ALERT_RINGTONE)
                true
            }

            disconnectGoogleFitPref.setOnPreferenceClickListener {
                disconnectGoogleFit()
                true
            }
        }

        override fun onStop() {
            super.onStop()
            notificationRingtonePref?.onPreferenceChangeListener = null
            notificationRingtonePref?.onPreferenceClickListener = null

            disconnectGoogleFitPref.onPreferenceClickListener = null
        }

        override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            when (requestCode) {
                REQUEST_CODE_ALERT_RINGTONE -> {
                    if (data != null) {
                        val ringtone = data.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                        val ringtonePrefValue = ringtone?.toString() ?: ""
                        Timber.d("got uri: $ringtone as prefValue: $ringtonePrefValue")

                        setRingtonePreferenceValue(ringtonePrefValue)
                        updateRingtoneSummary(ringtone)
                    }
                }
                else -> super.onActivityResult(requestCode, resultCode, data)
            }
        }

        private fun getRingtonePreferenceValue(): Uri? {
            return when (val strValue = prefs.getString(keyNotificationRingtone, null)) {
                    null -> Settings.System.DEFAULT_NOTIFICATION_URI
                    "" -> null
                    else -> Uri.parse(strValue)
            }
        }

        private fun setRingtonePreferenceValue(newValue: String) {
            prefs.edit().putString(keyNotificationRingtone, newValue).apply()
        }

        private fun updateRingtoneSummary(ringtoneUri: Uri?) {
            notificationRingtonePref!!.summary =
                    if (ringtoneUri == null) {
                        getString(R.string.pref_notification_ringtone_silent)
                    } else {
                        RingtoneManager.getRingtone(activity, ringtoneUri)
                                .getTitle(activity)
                    }
        }

        private fun disconnectGoogleFit() {
            fun showDisconnectSuccess() = Toast.makeText(context, R.string.msg_fit_disconnect_success, Toast.LENGTH_SHORT).show()

            fun showDisconnectFailure() = Toast.makeText(context, R.string.msg_fit_disconnect_failure, Toast.LENGTH_SHORT).show()

            Fitness.getConfigClient(requireContext(), GoogleSignIn.getAccountForExtension(requireContext(), FITNESS_API_OPTIONS))
                    .disableFit()
                    // See https://github.com/android/fit-samples/issues/28 - all this seems necessary to actually properly disconnect
                    // this app from Google Fit
                    .continueWithTask {
                        val signInOptions = GoogleSignInOptions.Builder()
                                .addExtension(FITNESS_API_OPTIONS)
                                .build()
                        GoogleSignIn.getClient(requireContext(), signInOptions)
                                .revokeAccess()
                    }
                    .addOnFailureListener {
                        if (it is ApiException && it.statusCode == 4) {
                            // for unclear reasons (see https://github.com/android/fit-samples/issues/28), this is the expected
                            // result of successfully revoking access.
                            showDisconnectSuccess()
                        } else {
                            Timber.e(it, "Failure disconnecting from Google Fit.")
                            showDisconnectFailure()
                        }
                    }
                    .addOnSuccessListener {
                        showDisconnectSuccess()
                    }
        }

        companion object {
            private const val REQUEST_CODE_ALERT_RINGTONE = 1
        }
    }
}
