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
package org.savapage.core.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.UnableToInterruptJobException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.enums.AppLogLevelEnum;
import org.savapage.core.dto.PrinterSnmpDto;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrinterSnmpReader;
import org.savapage.core.services.helpers.SnmpPrinterQueryDto;
import org.savapage.core.snmp.SnmpClientSession;
import org.savapage.core.snmp.SnmpConnectException;
import org.savapage.core.util.AppLogHelper;
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
    public static final String ATTR_PRINTER_ID = "printerID";

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

        final DaoContext daoContext = ServiceContext.getDaoContext();
        final AdminPublisher publisher = AdminPublisher.instance();

        final ProxyPrintService srvPrint =
                ServiceContext.getServiceFactory().getProxyPrintService();

        final PrinterService srvPrinter =
                ServiceContext.getServiceFactory().getPrinterService();

        String msg = null;
        PubLevelEnum level = PubLevelEnum.INFO;

        int count = 0;

        final List<SnmpPrinterQueryDto> queries;

        final JobDataMap map = ctx.getJobDetail().getJobDataMap();

        if (map.containsKey(ATTR_PRINTER_ID)) {
            queries = new ArrayList<>();
            final SnmpPrinterQueryDto query =
                    srvPrint.getSnmpQuery(map.getLong(ATTR_PRINTER_ID));
            if (query != null) {
                queries.add(query);
            }
        } else {
            queries = srvPrint.getSnmpQueries();
        }

        if (queries.isEmpty()) {
            publisher.publish(PubTopicEnum.SNMP, level,
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

        try {

            // There might be multiple queues for a single host, so we cache
            // results.
            final Map<String, PrinterSnmpDto> hostCache = new HashMap<>();

            for (final SnmpPrinterQueryDto query : queries) {

                final String host = query.getUriHost();

                PrinterSnmpDto dto = hostCache.get(host);

                if (dto == null) {
                    try {
                        dto = PrinterSnmpReader.read(host,
                                SnmpClientSession.DEFAULT_PORT_READ,
                                SnmpClientSession.DEFAULT_COMMUNITY, null);

                        hostCache.put(host, dto);

                    } catch (SnmpConnectException e) {

                        dto = null;

                        msg = AppLogHelper.logError(getClass(),
                                "PrinterSnmp.retrieve.error",
                                query.getPrinter().getPrinterName(),
                                query.getUriHost(), e.getMessage());

                        publisher.publish(PubTopicEnum.SNMP, PubLevelEnum.ERROR,
                                msg);
                    }
                }

                if (dto == null) {
                    continue;
                }

                daoContext.beginTransaction();

                srvPrinter.setSmtpInfo(query.getPrinter(), dto);

                daoContext.commit();

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
                    e.getMessage());

            AppLogHelper.log(AppLogLevelEnum.ERROR, msg);
        }

        publisher.publish(PubTopicEnum.SNMP, level, msg);
    }

}