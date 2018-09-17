/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.core.dao.impl;

import javax.persistence.EntityManager;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.AccountVoucherDao;
import org.savapage.core.dao.AppLogDao;
import org.savapage.core.dao.ConfigPropertyDao;
import org.savapage.core.dao.CostChangeDao;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.DeviceAttrDao;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.DocInDao;
import org.savapage.core.dao.DocInOutDao;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.DocOutDao;
import org.savapage.core.dao.IppQueueAttrDao;
import org.savapage.core.dao.IppQueueDao;
import org.savapage.core.dao.PdfOutDao;
import org.savapage.core.dao.PosPurchaseDao;
import org.savapage.core.dao.PrintInDao;
import org.savapage.core.dao.PrintOutDao;
import org.savapage.core.dao.PrinterAttrDao;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.PrinterGroupDao;
import org.savapage.core.dao.PrinterGroupMemberDao;
import org.savapage.core.dao.UserAccountDao;
import org.savapage.core.dao.UserAttrDao;
import org.savapage.core.dao.UserCardDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.UserEmailDao;
import org.savapage.core.dao.UserGroupAccountDao;
import org.savapage.core.dao.UserGroupAttrDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.UserNumberDao;
import org.savapage.core.dao.helpers.DaoBatchCommitter;
import org.savapage.core.jpa.tools.DbVersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DaoContext} with one (1) SingletonHolder for all DAO's.
 * <p>
 * A SingletonHolder is loaded on first access to one of its public static
 * members, not before. See <a href=
 * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh" >
 * The Singleton solution of Bill Pugh</a>.
 * </p>
 * <p>
 * IMPORTANT: This global granularity can be applied because DAO's are singular
 * and do not point to other DOA's.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class DaoContextImpl implements DaoContext {

    /**
     * The number of open DaoContexts.
     */
    private static int openCount = 0;

    /**
     * @return The number of open {@link DaoContext} objects.
     */
    public static int getOpenCount() {
        synchronized (DaoContextImpl.class) {
            return openCount;
        }
    }

    private static void incrementOpenCount() {
        synchronized (DaoContextImpl.class) {
            openCount++;
        }
    }

    private static void decrementOpenCount() {
        synchronized (DaoContextImpl.class) {
            openCount--;
        }
    }

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(DaoContextImpl.class);

    /**
     * The JPA entity manager.
     */
    private EntityManager em;

    /**
     *
     */
    private static class DaoHolder {

        /**  */
        public static final AccountDao ACCOUNT_DAO = new AccountDaoImpl();

        /**  */
        public static final AccountTrxDao ACCOUNT_TRX_DAO =
                new AccountTrxDaoImpl();

        /**  */
        public static final AccountVoucherDao ACCOUNT_VOUCHER_DAO =
                new AccountVoucherDaoImpl();

        /**  */
        public static final AppLogDao APP_LOG_DAO = new AppLogDaoImpl();

        /**  */
        public static final ConfigPropertyDao CONFIG_PROPERTY_DAO =
                new ConfigPropertyDaoImpl();

        /**  */
        public static final CostChangeDao COST_CHANGE_DAO =
                new CostChangeDaoImpl();

        /**  */
        public static final DocInDao DOC_IN_DAO = new DocInDaoImpl();

        /**  */
        public static final DocInOutDao DOC_IN_OUT_DAO = new DocInOutDaoImpl();

        /**  */
        public static final DocLogDao DOC_LOG_DAO = new DocLogDaoImpl();

        /**  */
        public static final DocOutDao DOC_OUT_DAO = new DocOutDaoImpl();

        /**  */
        public static final DeviceDao DEVICE_DAO = new DeviceDaoImpl();

        /**  */
        public static final DeviceAttrDao DEVICE_ATTR_DAO =
                new DeviceAttrDaoImpl();

        /**  */
        public static final IppQueueDao IPP_QUEUE_DAO = new IppQueueDaoImpl();

        /**  */
        public static final IppQueueAttrDao IPP_QUEUE_ATTR_DAO =
                new IppQueueAttrDaoImpl();

        /**  */
        public static final PrinterDao PRINTER_DAO = new PrinterDaoImpl();

        /**  */
        public static final PrinterAttrDao PRINTER_ATTR_DAO =
                new PrinterAttrDaoImpl();

        /**  */
        public static final PrinterGroupMemberDao PRINTER_GROUP_MEMBER_DAO =
                new PrinterGroupMemberDaoImpl();

        /**  */
        public static final PrinterGroupDao PRINTER_GROUP_DAO =
                new PrinterGroupDaoImpl();

        /**  */
        public static final PrintInDao PRINT_IN_DAO = new PrintInDaoImpl();

        /**  */
        public static final PrintOutDao PRINT_OUT_DAO = new PrintOutDaoImpl();

        /**  */
        public static final PdfOutDao PDF_OUT_DAO = new PdfOutDaoImpl();

        /**  */
        public static final PosPurchaseDao POS_PURCHASE_DAO =
                new PosPurchaseDaoImpl();

        /**  */
        public static final UserAccountDao USER_ACCOUNT_DAO =
                new UserAccountDaoImpl();

        /**  */
        public static final UserAttrDao USER_ATTR_DAO = new UserAttrDaoImpl();

        /**  */
        public static final UserCardDao USER_CARD_DAO = new UserCardDaoImpl();

        /**  */
        public static final UserDao USER_DAO = new UserDaoImpl();

        /**  */
        public static final UserEmailDao USER_EMAIL_DAO =
                new UserEmailDaoImpl();

        /**  */
        public static final UserGroupDao USER_GROUP_DAO =
                new UserGroupDaoImpl();

        /**  */
        public static final UserGroupAccountDao USER_GROUP_ACCOUNT_DAO =
                new UserGroupAccountDaoImpl();

        /**  */
        public static final UserGroupAttrDao USER_GROUP_ATTR_DAO =
                new UserGroupAttrDaoImpl();

        /**  */
        public static final UserGroupMemberDao USER_GROUP_MEMBER_DAO =
                new UserGroupMemberDaoImpl();

        /**  */
        public static final UserNumberDao USER_NUMBER_DAO =
                new UserNumberDaoImpl();
    }

    /**
     *
     */
    private static final ThreadLocal<DaoContextImpl> DAO_CONTEXT =
            new ThreadLocal<DaoContextImpl>() {

                @Override
                protected DaoContextImpl initialValue() {
                    incrementOpenCount();
                    LOGGER.trace("  initialValue()");
                    return new DaoContextImpl();
                }

                @Override
                public void remove() {
                    LOGGER.trace("  remove()");
                    super.remove();
                    decrementOpenCount();
                }
            };

    /**
     * Lazy instantiation of the (@link {@link ThreadLocal} singleton.
     *
     * @return The singleton.
     */
    public static DaoContextImpl instance() {
        return DAO_CONTEXT.get();
    }

    @Override
    public AccountDao getAccountDao() {
        return DaoHolder.ACCOUNT_DAO;
    }

    @Override
    public AccountTrxDao getAccountTrxDao() {
        return DaoHolder.ACCOUNT_TRX_DAO;
    }

    @Override
    public AccountVoucherDao getAccountVoucherDao() {
        return DaoHolder.ACCOUNT_VOUCHER_DAO;
    }

    @Override
    public AppLogDao getAppLogDao() {
        return DaoHolder.APP_LOG_DAO;
    }

    @Override
    public ConfigPropertyDao getConfigPropertyDao() {
        return DaoHolder.CONFIG_PROPERTY_DAO;
    }

    @Override
    public CostChangeDao getCostChangeDao() {
        return DaoHolder.COST_CHANGE_DAO;
    }

    @Override
    public DocInDao getDocInDao() {
        return DaoHolder.DOC_IN_DAO;
    }

    @Override
    public DocInOutDao getDocInOutDao() {
        return DaoHolder.DOC_IN_OUT_DAO;
    }

    @Override
    public DocLogDao getDocLogDao() {
        return DaoHolder.DOC_LOG_DAO;
    }

    @Override
    public DocOutDao getDocOutDao() {
        return DaoHolder.DOC_OUT_DAO;
    }

    @Override
    public DeviceDao getDeviceDao() {
        return DaoHolder.DEVICE_DAO;
    }

    @Override
    public DeviceAttrDao getDeviceAttrDao() {
        return DaoHolder.DEVICE_ATTR_DAO;
    }

    @Override
    public IppQueueDao getIppQueueDao() {
        return DaoHolder.IPP_QUEUE_DAO;
    }

    @Override
    public IppQueueAttrDao getIppQueueAttrDao() {
        return DaoHolder.IPP_QUEUE_ATTR_DAO;
    }

    @Override
    public PrinterDao getPrinterDao() {
        return DaoHolder.PRINTER_DAO;
    }

    @Override
    public PrinterAttrDao getPrinterAttrDao() {
        return DaoHolder.PRINTER_ATTR_DAO;
    }

    @Override
    public PrinterGroupMemberDao getPrinterGroupMemberDao() {
        return DaoHolder.PRINTER_GROUP_MEMBER_DAO;
    }

    @Override
    public PrinterGroupDao getPrinterGroupDao() {
        return DaoHolder.PRINTER_GROUP_DAO;
    }

    @Override
    public PrintInDao getPrintInDao() {
        return DaoHolder.PRINT_IN_DAO;
    }

    @Override
    public PrintOutDao getPrintOutDao() {
        return DaoHolder.PRINT_OUT_DAO;
    }

    @Override
    public PdfOutDao getPdfOutDao() {
        return DaoHolder.PDF_OUT_DAO;
    }

    @Override
    public PosPurchaseDao getPosPurchaseDao() {
        return DaoHolder.POS_PURCHASE_DAO;
    }

    @Override
    public UserAccountDao getUserAccountDao() {
        return DaoHolder.USER_ACCOUNT_DAO;
    }

    @Override
    public UserAttrDao getUserAttrDao() {
        return DaoHolder.USER_ATTR_DAO;
    }

    @Override
    public UserCardDao getUserCardDao() {
        return DaoHolder.USER_CARD_DAO;
    }

    @Override
    public UserDao getUserDao() {
        return DaoHolder.USER_DAO;
    }

    @Override
    public UserEmailDao getUserEmailDao() {
        return DaoHolder.USER_EMAIL_DAO;
    }

    @Override
    public UserGroupDao getUserGroupDao() {
        return DaoHolder.USER_GROUP_DAO;
    }

    @Override
    public UserGroupAccountDao getUserGroupAccountDao() {
        return DaoHolder.USER_GROUP_ACCOUNT_DAO;
    }

    @Override
    public UserGroupAttrDao getUserGroupAttrDao() {
        return DaoHolder.USER_GROUP_ATTR_DAO;
    }

    @Override
    public UserGroupMemberDao getUserGroupMemberDao() {
        return DaoHolder.USER_GROUP_MEMBER_DAO;
    }

    @Override
    public UserNumberDao getUserNumberDao() {
        return DaoHolder.USER_NUMBER_DAO;
    }

    /**
     * @deprecated Gets the {@link ThreadLocal} {@link EntityManager}: a facade
     *             for the "internal" {@link #lazyEntityManager()}.
     *             <p>
     *             This is a transitional solution till all services use the
     *             full Dao setup.
     *             </p>
     * @return The {@link EntityManager}.
     */
    @Deprecated
    public static EntityManager peekEntityManager() {
        return lazyEntityManager();
    }

    /**
     * Returns the {@link ThreadLocal} {@link EntityManager}, or (when not set)
     * creates lazy using {@link ConfigManager#getEntityManagerFactory()}.
     *
     * @return The {@link EntityManager}.
     */
    public static EntityManager lazyEntityManager() {
        if (instance().em == null) {
            instance().em = ConfigManager.instance().getEntityManagerFactory()
                    .createEntityManager();
        }
        return instance().em;
    }

    @Override
    public void clear() {
        lazyEntityManager().clear();
    }

    @Override
    public void close() {
        if (em != null && em.isOpen()) {
            em.close();
        }
        em = null;

        /*
         * NOTE: this is crucial, since it will clean-up.
         */
        DAO_CONTEXT.remove();
    }

    @Override
    public void beginTransaction() {
        lazyEntityManager().getTransaction().begin();
    }

    @Override
    public boolean isTransactionActive() {
        return em != null && em.getTransaction().isActive();
    }

    @Override
    public void commit() {

        if (isTransactionActive()) {
            em.getTransaction().commit();
        }
    }

    @Override
    public void commitInBetween() {
        if (this.isTransactionActive()) {
            this.commit();
            this.beginTransaction();
        }
    }

    @Override
    public void rollback() {
        if (isTransactionActive()) {
            em.getTransaction().rollback();
        }
    }

    @Override
    public DaoBatchCommitter createBatchCommitter(final int chunkSize) {
        return new DaoBatchCommitterImpl(this, chunkSize);
    }

    @Override
    public DbVersionInfo getDbVersionInfo() {
        return new DbVersionInfo(lazyEntityManager());
    }

}
