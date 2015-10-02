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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.daon.identityx.controller.model.AuthenticatorInfo;

import java.text.DateFormat;

/**
 * A layout item used in the creation of the list of authenticators
 */
public class AuthenticatorInfosAdapter extends ArrayAdapter<AuthenticatorInfo> {
    public AuthenticatorInfosAdapter(Context context, AuthenticatorInfo[] authenticatorInfos) {
        super(context, 0, authenticatorInfos);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Get the data item for this position
        AuthenticatorInfo authenticatorInfo = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_authenticator_info, parent, false);
        }

        // Set the actual authenticator associated with the list item
        convertView.setTag(authenticatorInfo);

        // Lookup views for data population
        ImageView image = (ImageView) convertView.findViewById(R.id.auth_icon);
        TextView name = (TextView) convertView.findViewById(R.id.auth_name);
        TextView lastUsed = (TextView) convertView.findViewById(R.id.auth_last_used);
        TextView created = (TextView) convertView.findViewById(R.id.auth_created);
        TextView status = (TextView) convertView.findViewById(R.id.auth_status);

        // Populate the data into the template view using the data object
        name.setText(authenticatorInfo.getName());
        created.setText(DateFormat.getDateTimeInstance().format(authenticatorInfo.getCreated()));
        if(authenticatorInfo.getLastUsed()==null) {
            lastUsed.setText(R.string.never_used);
        } else {
            lastUsed.setText(DateFormat.getDateTimeInstance().format(authenticatorInfo.getLastUsed()));
        }
        status.setText(authenticatorInfo.getStatus());

        // Create the icon to be displayed
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inMutable = true;
        byte[] imgBytes = Base64.decode(authenticatorInfo.getIcon(), Base64.DEFAULT);
        Bitmap bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length, options);
        image.setImageBitmap(bmp);

        // Return the completed view to render on screen
        return convertView;
    }
}
