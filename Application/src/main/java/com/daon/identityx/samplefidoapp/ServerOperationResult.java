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

import com.daon.identityx.controller.model.Error;

/**
 * Simple class to hold the response from the server or an error.
 *
 */
public class ServerOperationResult<T> {

    private Error error;
    private T response;

    public ServerOperationResult(T response) {
        this.setResponse(response);
    }

    public ServerOperationResult(Error error) {
        this.setError(error);
    }

    public boolean isSuccessful() {
        return this.getError() == null;
    }

    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    public T getResponse() {
        return response;
    }

    public void setResponse(T response) {
        this.response = response;
    }
}
