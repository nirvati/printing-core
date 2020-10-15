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
package org.savapage.core.job;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dto.PrinterSnmpDto;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.SnmpRetrieveService;
import org.savapage.core.services.helpers.PrinterSnmpReader;
import org.savapage.core.services.helpers.SnmpPrinterQueryDto;
import org.savapage.core.snmp.SnmpClientSession;
import org.savapage.core.snmp.SnmpConnectException;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.JsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterSnmpJob extends AbstractJob {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PrinterSnmpJob.class);

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /** */
    private static final PrinterService PRINTER_SERVICE =
            ServiceContext.getServiceFactory().getPrinterService();

    /** */
    private static final SnmpRetrieveService SNMP_RETRIEVE_SERVICE =
            ServiceContext.getServiceFactory().getSnmpRetrieveService();

    /**
     * Type: Long.
     */
    public static final String ATTR_PRINTER_ID = "printerID";

    /**
     * Type: JSON string of Set.
     */
    public static final String ATTR_HOST_SET = "hostSet";

    /**
     * Checks if execution context is for SNMP retrieve of all printers.
     *
     * @param ctx
     *            The {@link JobExecutionContext}.
     * @return {@code true} when this is a full retrieve. {@code false}, if just
     *         for one (1) printer.
     */
    public static boolean isAllPrinters(final JobExecutionContext ctx) {
        return getPrinterID(ctx) == null && getHosts(ctx) == null;
    }

    /**
     * Gets the printer ID from the execution context.
     *
     * @param ctx
     *            The {@link JobExecutionContext}.
     * @return {@code null} when not present.
     */
    private static Long getPrinterID(final JobExecutionContext ctx) {

        final JobDataMap map = ctx.getJobDetail().getJobDataMap();

        if (map.containsKey(ATTR_PRINTER_ID)) {
            return map.getLong(ATTR_PRINTER_ID);
        }
        return null;
    }

    /**
     * Gets the set of printer host addresses from the execution context.
     *
     * @param ctx
     *            The {@link JobExecutionContext}.
     * @return {@code null} when not present.
     */
    private static Set<String> getHosts(final JobExecutionContext ctx) {

        final JobDataMap map = ctx.getJobDetail().getJobDataMap();

        if (map.containsKey(ATTR_HOST_SET)) {
            try {
                return JsonHelper.createStringSet(map.getString(ATTR_HOST_SET));
            } catch (IOException e) {
                throw new IllegalStateException(e.getMessage());
            }
        }
        return null;
    }

    @Override
    protected void onInterrupt() throws UnableToInterruptJobException {
        // noop
    }

    @Override
    protected void onInit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(true);
    }

    @Override
    protected void onExit(final JobExecutionContext ctx) {
        ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
    }

    @Override
    protected void onExecute(final JobExecutionContext ctx)
            throws JobExecutionException {

        final ConfigManager cm = ConfigManager.instance();

        /*
         * Return if this is a scheduled (not a one-shot) job and printer SNMP
         * is DISABLED.
         */
        if (ctx.getJobDetail().getKey().getGroup()
                .equals(SpJobScheduler.JOB_GROUP_SCHEDULED)
                && !cm.isConfigValue(Key.PRINTER_SNMP_ENABLE)) {
            return;
        }

        final DaoContext daoContext = ServiceContext.getDaoContext();
        final AdminPublisher publisher = AdminPublisher.instance();

        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        int count = 0;

        final List<SnmpPrinterQueryDto> queriesAll =
                PROXY_PRINT_SERVICE.getSnmpQueries();
        final List<SnmpPrinterQueryDto> queries;

        final Long printerID = getPrinterID(ctx);
        final Set<String> printerHosts = getHosts(ctx);

        if (printerID != null) {

            /*
             * Find uriHost.
             */
            String uriHost = null;

            for (final SnmpPrinterQueryDto queryWlk : queriesAll) {
                if (queryWlk.getPrinter().getId().equals(printerID)) {
                    uriHost = queryWlk.getUriHost();
                    break;
                }
            }

            /*
             * Collect uriHost instances (in case of multiple queues for one
             * uriHost).
             */
            queries = new ArrayList<>();

            if (uriHost != null) {
                for (final SnmpPrinterQueryDto queryWlk : queriesAll) {
                    if (queryWlk.getUriHost().equals(uriHost)) {
                        queries.add(queryWlk);
                    }
                }
            }

        } else if (printerHosts != null) {

            queries = new ArrayList<>();

            for (final SnmpPrinterQueryDto queryWlk : queriesAll) {
                if (printerHosts.contains(queryWlk.getUriHost())) {
                    queries.add(queryWlk);
                }
            }

        } else {
            queries = queriesAll;
        }

        if (queries.isEmpty()) {
            publisher.publish(PubTopicEnum.SNMP, PubLevelEnum.WARN,
                    localizeSysMsg("PrinterSnmp.none"));
            return;
        }

        final String msgStart;

        if (queries.size() == 1) {
            msgStart = localizeSysMsg("PrinterSnmp.start.single");
        } else {
            msgStart = localizeSysMsg("PrinterSnmp.start.plural",
                    String.valueOf(queries.size()));
        }
        publisher.publish(PubTopicEnum.SNMP, level, msgStart);

        final PrinterSnmpReader snmpReader = new PrinterSnmpReader(
                cm.getConfigInt(Key.PRINTER_SNMP_READ_RETRIES),
                cm.getConfigInt(Key.PRINTER_SNMP_READ_TIMEOUT_MSECS));

        /*
         * Since there might be multiple queues for a single host, we cache
         * results for easy retrieval.
         */
        final Map<String, PrinterSnmpDto> hostCache = new HashMap<>();

        String hostWlk = null;

        try {

            for (final SnmpPrinterQueryDto query : queries) {

                final String host = query.getUriHost();
                hostWlk = host;

                PrinterSnmpDto dto = null;

                if (hostCache.containsKey(host)) {

                    dto = hostCache.get(host);

                } else {

                    if (printerID == null) {
                        SNMP_RETRIEVE_SERVICE.claimSnmpRetrieve(host);
                    }

                    if (SNMP_RETRIEVE_SERVICE.lockSnmpRetrieve(host)) {

                        try {
                            dto = snmpReader.read(host,
                                    SnmpClientSession.DEFAULT_PORT_READ,
                                    SnmpClientSession.DEFAULT_COMMUNITY);

                        } catch (SnmpConnectException e) {

                            dto = null;

                            msg = AppLogHelper.logWarning(getClass(),
                                    "PrinterSnmp.retrieve.failure",
                                    query.getPrinter().getPrinterName(),
                                    query.getUriHost(), e.getMessage());

                            publisher.publish(PubTopicEnum.SNMP,
                                    PubLevelEnum.WARN, msg);

                        }

                        hostCache.put(host, dto);
                    }
                }

                ServiceContext.resetTransactionDate();

                daoContext.beginTransaction();

                PRINTER_SERVICE.setSnmpInfo(query.getPrinter(), dto);

                daoContext.commit();

                if (dto == null) {
                    continue;
                }

                publisher.publish(PubTopicEnum.SNMP, level,
                        localizeSysMsg("PrinterSnmp.retrieved",
                                query.getPrinter().getPrinterName(),
                                query.getUriHost()));
                count++;
            }

            if (count == 0) {
                msg = AppLogHelper.logWarning(getClass(), "PrinterSnmp.none");
            } else if (count == 1) {
                msg = AppLogHelper.logInfo(getClass(),
                        "PrinterSnmp.success.single");
            } else {
                msg = AppLogHelper.logInfo(getClass(),
                        "PrinterSnmp.success.plural", String.valueOf(count));
            }

            if (count == queries.size()) {
                level = PubLevelEnum.CLEAR;
            } else {
                level = PubLevelEnum.WARN;
            }

        } catch (Exception e) {

            daoContext.rollback();

            LOGGER.error(e.getMessage(), e);

            level = PubLevelEnum.ERROR;

            msg = AppLogHelper.logError(getClass(), "PrinterSnmp.error",
                    String.format("Printer [%s] %s - %s",
                            StringUtils.defaultString(hostWlk, "?"),
                            e.getClass().getSimpleName(), e.getMessage()));

            AppLogHelper.log(AppLogLevelEnum.ERROR, msg);

        } finally {
            for (final String host : hostCache.keySet()) {
                SNMP_RETRIEVE_SERVICE.releaseSnmpRetrieve(host);
            }
        }

        publisher.publish(PubTopicEnum.SNMP, level, msg);
    }

}