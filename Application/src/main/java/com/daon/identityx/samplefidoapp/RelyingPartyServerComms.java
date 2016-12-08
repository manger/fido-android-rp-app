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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.daon.identityx.controller.model.*;
import com.daon.identityx.controller.model.Error;
import com.daon.identityx.exception.CommunicationsException;
import com.daon.identityx.exception.ServerError;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.net.ssl.TrustManagerFactory;

/***
 * The core class used to communicate with the server.
 *
 * Created by Daon
 */
public class RelyingPartyServerComms implements IRelyingPartyComms {

    private static final int CONNECTION_TIMEOUT = 20000;
    private static final int READ_TIMEOUT = 20000;
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String SESSION_IDENTIFIER_HEADER = "Session-Id";
    private static final String CONTENT_TYPE = "application/json";
    private static final String POST_METHOD = "POST";
    private static final String GET_METHOD = "GET";
    private static final String DELETE_METHOD = "DELETE";

    private GsonBuilder builder;
    private Context context;

    protected Context getContext() {
        return this.context;
    }

    public RelyingPartyServerComms(Context context) {
        builder = new GsonBuilder();
//        builder.registerTypeAdapter(Timestamp.class, new JsonDeserializer<Timestamp>() {
//            public Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
//                return new Timestamp(json.getAsJsonPrimitive().getAsLong());
//            }
//        });
        builder.registerTypeAdapter(Date.class, new JsonDeserializer<Date>() {
            public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
                try {
                    String dateAsString = json.getAsJsonPrimitive().getAsString().replaceAll("\"", "");
                    return formatter.parse(dateAsString);
                }catch (Exception ex) {
                    return new Date();
                }
            }
        });

        this.context = context;
    }

    public class HttpResponse {
        private final String payload;
        private final int httpStatusCode;

        public HttpResponse(String payload, int httpStatusCode) {
            this.httpStatusCode = httpStatusCode;
            this.payload = payload;
        }

        public String getPayload() {
            return payload;
        }

        public int getHttpStatusCode() {
            return httpStatusCode;
        }
    }

    public CreateAccountResponse createAccount(CreateAccount createAccount) {

        return this.post("accounts", createAccount, CreateAccountResponse.class);
    }

    public CreateSessionResponse createSession(CreateSession createSession) {

        return this.post("sessions", createSession, CreateSessionResponse.class);
    }

    public ListAuthenticatorsResponse listAuthenticators() {
        return this.get("listAuthenticators", ListAuthenticatorsResponse.class);
    }

    public GetAuthenticatorResponse getAuthenticator(String id) {
        return this.get("authenticators", id, GetAuthenticatorResponse.class);
    }

    public void deleteSession(String id) {

        this.deleteResource("sessions", id, false);
    }


    public DeleteAccountResponse deleteAccount(String sessionId) {
        return this.deleteResource("accounts", sessionId, DeleteAccountResponse.class);
    }

    public String deleteAuthenticator(String authenticatorId) {
        return this.deleteResource("authenticators", authenticatorId, true);
    }

    public CreateAuthRequestResponse createAuthRequest() {

        return this.get("authRequests", CreateAuthRequestResponse.class);
    }

    public CreateAuthRequestResponse createTransactionAuthRequest(CreateTransactionAuthRequest createTransactionAuthRequest) {

        return this.post("transactionAuthRequests", createTransactionAuthRequest, CreateAuthRequestResponse.class);
    }

    public ValidateTransactionAuthResponse validateTransactionAuthRequest(ValidateTransactionAuth validateTransactionAuth) {

        return this.post("transactionAuthValidation", validateTransactionAuth, ValidateTransactionAuthResponse.class);
    }

    public CreateAuthenticatorResponse createAuthenticator(CreateAuthenticator createAuthenticator) {

        return this.post("authenticators", createAuthenticator, CreateAuthenticatorResponse.class);
    }


    public CreateRegRequestResponse createRegRequest() {

        return this.get("regRequests", CreateRegRequestResponse.class);
    }

    protected String deleteResource(String resource, String resourceId, boolean withOutput) {

        HttpResponse response = this.delete(resource, resourceId, withOutput);
        if (response.getHttpStatusCode() == HttpURLConnection.HTTP_OK) {
            if (withOutput) {
                    return response.getPayload();
            } else {
                return null;
            }
        } else {
            Gson outputGson = builder.create();
            Error error = outputGson.fromJson(response.getPayload(), Error.class);
            throw new ServerError(error);
        }
    }

    protected <T> T deleteResource(String resource, String resourceId, Class<T> clazz) {

        HttpResponse response = this.delete(resource, resourceId, true);
        if (response.getHttpStatusCode() == HttpURLConnection.HTTP_OK) {
            Gson outputGson = builder.create();
            return outputGson.fromJson(response.getPayload(), clazz);
        } else {
            Gson outputGson = builder.create();
            Error error = outputGson.fromJson(response.getPayload(), Error.class);
            throw new ServerError(error);
        }
    }

    protected <T> T get(String resource, String id, Class<T> clazz) {
        return this.get(resource + "/" + id, clazz);
    }

    protected <T> T get(String resource, Class<T> clazz) {

        HttpResponse response = this.get(resource);
        if (response.getHttpStatusCode() == HttpURLConnection.HTTP_CREATED || response.getHttpStatusCode() == HttpURLConnection.HTTP_OK) {
            Gson outputGson = builder.create();
            return outputGson.fromJson(response.getPayload(), clazz);
        } else {
            Gson outputGson = builder.create();
            Error error = outputGson.fromJson(response.getPayload(), Error.class);
            throw new ServerError(error);
        }
    }

    protected <T> T post(String resource, String id, Object object, Class<T> clazz) {
        return this.post(resource + "/" + id, object, clazz);
    }

    protected <T> T post(String resource, Object object, Class<T> clazz) {

        Gson inputGson = builder.create();
        String payload = inputGson.toJson(object);
        HttpResponse response = this.post(resource, payload);
        if (response.getHttpStatusCode() == HttpURLConnection.HTTP_CREATED || response.getHttpStatusCode() == HttpURLConnection.HTTP_OK) {
            Gson outputGson = builder.create();
            return outputGson.fromJson(response.getPayload(), clazz);
        } else {
            Gson outputGson = builder.create();
            Error error = outputGson.fromJson(response.getPayload(), Error.class);
            throw new ServerError(error);
        }
    }

    protected String getAbsoluteUrl(String relativeUrl) {
        return getBaseUrl() + relativeUrl;
    }

    protected String getBaseUrl() throws MalformedURLException {
    SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
    String serverUrl = sharedPref.getString(SettingsActivity.PREF_SERVER_URL, "acme.com");
    String port = sharedPref.getString(SettingsActivity.PREF_SERVER_PORT, "8443");
    String scheme = sharedPref.getBoolean(SettingsActivity.PREF_SERVER_SECURE, false) ? "https" : "http";

    URL url = new URL(scheme + "://" + serverUrl);

    return scheme + "://" + url.getHost() + ":" + port + url.getPath() + "/";
    }

    protected HttpResponse delete(String relativeUrl, String id, boolean withOutput) {
        return this.delete(relativeUrl + "/" + id, withOutput);
    }


    protected HttpResponse delete(String relativeUrl, boolean withOutput) {

        HttpURLConnection urlConnection=null;
        try {
            urlConnection = this.createConnection(relativeUrl, DELETE_METHOD, withOutput);
            int httpResult = urlConnection.getResponseCode();
            String response = null;
            if (httpResult == HttpURLConnection.HTTP_OK) {
                if (withOutput) {
                    response = this.readStream(urlConnection.getInputStream());
                }
                return new HttpResponse(response, httpResult);
            } else {
                response = this.readStream(urlConnection.getErrorStream());
                return new HttpResponse(response, httpResult);
            }

        } catch (MalformedURLException e) {

            Error error = new Error();
            error.setCode(-1);
            error.setMessage("Unable to connect to the server - likely a programming error");
            throw new CommunicationsException(error);
        }
        catch (IOException e) {

            Error error = new Error();
            error.setCode(-2);
            error.setMessage("Unable to connect to the server.  Is the server running?");
            throw new CommunicationsException(error);
        } catch(GeneralSecurityException e) {
            Error error = new Error();
            error.setCode(-3);
            error.setMessage("Security error initialising HTTPS connection");
            throw new CommunicationsException(error);
        } finally{
            if(urlConnection!=null)
                urlConnection.disconnect();
        }
    }

    protected HttpResponse get(String relativeUrl) {

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = this.createConnection(relativeUrl, GET_METHOD, false);

            int httpResult = urlConnection.getResponseCode();
            String response;
            if (httpResult == HttpURLConnection.HTTP_CREATED || httpResult == HttpURLConnection.HTTP_OK) {
                response = this.readStream(urlConnection.getInputStream());
            } else {
                response = this.readStream(urlConnection.getErrorStream());
            }

            return new HttpResponse(response, httpResult);

        } catch (MalformedURLException e) {
            Error error = new Error();
            error.setCode(-1);
            error.setMessage("Unable to connect to the server - likely a programming error");
            throw new CommunicationsException(error);
        } catch (IOException e) {
            Error error = new Error();
            error.setCode(-2);
            error.setMessage("Unable to connect to the server.  Is the server running?");
            throw new CommunicationsException(error);
        } catch(GeneralSecurityException e) {
            Error error = new Error();
            error.setCode(-3);
            error.setMessage("Security error initialising HTTPS connection");
            throw new CommunicationsException(error);
        } finally{
            if(urlConnection!=null)
                urlConnection.disconnect();
        }
    }



    protected HttpResponse post(String relativeUrl, String payload) {

        HttpURLConnection urlConnection = null;
        try {
            urlConnection = this.createConnection(relativeUrl, POST_METHOD, true);
            OutputStreamWriter out = new   OutputStreamWriter(urlConnection.getOutputStream());
            out.write(payload);
            out.close();

            int httpResult = urlConnection.getResponseCode();
            String response;
            if (httpResult == HttpURLConnection.HTTP_CREATED || httpResult == HttpURLConnection.HTTP_OK) {
                response = this.readStream(urlConnection.getInputStream());
            } else {
                response = this.readStream(urlConnection.getErrorStream());
            }

            return new HttpResponse(response, httpResult);

        } catch (MalformedURLException e) {
            Error error = new Error();
            error.setCode(-1);
            error.setMessage("Unable to connect to the server - likely a programming error");
            throw new CommunicationsException(error);
        } catch (IOException e) {
            Error error = new Error();
            error.setCode(-2);
            error.setMessage("Unable to connect to the server.  Is the server running?");
            throw new CommunicationsException(error);
        } catch(GeneralSecurityException e) {
            Error error = new Error();
            error.setCode(-3);
            error.setMessage("Security error initialising HTTPS connection");
            throw new CommunicationsException(error);
        } finally{
            if(urlConnection!=null)
                urlConnection.disconnect();
        }
    }

    protected HttpURLConnection createConnection(String relativeUrl, String method, boolean output) throws
            IOException, KeyManagementException, NoSuchAlgorithmException {
        URL url = new URL(getAbsoluteUrl(relativeUrl));
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(output);
        urlConnection.setRequestMethod(method);
        urlConnection.setUseCaches(false);
        urlConnection.setConnectTimeout(CONNECTION_TIMEOUT);
        urlConnection.setReadTimeout(READ_TIMEOUT);
        urlConnection.setRequestProperty(CONTENT_TYPE_HEADER, CONTENT_TYPE);
        if (CoreApplication.getSessionId() != null) {
            urlConnection.setRequestProperty(SESSION_IDENTIFIER_HEADER, CoreApplication.getSessionId());
        }
        urlConnection.connect();
        return urlConnection;
    }

    protected String readStream(InputStream stream) throws IOException {

        StringBuilder sb = new StringBuilder();
        BufferedReader br = new BufferedReader(new InputStreamReader(stream, "utf-8"));
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }


}
