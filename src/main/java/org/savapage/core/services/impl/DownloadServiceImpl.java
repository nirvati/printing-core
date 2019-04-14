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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

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
import org.savapage.core.UnavailableException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.print.server.DocContentPrintReq;
import org.savapage.core.services.DownloadService;
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

    @Override
    public void start() {

        LOGGER.debug("{} is starting...", ALIAS_NAME);

        final ConfigManager cm = ConfigManager.instance();

        final int maxConnections =
                cm.getConfigInt(IConfigProp.Key.DOWNLOAD_MAX_CONNECTIONS);

        final PoolingHttpClientConnectionManager connManager =
                new PoolingHttpClientConnectionManager();

        connManager.setDefaultMaxPerRoute(maxConnections);
        connManager.setMaxTotal(maxConnections);

        final HttpClientBuilder builder =
                HttpClientBuilder.create().setConnectionManager(connManager);

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

        final ConfigManager cm = ConfigManager.instance();

        final int connectTimeout =
                cm.getConfigInt(IConfigProp.Key.DOWNLOAD_CONNECT_TIMEOUT_MSEC);
        final int socketTimeout =
                cm.getConfigInt(IConfigProp.Key.DOWNLOAD_SOCKET_TIMEOUT_MSEC);

        return RequestConfig.custom().setConnectTimeout(connectTimeout)
                .setSocketTimeout(socketTimeout)
                .setConnectionRequestTimeout(socketTimeout).build();
    }

    @Override
    public String download(final URL source, final File target)
            throws IOException {

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

        try (InputStream istr = entity.getContent();
                FileOutputStream fos = new FileOutputStream(target);) {

            int read = bufferSize;
            while (read == bufferSize) {
                read = istr.read(buffer);
                fos.write(buffer, 0, read);
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
            final User user, final InternalFontFamilyEnum preferredFont)
            throws IOException {

        final File target = createUniqueTempFile();

        try {

            final String fileName = String.format("%s-%s-%s-%s",
                    source.getProtocol(), source.getHost(),
                    StringUtils.replaceChars(source.getPath(), '/', '-'),
                    StringUtils.replaceChars(StringUtils.replaceChars(
                            source.getQuery(), '?', '-'), '&', '-'));

            final String contentTypeReturn = this.download(source, target);

            DocContentTypeEnum contentType =
                    DocContent.getContentTypeFromMime(contentTypeReturn);

            if (contentType == null) {
                contentType = DocContent.getContentTypeFromFile(fileName);
                if (contentType == null) {
                    return false;
                }
                LOGGER.info("No content type found for [{}]: using [{}]",
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

            try (InputStream fos = new FileInputStream(target)) {
                queueService().printDocContent(ReservedIppQueueEnum.WEBPRINT,
                        user, true, docContentPrintReq, fos);
            }

        } catch (DocContentPrintException | UnavailableException e) {
            throw new IOException(e.getMessage());
        } finally {
            target.delete();
        }
        return true;
    }

    @Override
    public void shutdown() {
        LOGGER.debug("{} is shutting down...", ALIAS_NAME);
        IOHelper.closeQuietly(this.httpclientApache);
        LOGGER.debug("{} shut down.", ALIAS_NAME);
    }

}
