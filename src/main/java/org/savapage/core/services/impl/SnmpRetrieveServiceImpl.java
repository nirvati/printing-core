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
package org.savapage.core.services.impl;

import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.jpa.Printer;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.services.SnmpRetrieveService;
import org.savapage.core.util.CupsPrinterUriHelper;
import org.savapage.core.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SnmpRetrieveServiceImpl extends AbstractService
        implements SnmpRetrieveService {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SnmpRetrieveServiceImpl.class);

    /** */
    private enum RetrieveStatus {
        /** */
        CLAIMED,
        /** */
        STARTED
    }

    /**
     * A concurrent map with host addresses with SNMP retrieve status.
     */
    private ConcurrentHashMap<String, RetrieveStatus> activeSnmpHosts;

    @Override
    public void start() {
        this.activeSnmpHosts = new ConcurrentHashMap<>();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public boolean isSnmpRetrieveActive(final String printerHost) {

        return this.activeSnmpHosts.containsKey(printerHost);
    }

    @Override
    public boolean claimSnmpRetrieve(final String printerHost) {
        return this.activeSnmpHosts.putIfAbsent(printerHost,
                RetrieveStatus.CLAIMED) == null;
    }

    @Override
    public boolean lockSnmpRetrieve(final String printerHost) {
        return this.activeSnmpHosts.replace(printerHost, RetrieveStatus.CLAIMED,
                RetrieveStatus.STARTED);
    }

    @Override
    public boolean releaseSnmpRetrieve(final String printerHost) {
        return this.activeSnmpHosts.remove(printerHost) != null;
    }

    @Override
    public void probeSnmpRetrieve(final Set<URI> cupsPrinterUris) {

        final Set<String> hosts = new HashSet<>();

        for (final URI uri : cupsPrinterUris) {

            final String host = CupsPrinterUriHelper.resolveHost(uri);

            if (host == null || this.isSnmpRetrieveActive(host)) {
                continue;
            }

            hosts.add(host);
        }

        if (!hosts.isEmpty()) {
            SpJobScheduler.instance().scheduleOneShotPrinterSnmp(hosts, 0L);
        }
    }

    /**
     * Triggers SNMP retrieval for a printer when time threshold is reached.
     *
     * @param printer
     *            The printer.
     */
    @Override
    public void probeSnmpRetrieveTrigger(final Printer printer) {

        final ConfigManager cm = ConfigManager.instance();

        /*
         * SNMP enabled?
         */
        if (!cm.isConfigValue(Key.PRINTER_SNMP_ENABLE)) {
            return;
        }

        final JsonProxyPrinter proxyPrinter =
                proxyPrintService().getCachedPrinter(printer.getPrinterName());

        final String host =
                CupsPrinterUriHelper.resolveHost(proxyPrinter.getDeviceUri());

        /*
         * SNMP applicable?
         */
        if (host == null) {
            return;
        }

        /*
         * Retrieve active?
         */
        if (this.isSnmpRetrieveActive(host)) {
            return;
        }

        /*
         * Trigger retrieve?
         */
        final int triggerMins =
                cm.getConfigInt(Key.PRINTER_SNMP_READ_TRIGGER_MINS);

        final Date lastDate = printerAttrDAO().getSnmpDate(printer.getId());

        if (lastDate != null && triggerMins > DateUtil.minutesBetween(lastDate,
                printer.getLastUsageDate())) {
            return;
        }

        /*
         * Claim retrieve.
         */
        if (!this.claimSnmpRetrieve(host)) {
            // Oops, someone else just claimed before us.
            return;
        }

        /*
         * Schedule retrieval.
         */
        try {
            SpJobScheduler.instance()
                    .scheduleOneShotPrinterSnmp(printer.getId(), 0L);

        } catch (Exception e) {
            this.releaseSnmpRetrieve(host);
            LOGGER.error("SNMP scheduling for printer {} ({} failed: {}",
                    printer.getPrinterName(), host, e.getMessage());
        }
    }

    @Override
    public void retrieveAll() {
        SpJobScheduler.instance().scheduleOneShotPrinterSnmp(0L);
    }

    @Override
    public void retrieve(final Long printerID) {
        SpJobScheduler.instance().scheduleOneShotPrinterSnmp(printerID, 0L);
    }

}
