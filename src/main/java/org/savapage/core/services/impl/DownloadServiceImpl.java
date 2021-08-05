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
package org.savapage.core.services.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.UUID;

import javax.mail.internet.ContentType;
import javax.mail.internet.ParseException;
import javax.naming.LimitExceededException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.savapage.core.SpException;
import org.savapage.core.UnavailableException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.DocContentToPdfException;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.doc.WkHtmlToPdf;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.i18n.PhraseEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.print.server.DocContentPrintReq;
import org.savapage.core.services.DownloadService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DownloadServiceImpl extends AbstractService
        implements DownloadService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DownloadServiceImpl.class);

    /** */
    private static final String ALIAS_NAME = "Download Service";

    /**
     * The Apache HttpClient is thread safe.
     */
    private CloseableHttpClient httpclientApache = null;

    /** */
    private PoolingHttpClientConnectionManager connManager = null;

    @Override
    public void start() {

        LOGGER.debug("{} is starting...", ALIAS_NAME);

        final ConfigManager cm = ConfigManager.instance();

        final int maxConnections =
                cm.getConfigInt(IConfigProp.Key.DOWNLOAD_MAX_CONNECTIONS);

        final int maxConnectionsPerRoute = cm.getConfigInt(
                IConfigProp.Key.DOWNLOAD_MAX_CONNECTIONS_PER_ROUTE);

        this.connManager = new PoolingHttpClientConnectionManager();

        this.connManager.setMaxTotal(maxConnections);
        this.connManager.setDefaultMaxPerRoute(maxConnectionsPerRoute);

        final HttpClientBuilder builder = HttpClientBuilder.create()
                .setConnectionManager(this.connManager);

        /*
         * While HttpClient instances are thread safe and can be shared between
         * multiple threads of execution, it is highly recommended that each
         * thread maintains its own dedicated instance of HttpContext.
         */
        this.httpclientApache = builder.build();

        LOGGER.debug("{} started.", ALIAS_NAME);
    }

    /**
     * @return The {@link RequestConfig}.
     */
    private static RequestConfig buildRequestConfig() {

        final int connectTimeout = getConnectTimeout();
        final int socketTimeout = getSocketTimeout();

        return RequestConfig.custom().setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .setConnectionRequestTimeout(socketTimeout).build();
    }

    /**
     * @return Connect timeout in milliseconds.
     */
    private static int getConnectTimeout() {
        return ConfigManager.instance()
                .getConfigInt(IConfigProp.Key.DOWNLOAD_CONNECT_TIMEOUT_MSEC);
    }

    /**
     * @return Socket (read) timeout in milliseconds.
     */
    private static int getSocketTimeout() {
        return ConfigManager.instance()
                .getConfigInt(IConfigProp.Key.DOWNLOAD_SOCKET_TIMEOUT_MSEC);
    }

    /**
     * Downloads source to target.
     *
     * @param source
     *            URL source.
     * @param target
     *            File target.
     * @param maxMB
     *            Max MB to download.
     * @return The HTTP content type. {@code null} when unknown.
     * @throws IOException
     *             If IO error.
     * @throws LimitExceededException
     *             If download exceeded max MB.
     */
    private String download(final URL source, final File target,
            final int maxMB) throws IOException, LimitExceededException {

        final HttpGet request = new HttpGet(source.toString());
        request.setConfig(buildRequestConfig());

        final HttpResponse response = this.httpclientApache.execute(request);

        final int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != HttpStatus.SC_OK) {
            throw new IOException(String.format("HTTP status %d: %s.",
                    statusCode, response.getStatusLine().getReasonPhrase()));
        }

        final Header contentType = response.getEntity().getContentType();

        final HttpEntity entity = response.getEntity();
        if (entity == null) {
            throw new IOException("no entity.");
        }

        final int bufferSize = 1024;
        final byte[] buffer = new byte[bufferSize];

        final long maxBytes = 1024 * 1024 * maxMB;
        long bytesDownloaded = 0;

        try (InputStream istr = entity.getContent();
                FileOutputStream fos = new FileOutputStream(target);) {

            int read = istr.read(buffer);

            while (read > 0) {
                bytesDownloaded += read;

                if (bytesDownloaded > maxBytes) {
                    throw new LimitExceededException(
                            String.format("%d MB limit exceeded.", maxMB));
                }
                fos.write(buffer, 0, read);
                read = istr.read(buffer);
            }

        } finally {
            request.reset();
        }

        if (contentType == null) {
            return null;
        }

        return contentType.getValue();
    }

    /**
     *
     * @return A unique temp file.
     */
    private static File createUniqueTempFile() {
        final StringBuilder name = new StringBuilder();
        return new File(name.append(ConfigManager.getAppTmpDir())
                .append("/savapage-download.").append(UUID.randomUUID())
                .toString());
    }

    @Override
    public boolean download(final URL source, final String originatorIp,
            final User user, final InternalFontFamilyEnum preferredFont,
            final int maxMB) throws IOException, LimitExceededException {

        final File target = createUniqueTempFile();

        try {
            final StringBuilder nameBuilder = new StringBuilder();
            nameBuilder.append(source.getProtocol()).append('-')
                    .append(source.getHost());
            nameBuilder.append(
                    StringUtils.replaceChars(source.getPath(), '/', '-'));
            nameBuilder.append(StringUtils.replaceChars(StringUtils
                    .replaceChars(StringUtils.defaultString(source.getQuery()),
                            '?', '-'),
                    '&', '-'));

            final String fileName = nameBuilder.toString();
            final DocContentTypeEnum contentTypeFileName =
                    DocContent.getContentTypeFromFile(fileName);

            boolean isWkHtmlToPdf = false;

            if (WkHtmlToPdf.isAvailable()) {

                DocContentTypeEnum contentTypeWk = contentTypeFileName;

                if (contentTypeWk == null) {
                    contentTypeWk = this.getContentType(source);
                }
                if (contentTypeWk == null) {
                    throw new DocContentPrintException(PhraseEnum.NOT_FOUND
                            .uiText(ServiceContext.getLocale()).toLowerCase());
                }
                if (contentTypeWk == DocContentTypeEnum.HTML) {
                    final WkHtmlToPdf converter = new WkHtmlToPdf();
                    try {
                        converter.convert(source, target);
                        isWkHtmlToPdf = true;
                    } catch (DocContentToPdfException e) {
                        throw new SpException(e);
                    }
                }
            }

            DocContentTypeEnum contentType;

            if (isWkHtmlToPdf) {
                contentType = DocContentTypeEnum.PDF;
            } else {
                final String contentTypeReturn =
                        this.download(source, target, maxMB);
                contentType =
                        DocContent.getContentTypeFromMime(contentTypeReturn);
            }

            if (contentType == null) {
                contentType = contentTypeFileName;
                if (contentType == null) {
                    contentType = DocContentTypeEnum.HTML;
                }
                LOGGER.info("No content type found for [{}]: asuming [{}]",
                        fileName, contentType);
            }

            final DocContentPrintReq docContentPrintReq =
                    new DocContentPrintReq();

            docContentPrintReq.setContentType(contentType);
            docContentPrintReq.setFileName(fileName);
            docContentPrintReq.setOriginatorEmail(null);
            docContentPrintReq.setOriginatorIp(originatorIp);
            docContentPrintReq.setPreferredOutputFont(preferredFont);
            docContentPrintReq.setProtocol(DocLogProtocolEnum.HTTP);
            docContentPrintReq.setTitle(fileName);
            docContentPrintReq
                    .setContentTypePdfIsClean(Boolean.valueOf(isWkHtmlToPdf));

            try (InputStream fos = new FileInputStream(target)) {
                queueService().printDocContent(ReservedIppQueueEnum.WEBPRINT,
                        user.getUserId(), docContentPrintReq, fos);
            }

        } catch (DocContentPrintException | UnavailableException e) {
            throw new IOException(e.getMessage());
        } finally {
            target.delete();
        }
        return true;
    }

    /**
     * Gets content type of an Web URL.
     *
     * @param url
     *            URL
     * @throws IOException
     *             if an I/O exception occurs.
     * @return Content Type;
     */
    private DocContentTypeEnum getContentType(final URL url)
            throws IOException {

        final URLConnection c = url.openConnection();

        c.setConnectTimeout(getConnectTimeout());
        c.setReadTimeout(getSocketTimeout());

        DocContentTypeEnum contentTypeReturn = null;

        final String contentType = c.getContentType();

        if (contentType != null) {
            try {
                final ContentType ct = new ContentType(contentType);
                final DocContentTypeEnum contentTypeTmp =
                        DocContent.getContentTypeFromMime(ct.getBaseType());
                if (contentTypeTmp != null) {
                    contentTypeReturn = contentTypeTmp;
                }
            } catch (ParseException e) {
                LOGGER.warn("[{}] : {}", url.toString(), e.getMessage());
            }
        }
        return contentTypeReturn;
    }

    @Override
    public void shutdown() {
        LOGGER.debug("{} is shutting down...", ALIAS_NAME);
        IOHelper.closeQuietly(this.httpclientApache);
        IOHelper.closeQuietly(this.connManager);
        LOGGER.debug("{} shut down.", ALIAS_NAME);
    }

}
