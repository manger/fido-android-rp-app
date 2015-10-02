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
import android.content.Intent;
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
import com.daon.identityx.exception.CommunicationsException;
import com.daon.identityx.exception.ServerError;
import com.daon.identityx.uaf.FidoOperation;
import com.daon.identityx.uaf.UafServerResponseCodes;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * The first screen displayed which offers the user the option to
 *      1. Login with username & password
 *      2. Login with FIDO
 *      3. Add a new account
 *
 *
 */
public class IntroActivity extends BaseActivity  {

    private boolean attemptingAuthentication;
    private CreateAuthRequestTask mCreateAuthRequestTask = null;
    private UserLoginWithFIDOTask mUserLoginWithFIDOTask = null;
    private CreateAuthRequestResponse mCreateAuthRequestResponse;

    // UI Components
    private View mProgressView;
    private View mIntroView;
    private Button mFidoLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intro);

        mIntroView = findViewById(R.id.intro_form);
        mProgressView = findViewById(R.id.intro_progress);

        Button mNewAccountButton = (Button) findViewById(R.id.new_account_button);
        mNewAccountButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToNewAccount();
            }
        });

        Button mPasswordLoginButton = (Button) findViewById(R.id.login_password_button);
        mPasswordLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToLogin();
            }
        });

        mFidoLoginButton = (Button)findViewById(R.id.login_fido_button);
        mFidoLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptFIDOLogin();
            }
        });

        if(this.hasFIDOClient() && this.getAvailableAuthenticatorAaids().length > 0) {
            mFidoLoginButton.setEnabled(true);
        } else {
            mFidoLoginButton.setEnabled(false);
        }
        mIntroView.setVisibility(View.VISIBLE);
    }

    public void attemptFIDOLogin() {
        if(isAnAsyncTaskRunning()) {
            return;
        }

        showProgress(true);
        attemptingAuthentication = true;
        mCreateAuthRequestTask = new CreateAuthRequestTask();
        mCreateAuthRequestTask.execute((Void) null);
    }

    protected boolean isAnAsyncTaskRunning() {
        return (mUserLoginWithFIDOTask != null || mCreateAuthRequestTask != null);
    }



    @Override
    protected void processUafClientResponse(String uafResponseJson) {

        if (this.getCurrentFidoOperation() == FidoOperation.Authentication) {
            // Continue FIDO authentication (log-in with FIDO)
            mUserLoginWithFIDOTask = new UserLoginWithFIDOTask(mCreateAuthRequestResponse.getAuthenticationRequestId(),
                    uafResponseJson);
            mUserLoginWithFIDOTask.execute((Void) null);
            mCreateAuthRequestTask = null;
        }

    }

    @Override
    protected void onActivityResultFailure(String errorMsg) {

        if(this.getCurrentFidoOperation() == FidoOperation.Authentication) {
            showProgress(false);
            Toast.makeText(this,getString(R.string.message_authentication_fido_client_error) + errorMsg, Toast.LENGTH_LONG).show();
        }

    }

    protected void switchToNewAccount() {

        try {
            Intent newIntent = new Intent( this, CreateAccountActivity.class);
            startActivity(newIntent);
        } catch (Throwable ex) {
            displayError(ex.getMessage());
        }
    }

    protected void switchToLogin() {

        try {
            Intent newIntent = new Intent( this, LoginActivity.class);
            startActivity(newIntent);
        } catch (Throwable ex) {
            displayError(ex.getMessage());
        }
    }

    /***
     * The login has been successful and the server has returned a session Id and some additional
     * details to present to the user.
     *
     * @param response the create session response
     */
    protected void showLoggedIn(CreateSessionResponse response) {

        try {
            attemptingAuthentication = false;

            CoreApplication.setSessionId(response.getSessionId());
            CoreApplication.setEmail(response.getEmail());

            Intent newIntent = new Intent(this, HomeActivity.class);
            newIntent.putExtra("LOGGED_IN_WITH", response.getLoggedInWith().toString());
            if (response.getLastLoggedIn() == null) {
                newIntent.putExtra("LAST_LOGGED_IN", getString(R.string.message_first_login));
            } else {
                String dateString = DateFormat.getDateTimeInstance().format(response.getLastLoggedIn());
                newIntent.putExtra("LAST_LOGGED_IN", dateString);
            }

            startActivity(newIntent);
        } catch (Throwable e) {
            displayError(e.getMessage());
        }
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

            mIntroView.setVisibility(show ? View.GONE : View.VISIBLE);
            mIntroView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mIntroView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mIntroView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    protected void endProgressWithError(String errorMsg) {
        attemptingAuthentication = false;
        showProgress(false);
        displayError(errorMsg);
        mIntroView.requestFocus();
    }

    /***
     * Class to handle the creation of the FIDO authentication request
     *
     */
    public class CreateAuthRequestTask extends AsyncTask<Void, Void, ServerOperationResult<CreateAuthRequestResponse>> {

        CreateAuthRequestTask() {

        }

        @Override
        protected ServerOperationResult<CreateAuthRequestResponse> doInBackground(Void... params) {

            ServerOperationResult<CreateAuthRequestResponse> result;
            try {
                CreateAuthRequestResponse response = getRelyingPartyComms().createAuthRequest();
                result = new ServerOperationResult<>(response);
            } catch (ServerError e) {
                result = new ServerOperationResult<>(e.getError());
            } catch (CommunicationsException e) {
                result = new ServerOperationResult<>(e.getError());
            }

            return result;
        }

        @Override
        protected void onPostExecute(final ServerOperationResult<CreateAuthRequestResponse> result) {
            mCreateAuthRequestTask = null;

            if (result.isSuccessful()) {
                mCreateAuthRequestResponse = result.getResponse();

                // Send authentication request to the UAF client
                setCurrentFidoOperation(FidoOperation.Authentication);
                Intent intent = getUafClientUtils().getUafOperationIntent(FidoOperation.Authentication,
                        result.getResponse().getFidoAuthenticationRequest());
                sendUafClientIntent(intent, FidoOpCommsType.Return);
            } else {
                endProgressWithError(result.getError().getMessage());
            }
        }

        @Override
        protected void onCancelled() {
            mUserLoginWithFIDOTask = null;
            showProgress(false);
        }

    }

    /***
     * Class to handle the actual authentication of the user with FIDO.  The
     * response from the FIDO client is sent to the server where the authentication is
     * performed.
     *
     */
    public class UserLoginWithFIDOTask extends AsyncTask<Void, Void, ServerOperationResult<CreateSessionResponse>> {

        private final CreateSession createSession;

        UserLoginWithFIDOTask(String authenticationRequestId, String fidoAuthenticationResponse) {
            createSession = new CreateSession();
            getCreateSession().setAuthenticationRequestId(authenticationRequestId);
            getCreateSession().setFidoAuthenticationResponse(fidoAuthenticationResponse);
        }

        @Override
        protected ServerOperationResult<CreateSessionResponse> doInBackground(Void... params) {

            ServerOperationResult<CreateSessionResponse> result;
            try {
                CreateSessionResponse response = getRelyingPartyComms().createSession(this.getCreateSession());
                result = new ServerOperationResult<>(response);
            } catch (ServerError e) {
                result = new ServerOperationResult<>(e.getError());
            } catch (CommunicationsException e) {
                result = new ServerOperationResult<>(e.getError());
            }

            return result;
        }

        @Override
        protected void onPostExecute(final ServerOperationResult<CreateSessionResponse> result) {
            mUserLoginWithFIDOTask = null;

            if (result.isSuccessful()) {
                Intent intent = getUafClientUtils().getUafOperationCompletionStatusIntent(
                        result.getResponse().getFidoAuthenticationResponse(), result.getResponse().getFidoResponseCode().intValue(),
                        result.getResponse().getFidoResponseMsg());
                sendFidoOperationCompletionIntent(intent);

                // UAF AUTHENTICATION SUCCESS
                if(result.getResponse().getFidoResponseCode().intValue() == UafServerResponseCodes.OPERATION_COMPLETED) {
                    showProgress(false);
                    showLoggedIn(result.getResponse());
                } else {
                    // UAF AUTHENTICATION FAILURE
                    endProgressWithError(result.getResponse().getFidoResponseMsg());
                }
            } else {
                // SERVER ERROR
                // Now we need to send the registration response and server error back to the UAF client.
                Intent intent = getUafClientUtils().getUafOperationCompletionStatusIntent(
                        this.getCreateSession().getFidoAuthenticationResponse(), UafServerResponseCodes.INTERNAL_SERVER_ERROR,
                        (String)getText(R.string.internal_server_error));
                sendFidoOperationCompletionIntent(intent);

                endProgressWithError(result.getError().getMessage());
            }
        }

        @Override
        protected void onCancelled() {
            mUserLoginWithFIDOTask = null;
            showProgress(false);
        }

        public CreateSession getCreateSession() {
            return createSession;
        }
    }

}

