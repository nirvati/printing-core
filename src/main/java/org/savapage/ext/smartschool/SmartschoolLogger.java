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
package org.savapage.ext.smartschool;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Locale;

import javax.xml.soap.SOAPException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SmartschoolLogger {

    /**
     * .
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SmartschoolLogger.class);

    /**
     * Constructor.
     */
    private SmartschoolLogger() {

    }

    /**
     *
     * @return The logger.
     */
    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     *
     * @param url
     *            The URL, or {@code null} when PDF was base64 embedded.
     * @param documentName
     *            The name of the document.
     * @param documentByteCount
     *            The size of the document.
     */
    public static void logPdfDownload(final URL url, final String documentName,
            final long documentByteCount) {

        if (!LOGGER.isInfoEnabled()) {
            return;
        }

        final StringBuilder detail = new StringBuilder();

        if (url == null) {
            detail.append("Base64");
        } else {
            detail.append("URL");
        }

        detail.append(": ").append(documentName).append(" (")
                .append(NumberUtil.humanReadableByteCountSI(Locale.getDefault(),
                        documentByteCount))
                .append(")");

        if (url != null) {
            detail.append("\n\n").append(url.toString());
        }

        LOGGER.info(String.format(
                "|_______________ Downloaded ______________|\n\n%s\n",
                detail.toString()));
    }

    /**
     * @param xml
     *            The XML string.
     */
    public static void logRequest(final String xml) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(
                    "|________________ Request ________________|\n\n%s\n",
                    formatXml(xml)));
        }
    }

    /**
     * @param xml
     *            The XML string.
     */
    public static void logResponse(final String xml) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(
                    "|............... Response ...............|\n\n%s\n",
                    formatXml(xml)));
        }
    }

    /**
     * @param msg
     *            The message string.
     */
    public static void logDebug(final String msg) {
        LOGGER.debug(msg);
    }

    /**
     * Logs a {@link SOAPException}.
     * <p>
     * Note: the content of the request is not logged, since this exposes the
     * password.
     * </p>
     *
     * @param request
     *            The request.
     * @param e
     *            The exception.
     */
    public static void logError(final SmartschoolRequestEnum request,
            final SOAPException e) {

        final String cause;

        if (e.getCause() == null) {
            cause = "unknown";
        } else {
            cause = String.format("%s, %s",
                    e.getCause().getClass().getSimpleName(),
                    StringUtils.defaultString(e.getCause().getMessage()));
        }

        LOGGER.error(String.format(
                "|_______________ SOAPException_________________|\n"
                        + "Request: %s\nError: %s\nCause: %s\n",
                request.toString(), e.getMessage(), cause));
    }

    /**
     * @param msg
     *            The message string.
     */
    public static void logError(final String msg) {
        LOGGER.error(msg);
    }

    /**
     *
     * @return {@code true} when enabled.
     */
    public static boolean isEnabled() {
        return LOGGER.isInfoEnabled();
    }

    /**
     * Pretty formats raw XML input.
     *
     * @param input
     *            The XML input.
     * @return The pretty formatted XML.
     */
    private static String formatXml(final String input) {

        final int indent = 4;

        try {
            final Source xmlInput = new StreamSource(new StringReader(input));
            final StringWriter stringWriter = new StringWriter();
            final StreamResult xmlOutput = new StreamResult(stringWriter);
            final TransformerFactory transformerFactory =
                    TransformerFactory.newInstance();
            final Transformer transformer = transformerFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount",
                    String.valueOf(indent));
            transformer.transform(xmlInput, xmlOutput);

            return xmlOutput.getWriter().toString();

        } catch (Exception e) {
            throw new SpException(e.getMessage(), e);
        }
    }

}
