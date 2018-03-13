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

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PrintOut;

/**
 * Processor for creating comments of PaperCut (shared) account transactions for
 * a single print job issued to a PaperCut managed printer.
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutPrintCommentProcessor {

    /**
     * User requesting the print.
     */
    private final String requestingUserId;

    /**
     * Total number of printed copies.
     */
    private final int totalNumberOfCopies;

    /**
     * Total number of printed "Klas" copies.
     */
    private int totalCopiesForKlasCounter;

    /**
     * Number of pages in the printed document.
     */
    private final Integer numberOfDocumentPages;

    /**
     *
     */
    private final String indicatorPaperSize;
    private final String indicatorDuplex;
    private final String indicatorColor;
    private final String indicatorExternalId;

    private final String documentName;
    private final String trxComment;

    /**
     * {@code 1} or {@code -1} for plus or minus sign to be used for number of
     * copies in the comment.
     */
    private final int copiesSign;

    /**
     *
     */
    private final StringBuilder jobTrxComment;

    /**
     * @param docLogTrx
     *            The {@link DocLog} container of the {@link AccountTrx}
     *            objects.
     * @param docLogOut
     *            The {@link DocLog} container of the {@link DocOut} object.
     * @param totalCopies
     *            Total number of printed copies.
     * @param isRefund
     *            {@code true} when this is a refund comment.
     */
    public PaperCutPrintCommentProcessor(final DocLog docLogTrx,
            final DocLog docLogOut, final int totalCopies,
            final boolean isRefund) {

        final PrintOut printOutLog = docLogOut.getDocOut().getPrintOut();

        if (isRefund) {
            this.copiesSign = -1;
        } else {
            this.copiesSign = 1;
        }

        this.requestingUserId = docLogTrx.getUser().getUserId();
        this.totalNumberOfCopies = totalCopies;
        this.numberOfDocumentPages = docLogTrx.getNumberOfPages();

        this.indicatorPaperSize =
                convertToPaperSizeIndicator(printOutLog.getPaperSize());

        this.indicatorExternalId =
                StringUtils.defaultString(docLogOut.getExternalId(),
                        printOutLog.getCupsJobId().toString());

        if (printOutLog.getGrayscale().booleanValue()) {
            indicatorColor = PaperCutPrintCommentSyntax.INDICATOR_COLOR_OFF;
        } else {
            indicatorColor = PaperCutPrintCommentSyntax.INDICATOR_COLOR_ON;
        }

        if (printOutLog.getDuplex().booleanValue()) {
            indicatorDuplex = PaperCutPrintCommentSyntax.INDICATOR_DUPLEX_ON;
        } else {
            indicatorDuplex = PaperCutPrintCommentSyntax.INDICATOR_DUPLEX_OFF;
        }

        this.documentName = StringUtils.defaultString(docLogTrx.getTitle());
        this.trxComment = StringUtils.defaultString(docLogTrx.getLogComment());

        jobTrxComment = new StringBuilder();
    }

    /**
     * Starts the process.
     */
    public void initProcess() {
        // user | copies | pages
        jobTrxComment.append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR_FIRST)
                .append(requestingUserId)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(this.copiesSign * this.totalNumberOfCopies)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(this.numberOfDocumentPages);

        // ... | A4 | S | G | id
        jobTrxComment.append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR);

        appendIndicatorFields(jobTrxComment);

        this.totalCopiesForKlasCounter = 0;

    }

    /**
     * Processes a "Klas" transaction.
     *
     * @param klasName
     *            The "Klas" name.
     * @param copies
     *            The number of printed document copies.
     * @return The transaction comment.
     */
    public String processKlasTrx(final String klasName, final int copies) {
        this.totalCopiesForKlasCounter += copies;
        appendKlasCopiesToJobTrxComment(klasName, copies);
        return createKlasTrxComment(copies);
    }

    /**
     * Processes a "Klas" transaction.
     *
     * @param copies
     *            The number of printed document copies.
     * @return The transaction comment.
     */
    private String createKlasTrxComment(final int copies) {

        // requester | copies | pages
        final StringBuilder klasTrxComment = new StringBuilder();

        klasTrxComment.append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR_FIRST)
                .append(requestingUserId)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(this.copiesSign * copies)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(numberOfDocumentPages);

        // ... | A4 | S | G | id
        klasTrxComment.append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR);

        appendIndicatorFields(klasTrxComment);

        // ... | document | comment
        klasTrxComment
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(documentName)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(trxComment)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR_LAST);

        return klasTrxComment.toString();
    }

    /**
     * Appends "Klas" copies to the Job comment.
     *
     * @param klasName
     *            The "Klas" name.
     * @param copies
     *            The number of printed document copies.
     */
    private void appendKlasCopiesToJobTrxComment(final String klasName,
            final int copies) {

        // ... | user@class-n | copies-n
        jobTrxComment.append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(requestingUserId)
                //
                .append(PaperCutPrintCommentSyntax.USER_CLASS_SEPARATOR)
                .append(klasName)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(this.copiesSign * copies);
    }

    /**
     * Processes a user transaction.
     *
     * @param klasName
     *            The name of the "Klas" the user belongs to.
     * @param copies
     *            The number of printed document copies.
     * @return The transaction comment.
     */
    public String processUserTrx(final String klasName, final int copies) {

        final StringBuilder userCopiesComment = new StringBuilder();

        // class | requester | copies | pages
        userCopiesComment
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR_FIRST)
                .append(StringUtils.defaultString(klasName,
                        PaperCutPrintCommentSyntax.DUMMY_KLAS))
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(requestingUserId)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(this.copiesSign * copies)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(this.numberOfDocumentPages);

        //
        // ... | A4 | S | G | id
        userCopiesComment.append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR);

        appendIndicatorFields(userCopiesComment);

        // ... | document | comment
        userCopiesComment
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(documentName)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(trxComment)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR_LAST);

        return userCopiesComment.toString();
    }

    /**
     * Terminates the process.
     *
     * @return The accumulated transaction comment for the Job.
     */
    public String exitProcess() {

        // ... |
        if (totalCopiesForKlasCounter != totalNumberOfCopies) {

            jobTrxComment.append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                    .append(requestingUserId)
                    //
                    .append(PaperCutPrintCommentSyntax.USER_CLASS_SEPARATOR)
                    .append(PaperCutPrintCommentSyntax.DUMMY_KLAS)
                    //
                    .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                    .append(this.copiesSign * (totalNumberOfCopies
                            - totalCopiesForKlasCounter));
        }

        // ... | document | comment
        jobTrxComment.append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(documentName)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(trxComment)
                //
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR_LAST);

        //
        return StringUtils.abbreviate(jobTrxComment.toString(),
                PaperCutDbProxy.COL_LEN_TXN_COMMENT);
    }

    /**
     * Appends indicator fields to {@link StringBuilder} without leading and
     * trailing field separator. Example:
     * <p>
     * {@code "A4 | D | C | 243"}
     * </p>
     *
     * @param str
     *            The {@link StringBuilder} to append to.
     * @return The {@link StringBuilder} that was appended to.
     */
    private StringBuilder appendIndicatorFields(final StringBuilder str) {

        str.append(indicatorPaperSize)
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(indicatorDuplex)
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(indicatorColor)
                .append(PaperCutPrintCommentSyntax.FIELD_SEPARATOR)
                .append(indicatorExternalId);
        return str;
    }

    /**
     * Returns a short upper-case indicator of a paper size description.
     *
     * <pre>
     * "aa-bb"      --> "BB"
     * "aa"         --> "AA"
     * "is-a4"      --> "A4"
     * "na-letter"  --> "LETTER"
     * </pre>
     *
     * @param papersize
     *            The full paper size description.
     * @return The paper size indicator.
     */
    public static String convertToPaperSizeIndicator(final String papersize) {

        int index = StringUtils.indexOf(papersize, '-');

        if (index == StringUtils.INDEX_NOT_FOUND) {
            return papersize.toUpperCase();
        }
        index++;
        if (index == papersize.length()) {
            return "";
        }
        return StringUtils.substring(papersize, index).toUpperCase();
    }
}
