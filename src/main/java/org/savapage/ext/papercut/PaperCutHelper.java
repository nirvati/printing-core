/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.ext.papercut;

import java.net.URI;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutHelper {

    /**
     * No public instantiation.
     */
    private PaperCutHelper() {
    }

    /**
     * Converts a unicode to 7-bit ascii character string. Latin-1 diacritics
     * are flattened and chars > 127 are replaced by '?'.
     *
     * @param unicode
     *            The unicode string.
     * @return The 7-bit ascii character string.
     */
    private static String unicodeToAscii(final String unicode) {

        final String stripped = StringUtils.stripAccents(unicode);
        final StringBuilder output = new StringBuilder(stripped.length());

        for (char a : stripped.toCharArray()) {
            if (a > 127) {
                a = '?';
            }
            output.append(a);
        }
        return output.toString();
    }

    /**
     * Encodes the job name of the proxy printed document to a unique name that
     * can be used to query the PaperCut's tbl_printer_usage_log table about the
     * print status. The format is: {@code documentName.accountName.documentId}
     *
     * <p>
     * Note: {@link #unicodeToAscii(String)} is applied to the document name,
     * since PaperCut converts the job name to 7-bit ascii. So we better do the
     * convert ourselves.
     * </p>
     *
     * @param accountName
     *            The account name (as mnemonic, or as part of the unique
     *            result).
     * @param documentId
     *            The document ID (as part of the unique result).
     * @param documentName
     *            The document name (as mnemonic).
     * @return The encoded unique name.
     */
    public static String encodeProxyPrintJobName(final String accountName,
            final String documentId, final String documentName) {

        final StringBuilder sfx = new StringBuilder();

        if (StringUtils.isNotBlank(documentName)) {
            sfx.append(DelegatedPrintCommentSyntax.JOB_NAME_INFO_SEPARATOR);
        }

        if (StringUtils.isNotBlank(accountName)) {
            sfx.append(accountName);
        }

        sfx.append(DelegatedPrintCommentSyntax.JOB_NAME_INFO_SEPARATOR).append(
                documentId);

        final String suffix = sfx.toString();

        if (suffix.length() > PaperCutDbProxy.COL_LEN_DOCUMENT_NAME) {
            throw new IllegalArgumentException(
                    "PaperCut database column length exceeded");
        }

        return unicodeToAscii(String.format("%s%s", StringUtils.abbreviate(
                StringUtils.defaultString(documentName),
                PaperCutDbProxy.COL_LEN_DOCUMENT_NAME - suffix.length()),
                suffix));
    }

    /**
     *
     * @param encodedJobName
     *            The encoded job name.
     * @return {@code null} when not found.
     */
    public static String getAccountFromEncodedProxyPrintJobName(
            final String encodedJobName) {

        final String[] parts =
                StringUtils.split(encodedJobName,
                        DelegatedPrintCommentSyntax.JOB_NAME_INFO_SEPARATOR);

        if (parts.length < 3) {
            return null;
        }

        return parts[parts.length - 2];
    }

    /**
     * Checks if the printer device URI indicates a PaperCut managed printer.
     *
     * @param deviceUri
     *            The device {@link URI} of the CUPS printer.
     * @return {@code true} When device {@link URI} indicates a PaperCut managed
     *         printer.
     */
    public static boolean isPaperCutPrinter(final URI deviceUri) {
        return deviceUri != null
                && deviceUri.toString().startsWith("papercut:");
    }

}
