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


import com.daon.identityx.controller.model.CreateAuthRequestResponse;
import com.daon.identityx.controller.model.CreateAuthenticator;
import com.daon.identityx.controller.model.CreateAuthenticatorResponse;
import com.daon.identityx.controller.model.DeleteAccountResponse;
import com.daon.identityx.controller.model.GetAuthenticatorResponse;
import com.daon.identityx.controller.model.ListAuthenticatorsResponse;
import com.daon.identityx.controller.model.CreateRegRequestResponse;
import com.daon.identityx.controller.model.CreateSession;
import com.daon.identityx.controller.model.CreateSessionResponse;
import com.daon.identityx.controller.model.CreateAccount;
import com.daon.identityx.controller.model.CreateAccountResponse;
import com.daon.identityx.controller.model.CreateTransactionAuthRequest;
import com.daon.identityx.controller.model.ValidateTransactionAuth;
import com.daon.identityx.controller.model.ValidateTransactionAuthResponse;

/***
 * This interface is used by the app to communicate with the server
 */
public interface IRelyingPartyComms {

    /***
     * Create an account on the server.  This can also create a FIDO registration request
     * if required.
     *
     * @param createAccount - the details of the account to be created
     * @return CreateAccountResponse
     */
    CreateAccountResponse createAccount(CreateAccount createAccount);

    /***
     * Create a session for the user - basically authenticate the user with FIDO
     * or username & password
     *
     * @param createSession - the details of the session to be created
     * @return CreateSessionResponse
     */
    CreateSessionResponse createSession(CreateSession createSession);

    /***
     * In order to authenticate with FIDO, an authentication request must be created.
     * This call does that.
     *
     * @return CreateAuthRequestResponse
     */
    CreateAuthRequestResponse createAuthRequest();

    /***
     * PROTECTED OPERATION - the server will only process this if a valid session is in place
     *
     * Delete the session thereby logging the user off
     *
     * @param sessionId - the session to be deleted
     */
    void deleteSession(String sessionId);

    /***
     * PROTECTED OPERATION - the server will only process this if a valid session is in place
     *
     * Delete the account and deregister all active FIDO authenticators
     *
     * @param sessionId - the session associated with the account to be deleted
     * @return DeleteAccountResponse
     */
    DeleteAccountResponse deleteAccount(String sessionId);

    /***
     * PROTECTED OPERATION - the server will only process this if a valid session is in place
     *
     * This creates a FIDO confirmation transaction - like the authentication request
     * but with content to be displayed to the user in the form of text or a graphic
     *
     * @param createTransactionAuthRequest - the details of the transaction
     * @return CreateAuthRequestResponse
     */
    CreateAuthRequestResponse createTransactionAuthRequest(CreateTransactionAuthRequest createTransactionAuthRequest);

    /***
     * PROTECTED OPERATION - the server will only process this if a valid session is in place
     *
     * The process of validating the transaction authenticated by the user to ensure it is valid
     *
     * @param validateTransactionAuth - the details of the transaction to be validated
     * @return ValidateTransactionAuthResponse
     */
    ValidateTransactionAuthResponse validateTransactionAuthRequest(ValidateTransactionAuth validateTransactionAuth);

    /***
     * PROTECTED OPERATION - the server will only process this if a valid session is in place
     *
     * Create a FIDO registration request, allowing a user to assocaited a FIDO authenticator with
     * the account.
     *
     * @return CreateRegRequestResponse
     */
    CreateRegRequestResponse createRegRequest();

    /***
     * PROTECTED OPERATION - the server will only process this if a valid session is in place
     *
     * After the user has confirmed the creation of a new FIDO authenticator in response to the
     * registration request, this operation processes the response form the FIDO authenticator
     *
     * @param createAuthenticator - the details of the authenticator to create
     * @return CreateAuthenticatorResponse
     */
    CreateAuthenticatorResponse createAuthenticator(CreateAuthenticator createAuthenticator);

    /***
     * PROTECTED OPERATION - the server will only process this if a valid session is in place
     *
     * Returns a list of FIDO authenticators associated with this account.
     *
     * The account is determined from the session.
     *
     * @return ListAuthenticatorsResponse
     */
    ListAuthenticatorsResponse listAuthenticators();

    /***
     * PROTECTED OPERATION - the server will only process this if a valid session is in place
     *
     * Returns more details about the specified authenticator.
     *
     * @param authenticatorId - the ID of the authenticator to retrieve
     * @return GetAuthenticatorResponse
     */
    GetAuthenticatorResponse getAuthenticator(String authenticatorId);

    /***
     * PROTECTED OPERATION - the server will only process this if a valid session is in place
     *
     * Deletes the specified authenticator returning the FIDO deregistration request allowing the
     * FIDO authenticator to be disconnected from the relying party application.
     *
     * @param authenticatorId - the ID of the authenticator to delete
     * @return String
     */
    String deleteAuthenticator(String authenticatorId);
}
