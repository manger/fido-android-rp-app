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

package com.daon.identityx.controller.model;

import java.util.Date;

public class AuthenticatorInfo {
    private String id;
    private Date created;
    private Date lastUsed;
    private String name;
    private String description;
    private String vendorName;
    private String icon;
    private String status;
    private String fidoDeregistrationRequest;
    private String aaid;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public Date getCreated() {
        return created;
    }
    public void setCreated(Date created) {
        this.created = created;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getVendorName() {
        return vendorName;
    }
    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
    }
    public String getIcon() {
        return icon;
    }
    public void setIcon(String icon) {
        this.icon = icon;
    }
    public Date getLastUsed() {
        return lastUsed;
    }
    public void setLastUsed(Date lastUsed) {
        this.lastUsed = lastUsed;
    }
    public String getFidoDeregistrationRequest() {
        return fidoDeregistrationRequest;
    }
    public void setFidoDeregistrationRequest(String fidoDeregistrationRequest) {
        this.fidoDeregistrationRequest = fidoDeregistrationRequest;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getAaid() {
        return aaid;
    }
    public void setAaid(String aaid) {
        this.aaid = aaid;
    }
}
