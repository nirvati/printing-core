/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.services.helpers;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;

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
public class SmartSchoolConnection {

    /**
     * .
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(SmartSchoolConnection.class);

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
    *
    */
    private final URL endpointUrl;

    /**
     *
     */
    private final URI endpointUri;

    /**
     * The SmartSchool account name as part of the {@link #endpointUrl}.
     */
    private final String accountName;

    /**
     *
     */
    private final String endpointIpAddress;

    /**
     * .
     */
    private final char[] password;

    /**
     * .
     *
     */
    private volatile boolean shutdownRequested = false;

    private final String proxyPrinterName;

    /**
     *
     * @param endpoint
     * @param password
     * @param proxyPrinterName
     *            Name of the proxy printer, can be {@code null} or empty.
     * @throws SOAPException
     */
    public SmartSchoolConnection(final String endpoint, final char[] password,
            final String proxyPrinterName) throws SOAPException {

        try {
            this.endpointUri = new URI(endpoint);
            this.endpointUrl = new URL(endpoint);

            this.endpointIpAddress =
                    InetAddress.getByName(this.endpointUrl.getHost())
                            .getHostAddress();

        } catch (URISyntaxException | MalformedURLException
                | UnknownHostException e) {
            throw new SOAPException(String.format("%s: %s", e.getClass()
                    .getSimpleName(), e.getMessage()), e);
        }

        this.accountName =
                StringUtils.substringBefore(this.endpointUrl.getHost(), ".");

        if (this.accountName == null) {
            throw new SpException("No account found in endpoint " + endpoint);
        }

        this.password = password;
        this.connection =
                SOAPConnectionFactory.newInstance().createConnection();

        final HttpClientBuilder builder = HttpClientBuilder.create();
        this.httpClient = builder.build();

        this.proxyPrinterName = proxyPrinterName;
    }

    public SOAPConnection getConnection() {
        return connection;
    }

    public URL getEndpointUrl() {
        return endpointUrl;
    }

    public URI getEndpointUri() {
        return endpointUri;
    }

    /**
     *
     * @return The SmartSchool account name as part of the SOAP endpoint.
     */
    public String getAccountName() {
        return accountName;
    }

    public String getEndpointIpAddress() {
        return endpointIpAddress;
    }

    public char[] getPassword() {
        return password;
    }

    public boolean isShutdownRequested() {
        return shutdownRequested;
    }

    public void setShutdownRequested(boolean shutdownRequested) {
        this.shutdownRequested = shutdownRequested;
    }

    public void close() {

        try {
            this.connection.close();
        } catch (SOAPException e) {
            LOGGER.warn("Error closing SOAP connection: " + e.getMessage());
        }

        try {
            this.httpClient.close();
        } catch (IOException e) {
            LOGGER.warn("Error closing HTTP connection: " + e.getMessage());
        }
    }

    public CloseableHttpClient getHttpClient() {
        return httpClient;
    }

    public String getProxyPrinterName() {
        return proxyPrinterName;
    }
}
