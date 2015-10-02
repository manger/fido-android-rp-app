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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.daon.identityx.controller.model.CreateAuthRequestResponse;
import com.daon.identityx.controller.model.CreateSession;
import com.daon.identityx.controller.model.CreateSessionResponse;
import com.daon.identityx.controller.model.DeleteAccountResponse;
import com.daon.identityx.exception.CommunicationsException;
import com.daon.identityx.exception.ServerError;
import com.daon.identityx.uaf.AndroidClientIntentParameters;
import com.daon.identityx.uaf.FidoOperation;
import com.daon.identityx.uaf.UafClientLogUtils;
import com.daon.identityx.uaf.UafServerResponseCodes;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * The first screen displayed which determines if there are FIDO clients on the
 * device and if there are clients, what authenticators are available.
 *
 * This process is a little difficult as it requires that we get a list of all the
 * FIDO Clients and then call each client asking for its authenticators.
 *
 * While it is not specified in the FIDO Specifications there will be issues if the
 * FIDO clients do not all return the same authenticators.  If a FIDO Client only
 * works with an subset of the authenticator on the device and another FIDO Client
 * works with all the authenticators then issues will arise if the wrong client is
 * called.
 *
 */
public class SplashActivity extends BaseActivity  {

    private static final int SPLASH_DISPLAY = 1500;

    // Used during the process of iterating over all the FIDO clients on the device
    private int uafClientIdx = 0;
    private Boolean aaidRetrievalAttempted = false;
    private long start;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        this.start = System.currentTimeMillis();

        FindClientsAndAuthenticators findOp = new FindClientsAndAuthenticators();
        findOp.execute();
    }


    /***
     * Attempt to get the list of UAF Clients on the device and
     * add these to the static list.
     *
     */
    protected void loadUafClientList() {

        List<ResolveInfo> clientList;
        final Intent intent = new Intent();
        intent.setAction(AndroidClientIntentParameters.intentAction);
        intent.setType(AndroidClientIntentParameters.intentType);

        PackageManager manager = this.getPackageManager();
        clientList = manager.queryIntentActivities(intent, 0);
        UafClientLogUtils.logUafClientActivities(clientList);
        getUafClientList().addAll(clientList);
    }



    /***
     * Retrieve the set of authenticators from the FIDO clients by using the FIDO discovery
     *
     * The list of authenticators is cached within this class so if authenticators can be
     * dynamically added or removed from the device, they will not be picked up after the
     * app is initialized.
     *
     */
    protected void retrieveAvailableAuthenticatorAaids() {

        if(!aaidRetrievalAttempted) {
            LogUtils.logAaidRetrievalStart();
            List<ResolveInfo> clientList = getUafClientList();
            this.setCurrentFidoOperation(FidoOperation.Discover);
            Intent intent = getUafClientUtils().getDiscoverIntent();
            if (clientList != null && clientList.size() > 0) {
                intent.setComponent(new ComponentName(clientList.get(uafClientIdx).activityInfo.packageName,
                        clientList.get(uafClientIdx).activityInfo.name));
                UafClientLogUtils.logUafDiscoverRequest(intent);
                UafClientLogUtils.logUafClientDetails(clientList.get(uafClientIdx));
                startActivityForResult(intent, AndroidClientIntentParameters.requestCode);
                return;
            } else {
                // End now if there are no clients
                LogUtils.logDebug(LogUtils.TAG, (String) getText(R.string.no_fido_client_found));
                LogUtils.logAaidRetrievalEnd();
                aaidRetrievalAttempted = true;
            }
        }
        if (System.currentTimeMillis() < (start+SPLASH_DISPLAY)) {
            try {
                Thread.sleep(start + SPLASH_DISPLAY - System.currentTimeMillis());
            } catch (InterruptedException ex) {
                // ignore and carry on
            }
        }
        finish();
        try {
            Intent newIntent = new Intent( this, IntroActivity.class);
            startActivity(newIntent);
        } catch (Throwable ex) {
            displayError(ex.getMessage());
        }


    }

    /***
     * Add the discovered authenticators to the set of authenticators.
     * @param retrievedAaids the list of AAIDs
     */
    protected void updateAvailableAuthenticatorAaidList(List<String> retrievedAaids) {

        this.getAvailableAuthenticatorAaidsAsSet().addAll(retrievedAaids);
        LogUtils.logAaidRetrievalUpdate(uafClientIdx, retrievedAaids);
        uafClientIdx++;
        if(uafClientIdx < getUafClientList().size()) {
            LogUtils.logAaidRetrievalContinue(uafClientIdx);
        } else {
            LogUtils.logAaidRetrievalEnd();
            aaidRetrievalAttempted = true;
        }
    }

    /***
     * Callback from the FIDO Client with the response from the discovery request
     *
     * @param uafResponseJson the response
     */
    @Override
    protected void processUafClientResponse(String uafResponseJson) {

        updateAvailableAuthenticatorAaidList(getAaidsFromDiscoveryData(uafResponseJson));
        retrieveAvailableAuthenticatorAaids();
    }

    @Override
    protected void onActivityResultFailure(String errorMsg) {

        updateAvailableAuthenticatorAaidList(new ArrayList<String>());
        retrieveAvailableAuthenticatorAaids();
    }

    /***
     * From the discovery data, create the list of AAIDs
     *
     * @param discoveryData the discovery data
     * @return a list of AAIDs
     */
    protected List<String> getAaidsFromDiscoveryData(String discoveryData) {

        List<String> aaidList = new ArrayList<>();
        if(discoveryData != null) {
            try {
                JSONObject discoveryDataJsonObj = new JSONObject(new JSONTokener(discoveryData));

                JSONArray availableAuthenticators = discoveryDataJsonObj.getJSONArray("availableAuthenticators");
                for(int i=0; i<availableAuthenticators.length(); i++) {
                    JSONObject authenticator = availableAuthenticators.getJSONObject(i);
                    aaidList.add(authenticator.getString("aaid"));
                }
                return aaidList;
            } catch (Exception e) {
                Log.e(LogUtils.TAG, "Invalid discovery data format returned by the client.");
            }

        }
        return aaidList;

    }

    /**
     * Represents an asynchronous task used to delete a user account
     */
    public class FindClientsAndAuthenticators extends AsyncTask<Void, Void, Void> {

        FindClientsAndAuthenticators() {
        }

        @Override
        protected Void doInBackground(Void... params) {

            loadUafClientList();
            retrieveAvailableAuthenticatorAaids();
            return null;
        }

    }


}

