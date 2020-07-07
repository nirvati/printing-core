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
package org.savapage.core.ipp.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.savapage.core.SpException;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.circuitbreaker.CircuitBreakerOperation;
import org.savapage.core.circuitbreaker.CircuitTrippingException;
import org.savapage.core.config.CircuitBreakerEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.ipp.IppResponseHeader;
import org.savapage.core.ipp.IppSyntaxException;
import org.savapage.core.ipp.IppVersionEnum;
import org.savapage.core.ipp.attribute.IppAttrGroup;
import org.savapage.core.ipp.encoding.IppContentParser;
import org.savapage.core.ipp.encoding.IppDelimiterTag;
import org.savapage.core.ipp.encoding.IppEncoder;
import org.savapage.core.ipp.operation.IppMessageMixin;
import org.savapage.core.ipp.operation.IppOperationId;
import org.savapage.core.ipp.operation.IppStatusCode;
import org.savapage.core.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The IPP client responsible for communication with the IPP server (CUPS).
 *
 * @author Rijk Ravestein
 *
 */
public final class IppClient {

    /**
     * .
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppClient.class);

    /** */
    private volatile boolean shutdownRequested;

    /**
     *
     */
    private class IppResponseParser extends IppContentParser {

        private final List<IppAttrGroup> groups = new ArrayList<>();
        private IppResponseHeader responseHeader;
        private Exception exception = null;
        private boolean contentEnd = false;

        @Override
        protected void onContentEnd() {
            contentEnd = true;
        }

        @Override
        protected void onHeader(IppResponseHeader responseHeader)
                throws Exception {
            this.responseHeader = responseHeader;
        }

        @Override
        protected void onGroup(IppAttrGroup group) throws Exception {
            groups.add(group);
        }

        @Override
        protected void onException(Exception e) {
            exception = e;
        }

        public IppResponseHeader getResponseHeader() {
            return responseHeader;
        }

        public boolean hasResponseHeader() {
            return (responseHeader != null);
        }

        public List<IppAttrGroup> getGroups() {
            return groups;
        }

        public boolean isContentEnd() {
            return contentEnd;
        }

        public Exception getException() {
            return exception;
        }

        public boolean hasException() {
            return (exception != null);
        }
    }

    /** */
    private static final IppVersionEnum IPP_VERSION = IppVersionEnum.V_1_1;

    /** */
    private int requestIdWlk = 0;

    /** */
    private static final String TRACE_SEP = "+---------------------------"
            + "-------------------------------------------+";

    /** */
    private PoolingHttpClientConnectionManager connManager = null;

    /**
     * The Apache HttpClient is thread safe.
     */
    private CloseableHttpClient httpclientApache = null;

    /**
     * Prevent public instantiation.
     */
    private IppClient() {
    }

    /**
     * @return {@code true} if shutdown is requested.
     */
    public boolean isShutdownRequested() {
        return shutdownRequested;
    }

    /**
     * @param shutdown
     *            {@code true} to request shutdown.
     */
    public void setShutdownRequested(final boolean shutdown) {
        this.shutdownRequested = shutdown;
    }

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link IppClient#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
        /** */
        public static final IppClient INSTANCE = new IppClient();
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static IppClient instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Initializes the IPP client.
     * <p>
     * See these <a href=
     * "http://hc.apache.org/httpcomponents-client-4.3.x/tutorial/html/connmgmt.html#d5e380"
     * >remarks</a> about {@link PoolingHttpClientConnectionManager}.
     * </p>
     */
    public void init() {

        /*
         * Clumsy attempt to be unique over server restarts :-)
         *
         * TODO: make a better solution (persistent requestId).
         */
        this.requestIdWlk = Integer.valueOf(
                new SimpleDateFormat("HHmmss'000'").format(new Date()), 10);

        this.connManager = new PoolingHttpClientConnectionManager();

        /*
         * Max per local and remote connection.
         */
        final ConfigManager cm = ConfigManager.instance();

        final int maxConnections =
                cm.getConfigInt(Key.CUPS_IPP_MAX_CONNECTIONS);

        this.connManager.setDefaultMaxPerRoute(maxConnections);

        /*
         * Total max for a local and remote connection.
         */
        this.connManager.setMaxTotal(2 * maxConnections);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                .setConnectionManager(this.connManager);

        /*
         * While HttpClient instances are thread safe and can be shared between
         * multiple threads of execution, it is highly recommended that each
         * thread maintains its own dedicated instance of HttpContext.
         */
        this.httpclientApache = builder.build();
    }

    /**
     * @param isLocalIppServer
     *            {@code true} when <i>local</i> IPP Server, {@code false} when
     *            urlServer is <i>remote</i>.
     *
     * @return The {@link RequestConfig}.
     */
    private static RequestConfig
            buildRequestConfig(final boolean isLocalIppServer) {

        final ConfigManager cm = ConfigManager.instance();

        final Key connectTimeout;
        final Key socketTimeout;

        if (isLocalIppServer) {
            connectTimeout = Key.CUPS_IPP_LOCAL_CONNECT_TIMEOUT_MSEC;
            socketTimeout = Key.CUPS_IPP_LOCAL_SOCKET_TIMEOUT_MSEC;
        } else {
            connectTimeout = Key.CUPS_IPP_REMOTE_CONNECT_TIMEOUT_MSEC;
            socketTimeout = Key.CUPS_IPP_REMOTE_SOCKET_TIMEOUT_MSEC;
        }

        return RequestConfig.custom()
                .setConnectTimeout(cm.getConfigInt(connectTimeout))
                .setSocketTimeout(cm.getConfigInt(socketTimeout))
                .setConnectionRequestTimeout(cm.getConfigInt(socketTimeout))
                .build();
    }

    /**
     *
     */
    public void shutdown() {
        IOHelper.closeQuietly(this.httpclientApache);
        IOHelper.closeQuietly(this.connManager);
    }

    /**
     * Sends a IPP request using {@link org.apache.http.client.HttpClient}.
     * <p>
     * Findings:
     * <ul>
     * <li>When using a "UTF-8" character set when constructing
     * {@link ContentType}, CUPS responds with an authorization error HTML page.
     * So, just use the {@link IppMessageMixin#CONTENT_TYPE_IPP}.</li>
     * </ul>
     * </p>
     *
     * @param urlServer
     *            The URL of the server.
     * @param isLocalUrlServer
     *            {@code true} when urlServer is <i>local</i> CUPS,
     *            {@code false} when urlServer is <i>remote</i> CUPS.
     * @param operationId
     *            The {@link IppOperationId}.
     * @param request
     *            The IPP request.
     * @param file
     *            The {@link File} to send (can be {@code null}.
     * @param response
     *            The IPP response.
     * @return The {@link IppStatusCode}.
     * @throws InterruptedException
     *             When interrupted.
     * @throws CircuitBreakerException
     *             When IPP connection circuit breaks.
     */
    private IppStatusCode send(final URL urlServer,
            final boolean isLocalUrlServer, final IppOperationId operationId,
            final List<IppAttrGroup> request, final File file,
            final List<IppAttrGroup> response)
            throws InterruptedException, CircuitBreakerException {

        ByteArrayOutputStream ostr = null;
        InputStreamList istr = null;
        URI uriIppServer = null;

        try {
            uriIppServer = urlServer.toURI();
            /*
             * Prepare the input stream.
             */
            ostr = new ByteArrayOutputStream(1024);
            write(ostr, operationId, request);

            final List<InputStream> istrList = new ArrayList<>();

            istrList.add(new ByteArrayInputStream(ostr.toByteArray()));

            if (file != null) {
                istrList.add(new FileInputStream(file));
            }

            istr = new InputStreamList(istrList);

        } catch (IOException | URISyntaxException e) {
            throw new SpException(e);
        }

        /*
         *
         */
        final ContentType contentType =
                ContentType.create(IppMessageMixin.CONTENT_TYPE_IPP);

        long length = ostr.size();

        if (file != null) {
            length += file.length();
        }

        final HttpEntity entity =
                new InputStreamEntity(istr, length, contentType);

        /*
         *
         */
        final HttpPost httppost = new HttpPost(uriIppServer);

        httppost.setConfig(buildRequestConfig(isLocalUrlServer));

        /*
         * Our own signature :-)
         */
        httppost.setHeader(HttpHeaders.USER_AGENT,
                ConfigManager.getAppNameVersion());

        httppost.setEntity(entity);

        /*
         * Custom handler.
         */
        final ResponseHandler<byte[]> handler = new ResponseHandler<byte[]>() {

            @Override
            public byte[] handleResponse(final HttpResponse response)
                    throws ClientProtocolException, IOException {

                final HttpEntity entity = response.getEntity();

                if (entity != null) {
                    return EntityUtils.toByteArray(entity);
                } else {
                    return null;
                }
            }
        };

        final CircuitBreaker circuitBreaker;

        if (isLocalUrlServer) {
            circuitBreaker = ConfigManager.getCircuitBreaker(
                    CircuitBreakerEnum.CUPS_LOCAL_IPP_CONNECTION);
        } else {
            circuitBreaker = ConfigManager.getCircuitBreaker(
                    CircuitBreakerEnum.CUPS_REMOTE_IPP_CONNECTIONS);
        }

        //
        return execute(circuitBreaker, httppost, response, handler);
    }

    /**
     * Executes the {@link HttpPost} request and processes the IPP response
     * using the {@link ResponseHandler}.
     * <p>
     * NOTE: The {@link CircuitBreakerEnum#CUPS_LOCAL_IPP_CONNECTION} is
     * executed <i>after</i> the
     * {@link CloseableHttpClient#execute(org.apache.http.client.methods.HttpUriRequest, ResponseHandler)}
     * , since we do NOT want to wait for the internal
     * {@link Semaphore#acquire()} of the {@link CircuitBreaker} while sending
     * the IPP request.
     * </p>
     * <p>
     * So, we {@link CircuitBreaker#execute(CircuitBreakerOperation)} with the
     * deferred {@link Exception}.
     * </p>
     *
     * @param circuitBreaker
     *            The {@link CircuitBreaker}.
     * @param httppost
     *            The the {@link HttpPost} request.
     * @param response
     *            The IPP response.
     * @param handler
     *            The {@link ResponseHandler}.
     * @return The {@link IppStatusCode}.
     * @throws CircuitBreakerException
     * @throws InterruptedException
     */
    private IppStatusCode execute(final CircuitBreaker circuitBreaker,
            final HttpPost httppost, final List<IppAttrGroup> response,
            final ResponseHandler<byte[]> handler)
            throws InterruptedException, CircuitBreakerException {

        Exception deferredException = null;
        IppStatusCode statusCode = null;

        try {

            final byte[] responseBytes =
                    httpclientApache.execute(httppost, handler);

            final IppResponseParser ippParser = new IppResponseParser();

            ippParser.read(responseBytes);

            if (ippParser.hasException()) {

                throw ippParser.getException();

            } else {

                statusCode = ippParser.getResponseHeader().getStatusCode();

                for (IppAttrGroup group : ippParser.getGroups()) {
                    response.add(group);
                }
            }

        } catch (Exception e) {
            /*
             * During a system shutdown local CUPS might already be shutdown, so
             * an exception is to be expected. So, don't feed an exception to
             * the CircuitBreaker, cause we don't want obsolete alerts and
             * logging.
             *
             * See Mantis #374.
             */
            if (isShutdownRequested()) {
                throw new SpException(
                        "CUPS connection error while shutting down ", e);
            } else {
                deferredException = e;
            }
        } finally {
            /*
             * Mantis #487: release the connection.
             */
            httppost.reset();
        }

        circuitBreaker
                .execute(new DeferredCupsCircuitOperation(deferredException));

        return statusCode;
    }

    /**
     *
     * @author Rijk Ravestein
     *
     */
    private static class DeferredCupsCircuitOperation
            implements CircuitBreakerOperation {

        /** */
        final Exception sendException;

        public DeferredCupsCircuitOperation(final Exception sendException) {
            this.sendException = sendException;
        }

        @Override
        public Object execute(CircuitBreaker circuitBreaker) {
            if (this.sendException != null) {
                throw new CircuitTrippingException(sendException);
            }
            return null;
        }

    }

    /**
     * A list of {@link InputStream} object acting as one {@link InputStream}.
     */
    private static class InputStreamList extends InputStream {

        private static final int EOF = -1;

        private final List<InputStream> istrList;
        private final Iterator<InputStream> istrIter;
        private InputStream istrWlk = null;

        public InputStreamList(List<InputStream> istrList) {
            super();
            this.istrList = istrList;
            this.istrIter = this.istrList.iterator();
            if (this.istrIter.hasNext()) {
                this.istrWlk = this.istrIter.next();
            }
        }

        @Override
        public int read() throws IOException {

            int b = EOF;

            if (istrWlk != null) {
                b = istrWlk.read();
                while (b == EOF && istrIter.hasNext()) {
                    istrWlk = istrIter.next();
                    b = istrWlk.read();
                }
            }
            return b;
        }

        @Override
        public void close() throws IOException {
            if (istrList != null) {
                for (final InputStream istr : istrList) {
                    istr.close();
                }
            }
        }
    }

    /**
     * Sends an IPP request to <i>local</i> CUPS.
     *
     * @param urlServer
     *            The URL of the server.
     * @param operationId
     *            The {@link IppOperationId}.
     * @param request
     *            The IPP request.
     * @return The IPP response.
     * @throws IppConnectException
     *             When connection errors.
     */
    public List<IppAttrGroup> send(final URL urlServer,
            final IppOperationId operationId, final List<IppAttrGroup> request)
            throws IppConnectException {
        return this.send(urlServer, true, operationId, request);
    }

    /**
     * Sends an IPP request to CUPS.
     *
     * @param urlServer
     *            The URL of the server.
     * @param isLocalUrlServer
     *            {@code true} when urlServer is <i>local</i> CUPS,
     *            {@code false} when urlServer is <i>remote</i> CUPS.
     * @param operationId
     *            The {@link IppOperationId}.
     * @param request
     *            The IPP request.
     * @return The IPP response.
     * @throws IppConnectException
     *             When connection errors.
     */
    public List<IppAttrGroup> send(final URL urlServer,
            final boolean isLocalUrlServer, final IppOperationId operationId,
            final List<IppAttrGroup> request) throws IppConnectException {
        final File file = null;
        return send(urlServer, isLocalUrlServer, operationId, request, file);
    }

    /**
     * Sends an IPP request with file to <i>local</i> CUPS.
     *
     * @param urlServer
     *            The URL of the server.
     * @param operationId
     *            The {@link IppOperationId}.
     * @param request
     *            The IPP request.
     * @param file
     *            The {@link File} to send.
     * @return The IPP response.
     * @throws IppConnectException
     *             When connection errors.
     */
    public List<IppAttrGroup> send(final URL urlServer,
            final IppOperationId operationId, final List<IppAttrGroup> request,
            final File file) throws IppConnectException {
        return this.send(urlServer, true, operationId, request, file);
    }

    /**
     * Sends an IPP request with file to CUPS.
     *
     * @param urlServer
     *            The URL of the server.
     * @param isLocalUrlServer
     *            {@code true} when urlServer is <i>local</i> CUPS,
     *            {@code false} when urlServer is <i>remote</i> CUPS.
     * @param operationId
     *            The {@link IppOperationId}.
     * @param request
     *            The IPP request.
     * @param file
     *            The {@link File} to send.
     * @return The IPP response.
     * @throws IppConnectException
     *             When connection errors.
     */
    private List<IppAttrGroup> send(final URL urlServer,
            final boolean isLocalUrlServer, final IppOperationId operationId,
            final List<IppAttrGroup> request, final File file)
            throws IppConnectException {

        final List<IppAttrGroup> response = new ArrayList<>();

        IppStatusCode statusCode;

        try {
            statusCode = send(urlServer, isLocalUrlServer, operationId, request,
                    file, response);

            if (statusCode != IppStatusCode.OK
                    && statusCode != IppStatusCode.CLI_NOTFND) {
                throw new IppSyntaxException(statusCode.toString());
            }

        } catch (InterruptedException | CircuitBreakerException
                | IppSyntaxException e) {
            throw new IppConnectException(e);
        }

        return response;
    }

    /**
     * Sends an IPP request to <i>local</i> CUPS.
     *
     * @param urlServer
     * @param operationId
     * @param request
     * @param response
     * @return
     * @throws IppConnectException
     */
    public IppStatusCode send(final URL urlServer,
            final IppOperationId operationId, final List<IppAttrGroup> request,
            final List<IppAttrGroup> response) throws IppConnectException {
        return send(urlServer, true, operationId, request, response);
    }

    /**
     *
     * @param urlServer
     * @param isLocalUrlServer
     *            {@code true} when urlServer is <i>local</i> CUPS,
     *            {@code false} when urlServer is <i>remote</i> CUPS.
     * @param operationId
     * @param request
     * @param response
     * @return
     * @throws IppConnectException
     */
    private IppStatusCode send(final URL urlServer, boolean isLocalUrlServer,
            final IppOperationId operationId, final List<IppAttrGroup> request,
            final List<IppAttrGroup> response) throws IppConnectException {

        try {
            return send(urlServer, isLocalUrlServer, operationId, request, null,
                    response);
        } catch (InterruptedException | CircuitBreakerException e) {
            throw new IppConnectException(e);
        }
    }

    /**
     * Writes IPP data on {@link OutputStream}.
     *
     * @param ostr
     *            The {@link OutputStream}.
     * @param operationId
     *            Operation ID.
     * @param attrGroups
     *            IPP attribute groups.
     * @throws IOException
     *             When IO erors occur.
     */
    private void write(final OutputStream ostr,
            final IppOperationId operationId,
            final List<IppAttrGroup> attrGroups) throws IOException {

        // -----------------------------------------------
        // | version-number (2 bytes - required)
        // -----------------------------------------------
        ostr.write(IPP_VERSION.getVersionMajor());
        ostr.write(IPP_VERSION.getVersionMinor());

        // -----------------------------------------------
        // | operation-id (request) or status-code (response)
        // | (2 bytes - required)
        // -----------------------------------------------
        IppEncoder.writeInt16(ostr, operationId.asInt());

        // -----------------------------------------------
        // | request-id (4 bytes - required)
        // -----------------------------------------------
        IppEncoder.writeInt32(ostr, ++requestIdWlk); // Id MUST be GT zero

        // -----------------------------------------------
        // Attribute groups
        // -----------------------------------------------
        Charset myCharset = Charset.forName("US-ASCII");

        Writer traceLog = null;

        if (LOGGER.isTraceEnabled()) {
            traceLog = new StringWriter();
            traceLog.write("\n");
            traceLog.write(TRACE_SEP);
            traceLog.write("\n| " + operationId.toString() + " : request-id ["
                    + requestIdWlk + "]");
            traceLog.write("\n" + TRACE_SEP);
        }

        IppEncoder.writeAttributes(attrGroups, ostr, myCharset, traceLog);

        if (traceLog != null) {
            LOGGER.trace(traceLog.toString());
        }

        // -----------------------------------------------
        // End--of-Attr
        // -----------------------------------------------
        ostr.write(IppDelimiterTag.END_OF_ATTR.asInt());
    }

}
