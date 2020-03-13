/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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
package org.savapage.ext.google;

import org.savapage.core.config.IConfigProp.LdapTypeEnum;
import org.savapage.core.users.LdapUserSource;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class GoogleLdapUserSource extends LdapUserSource {

    /** */
    public static final String LDAP_HOST = "ldap.google.com";

    /**
     * LDAPS (SSL/TLS enabled).
     */
    public static final String LDAP_PORT = "636";

    /** */
    public GoogleLdapUserSource() {
        super(LdapTypeEnum.GOOGLE_CLOUD);
    }

    @Override
    protected Class<?> getCustomSSLSocketFactoryClass() {
        return GoogleLdapSSLSocketFactory.class;
    }

    @Override
    protected boolean isExtraUserAttributes() {
        return false;
    }

    @Override
    protected String getLdapHost() {
        return LDAP_HOST;

    }

    @Override
    protected String getLdapPort() {
        return LDAP_PORT;
    }

    @Override
    protected boolean isLdapSSL() {
        return true;
    }

    @Override
    protected boolean isLdapUseTrustSelfSignedSSL() {
        return false;
    }

}
