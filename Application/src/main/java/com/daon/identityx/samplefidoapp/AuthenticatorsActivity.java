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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.daon.identityx.controller.model.AuthenticatorInfo;
import com.daon.identityx.controller.model.CreateAuthenticator;
import com.daon.identityx.controller.model.CreateAuthenticatorResponse;
import com.daon.identityx.controller.model.CreateRegRequestResponse;
import com.daon.identityx.controller.model.GetAuthenticatorResponse;
import com.daon.identityx.controller.model.ListAuthenticatorsResponse;
import com.daon.identityx.exception.CommunicationsException;
import com.daon.identityx.exception.ServerError;
import com.daon.identityx.uaf.FidoOperation;
import com.daon.identityx.uaf.UafServerResponseCodes;

/**
 * A logged in screen
 */
public class AuthenticatorsActivity extends BaseActivity {

    private static final String ARCHIVED_STATUS = "ARCHIVED";
    private enum Action {NONE, REGISTER, DEREGISTER}

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private Action currentAction = Action.NONE;
    private ListAuthenticatorsTask mListAuthenticatorsTask = null;
    private GetAuthenticatorTask mGetAuthenticatorTask = null;
    private DeregisterAuthenticatorTask mDeregisterAuthenticatorTask = null;
    private CreateAuthenticatorTask mCreateAuthenticatorTask = null;
    private CreateRegRequestTask mCreateRegRequestTask = null;
    private CreateRegRequestResponse mCreateRegRequestResponse;
    private AuthenticatorInfo selectedAuthenticationInfo;

    // UI references.
    private View mProgressView;
    private View mAuthenticatorsFormView;
    private Button mDeregisterButton;

    private AuthenticatorInfo mSelectedAuthenticator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authenticators);

        mDeregisterButton = (Button) findViewById(R.id.deregister_button);
        mDeregisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptDeregister();
            }
        });
        mDeregisterButton.setEnabled(false);

        Button mRegisterButton = (Button) findViewById(R.id.register_new_authenticator_button);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegistration();
            }
        });

        ListView listView = (ListView) findViewById(R.id.list_view_authenticators);
        listView.setSelector(R.drawable.listitem_background);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                selectedAuthenticationInfo = (AuthenticatorInfo) view.getTag();
                if (!hasAuthenticator(selectedAuthenticationInfo.getAaid()) && selectedAuthenticationInfo.getStatus().equals(ARCHIVED_STATUS)) {
                    mDeregisterButton.setEnabled(false);
                } else {
                    mDeregisterButton.setEnabled(true);
                }


            }
        });

        mAuthenticatorsFormView = findViewById(R.id.fido_authenticators_form);
        mProgressView = findViewById(R.id.authenticators_progress);
        this.refreshAuthenticators();

    }

    protected void requestDeregOfInactiveAuth() {

        String message = getString(R.string.confirm_dereg_inactive_auth_present);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(message);

        alertDialogBuilder.setPositiveButton(R.string.dialog_confirm_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                deregInactiveAuthPresent();
            }
        });

        alertDialogBuilder.setNegativeButton(R.string.dialog_confirm_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    protected void requestDeregOfActiveAuthPresent() {

        String message = getString(R.string.confirm_dereg_active_auth_present);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(message);

        alertDialogBuilder.setPositiveButton(R.string.dialog_confirm_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                deregActiveAuthPresent();
            }
        });

        alertDialogBuilder.setNegativeButton(R.string.dialog_confirm_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    protected void requestDeregOfActiveAuthNotPresent() {

        String message = getString(R.string.confirm_dereg_active_auth_not_present);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setMessage(message);

        alertDialogBuilder.setPositiveButton(R.string.dialog_confirm_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                deregActiveAuthNotPresent();
            }
        });

        alertDialogBuilder.setNegativeButton(R.string.dialog_confirm_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    protected void attemptDeregister() {

        if (this.selectedAuthenticationInfo == null) {
            mDeregisterButton.setEnabled(false);
            return;
        }

        if (this.selectedAuthenticationInfo.getStatus().equals(ARCHIVED_STATUS)) {
            requestDeregOfInactiveAuth();
        } else {
            if (hasAuthenticator(this.selectedAuthenticationInfo.getAaid())) {
                requestDeregOfActiveAuthPresent();
            } else {
                requestDeregOfActiveAuthNotPresent();
            }
        }
    }

    /***
     * Attempt to deregister the inactive authenticator
     */
    protected void deregInactiveAuthPresent() {

        showProgress(true);
        this.currentAction = Action.DEREGISTER;
        mGetAuthenticatorTask = new GetAuthenticatorTask();
        mGetAuthenticatorTask.execute((Void) null);
    }

    protected void deregActiveAuthPresent() {

        showProgress(true);
        this.currentAction = Action.DEREGISTER;
        mDeregisterAuthenticatorTask = new DeregisterAuthenticatorTask();
        mDeregisterAuthenticatorTask.execute((Void) null);
    }

    protected void deregActiveAuthNotPresent() {

        showProgress(true);
        this.currentAction = Action.DEREGISTER;
        mDeregisterAuthenticatorTask = new DeregisterAuthenticatorTask();
        mDeregisterAuthenticatorTask.execute((Void) null);

    }


    protected void refreshAuthenticators() {

        showProgress(true);
        mDeregisterButton.setEnabled(false);
        mSelectedAuthenticator = null;
        mListAuthenticatorsTask = new ListAuthenticatorsTask();
        mListAuthenticatorsTask.execute((Void) null);
    }

    protected void attemptRegistration() {

        if (this.currentAction != Action.NONE) {
            return;
        }
        this.currentAction = Action.REGISTER;
        showProgress(true);
        mCreateRegRequestTask = new CreateRegRequestTask();
        mCreateRegRequestTask.execute((Void) null);
    }

    protected void returnToHome() {

        finish();
    }

    protected void showAuthSelection(AuthenticatorInfo[] authenticatorInfoList) {
        try {
            LayoutInflater inflater = getLayoutInflater();
            final ListView lv = (ListView) findViewById(R.id.list_view_authenticators);

            AuthenticatorInfosAdapter adapter = new AuthenticatorInfosAdapter(this, authenticatorInfoList);
            lv.setAdapter(adapter);

            showProgress(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void notifyDeactivationComplete() {

        Toast.makeText(this, R.string.deregistration_complete, Toast.LENGTH_LONG).show();
        this.currentAction = Action.NONE;
        refreshAuthenticators();
    }

    protected void processUafClientResponse(String uafResponseJson) {

        if (this.currentAction == Action.REGISTER) {
            // Continue FIDO registration (re-register user)
            mCreateAuthenticatorTask = new CreateAuthenticatorTask(CoreApplication.getSessionId(),
                    mCreateRegRequestResponse.getRegistrationRequestId(), uafResponseJson);
            mCreateAuthenticatorTask.execute((Void) null);
        }
        if (this.currentAction == Action.DEREGISTER) {
            Toast.makeText(this, R.string.deregistration_complete, Toast.LENGTH_LONG).show();
            this.currentAction = Action.NONE;
            this.refreshAuthenticators();
        }
    }

    protected void onActivityResultFailure(String errorMsg) {

        if (this.currentAction == Action.REGISTER) {
            Toast.makeText(this, getString(R.string.error_registering_authenticator) + errorMsg, Toast.LENGTH_LONG).show();
        }
        if (this.currentAction == Action.DEREGISTER) {
            Toast.makeText(this, R.string.error_deregistering_authenticator, Toast.LENGTH_LONG).show();
        }
        this.currentAction = Action.NONE;
        this.refreshAuthenticators();
    }


    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mAuthenticatorsFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mAuthenticatorsFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mAuthenticatorsFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mAuthenticatorsFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    protected void endProgressWithError(String errorMsg) {
        showProgress(false);
        displayError(errorMsg);
        this.currentAction = Action.NONE;
    }

    protected void showSuccessfulRegistration() {
        Toast.makeText(this, R.string.registration_complete, Toast.LENGTH_LONG).show();
        this.currentAction = Action.NONE;
        refreshAuthenticators();
    }

    /**
     * Represents an asynchronous task used to get a list of authenticators to display
     */
    public class ListAuthenticatorsTask extends AsyncTask<Void, Void, ServerOperationResult<ListAuthenticatorsResponse>> {

        ListAuthenticatorsTask() {
        }

        @Override
        protected ServerOperationResult<ListAuthenticatorsResponse> doInBackground(Void... params) {
            ServerOperationResult<ListAuthenticatorsResponse> result;
            try {
                ListAuthenticatorsResponse response = getRelyingPartyComms().listAuthenticators();
                result = new ServerOperationResult<>(response);
            } catch (ServerError e) {
                result = new ServerOperationResult<>(e.getError());
            } catch (CommunicationsException e) {
                result = new ServerOperationResult<>(e.getError());
            }

            return result;
        }

        @Override
        protected void onPostExecute(final ServerOperationResult<ListAuthenticatorsResponse> result) {
            mListAuthenticatorsTask = null;

            if (result.isSuccessful()) {
                AuthenticatorInfo[] authenticatorInfoList = result.getResponse().getAuthenticatorInfoList();
                showAuthSelection(authenticatorInfoList);
            } else {
                showProgress(false);
                displayError(result.getError().getMessage());
            }
        }

        @Override
        protected void onCancelled() {
            mListAuthenticatorsTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an asynchronous task used to get an authenticator to deregister
     */
    public class GetAuthenticatorTask extends AsyncTask<Void, Void, ServerOperationResult<GetAuthenticatorResponse>> {

        GetAuthenticatorTask() {
        }

        @Override
        protected ServerOperationResult<GetAuthenticatorResponse> doInBackground(Void... params) {
            ServerOperationResult<GetAuthenticatorResponse> result;
            try {
                GetAuthenticatorResponse response = getRelyingPartyComms().getAuthenticator(selectedAuthenticationInfo.getId());
                result = new ServerOperationResult<>(response);
            } catch (ServerError e) {
                result = new ServerOperationResult<>(e.getError());
            } catch (CommunicationsException e) {
                result = new ServerOperationResult<>(e.getError());
            }

            return result;
        }

        @Override
        protected void onPostExecute(final ServerOperationResult<GetAuthenticatorResponse> result) {
            mGetAuthenticatorTask = null;

            if (result.isSuccessful()) {
                AuthenticatorInfo authenticatorInfo = result.getResponse().getAuthenticatorInfo();
                setCurrentFidoOperation(FidoOperation.Deregistration);
                Intent intent = getUafClientUtils().getUafOperationIntent(FidoOperation.Deregistration, authenticatorInfo.getFidoDeregistrationRequest());
                sendUafClientIntent(intent, FidoOpCommsType.Return);
            } else {
                showProgress(false);
                displayError(result.getError().getMessage());
            }
        }

        @Override
        protected void onCancelled() {
            mGetAuthenticatorTask = null;
            showProgress(false);
        }
    }

    /**
     * Represents an asynchronous task used to deregsiter an authenticator
     */
    public class DeregisterAuthenticatorTask extends AsyncTask<Void, Void, ServerOperationResult<String>> {


        DeregisterAuthenticatorTask() {
        }

        @Override
        protected ServerOperationResult<String> doInBackground(Void... params) {

            ServerOperationResult<String> result ;
            try {
                String deregRequest = getRelyingPartyComms().deleteAuthenticator(selectedAuthenticationInfo.getId());
                result = new ServerOperationResult<>(deregRequest);
            } catch (ServerError e) {
                result = new ServerOperationResult<>(e.getError());
            } catch (CommunicationsException e) {
                result = new ServerOperationResult<>(e.getError());
            }

            return result;
        }

        @Override
        protected void onPostExecute(final ServerOperationResult<String> response) {
            mDeregisterAuthenticatorTask = null;

            if (response.isSuccessful()) {
                // Stage 2 - if there is an authenticator of this type on the device then attempt to deregister it
                if (hasAuthenticator(selectedAuthenticationInfo.getAaid())) {
                    setCurrentFidoOperation(FidoOperation.Deregistration);
                    Intent intent = getUafClientUtils().getUafOperationIntent(FidoOperation.Deregistration, response.getResponse());
                    sendUafClientIntent(intent, FidoOpCommsType.Return);
                } else {
                    notifyDeactivationComplete();
                }
            } else {
                displayError(response.getError().getMessage());
                currentAction = Action.NONE;
            }
        }

        @Override
        protected void onCancelled() {
            mDeregisterAuthenticatorTask = null;
            showProgress(false);
            currentAction = Action.NONE;
        }
    }

    /**
     * Represents an asynchronous task used to create a registration request.
     */
    public class CreateRegRequestTask extends AsyncTask<Void, Void, ServerOperationResult<CreateRegRequestResponse>> {

        CreateRegRequestTask() {
        }

        @Override
        protected ServerOperationResult<CreateRegRequestResponse> doInBackground(Void... params) {
            ServerOperationResult<CreateRegRequestResponse> result;
            try {
                CreateRegRequestResponse response = getRelyingPartyComms().createRegRequest();
                result = new ServerOperationResult<>(response);
            } catch (ServerError e) {
                result = new ServerOperationResult<>(e.getError());
            } catch (CommunicationsException e) {
                result = new ServerOperationResult<>(e.getError());
            }

            return result;
        }

        @Override
        protected void onPostExecute(final ServerOperationResult<CreateRegRequestResponse> result) {
            mCreateRegRequestTask = null;

            if (result.isSuccessful()) {
                mCreateRegRequestResponse = result.getResponse();

                setCurrentFidoOperation(FidoOperation.Registration);
                Intent intent = getUafClientUtils().getUafOperationIntent(FidoOperation.Registration,
                        mCreateRegRequestResponse.getFidoRegistrationRequest());
                sendUafClientIntent(intent, FidoOpCommsType.Return);

            } else {
                endProgressWithError(result.getError().getMessage());
            }
        }

        @Override
        protected void onCancelled() {
            mCreateRegRequestTask = null;
            showProgress(false);
            currentAction = Action.NONE;

        }
    }

    /**
     * Represents an asynchronous task used to create a FIDO authenticator after a successful registration by a UAF client.
     */
    public class CreateAuthenticatorTask extends AsyncTask<Void, Void, ServerOperationResult<CreateAuthenticatorResponse>> {
        private final CreateAuthenticator createAuthenticator;

        CreateAuthenticatorTask(String sessionId, String registrationChallengeId, String fidoRegistrationResponse) {
            createAuthenticator = new CreateAuthenticator();
            createAuthenticator.setRegistrationChallengeId(registrationChallengeId);
            createAuthenticator.setFidoReqistrationResponse(fidoRegistrationResponse);
        }

        @Override
        protected ServerOperationResult<CreateAuthenticatorResponse> doInBackground(Void... params) {
            ServerOperationResult<CreateAuthenticatorResponse> result;
            try {
                CreateAuthenticatorResponse response = getRelyingPartyComms().createAuthenticator(this.getCreateAuthenticator());
                result = new ServerOperationResult<>(response);
            } catch (ServerError e) {
                result = new ServerOperationResult<>(e.getError());
            } catch (CommunicationsException e) {
                result = new ServerOperationResult<>(e.getError());
            }

            return result;
        }

        @Override
        protected void onPostExecute(final ServerOperationResult<CreateAuthenticatorResponse> result) {

            mCreateAuthenticatorTask = null;

            // SERVER RESPONDED OK
            // Server responded OK but this doesn't necessarily mean that an authenticator was created successfully.
            // The response contains a code which indicates success or failure. This response code is sent on to the UAF
            // client so that it might delete the credential it generated on server failure. The response code
            // is also checked by the RP app. If the return code indicates that no error was returned by the server then
            // the log-in success screen is displayed.
            if (result.isSuccessful()) {
                Intent intent = getUafClientUtils().getUafOperationCompletionStatusIntent(
                        result.getResponse().getFidoRegistrationConfirmation(), result.getResponse().getFidoResponseCode().intValue(),
                        result.getResponse().getFidoResponseMsg());
                sendFidoOperationCompletionIntent(intent);

                // UAF REGISTRATION SUCCESS
                if(result.getResponse().getFidoResponseCode().intValue() == UafServerResponseCodes.OPERATION_COMPLETED) {
                    showSuccessfulRegistration();
                } else {
                    // UAF REGISTRATION FAILURE
                    endProgressWithError(result.getResponse().getFidoResponseMsg());
                }
            } else {
                // SERVER ERROR
                // Now we need to send the registration response and server error back to the UAF client.
                Intent intent = getUafClientUtils().getUafOperationCompletionStatusIntent(
                        this.getCreateAuthenticator().getFidoReqistrationResponse(), UafServerResponseCodes.INTERNAL_SERVER_ERROR,
                        (String)getText(R.string.internal_server_error));
                sendFidoOperationCompletionIntent(intent);

                endProgressWithError(result.getError().getMessage());
            }
        }

        @Override
        protected void onCancelled() {
            mCreateAuthenticatorTask = null;
            showProgress(false);
            currentAction = Action.NONE;
        }

        protected CreateAuthenticator getCreateAuthenticator() {
            return createAuthenticator;
        }
    }

}

