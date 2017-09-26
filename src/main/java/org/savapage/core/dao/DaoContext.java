/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.jpa.tools.DbVersionInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface DaoContext {

    /**
     *
     * @return Information about the database.
     */
    DbVersionInfo getDbVersionInfo();

    /**
     * Starts a database transaction.
     */
    void beginTransaction();

    /**
     *
     * @return {@code true} if a database transaction is active.
     */
    boolean isTransactionActive();

    /**
     * Commits the database transaction.
     */
    void commit();

    /**
     * Rolls the transaction back, when active.
     */
    void rollback();

    /**
     *
     */
    void close();

    /**
     *
     * @param chunkSize
     *            The size of the chunk.
     * @return The {@linkDaoBatchCommitter}.
     */
    DaoBatchCommitter createBatchCommitter(int chunkSize);

    /**
     * @return The DAO implementation.
     */
    AccountDao getAccountDao();

    /**
     * @return The DAO implementation.
     */
    AccountTrxDao getAccountTrxDao();

    /**
     * @return The DAO implementation.
     */
    AccountVoucherDao getAccountVoucherDao();

    /**
     * @return The DAO implementation.
     */
    AppLogDao getAppLogDao();

    /**
     * @return The DAO implementation.
     */
    ConfigPropertyDao getConfigPropertyDao();

    /**
     * @return The DAO implementation.
     */
    DocInOutDao getDocInOutDao();

    /**
     * @return The DAO implementation.
     */
    DocLogDao getDocLogDao();

    /**
     * @return The DAO implementation.
     */
    DeviceDao getDeviceDao();

    /**
     * @return The DAO implementation.
     */
    DeviceAttrDao getDeviceAttrDao();

    /**
     * @return The DAO implementation.
     */
    IppQueueDao getIppQueueDao();

    /**
     * @return The DAO implementation.
     */
    IppQueueAttrDao getIppQueueAttrDao();

    /**
     * @return The DAO implementation.
     */
    PdfOutDao getPdfOutDao();

    /**
     * @return The DAO implementation.
     */
    PosPurchaseDao getPosPurchaseDao();

    /**
     * @return The DAO implementation.
     */
    PrinterDao getPrinterDao();

    /**
     * @return The DAO implementation.
     */
    PrinterAttrDao getPrinterAttrDao();

    /**
     * @return The DAO implementation.
     */
    PrinterGroupDao getPrinterGroupDao();

    /**
     * @return The DAO implementation.
     */
    PrintInDao getPrintInDao();

    /**
     * @return The DAO implementation.
     */
    PrinterGroupMemberDao getPrinterGroupMemberDao();

    /**
     * @return The DAO implementation.
     */
    PrintOutDao getPrintOutDao();

    /**
     * @return The DAO implementation.
     */
    UserAccountDao getUserAccountDao();

    /**
     * @return The DAO implementation.
     */
    UserAttrDao getUserAttrDao();

    /**
     * @return The DAO implementation.
     */
    UserCardDao getUserCardDao();

    /**
     * @return The DAO implementation.
     */
    UserDao getUserDao();

    /**
     * @return The DAO implementation.
     */
    UserEmailDao getUserEmailDao();

    /**
     * @return The DAO implementation.
     */
    UserGroupDao getUserGroupDao();

    /**
     * @return The DAO implementation.
     */
    UserGroupAccountDao getUserGroupAccountDao();

    /**
     * @return The DAO implementation.
     */
    UserGroupAttrDao getUserGroupAttrDao();

    /**
     * @return The DAO implementation.
     */
    UserGroupMemberDao getUserGroupMemberDao();

    /**
     * @return The DAO implementation.
     */
    UserNumberDao getUserNumberDao();

}
