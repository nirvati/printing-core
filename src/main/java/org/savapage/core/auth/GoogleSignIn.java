/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.auth;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.apache.ApacheHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

/**
 * <a href= "https://developers.google.com/identity/sign-in/web/backend-auth">
 * Using a Google API Client Library</a>.
 *
 * @author Rijk Ravestein
 *
 */
public final class GoogleSignIn {

    /**
     * The issuer of the ID token.
     */
    private static final String ID_TOKEN_ISSUER = "accounts.google.com";

    /**
     * Constructor.
     */
    private GoogleSignIn() {

    }

    /**
     * Verify the integrity of the ID token and returns information about the
     * Google User.
     *
     * @param clientId
     * @param idToken
     *            The ID Token.
     * @return {@code null} when ID Token is invalid.
     * @throws GeneralSecurityException
     * @throws IOException
     */
    public static GoogleUserInfo getUserInfo(final String clientId,
            final String idToken) throws GeneralSecurityException, IOException {

        final HttpTransport transport = new ApacheHttpTransport();
        final JsonFactory jsonFactory = new JacksonFactory();

        final GoogleIdTokenVerifier verifier =
                new GoogleIdTokenVerifier.Builder(transport, jsonFactory)
                        .setAudience(Arrays.asList(clientId))
                        .setIssuer(ID_TOKEN_ISSUER).build();

        final GoogleIdToken idTokenObj = verifier.verify(idToken);
        final GoogleUserInfo userInfo;

        if (idTokenObj == null) {
            userInfo = null;
        } else {
            userInfo = new GoogleUserInfo(idTokenObj.getPayload());
        }

        return userInfo;
    }

}
