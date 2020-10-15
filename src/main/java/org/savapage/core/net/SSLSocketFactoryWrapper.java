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
package org.savapage.core.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.ssl.SSLSocketFactory;

/**
 * Wrapper of an SSLSocketFactory instance.
 *
 *
 * @author Rijk Ravestein
 *
 */
public abstract class SSLSocketFactoryWrapper extends SSLSocketFactory {

    /**
     * The wrapped instance.
     */
    private final SSLSocketFactory wrappedFactory;

    /**
     * @param factory
     *            The factory to wrap.
     */
    public SSLSocketFactoryWrapper(final SSLSocketFactory factory) {
        this.wrappedFactory = factory;
    }

    @Override
    public final Socket createSocket(final String host, final int port)
            throws IOException, UnknownHostException {
        return wrappedFactory.createSocket(host, port);
    }

    @Override
    public final Socket createSocket(final String host, final int port,
            final InetAddress localHost, final int localPort)
            throws IOException, UnknownHostException {
        return wrappedFactory.createSocket(host, port, localHost, localPort);
    }

    @Override
    public final Socket createSocket(final InetAddress host, final int port)
            throws IOException {
        return wrappedFactory.createSocket(host, port);
    }

    @Override
    public final Socket createSocket(final InetAddress address, final int port,
            final InetAddress localAddress, final int localPort)
            throws IOException {
        return wrappedFactory.createSocket(address, port, localAddress,
                localPort);
    }

    @Override
    public final String[] getDefaultCipherSuites() {
        return wrappedFactory.getDefaultCipherSuites();
    }

    @Override
    public final String[] getSupportedCipherSuites() {
        return wrappedFactory.getSupportedCipherSuites();
    }

    @Override
    public final Socket createSocket(final Socket s, final String host,
            final int port, final boolean autoClose) throws IOException {
        return wrappedFactory.createSocket(s, host, port, autoClose);
    }

}
