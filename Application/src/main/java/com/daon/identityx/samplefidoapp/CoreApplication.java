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

import com.daon.identityx.uaf.IUafClientUtils;
import com.daon.identityx.uaf.UafClientUtils;

/**
 * This contains a number of statics used by multiple activities within the application.
 */
public class CoreApplication {

    public static String USER_IDENTIFIER = "IdentityX_UserId";

    private static String sessionId;
    private static String email;
    private static boolean fidoSupported;

    public static String getSessionId() {
        return sessionId;
    }

    public static void setSessionId(String theSessionId) {
        sessionId = theSessionId;
    }

    public static String getEmail() {
        return email;
    }

    public static void setEmail(String theEmail) {
        email = theEmail;
    }

    public static boolean isFIDOSupported() {
        return fidoSupported;
    }

    public static void setFIDOSupported(boolean isFIDOSupported) {
        fidoSupported = isFIDOSupported;
    }
}
