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
package org.savapage.core.services.impl;

import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.AccountVoucherService;
import org.savapage.core.services.AccountingService;
import org.savapage.core.services.AppLogService;
import org.savapage.core.services.AtomFeedService;
import org.savapage.core.services.ConfigPropertyService;
import org.savapage.core.services.DeviceService;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.DocStoreService;
import org.savapage.core.services.DownloadService;
import org.savapage.core.services.EcoPrintPdfTaskService;
import org.savapage.core.services.EmailService;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.JobTicketService;
import org.savapage.core.services.OutboxService;
import org.savapage.core.services.PGPPublicKeyService;
import org.savapage.core.services.PrintDelegationService;
import org.savapage.core.services.PrinterGroupService;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.RestClientService;
import org.savapage.core.services.RfIdReaderService;
import org.savapage.core.services.SOfficeService;
import org.savapage.core.services.ServiceFactory;
import org.savapage.core.services.SnmpRetrieveService;
import org.savapage.core.services.StatefulService;
import org.savapage.core.services.UserGroupService;
import org.savapage.core.services.UserService;
import org.savapage.ext.papercut.services.PaperCutService;
import org.savapage.ext.papercut.services.impl.PaperCutServiceImpl;
import org.savapage.ext.smartschool.services.SmartschoolProxyService;
import org.savapage.ext.smartschool.services.SmartschoolService;
import org.savapage.ext.smartschool.services.impl.SmartschoolProxyServiceImpl;
import org.savapage.ext.smartschool.services.impl.SmartschoolServiceImpl;

/**
 * Service factory with one (1) SingletonHolder per Service.
 * <p>
 * A SingletonHolder is loaded on first access to one of its public static
 * members, not before. See <a href=
 * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh" >
 * The Singleton solution of Bill Pugh</a>.
 * </p>
 * <p>
 * IMPORTANT: This granularity is needed because Services have instance
 * variables pointing to other Services.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class ServiceFactoryImpl implements ServiceFactory {

    private static class AccessControlServiceHolder {
        public static final AccessControlService SERVICE =
                new AccessControlServiceImpl();
    }

    private static class AccountingServiceHolder {
        public static final AccountingService SERVICE =
                new AccountingServiceImpl();
    }

    private static class AccountVoucherServiceHolder {
        public static final AccountVoucherService SERVICE =
                new AccountVoucherServiceImpl();
    }

    private static class AppLogServiceHolder {
        public static final AppLogService SERVICE = new AppLogServiceImpl();
    }

    private static class AtomFeedServiceHolder {
        public static final AtomFeedService SERVICE = new AtomFeedServiceImpl();
    }

    private static class ConfigPropertyServiceHolder {
        public static final ConfigPropertyService SERVICE =
                new ConfigPropertyServiceImpl();
    }

    private static class DeviceServiceHolder {
        public static final DeviceService SERVICE = new DeviceServiceImpl();
    }

    private static class DocLogServiceHolder {
        public static final DocLogService SERVICE = new DocLogServiceImpl();
    }

    private static class DocStoreServiceHolder {
        public static final DocStoreService SERVICE = new DocStoreServiceImpl();
    }

    private static class DownloadServiceHolder {
        public static final DownloadService SERVICE = new DownloadServiceImpl();
    }

    private static class EcoPrintPdfTaskServiceHolder {
        public static final EcoPrintPdfTaskService SERVICE =
                new EcoPrintPdfTaskServiceImpl();
    }

    private static class EmailServiceHolder {
        public static final EmailService SERVICE = new EmailServiceImpl();
    }

    private static class InboxServiceHolder {
        public static final InboxService SERVICE = new InboxServiceImpl();
    }

    private static class JobTicketServiceHolder {
        public static final JobTicketService SERVICE =
                new JobTicketServiceImpl();
    }

    private static class OutboxServiceHolder {
        public static final OutboxService SERVICE = new OutboxServiceImpl();
    }

    private static class RestClientServiceHolder {
        public static final RestClientService SERVICE =
                new RestClientServiceImpl();
    }

    private static class PaperCutServiceHolder {
        public static final PaperCutService SERVICE = new PaperCutServiceImpl();
    }

    private static class SOfficeServiceHolder {
        public static final SOfficeService SERVICE = new SOfficeServiceImpl();
    }

    private static class UserServiceHolder {
        public static final UserService SERVICE = new UserServiceImpl();
    }

    private static class UserGroupServiceHolder {
        public static final UserGroupService SERVICE =
                new UserGroupServiceImpl();
    }

    private static class RfIdReaderServiceHolder {
        public static final RfIdReaderService SERVICE =
                new RfIdReaderServiceImpl();
    }

    private static class PGPPublicKeyServiceHolder {
        public static final PGPPublicKeyServiceImpl SERVICE =
                new PGPPublicKeyServiceImpl();
    }

    private static class PrintDelegationServiceHolder {
        public static final PrintDelegationService SERVICE =
                new PrintDelegationServiceImpl();
    }

    private static class PrinterGroupServiceHolder {
        public static final PrinterGroupService SERVICE =
                new PrinterGroupServiceImpl();
    }

    private static class PrinterServiceHolder {
        public static final PrinterService SERVICE = new PrinterServiceImpl();

    }

    private static class ProxyPrintServiceHolder {
        public static final ProxyPrintService SERVICE =
                new ProxyPrintServiceImpl();
    }

    private static class QueueServiceHolder {
        public static final QueueService SERVICE = new QueueServiceImpl();
    }

    private static class SmartSchoolServiceHolder {
        public static final SmartschoolService SERVICE =
                new SmartschoolServiceImpl();
    }

    private static class SmartSchoolProxyServiceHolder {
        public static final SmartschoolProxyService SERVICE =
                new SmartschoolProxyServiceImpl();
    }

    private static class SnmpRetrieveServiceHolder {
        public static final SnmpRetrieveService SERVICE =
                new SnmpRetrieveServiceImpl();
    }

    private final static StatefulService statefullServices[] =
            new StatefulService[] { //
                    AtomFeedServiceHolder.SERVICE, //
                    DocStoreServiceHolder.SERVICE, //
                    DownloadServiceHolder.SERVICE, //
                    EcoPrintPdfTaskServiceHolder.SERVICE, //
                    JobTicketServiceHolder.SERVICE, //
                    SOfficeServiceHolder.SERVICE, //
                    EmailServiceHolder.SERVICE, //
                    PaperCutServiceHolder.SERVICE, //
                    RestClientServiceHolder.SERVICE, //
                    SnmpRetrieveServiceHolder.SERVICE //
            };

    @Override
    public AccessControlService getAccessControlService() {
        return AccessControlServiceHolder.SERVICE;
    }

    @Override
    public AccountingService getAccountingService() {
        return AccountingServiceHolder.SERVICE;
    }

    @Override
    public AccountVoucherService getAccountVoucherService() {
        return AccountVoucherServiceHolder.SERVICE;
    }

    @Override
    public ConfigPropertyService getConfigPropertyService() {
        return ConfigPropertyServiceHolder.SERVICE;
    }

    @Override
    public AppLogService getAppLogService() {
        return AppLogServiceHolder.SERVICE;
    }

    @Override
    public AtomFeedService getAtomFeedService() {
        return AtomFeedServiceHolder.SERVICE;
    }

    @Override
    public DeviceService getDeviceService() {
        return DeviceServiceHolder.SERVICE;
    }

    @Override
    public DocLogService getDocLogService() {
        return DocLogServiceHolder.SERVICE;
    }

    @Override
    public DocStoreService getDocStoreService() {
        return DocStoreServiceHolder.SERVICE;
    }

    @Override
    public DownloadService getDownloadService() {
        return DownloadServiceHolder.SERVICE;
    }

    @Override
    public EcoPrintPdfTaskService getEcoPrintPdfTaskService() {
        return EcoPrintPdfTaskServiceHolder.SERVICE;
    }

    @Override
    public EmailService getEmailService() {
        return EmailServiceHolder.SERVICE;
    }

    @Override
    public InboxService getInboxService() {
        return InboxServiceHolder.SERVICE;
    }

    @Override
    public JobTicketService getJobTicketService() {
        return JobTicketServiceHolder.SERVICE;
    }

    @Override
    public OutboxService getOutboxService() {
        return OutboxServiceHolder.SERVICE;
    }

    @Override
    public UserService getUserService() {
        return UserServiceHolder.SERVICE;
    }

    @Override
    public UserGroupService getUserGroupService() {
        return UserGroupServiceHolder.SERVICE;
    }

    @Override
    public RfIdReaderService getRfIdReaderService() {
        return RfIdReaderServiceHolder.SERVICE;
    }

    @Override
    public PaperCutService getPaperCutService() {
        return PaperCutServiceHolder.SERVICE;
    }

    @Override
    public PGPPublicKeyService getPGPPublicKeyService() {
        return PGPPublicKeyServiceHolder.SERVICE;
    }

    @Override
    public PrinterGroupService getPrinterGroupService() {
        return PrinterGroupServiceHolder.SERVICE;
    }

    @Override
    public PrintDelegationService getPrintDelegationService() {
        return PrintDelegationServiceHolder.SERVICE;
    }

    @Override
    public PrinterService getPrinterService() {
        return PrinterServiceHolder.SERVICE;
    }

    @Override
    public ProxyPrintService getProxyPrintService() {
        return ProxyPrintServiceHolder.SERVICE;
    }

    @Override
    public QueueService getQueueService() {
        return QueueServiceHolder.SERVICE;
    }

    @Override
    public RestClientService getRestClientService() {
        return RestClientServiceHolder.SERVICE;
    }

    @Override
    public SmartschoolService getSmartSchoolService() {
        return SmartSchoolServiceHolder.SERVICE;
    }

    @Override
    public SmartschoolProxyService getSmartSchoolProxyService() {
        return SmartSchoolProxyServiceHolder.SERVICE;
    }

    @Override
    public SnmpRetrieveService getSnmpRetrieveService() {
        return SnmpRetrieveServiceHolder.SERVICE;
    }

    @Override
    public SOfficeService getSOfficeService() {
        return SOfficeServiceHolder.SERVICE;
    }

    @Override
    public void start() {
        for (final StatefulService service : statefullServices) {
            service.start();
        }
    }

    @Override
    public void shutdown() {
        for (final StatefulService service : statefullServices) {
            service.shutdown();
        }
    }

}
