/*
* Copyright Daon.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.daon.identityx.samplefidoapp;

import android.app.Activity;
import android.os.Bundle;

/**
 * Activity which displays application settings
 *
 */
public class SettingsActivity extends Activity {
    public static final String PREF_SERVER_URL = "pref_server_url";
    public static final String PREF_SERVER_PORT = "pref_server_port";
    public static final String PREF_SERVER_SECURE = "pref_server_secure";

    /**
     * Display the {@link SettingsFragment}
     * @param savedInstanceState saved instance state
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment()).commit();
    }
}
