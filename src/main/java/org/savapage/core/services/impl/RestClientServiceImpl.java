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
package org.savapage.core.services.impl;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.services.RestClientService;
import org.savapage.core.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class RestClientServiceImpl extends AbstractService
        implements RestClientService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(RestClientServiceImpl.class);

    /** */
    private static final String ALIAS_NAME = "RESTful Client Service";

    /** */
    private PoolingHttpClientConnectionManager connectionManager = null;

    /** */
    private SSLContext sslContextAllTrusted;

    @Override
    public void start() {

        LOGGER.debug("{} is starting...", ALIAS_NAME);

        final ConfigManager cm = ConfigManager.instance();

        this.sslContextAllTrusted = createSslContextAllTrusted();

        final int maxConnections =
                cm.getConfigInt(IConfigProp.Key.RESTFUL_CLIENT_MAX_CONNECTIONS);

        final int maxConnectionsPerRoute = cm.getConfigInt(
                IConfigProp.Key.RESTFUL_CLIENT_MAX_CONNECTIONS_PER_ROUTE);

        final boolean trustSelfSignedSSL =
                cm.isConfigValue(Key.RESTFUL_CLIENT_SSL_TRUST_SELF_SIGNED);

        if (trustSelfSignedSSL) {
            /*
             * Since we use a pooling manager, the ClientBuilder#sslContext and
             * ClientBuilder#hostnameVerifier setters are silently ignored.
             * Therefore we set trust at pooling manager level.
             */
            this.connectionManager = new PoolingHttpClientConnectionManager(
                    createAllTrustedRegistry());
        } else {
            this.connectionManager = new PoolingHttpClientConnectionManager();
        }

        /*
         * The default limit of 2 concurrent connections per given route and no
         * more 20 connections in total may prove too constraining for many
         * real-world applications these limits, especially if they use HTTP as
         * a transport protocol for their services.
         */
        this.connectionManager.setMaxTotal(maxConnections);
        this.connectionManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

        /*
         * You can also be much more fine grained about how many connections per
         * host are allowed with the setMaxPerRoute method. For example, let's
         * say it is OK to have 40 max connections when hitting localhost:
         */
        // TODO
        // this.connectionManager
        // .setMaxPerRoute(new HttpRoute(new HttpHost("localhost")), 40);

        LOGGER.debug("{} started.", ALIAS_NAME);
    }

    @Override
    public Client createClient() {
        return this.createClientBuilder().build();
    }

    @Override
    public Client createClientAuth(final String username,
            final String password) {

        final ClientBuilder clientBuilder = this.createClientBuilder();

        clientBuilder.register(HttpAuthenticationFeature.basicBuilder()
                .credentials(username, password).build());

        return clientBuilder.build();
    }

    /**
     * @return The {@link ClientBuilder}.
     */
    private ClientBuilder createClientBuilder() {

        final ConfigManager cm = ConfigManager.instance();

        final long connectTimeout = cm.getConfigLong(
                IConfigProp.Key.RESTFUL_CLIENT_CONNECT_TIMEOUT_MSEC);

        final long readTimeout = cm.getConfigLong(
                IConfigProp.Key.RESTFUL_CLIENT_READ_TIMEOUT_MSEC);

        final ClientConfig clientConfig = new ClientConfig();

        clientConfig.property(ApacheClientProperties.CONNECTION_MANAGER,
                this.connectionManager);

        clientConfig.connectorProvider(new ApacheConnectorProvider());

        final ClientBuilder builder = ClientBuilder.newBuilder();

        builder.withConfig(clientConfig)
                .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeout, TimeUnit.MILLISECONDS);

        /*
         * Since we use a pooling manager, the ClientBuilder#sslContext and
         * ClientBuilder#hostnameVerifier setters are silently ignored.
         * Therefore we have set trust at pooling manager level.
         */
        // builder.sslContext(this.sslContextAllTrusted)
        // .hostnameVerifier((s1, s2) -> true);

        return builder;
    }

    /**
     * Create ConnectionSocketFactory registry for trusting self-signed SSL
     * certs and accepting hostname cert name mismatch.
     * <p>
     * <b>Note</b>: Since we use a pooling manager, the
     * {@link ClientBuilder#sslContext(SSLContext)} and
     * {@link ClientBuilder#hostnameVerifier(HostnameVerifier)} setters are
     * silently ignored.
     * </p>
     *
     * @return The registry.
     */
    private Registry<ConnectionSocketFactory> createAllTrustedRegistry() {
        /*
         */
        final Registry<ConnectionSocketFactory> socketFactoryRegistry =
                RegistryBuilder.<ConnectionSocketFactory> create()
                        .register("http",
                                PlainConnectionSocketFactory.getSocketFactory())
                        .register("https",
                                new SSLConnectionSocketFactory(
                                        this.sslContextAllTrusted,
                                        new HostnameVerifier() {
                                            @Override
                                            public boolean verify(
                                                    final String hostname,
                                                    final SSLSession session) {
                                                return true;
                                            }
                                        }))
                        .build();

        return socketFactoryRegistry;
    }

    /**
     *
     * @return {@link SSLContext}.
     */
    private static SSLContext createSslContextAllTrusted() {

        try {
            final SSLContext sslCtx = SSLContext.getInstance("SSL");

            final TrustManager[] trustAllCerts =
                    new X509TrustManager[] { new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[] {};
                        }

                        @Override
                        public void checkClientTrusted(
                                final X509Certificate[] certs,
                                final String authType) {
                        }

                        @Override
                        public void checkServerTrusted(
                                final X509Certificate[] certs,
                                final String authType) {
                        }
                    } };

            sslCtx.init(null, trustAllCerts, null);

            return sslCtx;

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new SpException(e.getMessage());
        }
    }

    @Override
    public void test() {

        final ConfigManager cm = ConfigManager.instance();

        final Client client = this.createClientAuth(
                cm.getConfigValue(IConfigProp.Key.API_RESTFUL_AUTH_USERNAME),
                cm.getConfigValue(IConfigProp.Key.API_RESTFUL_AUTH_PASSWORD));

        final WebTarget webTarget = client.target("https://localhost:"
                + ConfigManager.getServerSslPort() + "/restful/v1")
                .path("system/version");

        final Invocation.Builder invocationBuilder =
                webTarget.request(MediaType.TEXT_PLAIN);

        try (Response response = invocationBuilder.get();) {
            final String version = response.readEntity(String.class);
            SpInfo.instance()
                    .log(String.format("%s test: GET %s -> %s [%s] [%s]",
                            ALIAS_NAME, webTarget.getUri().toString(),
                            response.getStatus(), response.getStatusInfo(),
                            version));

        } catch (ProcessingException e) {
            LOGGER.error(e.getMessage());
        } finally {
            client.close();
        }
    }

    @Override
    public void shutdown() {
        LOGGER.debug("{} is shutting down...", ALIAS_NAME);
        IOHelper.closeQuietly(this.connectionManager);
        LOGGER.debug("{} shut down.", ALIAS_NAME);
    }

}
