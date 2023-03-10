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
package org.savapage.core.dao;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.dto.IppMediaCostDto;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterAttr;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface PrinterDao extends GenericDao<Printer> {

    /**
     * Field identifiers used for select and sort.
     */
    enum Field {

        /**
         * The display name.
         */
        DISPLAY_NAME
    }

    /**
     *
     */
    class ListFilter {

        private String containingText;
        private String printerGroup;
        private Boolean disabled;
        private Boolean deleted;

        /**
         * {@link PrinterAttrEnum#ACCESS_INTERNAL} value to find in database.
         * Absence in database is interpreted as "N", i.e. "public printer".
         */
        private Boolean internal;

        /**
         * {@link PrinterAttrEnum#JOBTICKET_ENABLE} value to find in database.
         * Absence in database is interpreted as "N".
         */
        private Boolean jobTicket;

        private Boolean snmp;

        public String getContainingText() {
            return containingText;
        }

        public void setContainingText(String containingText) {
            this.containingText = containingText;
        }

        public String getPrinterGroup() {
            return printerGroup;
        }

        public void setPrinterGroup(String printerGroup) {
            this.printerGroup = printerGroup;
        }

        public Boolean getDisabled() {
            return disabled;
        }

        public void setDisabled(Boolean disabled) {
            this.disabled = disabled;
        }

        public Boolean getDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }

        /**
         * @return {@link PrinterAttrEnum#ACCESS_INTERNAL} value to find in
         *         database. Absence in database is interpreted as "N", i.e.
         *         "public printer".
         */
        public Boolean getInternal() {
            return internal;
        }

        /**
         * @param internal
         *            {@link PrinterAttrEnum#ACCESS_INTERNAL} value to find in
         *            database. Absence in database is interpreted as "N", i.e.
         *            "public printer".
         */
        public void setInternal(Boolean internal) {
            this.internal = internal;
        }

        /**
         * @return {@link PrinterAttrEnum#JOBTICKET_ENABLE} value to find in
         *         database. Absence in database is interpreted as "N".
         */
        public Boolean getJobTicket() {
            return jobTicket;
        }

        /**
         * @param jobTicket
         *            {@link PrinterAttrEnum#JOBTICKET_ENABLE} value to find in
         *            database. Absence in database is interpreted as "N".
         */
        public void setJobTicket(Boolean jobTicket) {
            this.jobTicket = jobTicket;
        }

        public Boolean getSnmp() {
            return snmp;
        }

        public void setSnmp(Boolean snmp) {
            this.snmp = snmp;
        }

    }

    /**
     * Wrapper class for PWG5101.1 IPP media-source (name) to get its name (key)
     * to be used in {@link PrinterAttr#setName(String)}.
     * <p>
     * The key has format: {@code media-source.[default|ippMediaSourceName]}
     * </p>
     */
    class MediaSourceAttr {

        final private String ippMediaSourceName;

        public MediaSourceAttr(String ippMediaSourceName) {
            this.ippMediaSourceName = ippMediaSourceName;
        }

        /**
         * Creates a {@link MediaSourceAttr} from a database key value.
         *
         * @param dbKey
         *            The database key.
         * @return {@code null} when the database key is invalid (is not of a
         *         media-source {@link PrinterAttr}).
         */
        public static MediaSourceAttr createFromDbKey(String dbKey) {

            MediaSourceAttr mediaAttr = null;

            final String dbKeyPrefix = getKeyPrefix();

            if (dbKey.startsWith(dbKeyPrefix)) {

                final String mediaSource =
                        dbKey.substring(dbKeyPrefix.length());

                if (StringUtils.isNotBlank(mediaSource)) {
                    mediaAttr = new MediaSourceAttr(mediaSource);
                }
            }
            return mediaAttr;
        }

        public String getIppMediaSourceName() {
            return ippMediaSourceName;
        }

        /**
         * Composes the key used to store/retrieve.
         * <p>
         * IMPORTANT: this key is used for database storage.
         * </p>
         *
         * @return
         */
        public String getKey() {
            return String.format("%s%s", getKeyPrefix(),
                    this.ippMediaSourceName);
        }

        /**
         *
         * @return The prefix of all media-source keys.
         */
        public static String getKeyPrefix() {
            return PrinterAttrEnum.PFX_MEDIA_SOURCE.getDbName();
        }

    }

    /**
     * Wrapper class for an IPP keyword attribute value to get its name (key) to
     * be used in {@link PrinterAttr#setName(String)}.
     * <p>
     * The key has format: {@code ipp.[ipp-keyword]}
     * </p>
     */
    class IppKeywordAttr {

        /** */
        final private String ippKeyword;

        /** */
        public IppKeywordAttr(final String keyword) {
            this.ippKeyword = keyword;
        }

        /**
         * Creates a {@link IppKeywordAttr} from a database key value.
         *
         * @param dbKey
         *            The database key.
         * @return {@code null} when the database key is invalid (is not of a
         *         ipp {@link PrinterAttr}).
         */
        public static IppKeywordAttr createFromDbKey(String dbKey) {

            IppKeywordAttr ippAttr = null;

            if (dbKey.startsWith(getKeyPrefix())) {

                final String keyword = dbKey.substring(getKeyPrefix().length());

                if (StringUtils.isNotBlank(keyword)) {
                    ippAttr = new IppKeywordAttr(keyword);
                }
            }
            return ippAttr;
        }

        /**
         *
         * @return The IPP keyword.
         */
        public String getIppKeyword() {
            return this.ippKeyword;
        }

        /**
         * Composes the key used to store/retrieve.
         *
         * @return The key is used for database storage.
         */
        public String getKey() {
            return getKey(this.ippKeyword);
        }

        /**
         * Composes the key used to store/retrieve.
         *
         * @param ippKeyword
         *            The IPP keyword.
         * @return The key is used for database storage.
         */
        public static String getKey(final String ippKeyword) {
            return String.format("%s%s", getKeyPrefix(), ippKeyword);
        }

        /**
         *
         * @return The prefix of all media-source keys.
         */
        public static String getKeyPrefix() {
            return PrinterAttrEnum.PFX_IPP_KEYWORD.getDbName();
        }

    }

    /**
     * Wrapper class for PWG5101.1 IPP media (name) to get its name (key) to be
     * used in {@link PrinterAttr#setName(String)}.
     * <p>
     * The key has format: {@code cost.media.[default|ippMediaName]}
     * </p>
     */
    class CostMediaAttr {

        /** */
        public static final String COST_3_MEDIA_DEFAULT =
                IppMediaCostDto.DEFAULT_MEDIA;

        /** */
        private final String ippMediaName;

        /**
         * Creates default attribute.
         */
        public CostMediaAttr() {
            ippMediaName = COST_3_MEDIA_DEFAULT;
        }

        public CostMediaAttr(String ippMediaName) {
            if (StringUtils.isBlank(ippMediaName)) {
                this.ippMediaName = COST_3_MEDIA_DEFAULT;
            } else {
                this.ippMediaName = ippMediaName;
            }
        }

        public String getIppMediaName() {
            return ippMediaName;
        }

        public boolean isDefault() {
            return this.ippMediaName.equals(COST_3_MEDIA_DEFAULT);
        }

        /**
         * Composes the key used to store/retrieve default cost.
         * <p>
         * IMPORTANT: this key is used for database storage.
         * </p>
         *
         * @return The database key.
         */
        public String getKey() {
            return String.format("%s%s", getKeyPrefix(), this.ippMediaName);
        }

        /**
         *
         * @return The prefix of all media-source keys.
         */
        public static String getKeyPrefix() {
            return PrinterAttrEnum.PFX_COST_MEDIA.getDbName();
        }

        /**
         * Checks if key format is valid.
         *
         * @param key
         *            The key.
         * @return {@code true} when this is a valid key.
         */
        public static boolean isValidKey(final String key) {

            if (key.length() > getKeyPrefix().length()) {

                final String media = key.substring(getKeyPrefix().length());

                if (StringUtils.isNotBlank(media)) {
                    return media.equals(COST_3_MEDIA_DEFAULT)
                            || IppMediaSizeEnum.find(media) != null;
                }
            }
            return false;
        }

        /**
         * Creates a {@link CostMediaAttr} from a database key value.
         *
         * @param dbKey
         *            The database key.
         * @return {@code null} when the database key is invalid (is not of a
         *         cost.media {@link PrinterAttr}).
         */
        public static CostMediaAttr createFromDbKey(String dbKey) {

            CostMediaAttr mediaAttr = null;

            if (dbKey.startsWith(getKeyPrefix())) {

                final String media = dbKey.substring(getKeyPrefix().length());

                if (StringUtils.isNotBlank(media)) {
                    if (media.equals(COST_3_MEDIA_DEFAULT)
                            || IppMediaSizeEnum.find(media) != null) {
                        mediaAttr = new CostMediaAttr(media);
                    }
                }
            }
            return mediaAttr;
        }
    }

    /**
     * Gets the {@link PrinterAttr} key (name) for cost of the default media.
     *
     * @return
     */
    CostMediaAttr getCostMediaAttr();

    /**
     * Gets the {@link PrinterAttr} key (name) for cost of the IPP media.
     *
     * @param ippMediaName
     *            The name of the IPP media.
     * @return
     */
    CostMediaAttr getCostMediaAttr(String ippMediaName);

    /**
     * Gets the {@link PrinterAttr} key (name) for te IPP media-source.
     *
     * @param ippMediaSourceName
     *            The name of the IPP media-source.
     * @return
     */
    MediaSourceAttr getMediaSourceAttr(String ippMediaSourceName);

    /**
     *
     * @param chargeType
     * @return
     */
    Printer.ChargeType getChargeType(String chargeType);

    /**
     * Gets the media cost for this printer and media.
     *
     * @param printer
     * @param ippMediaName
     *            The PWG5101.1 IPP media name.
     * @return {@code null} when no cost is found.
     */
    IppMediaCostDto getMediaCost(Printer printer, String ippMediaName);

    /**
     * Resets the totals to zero for all {@link Printer} instances.
     *
     * @param resetDate
     *            The reset date.
     * @param resetBy
     *            The actor.
     */
    void resetTotals(Date resetDate, String resetBy);

    /**
     * Removes printers (cascade delete) who are logically deleted, and who do
     * not have any related DocLog.
     *
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @return The number of removed printers.
     */
    int prunePrinters(DaoBatchCommitter batchCommitter);

    /**
     * Counts the number of {@link PrintOut} documents of a {@link Printer}.
     *
     * @param id
     *            The primary key of the {@link Printer}.
     * @return The count.
     */
    long countPrintOuts(Long id);

    /**
     * Checks if {@link Printer} can be used as Job Ticket redirect printer.
     *
     * @param id
     *            The primary key of the {@link Printer}.
     * @return {@code true} when printer is a job ticket redirect printer.
     */
    boolean isJobTicketRedirectPrinter(Long id);

    /**
     * Finds the {@link Printer} by name, when not found {@code null} is
     * returned.
     *
     * @param printerName
     *            The unique name of the printer.
     * @return The printer object or {@code null} when not found.
     */
    Printer findByName(String printerName);

    /**
     * Finds the printer by name, when not found the printer is created, so
     * after the read the printer persists in the database.
     *
     * @param printerName
     *            The unique name of the printer.
     * @param lazyCreated
     *            Telling if instance was lazy created or not.
     * @return The printer row instance.
     */
    Printer findByNameInsert(String printerName, MutableBoolean lazyCreated);

    /**
     * Counts the number of printers according to filter.
     *
     * @param filter
     *            The filter.
     * @return The count.
     */
    long getListCount(ListFilter filter);

    /**
     * Gets a chunk of printers.
     *
     * @param filter
     *            The filter.
     * @param startPosition
     *            The zero-based start position of the chunk related to the
     *            total number of rows. If {@code null} the chunk starts with
     *            the first row.
     * @param maxResults
     *            The maximum number of rows in the chunk. If {@code null}, then
     *            ALL (remaining rows) are returned.
     * @param orderBy
     *            The sort field.
     * @param sortAscending
     *            {@code true} when sorted ascending.
     * @return The chunk.
     */
    List<Printer> getListChunk(ListFilter filter, Integer startPosition,
            Integer maxResults, Field orderBy, boolean sortAscending);

}
