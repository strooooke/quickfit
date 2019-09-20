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

package com.lambdasoup.quickfit.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import android.view.View
import android.view.WindowInsets
import android.webkit.WebView

import com.lambdasoup.quickfit.util.ui.*

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val webView = WebView(this)
        setContentView(webView)

        window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                // Tells the system that the window wishes the content to
                // be laid out as if the navigation bar was hidden
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        webView.loadUrl("file:///android_asset/html/about.html")
        webView.setOnApplyWindowInsetsListener { v, insets ->
            v.setOnApplyWindowInsetsListener(null)
            v.updateMargins { oldMargins ->
                oldMargins + insets.systemWindowInsetsRelative(v)
            }
            insets
        }
    }
}
