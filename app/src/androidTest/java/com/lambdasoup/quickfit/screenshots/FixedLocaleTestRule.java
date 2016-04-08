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

package com.lambdasoup.quickfit.screenshots;

import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.util.Log;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Locale;

/**
 * TestRule that allows easy execution of locale-dependent tests. The screengrab environment
 * runs tests with instrumentation arguments testLocale and endingLocale; this rule checks whether
 * those are present and if not, injects them with the set locale as value.
 */
public class FixedLocaleTestRule implements TestRule {
    private static final String TAG = FixedLocaleTestRule.class.getSimpleName();
    private final Locale fixedLocale;

    public FixedLocaleTestRule(Locale fixedLocale) {
        this.fixedLocale = fixedLocale;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Bundle args = InstrumentationRegistry.getArguments();
                if (!args.containsKey("testLocale")) {
                    args.putString("testLocale", fixedLocale.toString());
                    args.putString("endingLocale", fixedLocale.toString());
                    InstrumentationRegistry.registerInstance(InstrumentationRegistry.getInstrumentation(), args);
                    Log.i(TAG, "No testLocale in instrumentation args found. Re-registering with testLocale and endingLocale " + fixedLocale);
                }

                base.evaluate();
            }
        };
    }
}
