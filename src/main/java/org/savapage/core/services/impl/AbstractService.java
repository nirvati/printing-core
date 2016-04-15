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
package org.savapage.core.services.impl;

import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.AccountTrxDao;
import org.savapage.core.dao.AccountVoucherDao;
import org.savapage.core.dao.AppLogDao;
import org.savapage.core.dao.DeviceAttrDao;
import org.savapage.core.dao.DeviceDao;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.IppQueueAttrDao;
import org.savapage.core.dao.IppQueueDao;
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
import org.savapage.core.dao.UserGroupAttrDao;
import org.savapage.core.dao.UserGroupDao;
import org.savapage.core.dao.UserGroupMemberDao;
import org.savapage.core.dao.UserNumberDao;
import org.savapage.core.inbox.OutputProducer;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.AccountVoucherService;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.DeviceService;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserGroupService;
import org.savapage.core.services.UserService;
import org.savapage.core.util.Messages;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractService {

    protected static AccessControlService accessControlService() {
        return ServiceContext.getServiceFactory().getAccessControlService();
    }

    protected static AccountingService accountingService() {
        return ServiceContext.getServiceFactory().getAccountingService();
    }

    protected static AccountVoucherService accountVoucherService() {
        return ServiceContext.getServiceFactory().getAccountVoucherService();
    }

    protected static DeviceService deviceService() {
        return ServiceContext.getServiceFactory().getDeviceService();
    }

    protected static DocLogService docLogService() {
        return ServiceContext.getServiceFactory().getDocLogService();
    }

    protected static InboxService inboxService() {
        return ServiceContext.getServiceFactory().getInboxService();
    }

    protected static JobTicketService jobTicketService() {
        return ServiceContext.getServiceFactory().getJobTicketService();
    }

    protected static OutputProducer outputProducer() {
        return OutputProducer.instance();
    }

    protected static OutboxService outboxService() {
        return ServiceContext.getServiceFactory().getOutboxService();
    }

    protected static ProxyPrintService proxyPrintService() {
        return ServiceContext.getServiceFactory().getProxyPrintService();
    }

    protected static PrinterService printerService() {
        return ServiceContext.getServiceFactory().getPrinterService();
    }

    protected static QueueService queueService() {
        return ServiceContext.getServiceFactory().getQueueService();
    }

    protected static UserService userService() {
        return ServiceContext.getServiceFactory().getUserService();
    }

    protected static UserGroupService userGroupService() {
        return ServiceContext.getServiceFactory().getUserGroupService();
    }

    // ---------------------------------------------------------------------

    protected static AccountDao accountDAO() {
        return ServiceContext.getDaoContext().getAccountDao();
    }

    protected static AccountTrxDao accountTrxDAO() {
        return ServiceContext.getDaoContext().getAccountTrxDao();
    }

    protected static AccountVoucherDao accountVoucherDAO() {
        return ServiceContext.getDaoContext().getAccountVoucherDao();
    }

    protected static AppLogDao appLogDAO() {
        return ServiceContext.getDaoContext().getAppLogDao();
    }

    protected static DocLogDao docLogDAO() {
        return ServiceContext.getDaoContext().getDocLogDao();
    }

    protected static DeviceDao deviceDAO() {
        return ServiceContext.getDaoContext().getDeviceDao();
    }

    protected static DeviceAttrDao deviceAttrDAO() {
        return ServiceContext.getDaoContext().getDeviceAttrDao();
    }

    protected static IppQueueAttrDao ippQueueAttrDAO() {
        return ServiceContext.getDaoContext().getIppQueueAttrDao();
    }

    protected static IppQueueDao ippQueueDAO() {
        return ServiceContext.getDaoContext().getIppQueueDao();
    }

    protected static PosPurchaseDao purchaseDAO() {
        return ServiceContext.getDaoContext().getPosPurchaseDao();
    }

    protected static PrinterDao printerDAO() {
        return ServiceContext.getDaoContext().getPrinterDao();
    }

    protected static PrinterAttrDao printerAttrDAO() {
        return ServiceContext.getDaoContext().getPrinterAttrDao();
    }

    protected static PrinterGroupDao printerGroupDAO() {
        return ServiceContext.getDaoContext().getPrinterGroupDao();
    }

    protected static PrinterGroupMemberDao printerGroupMemberDAO() {
        return ServiceContext.getDaoContext().getPrinterGroupMemberDao();
    }

    protected static PrintInDao printInDAO() {
        return ServiceContext.getDaoContext().getPrintInDao();
    }

    protected static PrintOutDao printOutDAO() {
        return ServiceContext.getDaoContext().getPrintOutDao();
    }

    protected static UserDao userDAO() {
        return ServiceContext.getDaoContext().getUserDao();
    }

    protected static UserAccountDao userAccountDAO() {
        return ServiceContext.getDaoContext().getUserAccountDao();
    }

    protected static UserAttrDao userAttrDAO() {
        return ServiceContext.getDaoContext().getUserAttrDao();
    }

    protected static UserCardDao userCardDAO() {
        return ServiceContext.getDaoContext().getUserCardDao();
    }

    protected static UserEmailDao userEmailDAO() {
        return ServiceContext.getDaoContext().getUserEmailDao();
    }

    protected static UserNumberDao userNumberDAO() {
        return ServiceContext.getDaoContext().getUserNumberDao();
    }

    protected static UserGroupDao userGroupDAO() {
        return ServiceContext.getDaoContext().getUserGroupDao();
    }

    protected static UserGroupAttrDao userGroupAttrDAO() {
        return ServiceContext.getDaoContext().getUserGroupAttrDao();
    }

    protected static UserGroupMemberDao userGroupMemberDAO() {
        return ServiceContext.getDaoContext().getUserGroupMemberDao();
    }

    /**
     * Return a localized message string. The locale from the
     * {@link ServiceContext} is used.
     *
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    protected final String localize(final String key, final String... args) {
        return Messages.getMessage(getClass(), ServiceContext.getLocale(), key,
                args);
    }

    /**
     * Creates a {@link JsonRpcMethodError}.
     *
     * @param key
     *            The key of the message. Used in
     *            {@link JsonRpcError#setMessage(String)}.
     * @param args
     *            The arguments of the message.
     * @return
     */
    protected final JsonRpcMethodError createErrorMsg(final String key,
            final String... args) {
        return JsonRpcMethodError.createBasicError(
                JsonRpcError.Code.INVALID_PARAMS, null, localize(key, args));
    }

    /**
     *
     * @param msgKey
     * @param msgArgs
     * @return
     */
    protected final JsonRpcMethodError createError(final String msgKey,
            final String... msgArgs) {
        return JsonRpcMethodError.createBasicError(
                JsonRpcError.Code.INVALID_PARAMS, null,
                localize(msgKey, msgArgs));
    }

    /**
     *
     * @param msgKey
     * @param msgArgs
     * @return
     */
    protected final JsonRpcMethodResult createOkResult(final String msgKey,
            final String... msgArgs) {
        return JsonRpcMethodResult.createOkResult(localize(msgKey, msgArgs));
    }

}
