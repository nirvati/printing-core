/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.savapage.core.util.InetUtils;

/**
 * Wrapper of an SSLSocketFactory instance that accepts self-signed certificates
 * as trusted. Verification of all other certificates is done by the trust
 * manager configured in the SSL context.
 *
 * <p>
 * See: <i>How to accept self-signed certificates for JNDI/LDAP connections?</i>
 * on <a href=
 * "https://stackoverflow.com/questions/4615163/how-to-accept-self-signed-certificates-for-jndi-ldap-connections">
 * stackoverflow</a>, and <a href=
 * "https://docs.oracle.com/javase/7/docs/technotes/guides/jndi/jndi-ldap-gl.html">JNDI
 * Implementor Guidelines for LDAP Service Providers</a>
 * <p>
 *
 * @author Rijk Ravestein
 *
 */
public final class TrustSelfSignedCertSocketFactory extends SSLSocketFactory {

    /**
     * The wrapped instance.
     */
    private final SSLSocketFactory wrappedFactory;

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
        return new TrustSelfSignedCertSocketFactory();
    }

    /**
     *
     */
    public TrustSelfSignedCertSocketFactory() {
        final SSLContext ctx = InetUtils.createSslContextTrustSelfSigned();
        this.wrappedFactory = ctx.getSocketFactory();
    }

    @Override
    public Socket createSocket(final String host, final int port)
            throws IOException, UnknownHostException {
        return wrappedFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(final String host, final int port,
            final InetAddress localHost, final int localPort)
            throws IOException, UnknownHostException {
        return wrappedFactory.createSocket(host, port, localHost, localPort);
    }

    @Override
    public Socket createSocket(final InetAddress host, final int port)
            throws IOException {
        return wrappedFactory.createSocket(host, port);
    }

    @Override
    public Socket createSocket(final InetAddress address, final int port,
            final InetAddress localAddress, final int localPort)
            throws IOException {
        return wrappedFactory.createSocket(address, port, localAddress,
                localPort);
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return wrappedFactory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return wrappedFactory.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(final Socket s, final String host,
            final int port, final boolean autoClose) throws IOException {
        return wrappedFactory.createSocket(s, host, port, autoClose);
    }

}
