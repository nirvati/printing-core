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

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class GoogleUserInfo {

    private final String userId;
    private final String email;
    private final boolean emailVerified;
    private final String hostedDomain;

    private final String name;
    private final String pictureUrl;
    private final String locale;
    private final String familyName;
    private final String givenName;

    /**
     *
     * @param payload
     */
    public GoogleUserInfo(final Payload payload) {

        this.userId = payload.getSubject();
        this.email = payload.getEmail();
        this.emailVerified = Boolean.valueOf(payload.getEmailVerified());
        this.hostedDomain = payload.getHostedDomain();

        this.name = (String) payload.get("name");
        this.pictureUrl = (String) payload.get("picture");
        this.locale = (String) payload.get("locale");
        this.familyName = (String) payload.get("family_name");
        this.givenName = (String) payload.get("given_name");
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public String getHostedDomain() {
        return hostedDomain;
    }

    public String getName() {
        return name;
    }

    public String getPictureUrl() {
        return pictureUrl;
    }

    public String getLocale() {
        return locale;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getGivenName() {
        return givenName;
    }

}
