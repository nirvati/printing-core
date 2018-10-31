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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.savapage.core.SpException;
import org.savapage.core.dao.PrinterDao;
import org.savapage.core.dao.enums.AccessControlScopeEnum;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.dao.enums.ProxyPrintAuthModeEnum;
import org.savapage.core.dao.enums.ProxyPrinterSuppliesEnum;
import org.savapage.core.dao.helpers.JsonUserGroupAccess;
import org.savapage.core.dao.helpers.ProxyPrinterSnmpInfoDto;
import org.savapage.core.dto.IppMediaSourceCostDto;
import org.savapage.core.dto.PrinterSnmpDto;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterAttr;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.PrinterGroupMember;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.jpa.UserGroupMember;
import org.savapage.core.json.JsonRollingTimeSeries;
import org.savapage.core.json.TimeSeriesInterval;
import org.savapage.core.json.rpc.AbstractJsonRpcMessage;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcError.Code;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.PrinterAttrLookup;
import org.savapage.core.snmp.SnmpPrtMarkerColorantEntry;
import org.savapage.core.snmp.SnmpPrtMarkerColorantValueEnum;
import org.savapage.core.snmp.SnmpPrtMarkerSuppliesClassEnum;
import org.savapage.core.snmp.SnmpPrtMarkerSuppliesEntry;
import org.savapage.core.snmp.SnmpPrtMarkerSuppliesTypeEnum;
import org.savapage.core.util.JsonHelper;
import org.savapage.core.util.NumberUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterServiceImpl extends AbstractService
        implements PrinterService {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PrinterServiceImpl.class);

    /** */
    private static final boolean ACCESS_ALLOWED = true;

    /** */
    private static final boolean ACCESS_DENIED = !ACCESS_ALLOWED;

    @Override
    public boolean isInternalPrinter(final Printer printer) {

        final PrinterAttr attr = printerAttrDAO().findByName(printer.getId(),
                PrinterAttrEnum.ACCESS_INTERNAL);
        return printerAttrDAO().getBooleanValue(attr);
    }

    @Override
    public boolean isJobTicketPrinter(final Printer printer) {
        final PrinterAttr attr = printerAttrDAO().findByName(printer.getId(),
                PrinterAttrEnum.JOBTICKET_ENABLE);
        return printerAttrDAO().getBooleanValue(attr);
    }

    @Override
    public boolean canPrinterBeUsed(final Printer printer) {
        return !(printer.getDeleted() || printer.getDisabled());
    }

    @Override
    public boolean checkPrinterSecurity(final Printer printer,
            final MutableBoolean terminalSecured,
            final MutableBoolean readerSecured) {

        return this.checkPrinterSecurity(printer, terminalSecured,
                readerSecured, null, null);
    }

    @Override
    public boolean checkPrinterSecurity(final Printer printer,
            final MutableBoolean terminalSecured,
            final MutableBoolean readerSecured,
            final Map<String, Device> terminalDevices,
            final Map<String, Device> readerDevices) {

        boolean isReaderSecured = false;
        boolean isTerminalSecured = false;

        /*
         * Check associated Card Reader devices which act as Print
         * Authenticator.
         *
         * (1) Try via Devices.
         *
         * NOTE: Check for devices size() GT zero, cause Hibernate throws
         * exception when iterating an empty List (?). Don't know for sure...
         */
        final List<Device> devices = printer.getDevices();

        if (devices != null && !devices.isEmpty()) {

            for (final Device device : devices) {

                if (device.getDisabled()) {
                    continue;
                }

                if (deviceDAO().isTerminal(device)) {
                    isTerminalSecured = true;
                    if (terminalDevices != null) {
                        terminalDevices.put(device.getDeviceName(), device);
                    }
                } else if (deviceDAO().isCardReader(device)) {
                    isReaderSecured = true;
                    if (readerDevices != null) {
                        readerDevices.put(device.getDeviceName(), device);
                    }
                }
            }
        }

        if (terminalDevices == null && readerDevices == null && isReaderSecured
                && isTerminalSecured) {
            /*
             * We found we were looking for: no further code intended.
             */

        } else if (printer.getPrinterGroupMembers() != null) {
            /*
             * Try via PrinterGroupMembers.
             */
            for (final PrinterGroupMember member : printer
                    .getPrinterGroupMembers()) {

                final List<Device> devicesWlk = member.getGroup().getDevices();

                if (devicesWlk != null) {

                    for (final Device device : devicesWlk) {

                        if (device.getDisabled()) {
                            continue;
                        }

                        if (deviceDAO().isTerminal(device)) {
                            isTerminalSecured = true;
                            if (terminalDevices != null) {
                                terminalDevices.put(device.getDeviceName(),
                                        device);
                            }
                        } else if (deviceDAO().isCardReader(device)) {
                            isReaderSecured = true;
                            if (readerDevices != null) {
                                readerDevices.put(device.getDeviceName(),
                                        device);
                            }
                        }
                    }
                }
            }
        }

        readerSecured.setValue(isReaderSecured);
        terminalSecured.setValue(isTerminalSecured);

        return isReaderSecured || isTerminalSecured;
    }

    @Override
    public boolean checkDeviceSecurity(final Printer printer,
            final DeviceTypeEnum deviceType, final Device device) {

        /*
         * Right type?
         */
        boolean exception = false;

        switch (deviceType) {
        case CARD_READER:
            exception = !deviceDAO().isCardReader(device);
            break;
        case TERMINAL:
            exception = !deviceDAO().isTerminal(device);
            break;
        default:
            throw new SpException(
                    "Device Type [" + deviceType + "] not supported.");
        }

        if (exception) {
            throw new SpException("Device [" + device.getDisplayName()
                    + "] is not of type [" + deviceType + "]");
        }

        /*
         *
         */
        boolean isSecured = false;

        final Printer terminalPrinter = device.getPrinter();

        if (terminalPrinter != null) {

            isSecured = terminalPrinter.getId().equals(printer.getId());

        } else {

            final PrinterGroup terminalPrinterGroup = device.getPrinterGroup();

            if (terminalPrinterGroup != null
                    && isPrinterGroupMember(terminalPrinterGroup, printer)) {
                isSecured = true;
            }

        }

        return isSecured;

    }

    @Override
    public void setLogicalDeleted(final Printer printer) {

        final Date deletedDate = ServiceContext.getTransactionDate();

        printer.setDeleted(true);

        printer.setModifiedBy(ServiceContext.getActor());

        printer.setDeletedDate(deletedDate);
        printer.setModifiedDate(deletedDate);
    }

    @Override
    public void undoLogicalDeleted(final Printer printer) {

        printer.setDeleted(false);
        printer.setDeletedDate(null);
    }

    @Override
    public PrinterAttr removeAttribute(final Printer printer,
            final PrinterAttrEnum name) {

        final List<PrinterAttr> attributes = printer.getAttributes();

        if (attributes != null) {

            final String dbName = name.getDbName();

            final Iterator<PrinterAttr> iter = attributes.iterator();
            while (iter.hasNext()) {
                final PrinterAttr attr = iter.next();
                if (attr.getName().equals(dbName)) {
                    iter.remove();
                    return attr;
                }
            }
        }
        return null;
    }

    @Override
    public PrinterAttr getAttribute(final Printer printer,
            final PrinterAttrEnum name) {

        final List<PrinterAttr> attributes = printer.getAttributes();

        if (attributes != null) {

            final String dbName = name.getDbName();

            for (final PrinterAttr attr : attributes) {
                if (attr.getName().equals(dbName)) {
                    return attr;
                }
            }
        }
        return null;
    }

    @Override
    public String getAttributeValue(final Printer printer,
            final PrinterAttrEnum name) {

        final PrinterAttr attr = this.getAttribute(printer, name);

        if (attr != null) {
            return attr.getValue();
        }
        return null;
    }

    @Override
    public Set<String> getJobSheetsMediaSources(final Printer printer) {

        final PrinterAttrEnum name = PrinterAttrEnum.JOB_SHEETS_MEDIA_SOURCES;

        try {
            final String jsonJobSheetSources =
                    this.getAttributeValue(printer, name);

            if (StringUtils.isNotBlank(jsonJobSheetSources)) {
                return JsonHelper.createStringSet(jsonJobSheetSources);
            }

        } catch (IOException e) {
            LOGGER.warn("Printer [{}] attribute [{}] : {}",
                    printer.getPrinterName(), name.getDbName(), e.getMessage());
        }
        return null;
    }

    @Override
    public String getPrintColorModeDefault(final Printer printer) {

        final List<PrinterAttr> attributes = printer.getAttributes();

        if (attributes != null) {

            final String attrPrintColorModeDefault = PrinterDao.IppKeywordAttr
                    .getKey(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE_DFLT);

            for (final PrinterAttr printerAttr : attributes) {

                if (printerAttr.getName().equals(attrPrintColorModeDefault)) {
                    return printerAttr.getValue();
                }
            }
        }
        return null;
    }

    @Override
    public boolean isClientSideMonochrome(final Printer printer) {
        return isPrinterAttr(printer, PrinterAttrEnum.CLIENT_SIDE_MONOCHROME,
                false);
    }

    @Override
    public String getPrinterAttrValue(final Printer printer,
            final PrinterAttrEnum attr) {

        final List<PrinterAttr> attributes = printer.getAttributes();

        if (attributes != null) {
            final String targetName = attr.getDbName();
            for (final PrinterAttr printerAttr : attributes) {
                if (printerAttr.getName().equals(targetName)) {
                    return printerAttr.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Checks boolean value of printer attribute.
     *
     * @param printer
     *            The printer.
     * @param attr
     *            The attribute.
     * @param defaultValue
     *            The default value when printer attribute is not present.
     * @return Printer attribute value.
     */
    private boolean isPrinterAttr(final Printer printer,
            final PrinterAttrEnum attr, final boolean defaultValue) {
        final String value = this.getPrinterAttrValue(printer, attr);
        if (value != null) {
            return value.equals(Boolean.TRUE.toString());
        }
        return defaultValue;
    }

    @Override
    public boolean isClientSideBooklet(final Printer printer) {
        return proxyPrintService().getCachedPrinter(printer.getPrinterName())
                .isBookletClientSide();
    }

    @Override
    public void addJobTotals(final Printer printer, final Date jobDate,
            final int jobPages, final int jobSheets, final long jobEsu,
            final long jobBytes) {

        printer.setTotalJobs(printer.getTotalJobs().intValue() + 1);

        printer.setTotalPages(printer.getTotalPages().intValue() + jobPages);
        printer.setTotalSheets(printer.getTotalSheets().intValue() + jobSheets);
        printer.setTotalEsu(printer.getTotalEsu().longValue() + jobEsu);
        printer.setTotalBytes(printer.getTotalBytes().longValue() + jobBytes);

        printer.setLastUsageDate(jobDate);

    }

    @Override
    public boolean isPrinterGroupMember(final PrinterGroup group,
            final Printer printer) {

        for (final PrinterGroupMember member : group.getMembers()) {
            if (member.getPrinter().getId().equals(printer.getId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates or updates a {@link PrinterAttr} time series data {@link Integer}
     * point.
     *
     * @param printer
     *            The {@link Printer}.
     * @param name
     *            The {@link PrinterAttrEnum}.
     * @param observationTime
     *            The observation time.
     * @param observation
     *            The observation value.
     */
    private void addTimeSeriesDataPoint(final Printer printer,
            final PrinterAttrEnum name, final Date observationTime,
            final Integer observation) {

        JsonRollingTimeSeries<Integer> statsPages = null;

        if (name == PrinterAttrEnum.PRINT_OUT_ROLLING_DAY_PAGES
                || name == PrinterAttrEnum.PRINT_OUT_ROLLING_DAY_ESU
                || name == PrinterAttrEnum.PRINT_OUT_ROLLING_DAY_SHEETS) {
            statsPages = new JsonRollingTimeSeries<>(TimeSeriesInterval.DAY,
                    MAX_TIME_SERIES_INTERVALS_DAYS, 0);
        } else {
            throw new SpException("time series for attribute [" + name
                    + "] is not supported");
        }

        final PrinterAttr attr =
                printerAttrDAO().findByName(printer.getId(), name);

        String json = null;

        if (attr != null) {
            json = attr.getValue();
        }

        try {
            if (StringUtils.isNotBlank(json)) {
                statsPages.init(json);
            }

            statsPages.addDataPoint(observationTime, observation);
            setPrinterAttrValue(attr, printer, name, statsPages.stringify());

        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Creates or updates a {@link PrinterAttr} value to the database.
     *
     * @param printer
     *            The {@link Printer}.
     * @param printerAttr
     *            The {@link PrinterAttr}. When {@code null} the attribute is
     *            created.
     * @param attrEnum
     *            The {@link PrinterAttrEnum} (used when {@link PrinterAttr} is
     *            {@code null}).
     * @param attrValue
     *            The attribute value (used when {@link PrinterAttr} is
     *            {@code null}).
     */
    private void setPrinterAttrValue(final PrinterAttr printerAttr,
            final Printer printer, final PrinterAttrEnum attrEnum,
            final String attrValue) {

        if (printerAttr == null) {

            final PrinterAttr attrNew = new PrinterAttr();

            attrNew.setPrinter(printer);
            attrNew.setName(attrEnum.getDbName());
            attrNew.setValue(attrValue);

            printerAttrDAO().create(attrNew);

        } else {
            printerAttr.setValue(attrValue);
            printerAttrDAO().update(printerAttr);
        }
    }

    @Override
    public void logPrintOut(final Printer printer, final Date jobTime,
            final Integer jobPages, final Integer jobSheets,
            final Long jobEsu) {

        addTimeSeriesDataPoint(printer,
                PrinterAttrEnum.PRINT_OUT_ROLLING_DAY_PAGES, jobTime, jobPages);
        addTimeSeriesDataPoint(printer,
                PrinterAttrEnum.PRINT_OUT_ROLLING_DAY_SHEETS, jobTime,
                jobSheets);
        addTimeSeriesDataPoint(printer,
                PrinterAttrEnum.PRINT_OUT_ROLLING_DAY_ESU, jobTime, jobSheets);

        snmpRetrieveService().probeSnmpRetrieveTrigger(printer);
    }

    @Override
    public AbstractJsonRpcMethodResponse addAccessControl(
            final AccessControlScopeEnum scope, final String printerName,
            final String groupName) throws IOException {

        final Printer printer = printerDAO().findByName(printerName);

        /*
         * INVARIANT: printer MUST exist.
         */
        if (printer == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Printer [" + printerName + "] does not exist.", null);
        }

        final UserGroup userGroup = userGroupDAO().findByName(groupName);

        /*
         * INVARIANT: group MUST exist.
         */
        if (userGroup == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Group [" + groupName + "] does not exist.", null);
        }

        /*
         * Get the attribute to update.
         */
        PrinterAttr printerAttr = printerAttrDAO().findByName(printer.getId(),
                PrinterAttrEnum.ACCESS_USER_GROUPS);

        final boolean isNewAttr = printerAttr == null;

        /*
         * The attribute value as POJO.
         */
        JsonUserGroupAccess groupAccess = null;

        if (isNewAttr) {

            printerAttr = new PrinterAttr();
            printerAttr.setPrinter(printer);
            printerAttr.setName(PrinterAttrEnum.ACCESS_USER_GROUPS.getDbName());

        } else {

            final String json = this.getAttributeValue(printer,
                    PrinterAttrEnum.ACCESS_USER_GROUPS);

            if (json != null) {
                groupAccess = JsonHelper.createOrNull(JsonUserGroupAccess.class,
                        json);
            }
        }

        final boolean initGroups;

        if (groupAccess == null) {

            groupAccess = new JsonUserGroupAccess();
            groupAccess.setScope(scope);

            initGroups = true;

        } else {

            if (groupAccess.getScope() == scope) {
                initGroups = false;

                /*
                 * INVARIANT: group MUST not already be part of the list.
                 */
                for (final String group : groupAccess.getGroups()) {
                    if (group.equals(groupName)) {
                        return JsonRpcMethodError.createBasicError(
                                Code.INVALID_REQUEST,
                                "Group [" + groupName + "] is already part of "
                                        + "printer [" + printerName
                                        + "] access list.",
                                null);
                    }
                }
                groupAccess.getGroups().add(groupName);

            } else {
                groupAccess.setScope(scope);
                initGroups = true;
            }
        }

        if (initGroups) {

            final ArrayList<String> groups = new ArrayList<>();
            groups.add(groupName);

            groupAccess.setGroups(groups);
        }

        printerAttr.setValue(groupAccess.stringify());

        if (isNewAttr) {
            printerAttrDAO().create(printerAttr);
        } else {
            printerAttrDAO().update(printerAttr);
        }

        return JsonRpcMethodResult.createOkResult();
    }

    @Override
    public AbstractJsonRpcMessage removeAccessControl(final String printerName,
            final String groupName) throws IOException {

        final Printer printer = printerDAO().findByName(printerName);

        /*
         * INVARIANT: printer MUST exist.
         */
        if (printer == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Printer [" + printerName + "] does not exist.", null);
        }

        /*
         * Get the attribute to update.
         */
        final PrinterAttr printerAttr = printerAttrDAO().findByName(
                printer.getId(), PrinterAttrEnum.ACCESS_USER_GROUPS);

        /*
         * INVARIANT: User group MUST not already be present on the list.
         */
        boolean isGroupRemoved = false;

        if (printerAttr != null) {

            final JsonUserGroupAccess groupAccess = JsonHelper.createOrNull(
                    JsonUserGroupAccess.class, printerAttr.getValue());

            if (groupAccess == null) {

                /*
                 * Pretend removal and auto-clean after JSON syntax error.
                 */
                isGroupRemoved = true;
                printerAttrDAO().delete(printerAttr);

            } else {

                final Iterator<String> iter =
                        groupAccess.getGroups().iterator();

                while (iter.hasNext()) {

                    final String group = iter.next();

                    if (group.equals(groupName)) {
                        isGroupRemoved = true;
                        iter.remove();
                        break;
                    }
                }

                if (isGroupRemoved) {
                    if (groupAccess.getGroups().isEmpty()) {
                        printerAttrDAO().delete(printerAttr);
                    } else {
                        printerAttr.setValue(groupAccess.stringify());
                        printerAttrDAO().update(printerAttr);
                    }
                }

            }
        }

        if (!isGroupRemoved) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Group [" + groupName + "] is not part of " + "printer ["
                            + printerName + "] access list.",
                    null);
        }

        return JsonRpcMethodResult.createOkResult();
    }

    @Override
    public AbstractJsonRpcMessage
            removeAccessControl(final String printerName) {

        final Printer printer = printerDAO().findByName(printerName);

        /*
         * INVARIANT: printer MUST exist.
         */
        if (printer == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Printer [" + printerName + "] does not exist.", null);
        }

        /*
         * INVARIANT: printer attribute MUST exist.
         */
        final PrinterAttr printerAttr = printerAttrDAO().findByName(
                printer.getId(), PrinterAttrEnum.ACCESS_USER_GROUPS);

        if (printerAttr == null) {
            return JsonRpcMethodError.createBasicError(Code.INVALID_REQUEST,
                    "Access control not found for printer [" + printerName
                            + "].",
                    null);
        }

        printerAttrDAO().delete(printerAttr);

        return JsonRpcMethodResult.createOkResult();
    }

    @Override
    public JsonUserGroupAccess getAccessControl(final String printerName) {

        final Printer printer = printerDAO().findByName(printerName);

        /*
         * INVARIANT: printer MUST exist.
         */
        if (printer == null) {
            return null;
        }

        return getAccessControl(printer);
    }

    @Override
    public JsonUserGroupAccess getAccessControl(final Printer printer) {

        final PrinterAttr printerAttr = printerAttrDAO().findByName(
                printer.getId(), PrinterAttrEnum.ACCESS_USER_GROUPS);

        JsonUserGroupAccess groupAccess = null;

        if (printerAttr != null) {
            groupAccess = JsonHelper.createOrNull(JsonUserGroupAccess.class,
                    printerAttr.getValue());
        }

        if (groupAccess == null) {

            /*
             * Create a dummy, which denies none.
             */
            groupAccess = new JsonUserGroupAccess();
            groupAccess.setScope(AccessControlScopeEnum.DENY);
            groupAccess.setGroups(new ArrayList<String>());

        }
        return groupAccess;
    }

    /**
     * Checks if {@link Device} objects from the list supports hold/release
     * printing.
     *
     * @param devices
     *            The {@link Device} objects.
     * @return {@code true} when HOLD is supported.
     */
    private static boolean
            isHoldReleasePrintSupported(final List<Device> devices) {

        if (devices != null && !devices.isEmpty()) {

            for (final Device device : devices) {

                if (device.getDisabled() || !deviceDAO().isCardReader(device)) {
                    continue;
                }

                final ProxyPrintAuthModeEnum authMode =
                        deviceService().getProxyPrintAuthMode(device.getId());

                if (authMode != null && authMode.isHoldRelease()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isHoldReleasePrinter(final Printer printer) {

        /*
         * Check associated Card Reader devices which act as Print
         * Authenticator.
         *
         * (1) Try via Devices.
         */
        if (isHoldReleasePrintSupported(printer.getDevices())) {
            return true;
        }

        /*
         * (2) Try via PrinterGroupMembers.
         */
        if (printer.getPrinterGroupMembers() != null) {

            for (final PrinterGroupMember member : printer
                    .getPrinterGroupMembers()) {

                if (isHoldReleasePrintSupported(
                        member.getGroup().getDevices())) {
                    return true;
                }

            }
        }
        return false;
    }

    @Override
    public boolean isPrinterAccessGranted(final Printer printer,
            final User user) {

        final JsonUserGroupAccess access = this.getAccessControl(printer);

        final AccessControlScopeEnum scope = access.getScope();

        if (access.getGroups().isEmpty()) {

            if (scope == AccessControlScopeEnum.DENY) {
                /*
                 * Since no group is denied, everybody is allowed.
                 */
                return ACCESS_ALLOWED;
            } else {
                /*
                 * Since no group is allowed, everybody is denied.
                 */
                return ACCESS_DENIED;
            }
        }

        final List<UserGroupMember> memberships = user.getGroupMembership();

        /*
         * Group membership is needed for ALLOW scope.
         */
        if (memberships == null || memberships.isEmpty()) {

            if (scope == AccessControlScopeEnum.DENY) {
                /*
                 * Since some groups are denied, any non-member user is allowed.
                 */
                return ACCESS_ALLOWED;
            } else {
                /*
                 * Since some groups are allowed, any non-member user is denied.
                 */
                return ACCESS_DENIED;
            }
        }

        /*
         * Check printer access with user group membership.
         */
        final SortedSet<String> accessGroups = new TreeSet<>();
        accessGroups.addAll(access.getGroups());

        for (final UserGroupMember membership : memberships) {

            if (accessGroups.contains(membership.getGroup().getGroupName())) {

                /*
                 * User is part of printer access group.
                 */
                if (scope == AccessControlScopeEnum.DENY) {
                    return ACCESS_DENIED;
                } else {
                    return ACCESS_ALLOWED;
                }
            }
        }

        /*
         * User is NOT part of ANY printer access group.
         */
        if (scope == AccessControlScopeEnum.DENY) {
            return ACCESS_ALLOWED;
        } else {
            return ACCESS_DENIED;
        }
    }

    @Override
    public JsonProxyPrinterOptChoice findMediaSourceForMedia(
            final PrinterAttrLookup printerAttrLookup,
            final JsonProxyPrinterOpt mediaSource, final String requestedMedia,
            final Set<String> preferredMediaSources) {

        JsonProxyPrinterOptChoice firstFound = null;

        for (final JsonProxyPrinterOptChoice optChoice : mediaSource
                .getChoices()) {

            final IppMediaSourceCostDto assignedMediaSource = printerAttrLookup
                    .get(new PrinterDao.MediaSourceAttr(optChoice.getChoice()));

            if (assignedMediaSource != null && requestedMedia != null
                    && requestedMedia.equals(
                            assignedMediaSource.getMedia().getMedia())) {

                if (preferredMediaSources == null) {
                    if (BooleanUtils
                            .isTrue(assignedMediaSource.getPreferred())) {
                        return optChoice;
                    }
                } else {
                    if (preferredMediaSources.contains(optChoice.getChoice())) {
                        return optChoice;
                    }
                }

                if (firstFound == null) {
                    firstFound = optChoice;
                }
            }
        }
        return firstFound;
    }

    @Override
    public Map<String, String> getMediaSourceMediaMap(
            final PrinterAttrLookup printerAttrLookup,
            final JsonProxyPrinterOpt mediaSource) {

        final Map<String, String> map = new HashMap<>();

        for (final JsonProxyPrinterOptChoice optChoice : mediaSource
                .getChoices()) {

            final IppMediaSourceCostDto assignedMediaSource = printerAttrLookup
                    .get(new PrinterDao.MediaSourceAttr(optChoice.getChoice()));

            if (assignedMediaSource != null) {
                map.put(optChoice.getChoice(),
                        assignedMediaSource.getMedia().getMedia());
            }
        }
        return map;
    }

    @Override
    public void setSnmpInfo(final Printer printer, final PrinterSnmpDto info)
            throws IOException {

        final Map<PrinterAttrEnum, String> valueMap = new HashMap<>();

        final Date date = ServiceContext.getTransactionDate();

        valueMap.put(PrinterAttrEnum.SNMP_DATE, String.valueOf(date.getTime()));

        if (info != null) {
            valueMap.put(PrinterAttrEnum.SNMP_INFO,
                    createSmtpInfo(date, info).stringify());
        }

        for (final Entry<PrinterAttrEnum, String> entry : valueMap.entrySet()) {

            final PrinterAttr attr = printerAttrDAO()
                    .findByName(printer.getId(), entry.getKey());

            this.setPrinterAttrValue(attr, printer, entry.getKey(),
                    entry.getValue());
        }
    }

    @Override
    public ProxyPrinterSnmpInfoDto getSnmpInfo(final String json) {

        final ProxyPrinterSnmpInfoDto dto =
                JsonHelper.createOrNull(ProxyPrinterSnmpInfoDto.class, json);

        if (dto != null && dto.getDate() == null) {
            return null;
        }

        return dto;
    }

    @Override
    public void removeSnmpAttr(final Printer printer) {

        for (final PrinterAttrEnum attrEnum : EnumSet
                .of(PrinterAttrEnum.SNMP_DATE, PrinterAttrEnum.SNMP_INFO)) {

            final PrinterAttr attrWlk =
                    printerAttrDAO().findByName(printer.getId(), attrEnum);

            if (attrWlk != null) {
                removeAttribute(printer, attrEnum);
                printerAttrDAO().delete(attrWlk);
            }
        }
    }

    /**
     * Creates SNMP printer info.
     *
     * @param info
     *            The "raw" {@link PrinterSnmpDto}.
     * @return The {@link ProxyPrinterSnmpInfoDto}.
     */
    private static ProxyPrinterSnmpInfoDto createSmtpInfo(final Date date,
            final PrinterSnmpDto info) {

        if (info.getSuppliesEntries() == null) {
            return null;
        }

        final ProxyPrinterSnmpInfoDto obj = new ProxyPrinterSnmpInfoDto();

        obj.setDate(date);
        obj.setVendor(info.getEnterprise());
        obj.setModel(info.getSystemDescription());
        obj.setSerial(info.getSerialNumber());
        obj.setErrorStates(info.getErrorStates());

        for (final Entry<SnmpPrtMarkerSuppliesTypeEnum, List<SnmpPrtMarkerSuppliesEntry>> entry : info
                .getSuppliesEntries().entrySet()) {

            switch (entry.getKey()) {
            case TONER:
                obj.setSupplies(ProxyPrinterSuppliesEnum.TONER);
                break;
            case INK:
                obj.setSupplies(ProxyPrinterSuppliesEnum.INK);
                break;
            case UNDEFINED: // Make an assumption.
                obj.setSupplies(ProxyPrinterSuppliesEnum.TONER);
                break;
            default:
                continue;
            }

            final Map<SnmpPrtMarkerColorantValueEnum, Integer> colorants =
                    new HashMap<>();

            for (final SnmpPrtMarkerSuppliesEntry supplies : entry.getValue()) {

                final SnmpPrtMarkerSuppliesClassEnum suppliesClass =
                        supplies.getSuppliesClass();

                // Handle UNDEFINED as CONSUMED.
                if (suppliesClass != SnmpPrtMarkerSuppliesClassEnum.CONSUMED
                        && suppliesClass != SnmpPrtMarkerSuppliesClassEnum.UNDEFINED) {
                    continue;
                }

                final SnmpPrtMarkerColorantEntry colorantEntry =
                        supplies.getColorantEntry();

                final int perc;
                if (supplies.getLevel() == 0
                        || supplies.getMaxCapacity() == 0) {
                    perc = 0;
                } else {
                    perc = (NumberUtil.INT_HUNDRED * supplies.getLevel())
                            / supplies.getMaxCapacity();
                }

                final SnmpPrtMarkerColorantValueEnum color;

                if (colorantEntry == null) {
                    // Make an assumption.
                    color = SnmpPrtMarkerColorantValueEnum.BLACK;
                } else {

                    switch (colorantEntry.getValue()) {
                    case UNKNOWN:
                    case OTHER:
                        if (StringUtils.containsIgnoreCase(
                                supplies.getDescription(), "black")) {
                            color = SnmpPrtMarkerColorantValueEnum.BLACK;
                            break;
                        } else if (StringUtils.containsIgnoreCase(
                                supplies.getDescription(), "tri-color")) {
                            color = SnmpPrtMarkerColorantValueEnum.OTHER;
                            break;
                        }

                    default:
                        color = colorantEntry.getValue();
                        break;
                    }
                }

                colorants.put(color, Integer.valueOf(perc));
            }

            obj.setMarkers(colorants);

            // Just get the first one, ignore the rest.
            break;
        }

        return obj;
    }

}
