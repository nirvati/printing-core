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
package org.savapage.core.services;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.savapage.core.dao.enums.AccessControlScopeEnum;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dao.enums.PrinterAttrEnum;
import org.savapage.core.dao.helpers.JsonUserGroupAccess;
import org.savapage.core.dao.helpers.ProxyPrinterSnmpInfoDto;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.dto.PrinterSnmpDto;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterAttr;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserGroup;
import org.savapage.core.json.rpc.AbstractJsonRpcMessage;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodResponse;
import org.savapage.core.json.rpc.JsonRpcMethodError;
import org.savapage.core.json.rpc.JsonRpcMethodResult;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.services.helpers.PrinterAttrLookup;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface PrinterService {

    /**
    *
    */
    int MAX_TIME_SERIES_INTERVALS_DAYS = 40;

    /**
     * Reads the database to check if printer is internal use only.
     *
     * @param id
     *            The database primary key.
     * @return {@code true} when internal printer.
     */
    boolean isInternalPrinter(Long id);

    /**
     * Reads the database to check if if document store is disabled.
     *
     * @param store
     *            The document store.
     * @param id
     *            The database primary key.
     * @return {@code true} when document store is disabled.
     */
    boolean isDocStoreDisabled(DocStoreTypeEnum store, Long id);

    /**
     * Checks if document store is disabled.
     * <p>
     * Traverses the internal {@link PrinterAttr} list of a {@link Printer} to
     * find the {@link PrinterAttrEnum} value.
     * </p>
     *
     * @param store
     *            The document store.
     * @param printer
     *            The {@link Printer}.
     * @return {@code true} when document store is disabled.
     */
    boolean isDocStoreDisabled(DocStoreTypeEnum store, Printer printer);

    /**
     * Reads the database to check if printer is a Job Ticket printer.
     *
     * @param id
     *            The database primary key.
     * @return {@code true} when Job Ticket printer.
     */
    boolean isJobTicketPrinter(Long id);

    /**
     * Checks if the {@link Printer} can be used for proxy printing, i.e. it is
     * NOT disabled and NOT (logically) deleted.
     *
     * @param printer
     *            The {@link Printer}.
     * @return {@code true} if it can be used.
     */
    boolean canPrinterBeUsed(Printer printer);

    /**
     * Checks if a {@link Printer} is custom secured via Card Reader(s) acting
     * as Print Authenticator, or via a Terminal printer association.
     * <p>
     * Associated Card Reader devices are checked via
     * {@link Printer#getDevices()}:
     * </p>
     * <ol>
     * <li>Directly for this instance.</li>
     * <li>Via printers of the associated {@link PrinterGroup} objects</li>
     * </ol>
     *
     * @param printer
     *            The {@link Printer}.
     * @param terminalSecured
     *            Return value which holds {@code true} if Printer is secured
     *            via {@link Device.DeviceTypeEnum#TERMINAL}.
     * @param readerSecured
     *            Return value which holds {@code true} if Printer is secured
     *            via {@link Device.DeviceTypeEnum#CARD_READER}
     * @return {@code true} if Printer is custom secured (either via Reader or
     *         Terminal).
     */
    boolean checkPrinterSecurity(Printer printer,
            MutableBoolean terminalSecured, MutableBoolean readerSecured);

    /**
     * Checks if a {@link Printer} is custom secured via Card Reader(s) acting
     * as Print Authenticator, or via a Terminal printer association.
     * <p>
     * Associated Card Reader devices are checked via
     * {@link Printer#getDevices()}:
     * </p>
     * <ol>
     * <li>Directly for this instance.</li>
     * <li>Via printers of the associated {@link PrinterGroup} objects</li>
     * </ol>
     *
     * @param printer
     *            The {@link Printer}.
     * @param terminalSecured
     *            Return value which holds {@code true} if Printer is secured
     *            via {@link Device.DeviceTypeEnum#TERMINAL}.
     * @param readerSecured
     *            Return value which holds {@code true} if Printer is secured
     *            via {@link Device.DeviceTypeEnum#CARD_READER}
     * @param terminalDevices
     *            The Terminal Devices responsible for printer being secured.
     * @param readerDevices
     *            The Reader Devices responsible for printer being secured.
     * @return {@code true} if Printer is secured (either via Reader or
     *         Terminal). {@code false} is Printer is not custom secured.
     */
    boolean checkPrinterSecurity(Printer printer,
            MutableBoolean terminalSecured, MutableBoolean readerSecured,
            Map<String, Device> terminalDevices,
            Map<String, Device> readerDevices);

    /**
     * Checks if access to a {@link Printer} is secured via a {@link Device}.
     * <p>
     * The associated Printer or Printers (via PrinterGroup) of the Terminal is
     * (are) checked if they match this printer.
     * </p>
     *
     * @param printer
     *            The {@link Printer}.
     * @param deviceType
     *            Type of device. Used to check if the offered Device is
     *            expected type. If NOT an exception is thrown.
     * @param device
     *            The {@link Device}.
     * @return {@code true} when associated Printers are present, and a match is
     *         found. {@code false} when <i>no</i> associated Printers are
     *         present, or associated Printers are present, but no matching
     *         Printer is found.
     */
    boolean checkDeviceSecurity(Printer printer, DeviceTypeEnum deviceType,
            Device device);

    /**
     * Traverses the internal {@link PrinterAttr} list of a {@link Printer} to
     * remove a {@link PrinterAttr}.
     *
     * @param printer
     *            The {@link Printer}.
     * @param name
     *            The {@link PrinterAttrEnum}.
     *
     * @return The {@link PrinterAttr} that was removed, or {@code null} when
     *         not found.
     */
    PrinterAttr removeAttribute(Printer printer, PrinterAttrEnum name);

    /**
     * Traverses the internal {@link PrinterAttr} list of a {@link Printer} to
     * get the {@link PrinterAttr}.
     *
     * @param printer
     *            The {@link Printer}.
     * @param name
     *            The {@link PrinterAttrEnum}.
     *
     * @return {@code null} when not found.
     */
    PrinterAttr getAttribute(Printer printer, PrinterAttrEnum name);

    /**
     * Traverses the internal {@link PrinterAttr} list of a {@link Printer} to
     * get the value of an attribute.
     *
     * @param printer
     *            The {@link Printer}.
     * @param name
     *            The {@link PrinterAttrEnum}.
     *
     * @return {@code null} when not found.
     */
    String getAttributeValue(Printer printer, PrinterAttrEnum name);

    /**
     * Traverses the internal {@link PrinterAttr} list of a {@link Printer} to
     * get value of {@link IppDictJobTemplateAttr#ATTR_PRINT_COLOR_MODE_DFLT}.
     * This value, stored as printer attributes, overrides the default as
     * retrieved from CUPS/IPP.
     *
     * @param printer
     *            The {@link Printer}.
     * @return {@code null} when no default override is found.
     */
    String getPrintColorModeDefault(Printer printer);

    /**
     * Reads the database to get value of
     * {@link IppDictJobTemplateAttr#ATTR_PRINT_COLOR_MODE_DFLT}.
     *
     * @see {@link #getPrintColorModeDefault(Printer).
     *
     * @param id
     *            The database primary key.
     * @return {@code null} when no default override is found.
     */
    String getPrintColorModeDefault(Long id);

    /**
     * Checks if monochrome conversion is performed client-side (locally).
     * <p>
     * Traverses the internal {@link PrinterAttr} list of a {@link Printer} to
     * find the value of {@link PrinterAttrEnum#CLIENT_SIDE_MONOCHROME}.
     * </p>
     *
     * @param printer
     *            The {@link Printer}.
     * @return {@code true} if monochrome conversion is performed client-side.
     */
    boolean isClientSideMonochrome(Printer printer);

    /**
     * Gets printer attribute value (using the attribute list of the printer).
     *
     * @param printer
     *            The {@link Printer}.
     * @param attr
     *            The attribute type.
     * @return {@code null} if not present.
     */
    String getPrinterAttrValue(Printer printer, PrinterAttrEnum attr);

    /**
     * Checks if 2-up duplex booklet page ordering is performed client-side
     * (locally).
     *
     * @param printer
     *            The {@link Printer}.
     * @return {@code true} if booklet page ordering is performed client-side.
     */
    boolean isClientSideBooklet(Printer printer);

    /**
     * Sets printer instance as logically deleted (database is NOT updated).
     *
     * @param printer
     *            The {@link Printer}.
     */
    void setLogicalDeleted(Printer printer);

    /**
     * Reverses a logical delete (database is NOT updated).
     *
     * @param printer
     *            The {@link Printer}.
     */
    void undoLogicalDeleted(Printer printer);

    /**
     * Adds totals of a job to a {@link Printer} (database is NOT updated).
     *
     * @param printer
     *            The {@link Printer}.
     * @param jobDate
     *            The date.
     * @param jobPages
     *            The number of pages.
     * @param jobSheets
     *            The number of sheets.
     * @param jobEsu
     *            The number of ESU.
     * @param jobBytes
     *            The number of bytes.
     */
    void addJobTotals(Printer printer, Date jobDate, int jobPages,
            int jobSheets, long jobEsu, long jobBytes);

    /**
     * Checks if a {@link Printer} is member of a {@link PrinterGroup}.
     *
     * @param group
     *            The {@link PrinterGroup}.
     * @param printer
     *            The {@link Printer}.
     * @return {@code true} is printer is a member of the group.
     */
    boolean isPrinterGroupMember(PrinterGroup group, Printer printer);

    /**
     * Logs a PrintOut job, by adding a data point to the time series (database
     * IS updated).
     *
     * @param printer
     *            The printer.
     * @param jobTime
     *            The time of the job.
     * @param jobPages
     *            The number of pages.
     * @param jobSheets
     *            The number of sheets.
     * @param jobEsu
     *            The number of ESU.
     */
    void logPrintOut(Printer printer, Date jobTime, Integer jobPages,
            Integer jobSheets, Long jobEsu);

    /**
     * Adds access control for a {@link UserGroup} to a {@link Printer}.
     *
     * @param scope
     *            The access scope.
     * @param printerName
     *            The CUPS {@link Printer} name.
     * @param groupName
     *            The {@link UserGroup} name.
     * @return The JSON-RPC Return message: {@link JsonRpcMethodResult} or an
     *         {@link JsonRpcMethodError} instance.
     * @throws IOException
     *             When JSON errors.
     */
    AbstractJsonRpcMethodResponse addAccessControl(AccessControlScopeEnum scope,
            String printerName, String groupName) throws IOException;

    /**
     * Removes {@link UserGroup} access control from a {@link Printer}.
     *
     * @param printerName
     *            The CUPS {@link Printer} name.
     * @param groupName
     *            The {@link UserGroup} name.
     * @return The JSON-RPC Return message: {@link JsonRpcMethodResult} or an
     *         {@link JsonRpcMethodError} instance.
     * @throws IOException
     *             When JSON errors.
     */
    AbstractJsonRpcMessage removeAccessControl(String printerName,
            String groupName) throws IOException;

    /**
     * Removes access control from a {@link Printer}.
     *
     * @param printerName
     *            The CUPS {@link Printer} name.
     * @return The JSON-RPC Return message: {@link JsonRpcMethodResult} or an
     *         {@link JsonRpcMethodError} instance.
     */
    AbstractJsonRpcMessage removeAccessControl(String printerName);

    /**
     * Gets the access control of a {@link Printer}.
     *
     * @param printerName
     *            The CUPS {@link Printer} name.
     * @return the {@link JsonUserGroupAccess} instance.
     */
    JsonUserGroupAccess getAccessControl(String printerName);

    /**
     * Gets the access control of a {@link Printer}.
     *
     * @param printer
     *            The {@link Printer}.
     * @return the {@link JsonUserGroupAccess} instance.
     */
    JsonUserGroupAccess getAccessControl(Printer printer);

    /**
     * Checks if {@link Printer} access is granted to a {@link User}.
     *
     * @param printer
     *            The {@link Printer}.
     * @param user
     *            The {@link User}.
     * @return {@code true} when access is granted.
     */
    boolean isPrinterAccessGranted(Printer printer, User user);

    /**
     * Checks if printer supports hold/release printing.
     *
     * @param printer
     *            The {@link Printer}.
     * @return {@code true} if printer supports hold/release printing.
     */
    boolean isHoldReleasePrinter(Printer printer);

    /**
     * Finds the first "media-source" of a printer that matches a "media".
     *
     * @param printerAttrLookup
     *            The {@link PrinterAttrLookup} containing the "media-source" to
     *            "media" mapping.
     * @param mediaSource
     *            The "media-source" to search. See
     *            {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE}
     * @param requestedMedia
     *            The requested "media", as value of
     *            {@link IppDictJobTemplateAttr#ATTR_MEDIA}.
     * @param preferredMediaSources
     *            A set of preferred media sources (can be {@code null}).
     * @return The media source, or {@code null} when not found.
     */
    JsonProxyPrinterOptChoice findMediaSourceForMedia(
            PrinterAttrLookup printerAttrLookup,
            JsonProxyPrinterOpt mediaSource, String requestedMedia,
            Set<String> preferredMediaSources);

    /**
     * Gets the map with key (media-source choice) and value (media).
     *
     * @param printerAttrLookup
     *            The {@link PrinterAttrLookup} containing the "media-source" to
     *            "media" mapping.
     * @param mediaSource
     *            The "media-source" to search. See
     *            {@link IppDictJobTemplateAttr#ATTR_MEDIA_SOURCE}
     * @return The map (can be empty).
     */
    Map<String, String> getMediaSourceMediaMap(
            PrinterAttrLookup printerAttrLookup,
            JsonProxyPrinterOpt mediaSource);

    /**
     * Traverses the internal {@link PrinterAttr} list of a {@link Printer} to
     * get the set of media-sources from
     * {@link PrinterAttrEnum#JOB_SHEETS_MEDIA_SOURCES}.
     *
     * @param printer
     *            The printer.
     * @return {@code null} when printer attribute is not present.
     */
    Set<String> getJobSheetsMediaSources(Printer printer);

    /**
     * Sets SNMP printer info.
     *
     * @param printer
     *            The printer.
     * @param info
     *            The "raw" SNMP info. If {@code null}, no SNMP information is
     *            available.
     * @throws IOException
     *             When JSON serialization errors.
     */
    void setSnmpInfo(Printer printer, PrinterSnmpDto info) throws IOException;

    /**
     * De-serializes JSON string to object.
     *
     * @param json
     *            JSON string.
     * @return The object, or {@code null} when IO, parse or mapping error.
     */
    ProxyPrinterSnmpInfoDto getSnmpInfo(String json);

    /**
     * Removes all SNMP attributes from printer.
     * <p>
     * NOTE: Caller must execute database commit.
     * </p>
     *
     * @param printer
     *            The printer.
     */
    void removeSnmpAttr(Printer printer);

}
