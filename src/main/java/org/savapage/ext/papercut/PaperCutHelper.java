/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.ext.papercut;

import java.net.URI;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.services.helpers.ThirdPartyEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutHelper {

    /**
     * The dummy account name to be used for a SavaPage Delegated Print.
     */
    private static final String SAVAPAGE_PRINTJOB_ACCOUNT_NAME = "savapage";

    public static final char COMPOSED_ACCOUNT_NAME_SEPARATOR = '.';

    private static final String COMPOSED_ACCOUNT_NAME_PFX = "savapage";
    private static final String COMPOSED_ACCOUNT_NAME_CLASS_SHARED = "shared";
    private static final String COMPOSED_ACCOUNT_NAME_CLASS_GROUP = "group";

    /**
     * The number of words (split by {@link #COMPOSED_ACCOUNT_NAME_SEPARATOR}
     * for a composed shared account name for PaperCut, for just a SavaPage
     * parent account.
     */
    private static final int COMPOSED_ACCOUNT_WORDS_PARENT = 3;

    /**
     * The number of words (split by {@link #COMPOSED_ACCOUNT_NAME_SEPARATOR}
     * for a composed shared account name for PaperCut, for a SavaPage
     * parent/child account.
     */
    private static final int COMPOSED_ACCOUNT_WORDS_PARENT_CHILD = 4;

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
     * Encodes SavaPage Delegated Print job name of the proxy printed document
     * to a unique name that can be used to query the PaperCut's
     * tbl_printer_usage_log table about the print status. The format is:
     * {@code [documentName].savapage.[documentId]}
     *
     * @param documentName
     *            The document name (as mnemonic).
     * @return The encoded unique name.
     */
    public static String encodeProxyPrintJobName(final String documentName) {
        return encodeProxyPrintJobName(SAVAPAGE_PRINTJOB_ACCOUNT_NAME,
                UUID.randomUUID().toString(), documentName);
    }

    /**
     * Replaces the UUID suffix of an encoded Job name with a new random one.
     *
     * @param documentName
     *            The original encoded document name.
     * @return The new encoded document name.
     */
    public static String renewProxyPrintJobNameUUID(final String documentName) {
        /*
         * Strip the UUID suffix.
         */
        final String[] words = StringUtils.split(documentName,
                PaperCutPrintCommentSyntax.JOB_NAME_INFO_SEPARATOR);
        final String lastWord = words[words.length - 1];

        /*
         * Replace with new UUID.
         */
        return StringUtils.replace(documentName, lastWord,
                UUID.randomUUID().toString());
    }

    /**
     * Uses SavaPage {@link Account} data to compose a shared sub account name
     * for PaperCut.
     *
     * @param accountType
     *            The SavaPage {@link AccountTypeEnum}.
     * @param accountName
     *            The SavaPage account name.
     * @param accountNameParent
     *            The name of the SavaPage parent account. Is {@code null} when
     *            parent account is irrelevant.
     * @return The composed sub account name to be used in PaperCut.
     */
    public static String composeSharedAccountName(
            final AccountTypeEnum accountType, final String accountName,
            final String accountNameParent) {

        final StringBuilder name = new StringBuilder();

        name.append(COMPOSED_ACCOUNT_NAME_PFX)
                .append(COMPOSED_ACCOUNT_NAME_SEPARATOR);

        switch (accountType) {
        case GROUP:
            name.append(COMPOSED_ACCOUNT_NAME_CLASS_GROUP);
            break;

        case SHARED:
            name.append(COMPOSED_ACCOUNT_NAME_CLASS_SHARED);
            break;

        default:
            throw new IllegalArgumentException(
                    String.format("%s.%s is not supported",
                            accountType.getClass().getSimpleName(),
                            accountType.toString()));
        }

        name.append(COMPOSED_ACCOUNT_NAME_SEPARATOR);

        if (accountNameParent == null) {
            name.append(accountName);
        } else {
            name.append(composeSharedAccountNameSuffix(accountName,
                    accountNameParent));
        }

        return name.toString();
    }

    /**
     * Composes the suffix of the shared account name for PaperCut.
     *
     * @param accountName
     *            The SavaPage account name.
     * @param accountNameParent
     *            The name of the SavaPage parent account. Is {@code null} when
     *            parent account is irrelevant.
     * @return The composed suffix of the shared sub account name to be used in
     *         PaperCut.
     */
    public static String composeSharedAccountNameSuffix(
            final String accountName, final String accountNameParent) {
        return String.format("%s%c%s", accountNameParent,
                COMPOSED_ACCOUNT_NAME_SEPARATOR, accountName);
    }

    /**
     * Gets the SavaPage {@link Account} name from the composed shared account
     * name for PaperCut.
     * <p>
     * {@code "savapage.group.child"} returns {@code "child"} and
     * {@code "savapage.group.parent.child"} returns {@code "parent.child"}.
     * </p>
     *
     * @see {@link #composeSharedAccountName(AccountTypeEnum, String, String)}.
     *
     * @param composedAccountName
     *            The composed account name.
     * @return The account name.
     */
    public static String
            decomposeSharedAccountName(final String composedAccountName) {

        final String[] parts = StringUtils.split(composedAccountName,
                COMPOSED_ACCOUNT_NAME_SEPARATOR);

        if (parts.length < COMPOSED_ACCOUNT_WORDS_PARENT
                || parts.length > COMPOSED_ACCOUNT_WORDS_PARENT_CHILD) {
            throw new IllegalArgumentException(String.format(
                    "Composed group [%s] syntax error.", composedAccountName));
        }

        if (parts.length == COMPOSED_ACCOUNT_WORDS_PARENT) {
            return parts[parts.length - 1];
        }

        return String.format("%s%c%s", parts[parts.length - 2],
                COMPOSED_ACCOUNT_NAME_SEPARATOR, parts[parts.length - 1]);
    }

    /**
     * Encodes the job name of the proxy printed document to a unique name that
     * can be used to query the PaperCut's tbl_printer_usage_log table about the
     * print status. The format is:
     * {@code [documentName].[accountName].[documentId]}
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
            sfx.append(PaperCutPrintCommentSyntax.JOB_NAME_INFO_SEPARATOR);
        }

        if (StringUtils.isNotBlank(accountName)) {
            sfx.append(accountName);
        }

        sfx.append(PaperCutPrintCommentSyntax.JOB_NAME_INFO_SEPARATOR)
                .append(documentId);

        final String suffix = sfx.toString();

        if (suffix.length() > PaperCutDb.COL_LEN_DOCUMENT_NAME) {
            throw new IllegalArgumentException(
                    "PaperCut database column length exceeded");
        }

        return unicodeToAscii(String.format("%s%s", StringUtils.abbreviate(
                StringUtils.defaultString(documentName),
                PaperCutDb.COL_LEN_DOCUMENT_NAME - suffix.length()),
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

        final String[] parts = StringUtils.split(encodedJobName,
                PaperCutPrintCommentSyntax.JOB_NAME_INFO_SEPARATOR);

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

    /**
     * @return The initial {@link ExternalSupplierStatusEnum} when issuing a job
     *         to be monitored by {@link ThirdPartyEnum#PAPERCUT} Print
     *         Management.
     */
    public static ExternalSupplierStatusEnum getInitialPendingJobStatus() {
        return ExternalSupplierStatusEnum.PENDING_EXT;
    }
}
