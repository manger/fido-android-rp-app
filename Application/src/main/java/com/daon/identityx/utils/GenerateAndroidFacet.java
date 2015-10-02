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

package com.daon.identityx.utils;

import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.Certificate;



/***
 * Simple utility class that will generate the Android FacetID for the developer machine, if running Android Studio
 * Copy the result from sysout and:
 * 		1. Update the facets of the application in the Admin console of IdentityX 
 * 		2. Updates the facets document (if the facets are not retrieved from IdentityX)
 * 	   
 * @author Daon
 *
 */
public class GenerateAndroidFacet {

	private static final String DEFAULT_ANDROID_KEYSTORE_LOCATION = System.getProperty("user.home") + "/.android/debug.keystore";
	private static final String DEFAULT_ANDROID_KEYSTORE_PASSWORD = "android";
	private static final String DEFAULT_ANDROID_KEYSTORE_CERT_NAME = "androiddebugkey";
	private static final String DEFAULT_HASHING_ALGORITHM = "SHA1";
	
	
	public static void main(String[] args) {
		
		String androidKeystoreLocation = System.getProperty("ANDROID_KEYSTORE_LOCATION", DEFAULT_ANDROID_KEYSTORE_LOCATION);
		String androidKeystorePassword = System.getProperty("ANDROID_KEYSTORE_PASSWORD", DEFAULT_ANDROID_KEYSTORE_PASSWORD);
		String androidKeystoreCert = System.getProperty("ANDROID_KEYSTORE_CERT_NAME", DEFAULT_ANDROID_KEYSTORE_CERT_NAME);
		String hashingAlgorithm = System.getProperty("HASHING_ALGORITHM", DEFAULT_HASHING_ALGORITHM);
		

		try {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			File filePath = new File(androidKeystoreLocation);
			if (!filePath.exists()) {
				System.err.println("The filepath to the debug keystore could not be located at: " + androidKeystoreCert);
				System.exit(1);
			} else {
				System.out.println("Found the Android Studio keystore at: " + androidKeystoreLocation);
			}
			
			keyStore.load(new FileInputStream(filePath), androidKeystorePassword.toCharArray());
			System.out.println("Keystore loaded - password and location were OK");
			
			Certificate cert = keyStore.getCertificate(androidKeystoreCert);
			if (cert == null) {
				System.err.println("Could not location the certification in the store with the name: " + androidKeystoreCert);
				System.exit(1);
			} else {
				System.out.println("Certificate found in the store with name: " + androidKeystoreCert);
			}
			
			byte[] certBytes = cert.getEncoded();

			MessageDigest digest = MessageDigest.getInstance(hashingAlgorithm);
			System.out.println("Hashing algorithm: " + hashingAlgorithm + " found.");
			byte[] hashedCert = digest.digest(certBytes);
            String base64HashedCert = Base64.encodeToString(hashedCert, false);
			System.out.println("Base64 encoded SHA-1 hash of the certificate: " + base64HashedCert);
			String base64HashedCertRemoveTrailing = base64HashedCert.replaceAll("=", "");
			System.out.println("Add the following facet to the Facets file in order for the debug app to be trusted by the FIDO client");
			System.out.println("\"android:apk-key-hash:" + base64HashedCertRemoveTrailing+ "\"");
			
			
		} catch (Throwable ex) {
			ex.printStackTrace();
		}

	}

}
