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
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.daon.identityx.controller.model.CreateAuthRequestResponse;
import com.daon.identityx.controller.model.CreateTransactionAuthRequest;
import com.daon.identityx.controller.model.ValidateTransactionAuth;
import com.daon.identityx.controller.model.ValidateTransactionAuthResponse;
import com.daon.identityx.exception.CommunicationsException;
import com.daon.identityx.exception.ServerError;
import com.daon.identityx.uaf.FidoOperation;
import com.daon.identityx.uaf.UafServerResponseCodes;

/***
 * This activity presents the option of creating FIDO transactions to the user.
 * It should be noted that it is not required that FIDO authenticators support transactions
 * and as such these may not work on devices with some authenticators.
 *
 */
public class TransactionActivity extends BaseActivity {

    // UI references
    private View mProgressView;
    private View mTransactionsView;
    private EditText mTransactionTextView;
    private RadioGroup mTransactionTypeRadioGroup;
    private LinearLayout mTransactionDetails;
    private CheckBox mCheckBoxStepUp;
    private CheckBox mCheckBoxTxnConfirmation;

    private CreateTransactionAuthRequestTask mCreateTransactionAuthRequestTask = null;
    private ValidateTransactionAuthTask mValidateTransactionAuthTask = null;
    private CreateAuthRequestResponse mCreateAuthRequestResponse;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction);

        Intent intent = getIntent();

        mCheckBoxStepUp = (CheckBox) findViewById(R.id.step_up_auth_check_box);
        mCheckBoxTxnConfirmation = (CheckBox) findViewById(R.id.transaction_confirmation_check_box);
        mTransactionDetails = (LinearLayout) findViewById(R.id.transaction_details);
        mTransactionsView = findViewById(R.id.transaction_view);
        mProgressView = findViewById(R.id.transaction_progress);

        mTransactionTextView = (EditText) findViewById(R.id.transaction_text_field);
        mTransactionDetails.setVisibility(View.GONE);
        mTransactionTextView.setVisibility(View.GONE);

        mCheckBoxTxnConfirmation.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
               @Override
               public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                   if (isChecked) {
                       mTransactionDetails.setVisibility(View.VISIBLE);
                   } else {
                       mTransactionDetails.setVisibility(View.GONE);
                   }
               }
        });

        mTransactionTypeRadioGroup = (RadioGroup) findViewById(R.id.transactionType);
        mTransactionTypeRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.imageTransactionRadioButton) {
                    mTransactionTextView.setVisibility(View.GONE);
                } else {
                    mTransactionTextView.setVisibility(View.VISIBLE);
                }
            }
        });

        Button authenticateButton = (Button) findViewById(R.id.authenticate_button);
        authenticateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptAuthentication();
            }
        });

    }

    /***
     * Attempt to perform a text transaction by asking the server to create the
     * transaction.
     *
     */
    protected void attemptAuthentication() {
        if(isAnAsyncTaskRunning()) {
            return;
        }

        boolean stepUp = mCheckBoxStepUp.isChecked();
        boolean txnConfirmation = mCheckBoxTxnConfirmation.isChecked();
        boolean imageTransaction;
        String transactionType = null;
        String transactionContent = null;
        if (txnConfirmation) {
            int id = mTransactionTypeRadioGroup.getCheckedRadioButtonId();
            if (id == -1) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                alertDialogBuilder.setMessage(R.string.dialog_select_image_or_text);

                alertDialogBuilder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        return;
                    }
                });
                AlertDialog alertDialog = alertDialogBuilder.create();
                alertDialog.show();
                return;
            } else {
                imageTransaction = (id == R.id.imageTransactionRadioButton);
                if (imageTransaction) {
                    transactionType = "image/png";
                    transactionContent = (String) getText(R.string.transaction_image_content);
                } else {
                    transactionType = "text/plain";
                    transactionContent = mTransactionTextView.getText().toString();
                    if (transactionContent.length() == 0) {
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
                        alertDialogBuilder.setMessage(R.string.dialog_enter_transaction_text);

                        alertDialogBuilder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                return;
                            }
                        });
                        AlertDialog alertDialog = alertDialogBuilder.create();
                        alertDialog.show();
                        return;
                    }
                }
            }
        }
        showProgress(true);
        mCreateTransactionAuthRequestTask = new CreateTransactionAuthRequestTask(transactionType,
                transactionContent, stepUp);
        mCreateTransactionAuthRequestTask.execute((Void) null);
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

            mTransactionsView.setVisibility(show ? View.GONE : View.VISIBLE);
            mTransactionsView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mTransactionsView.setVisibility(show ? View.GONE : View.VISIBLE);
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
            mTransactionsView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    protected void endProgressWithError(String errorMsg) {
        showProgress(false);
        displayError(errorMsg);
        mTransactionsView.requestFocus();
    }

    protected boolean isAnAsyncTaskRunning() {
        return (mCreateTransactionAuthRequestTask != null || mValidateTransactionAuthTask != null);
    }

    @Override
    protected void processUafClientResponse(String uafResponseJson) {
        // Continue FIDO transaction authentication
        mValidateTransactionAuthTask = new ValidateTransactionAuthTask(mCreateAuthRequestResponse.getAuthenticationRequestId(),
                uafResponseJson);
        mValidateTransactionAuthTask.execute((Void) null);
    }

    @Override
    protected void onActivityResultFailure(String errorMsg) {
        // FIDO transaction authentication failed on UAF client
        endProgressWithError(errorMsg);
    }

    /***
     * This class is used to request a transaction authentication request from the server
     */
    public class CreateTransactionAuthRequestTask extends AsyncTask<Void, Void, ServerOperationResult<CreateAuthRequestResponse>> {

        private final CreateTransactionAuthRequest createTransactionAuthRequest;

        CreateTransactionAuthRequestTask(String transactionContentType, String transactionContent, boolean stepUpAuth) {

            createTransactionAuthRequest = new CreateTransactionAuthRequest();
            createTransactionAuthRequest.setTransactionContentType(transactionContentType);
            createTransactionAuthRequest.setTransactionContent(transactionContent);
            createTransactionAuthRequest.setStepUpAuth(stepUpAuth);
        }

        @Override
        protected ServerOperationResult<CreateAuthRequestResponse> doInBackground(Void... params) {

            ServerOperationResult<CreateAuthRequestResponse> result;
            try {
                CreateAuthRequestResponse response = getRelyingPartyComms().
                        createTransactionAuthRequest(this.getCreateTransactionAuthRequest());
                result = new ServerOperationResult<>(response);
            } catch (ServerError e) {
                result = new ServerOperationResult<>(e.getError());
            } catch (CommunicationsException e) {
                result = new ServerOperationResult<>(e.getError());
            }

            return result;
        }

        /***
         * If the response has been successful, send an intent to the FIDO client to
         * perform the authentication.
         *
         * @param result - the result of creating the authentication request
         */
        @Override
        protected void onPostExecute(final ServerOperationResult<CreateAuthRequestResponse> result) {
            mCreateTransactionAuthRequestTask = null;

            if (result.isSuccessful()) {
                mCreateAuthRequestResponse = result.getResponse();

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
            mCreateTransactionAuthRequestTask = null;
            showProgress(false);
        }

        public CreateTransactionAuthRequest getCreateTransactionAuthRequest() {
            return createTransactionAuthRequest;
        }
    }

    /***
     * This class is used to validate the transaction authentication by the server
     */
    public class ValidateTransactionAuthTask extends AsyncTask<Void, Void, ServerOperationResult<ValidateTransactionAuthResponse>> {

        private final ValidateTransactionAuth validateTransactionAuth;

        ValidateTransactionAuthTask(String authenticationRequestId, String fidoAuthenticationResponse) {
            validateTransactionAuth = new ValidateTransactionAuth();
            validateTransactionAuth.setAuthenticationRequestId(authenticationRequestId);
            validateTransactionAuth.setFidoAuthenticationResponse(fidoAuthenticationResponse);
        }

        @Override
        protected ServerOperationResult<ValidateTransactionAuthResponse> doInBackground(Void... params) {

            ServerOperationResult<ValidateTransactionAuthResponse> result;
            try {
                ValidateTransactionAuthResponse response = getRelyingPartyComms()
                        .validateTransactionAuthRequest(this.getValidateTransactionAuth());
                result = new ServerOperationResult<>(response);
            } catch (ServerError e) {
                result = new ServerOperationResult<>(e.getError());
            } catch (CommunicationsException e) {
                result = new ServerOperationResult<>(e.getError());
            }

            return result;
        }

        @Override
        protected void onPostExecute(final ServerOperationResult<ValidateTransactionAuthResponse> result) {
            mValidateTransactionAuthTask = null;

            // SERVER RESPONDED OK
            // Server responded OK but this doesn't necessarily mean that validation was successful.
            // The response contains a code which indicates success or failure. This response code is sent on to the UAF
            // client so that it might react to the server failure. The response code is also checked by the RP app. If
            // the return code indicates that no error was returned by the server then a success message is displayed.
            if (result.isSuccessful()) {
                Intent intent = getUafClientUtils().getUafOperationCompletionStatusIntent(
                        result.getResponse().getFidoAuthenticationResponse(), result.getResponse().getFidoResponseCode().intValue(),
                        result.getResponse().getFidoResponseMsg());
                sendFidoOperationCompletionIntent(intent);

                // UAF AUTHENTICATION SUCCESS
                if(result.getResponse().getFidoResponseCode().intValue() == UafServerResponseCodes.OPERATION_COMPLETED) {
                    showProgress(false);
                    Toast.makeText(TransactionActivity.this, R.string.transaction_validation_success, Toast.LENGTH_LONG).show();
                } else {
                    // UAF AUTHENTICATION FAILURE
                    endProgressWithError(result.getResponse().getFidoResponseMsg());
                }
            } else {
                // SERVER ERROR
                // Now we need to send the registration response and server error back to the UAF client.
                Intent intent = getUafClientUtils().getUafOperationCompletionStatusIntent(
                        this.getValidateTransactionAuth().getFidoAuthenticationResponse(), UafServerResponseCodes.INTERNAL_SERVER_ERROR,
                        (String)getText(R.string.internal_server_error));
                sendFidoOperationCompletionIntent(intent);

                endProgressWithError(result.getError().getMessage());
            }
        }

        @Override
        protected void onCancelled() {
            mValidateTransactionAuthTask = null;
            showProgress(false);
        }

        public ValidateTransactionAuth getValidateTransactionAuth() {
            return validateTransactionAuth;
        }
    }

}
