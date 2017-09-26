/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.dao;

import java.util.Date;
import java.util.List;

import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.dao.enums.ExternalSupplierStatusEnum;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.jpa.AccountTrx;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface DocLogDao extends GenericDao<DocLog> {

    /**
     * Field identifiers used for select and sort.
     */
    enum FieldEnum {
        CREATE_DATE, DOC_NAME, QUEUE, PRINTER
    }

    /**
     * Identifiers for DocLog type.
     */
    enum Type {
        ALL, IN, OUT, PDF, PRINT, TICKET
    }

    /**
     * Identifiers for Job State select
     */
    enum JobState {
        ALL, ACTIVE, COMPLETED, UNFINISHED
    }

    /**
     * Field identifiers used for select and sort.
     */
    enum Field {
        DATE_CREATED
    }

    /**
    *
    */
    class ListFilter {

        private DocLogProtocolEnum protocol;
        private ExternalSupplierEnum externalSupplier;
        private String externalId;
        private String containingExternalIdText;
        private ExternalSupplierStatusEnum externalStatus;

        public DocLogProtocolEnum getProtocol() {
            return protocol;
        }

        public void setProtocol(DocLogProtocolEnum protocol) {
            this.protocol = protocol;
        }

        public ExternalSupplierEnum getExternalSupplier() {
            return externalSupplier;
        }

        public void setExternalSupplier(ExternalSupplierEnum externalSupplier) {
            this.externalSupplier = externalSupplier;
        }

        public String getExternalId() {
            return externalId;
        }

        public void setExternalId(String externalId) {
            this.externalId = externalId;
        }

        public String getContainingExternalIdText() {
            return containingExternalIdText;
        }

        public void setContainingExternalIdText(String text) {
            this.containingExternalIdText = text;
        }

        public ExternalSupplierStatusEnum getExternalStatus() {
            return externalStatus;
        }

        public void
                setExternalStatus(ExternalSupplierStatusEnum externalStatus) {
            this.externalStatus = externalStatus;
        }
    }

    /**
     *
     * @param filter
     * @return
     */
    long getListCount(final ListFilter filter);

    /**
     *
     * @param filter
     * @return
     */
    List<DocLog> getListChunk(ListFilter filter);

    /**
     *
     * @param filter
     * @param startPosition
     * @param maxResults
     * @return
     */
    List<DocLog> getListChunk(ListFilter filter, Integer startPosition,
            Integer maxResults);

    /**
     *
     * @param filter
     * @param startPosition
     * @param maxResults
     * @param orderBy
     * @param sortAscending
     * @return
     */
    List<DocLog> getListChunk(ListFilter filter, Integer startPosition,
            Integer maxResults, Field orderBy, boolean sortAscending);

    /**
     * Reads DocLog from database for user and document uuid.
     *
     * @param userId
     *            The id (primary key) of the user.
     * @param uuid
     *            The uuid of the document.
     *
     * @return The DocLog instance from the database, or null when not found.
     */
    DocLog findByUuid(Long userId, String uuid);

    /**
     * Removes {@link DocLog} instances dating from daysBackInTime and older
     * which DO have a {@link DocOut} association.
     * <p>
     * Note: For each removed {@link DocLog} the associated {@link DocOut}
     * instance and {@link AccountTrx} instances are deleted by cascade.
     * </p>
     *
     * @param dateBackInTime
     *            The date criterion.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @return The number of deleted instances.
     */
    int cleanDocOutHistory(Date dateBackInTime,
            DaoBatchCommitter batchCommitter);

    /**
     * Removes {@link DocLog} instances dating from daysBackInTime and older
     * which DO have a {@link DocIn} association.
     * <p>
     * Note: For each removed {@link DocLog} the associated {@link DocIn}
     * instance and {@link AccountTrx} instances are deleted by cascade.
     * </p>
     *
     * @param dateBackInTime
     *            The date criterion.
     * @param batchCommitter
     *            The {@link DaoBatchCommitter}.
     * @return The number of deleted instances.
     */
    int cleanDocInHistory(Date dateBackInTime,
            DaoBatchCommitter batchCommitter);

    /**
     * Updates a {@link DocLog} instance with new external supplier data, and
     * optionally a new document title.
     *
     * <p>
     * NOTE: Use this method instead of {@link #update(DocLog)}, to make sure
     * updated data are available to other resident threads. Updates committed
     * with {@link #update(DocLog)}, i.e merge(), will <b>not</b> show in other
     * resident threads (this is a Hibernate "feature").
     * </p>
     *
     * @param docLogId
     *            The database primary key of the {@link DocLog} instance.
     * @param extSupplier
     *            The {@link ExternalSupplierEnum}. A {@code null} value is used
     *            as such.
     * @param extStatus
     *            The {@link ExternalSupplierStatusEnum}. A {@code null} value
     *            is used as such.
     * @param documentTitle
     *            The new document title. If {@code null} the document title is
     *            <i>not</i> updated.
     * @return {@code true} when instance is updated, {@code false} when not
     *         found.
     */
    boolean updateExtSupplier(Long docLogId, ExternalSupplierEnum extSupplier,
            ExternalSupplierStatusEnum extStatus, String documentTitle);
}
