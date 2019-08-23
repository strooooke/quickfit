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

package com.lambdasoup.quickfit.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Environment;
import androidx.test.InstrumentationRegistry;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;

import timber.log.Timber;
import tools.fastlane.screengrab.file.Chmod;

/**
 * Replacement for tools.fastlane.screengrab.Screengrab that takes
 * screenshots using UiDevice (so full system screenshots, including status bar, notifications,
 * dialogs etc.) See https://github.com/fastlane/fastlane/issues/2080
 */
public class SystemScreengrab {
    static final String NAME_SEPARATOR = "_";
    private static final String EXTENSION = ".png";
    private static final int FULL_QUALITY = 100;

    private SystemScreengrab() {
        // do not instantiate
    }

    public static void takeScreenshot(String filename) throws IOException {
        Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();

        File file = screenshotFile(filename);
        OutputStream fos = null;
        try {
            fos = new BufferedOutputStream(new FileOutputStream(file));
            bitmap.compress(Bitmap.CompressFormat.PNG, FULL_QUALITY, fos);
            Chmod.chmodPlusR(file);
        } finally {
            bitmap.recycle();
            if (fos != null) {
                fos.close();
            }
        }

    }

    private static File screenshotFile(String screenshotName) throws IOException {
        File screenshotDirectory = getFilesDirectory(InstrumentationRegistry.getTargetContext(), Locale.getDefault());
        String screenshotFileName = System.currentTimeMillis() + NAME_SEPARATOR + screenshotName + EXTENSION;
        return new File(screenshotDirectory, screenshotFileName);
    }

    private static File getFilesDirectory(Context context, Locale locale) throws IOException {
        File directory = null;
        File internalDir;
        if (Build.VERSION.SDK_INT >= 21) {
            internalDir = new File(Environment.getExternalStorageDirectory(), getDirectoryName(context, locale));
            directory = initializeDirectory(internalDir);
        }

        if (directory == null) {
            internalDir = new File(context.getDir("screengrab", 1), localeToDirName(locale));
            directory = initializeDirectory(internalDir);
        }

        if (directory == null) {
            throw new IOException("Unable to get a screenshot storage directory");
        } else {
            Timber.d("Using screenshot storage directory: %s", directory.getAbsolutePath());
            return directory;
        }
    }

    private static File initializeDirectory(File dir) {
        try {
            createPathTo(dir);
            if (dir.isDirectory() && dir.canWrite()) {
                return dir;
            }
        } catch (IOException e) {
            // masks missing write_external_storage permission when running this as part
            // of a regular morphing_add_expand_less, and not the screengrab flavor with added permissions.
        }

        return null;
    }

    private static void createPathTo(File dir) throws IOException {
        File parent = dir.getParentFile();
        if (!parent.exists()) {
            createPathTo(parent);
        }

        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Unable to create output dir: " + dir.getAbsolutePath());
        } else {
            Chmod.chmodPlusRWX(dir);
        }
    }

    private static String getDirectoryName(Context context, Locale locale) {
        return context.getPackageName() + "/" + "screengrab" + "/" + localeToDirName(locale);
    }

    private static String localeToDirName(Locale locale) {
        return locale.getLanguage() + "-" + locale.getCountry() + "/images/screenshots";
    }

}
