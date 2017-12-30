package org.savapage.core.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContextBuilder;

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
    private SSLSocketFactory wrappedFactory;

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

        final SSLContextBuilder builder = new SSLContextBuilder();

        try {
            builder.loadTrustMaterial(null, TrustSelfSignedStrategy.INSTANCE);

            final SSLContext ctx = builder.build();

            this.wrappedFactory = ctx.getSocketFactory();

        } catch (NoSuchAlgorithmException | KeyStoreException
                | KeyManagementException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
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
