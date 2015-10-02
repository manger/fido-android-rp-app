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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.daon.identityx.controller.model.*;
import com.daon.identityx.controller.model.Error;
import com.daon.identityx.exception.CommunicationsException;
import com.daon.identityx.exception.ServerError;
import com.daon.identityx.uaf.FidoOperation;
import com.daon.identityx.uaf.UafServerResponseCodes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A signup screen that offers signup via email/password.
 */
public class CreateAccountActivity extends BaseActivity {//implements LoaderCallbacks<Cursor> {

    /**
     * Keep track of the tasks to ensure we can cancel them if requested.
     */
    private UserSignupTask mSignupTask = null;
    private CreateAuthenticatorTask mCreateAuthenticatorTask = null;
    private CreateAccount mCreateAccount = null;
    private CreateAccountResponse mCreateAccountResponse = null;

    private static final String EMAIL_PATTERN =
            "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                    + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);


    // UI references.
    private EditText mFirstNameView;
    private EditText mLastNameView;
    private EditText mEmailView;
    private EditText mPasswordView;
    private EditText mConfirmPasswordView;
    private CheckBox mRegisterWithFidoView;
    private View mProgressView;
    private View mSignupFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_account);

        // Set up the signup form.
        mFirstNameView = (EditText) findViewById(R.id.first_name);
        mLastNameView = (EditText) findViewById(R.id.last_name);
        mEmailView = (EditText) findViewById(R.id.email);

        mPasswordView = (EditText) findViewById(R.id.password);
        mConfirmPasswordView = (EditText) findViewById(R.id.confirm_password);
        mConfirmPasswordView.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    attemptSignup();
                    return true;
                }
                return false;
            }
        });
        mRegisterWithFidoView = (CheckBox) findViewById(R.id.register_with_fido_check_box);

        // Only allow register with FIDO if it is an option
        if(!this.hasFIDOClient()) {
            mRegisterWithFidoView.setChecked(false);
            mRegisterWithFidoView.setVisibility(View.GONE);
        } else {
            mRegisterWithFidoView.setChecked(true);
            mRegisterWithFidoView.setVisibility(View.VISIBLE);
        }

        Button createButton = (Button) findViewById(R.id.create_button);
        createButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptSignup();
            }
        });

        mSignupFormView = findViewById(R.id.signup_form);
        mProgressView = findViewById(R.id.signup_progress);
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptSignup() {
        if (mSignupTask != null || mCreateAuthenticatorTask != null ) {
            return;
        }

        // Reset errors.
        mFirstNameView.setError(null);
        mLastNameView.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mConfirmPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String firstName = mFirstNameView.getText().toString();
        String lastName = mLastNameView.getText().toString();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String confirmPassword = mConfirmPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a first name, if the user entered one.
        if (TextUtils.isEmpty(firstName)) {
            mFirstNameView.setError(getString(R.string.error_required_first_name));
            focusView = mFirstNameView;
            cancel = true;
        }

        // Check for a last, if the user entered one.
        if (TextUtils.isEmpty(lastName)) {
            mLastNameView.setError(getString(R.string.error_required_last_name));
            focusView = mLastNameView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_required_email));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email_signup));
            focusView = mEmailView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_required_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid password, if the user entered one.
        if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password_signup));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid confirm password, if the user entered one.
        if (TextUtils.isEmpty(confirmPassword)) {
            mPasswordView.setError(getString(R.string.error_required_confirm_password));
            focusView = mConfirmPasswordView;
            cancel = true;
        }

        // Check for a valid confirm password, if the user entered one.
        if (!password.equals(confirmPassword)) {
            mConfirmPasswordView.setError(getString(R.string.error_inconsistent_password));
            focusView = mConfirmPasswordView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mConfirmPasswordView.getWindowToken(), 0);
            mSignupTask = new UserSignupTask(firstName, lastName, email, password, mRegisterWithFidoView.isChecked());
            mSignupTask.execute((Void) null);
        }
    }

    private boolean isEmailValid(String email) {

        Matcher matcher = emailPattern.matcher(email);
        return matcher.matches();
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 1;
    }

    protected void showLoggedIn() {

        try {
            Intent newIntent = new Intent(this, HomeActivity.class);
            newIntent.putExtra("LOGGED_IN_WITH", getString(R.string.message_account_just_created));
            newIntent.putExtra("LAST_LOGGED_IN", getString(R.string.message_first_login));
            startActivity(newIntent);
            finish();
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

            mSignupFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mSignupFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mSignupFormView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mSignupFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    protected void processUafClientResponse(String uafResponseJson) {
        mCreateAuthenticatorTask = new CreateAuthenticatorTask(mCreateAccountResponse.getRegistrationRequestId(), uafResponseJson);
        mCreateAuthenticatorTask.execute((Void) null);
    }

    @Override
    protected void onActivityResultFailure(String errorMsg) {
        endProgressWithError(errorMsg);
    }

    protected void endProgressWithError(String errorMsg) {
        showProgress(false);
        displayError(errorMsg);
        mPasswordView.requestFocus();
    }

    /**
     * Represents an asynchronous task used to create a FIDO authenticator after a successful registration by a UAF client.
     */
    public class CreateAuthenticatorTask extends AsyncTask<Void, Void, ServerOperationResult<CreateAuthenticatorResponse>> {
        private final CreateAuthenticator createAuthenticator;

        CreateAuthenticatorTask(String registrationChallengeId, String fidoRegistrationResponse) {
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
                    showProgress(false);
                    showLoggedIn();
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
        }

        protected CreateAuthenticator getCreateAuthenticator() {
            return createAuthenticator;
        }
    }

    /**
     * Represents an asynchronous task used to create a user account.
     */
    public class UserSignupTask extends AsyncTask<Void, Void, ServerOperationResult<CreateAccountResponse>> {

        private final CreateAccount createAccount;

        UserSignupTask(String firstName, String lastName, String email, String password, boolean registrationRequested) {

            createAccount = new CreateAccount();
            createAccount.setFirstName(firstName);
            createAccount.setLastName(lastName);
            createAccount.setEmail(email);
            createAccount.setPassword(password);
            createAccount.setRegistrationRequested(registrationRequested);
        }

        @Override
        protected ServerOperationResult<CreateAccountResponse> doInBackground(Void... params) {
            ServerOperationResult<CreateAccountResponse> result;
            try {
                CreateAccountResponse response = getRelyingPartyComms().createAccount(this.getCreateAccount());
                result = new ServerOperationResult<>(response);
            } catch (ServerError e) {
                result = new ServerOperationResult<>(e.getError());
            } catch (CommunicationsException e) {
                result = new ServerOperationResult<>(e.getError());
            }

            return result;
        }

        @Override
        protected void onPostExecute(final ServerOperationResult<CreateAccountResponse> result) {
            mSignupTask = null;

            if (result.isSuccessful()) {
                CoreApplication.setEmail(createAccount.getEmail());
                CoreApplication.setSessionId(result.getResponse().getSessionId());
                if(createAccount.isRegistrationRequested()) {
                    // If we do require a FIDO registration, now is the time to call the UAF client to perform the registration.
                    // First store the details as they are required by later stages of processing.
                    mCreateAccount = getCreateAccount();
                    mCreateAccountResponse = result.getResponse();

                    setCurrentFidoOperation(FidoOperation.Registration);
                    Intent intent = getUafClientUtils().getUafOperationIntent(FidoOperation.Registration,
                            mCreateAccountResponse.getFidoRegistrationRequest());
                    sendUafClientIntent(intent, FidoOpCommsType.Return);
                } else {
                    // If we don't require FIDO, we're done at this point
                    showProgress(false);
                }

            } else {
                endProgressWithError(result.getError().getMessage());
            }
        }

        @Override
        protected void onCancelled() {
            mSignupTask = null;
            showProgress(false);
        }

        protected CreateAccount getCreateAccount() {
            return createAccount;
        }
    }
}

