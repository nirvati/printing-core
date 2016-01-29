/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.ext.smartschool;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.savapage.core.SpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class SmartschoolConnection {

    /**
     * .
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SmartschoolConnection.class);

    /**
     *
     */
    private final SOAPConnection connection;

    /**
     * Used to (streaming) download document.
     * <p>
     * Note: The Apache HttpClient is thread safe.
     * </p>
     */
    private final CloseableHttpClient httpClient;

    /**
     * The SmartSchool account name as part of the {@link #endpointUrl}.
     */
    private final String accountName;

    /**
     * The SmartSchool end-point {@link URL}.
     */
    private final URL endpointUrl;

    /**
     * The SmartSchool end-point {@link URI}.
     */
    private final URI endpointUri;

    /**
     * The SmartSchool end-point IP address.
     */
    private final String endpointIpAddress;

    /**
     * The SmartSchool Proxy end-point {@link URL}.
     */
    private final URL endpointUrlProxy;

    /**
     * The SmartSchool Proxy end-point {@link URI}.
     */
    private final URI endpointUriProxy;

    /**
     * The SmartSchool Proxy end-point IP address.
     */
    private final String endpointIpAddressProxy;

    /**
     * .
     */
    private volatile boolean shutdownRequested = false;

    /**
     * The {@link SmartschoolAccount}.
     */
    private final SmartschoolAccount account;

    public final static String PROXY_URL_PARM_ACCOUNT = "account";
    public final static String PROXY_URL_PARM_NODE = "node";

    /**
     *
     * @param acc
     *            The {@link SmartschoolAccount}.
     * @throws SOAPException
     */
    public SmartschoolConnection(final SmartschoolAccount acc)
            throws SOAPException {

        this.account = acc;

        try {
            this.endpointUri = new URI(this.account.getEndpoint());
            this.endpointUrl = new URL(this.account.getEndpoint());

            this.endpointIpAddress =
                    InetAddress.getByName(this.endpointUrl.getHost())
                            .getHostAddress();

            this.accountName = extractAccountName(this.endpointUrl);

            if (this.accountName == null) {
                throw new SpException("No account found in endpoint "
                        + this.account.getEndpoint());
            }

            //
            final SmartschoolAccount.Node node = this.account.getNode();

            if (node == null || StringUtils.isBlank(node.getProxyEndpoint())) {

                this.endpointUriProxy = null;
                this.endpointUrlProxy = null;
                this.endpointIpAddressProxy = null;

            } else {

                final StringBuilder proxyEndpoint = new StringBuilder();

                proxyEndpoint.append(node.getProxyEndpoint()).append("?")
                        .append(PROXY_URL_PARM_ACCOUNT).append("=")
                        .append(this.accountName).append("&")
                        .append(PROXY_URL_PARM_NODE).append("=")
                        .append(StringUtils.defaultString(this.getNodeId()));

                this.endpointUriProxy = new URI(proxyEndpoint.toString());
                this.endpointUrlProxy = new URL(proxyEndpoint.toString());
                this.endpointIpAddressProxy =
                        InetAddress.getByName(this.endpointUrlProxy.getHost())
                                .getHostAddress();
            }

        } catch (URISyntaxException | MalformedURLException
                | UnknownHostException e) {
            throw new SOAPException(String.format("%s: %s", e.getClass()
                    .getSimpleName(), e.getMessage()), e);
        }

        this.connection =
                SOAPConnectionFactory.newInstance().createConnection();

        //
        final HttpClientBuilder builder = HttpClientBuilder.create();
        this.httpClient = builder.build();
    }

    /**
     * Extracts the account name from the native Smartschool SOAP endpoint.
     *
     * @param endpoint
     *            The Smartschool SOAP endpoint {@link URL}.
     * @return The Smartschool account name, or {@code null} when not found.
     */
    public static String extractAccountName(final URL endpoint) {
        return StringUtils.substringBefore(endpoint.getHost(), ".");
    }

    /**
     *
     * @return
     */
    public SOAPConnection getConnection() {
        return connection;
    }

    /**
     *
     * @return The HTTP client for streaming download of PDF documents.
     */
    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Checks if proxy is to be used for a Smartschool request.
     *
     * @param request
     *            The {@link SmartschoolRequestEnum}.
     * @return {@code true} when proxy is to be used.
     */
    private boolean useProxy(final SmartschoolRequestEnum request) {
        return request != SmartschoolRequestEnum.SET_DOCUMENTSTATUS
                && this.endpointUrlProxy != null && !this.isProxy();
    }

    /**
     * @param request
     *            The {@link SmartschoolRequestEnum}.
     * @return The (proxy) SOAP end-point as {@link URL}.
     */
    public URL getEndpointUrl(final SmartschoolRequestEnum request) {
        if (useProxy(request)) {
            return this.endpointUrlProxy;
        }
        return this.endpointUrl;
    }

    /**
     * @param request
     *            The {@link SmartschoolRequestEnum}.
     * @return The (proxy) SOAP end-point as {@link URI}.
     */
    public URI getEndpointUri(final SmartschoolRequestEnum request) {
        if (useProxy(request)) {
            return this.endpointUriProxy;
        }
        return this.endpointUri;
    }

    /**
     * @param request
     *            The {@link SmartschoolRequestEnum}.
     * @return The (proxy) SOAP end-point IP address.
     */
    public String getEndpointIpAddress(final SmartschoolRequestEnum request) {
        if (useProxy(request)) {
            return this.endpointIpAddressProxy;
        }
        return this.endpointIpAddress;
    }

    /**
     *
     * @return The SmartSchool account name as part of the SOAP endpoint.
     */
    public String getAccountName() {
        return accountName;
    }

    public char[] getPassword() {
        return this.account.getPassword();
    }

    public boolean isShutdownRequested() {
        return shutdownRequested;
    }

    public void setShutdownRequested(boolean shutdownRequested) {
        this.shutdownRequested = shutdownRequested;
    }

    /**
     * Closes end-point connection(s).
     */
    public void close() {

        try {
            this.connection.close();
        } catch (SOAPException e) {
            LOGGER.warn("Error closing SOAP connection: " + e.getMessage());
        }

        IOUtils.closeQuietly(this.httpClient);
    }

    /**
     * @return The {@link SmartschoolAccount.Config}.
     */
    public SmartschoolAccount.Config getAccountConfig() {
        return this.account.getConfig();
    }

    /**
     * @return The Cluster Node ID, or {@code null} when not the connection is
     *         not part of a Cluster.
     */
    public String getNodeId() {
        if (this.account.getNode() == null) {
            return null;
        }
        return this.account.getNode().getId();
    }

    /**
     *
     * @return {@code true} if this connection is part of a Cluster.
     */
    public boolean isPartOfCluster() {
        return StringUtils.isNotBlank(this.getNodeId());
    }

    /**
     * @return {@code true} when this connection acts as Smartschool Cluster
     *         Proxy.
     */
    public boolean isProxy() {
        return this.account.getNode() != null
                && this.account.getNode().isProxy();
    }

}
