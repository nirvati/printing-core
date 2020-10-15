/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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

import javax.net.SocketFactory;
import javax.net.ssl.SSLSocketFactory;

import org.savapage.core.net.IClientCertSSLSocketFactory;
import org.savapage.core.net.SSLSocketFactoryWrapper;

/**
 * Google LDAP SocketFactory.
 *
 * @author Rijk Ravestein
 *
 */
public final class GoogleLdapSSLSocketFactory extends SSLSocketFactoryWrapper
        implements IClientCertSSLSocketFactory {

    /**
     * Cached factory wrapper.
     */
    private static SocketFactory socketFactoryWrapper;

    /**
     * Returns the default SSL socket factory.
     * <p>
     * Note: this method is crucial to prevent an
     * {@link IllegalArgumentException} message "object is not an instance of
     * declaring class" when a socket is instantiated through reflection.
     * </p>
     *
     * @return An instance of this class.
     */
    public static SocketFactory getDefault() {
        if (socketFactoryWrapper == null) {
            socketFactoryWrapper = new GoogleLdapSSLSocketFactory(
                    GoogleLdapClient.getSSLSocketFactory());
        }
        return socketFactoryWrapper;
    }

    /**
     * @param factory
     *            The factory to wrap.
     */
    private GoogleLdapSSLSocketFactory(final SSLSocketFactory factory) {
        super(factory);
    }

}
