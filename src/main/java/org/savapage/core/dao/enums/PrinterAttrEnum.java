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
package org.savapage.core.dao.enums;

import java.util.HashMap;
import java.util.Map;

import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.PrinterAttrDao;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.jpa.PrinterAttr;

/**
 * {@link PrinterAttr} names. See {@link PrinterAttr#setName(String)}.
 *
 * @author Rijk Ravestein
 *
 */
public enum PrinterAttrEnum {

    /**
     * User groups to either allow or deny access to the Printer. Examples
     * <p>
     * {"scope":"allow","groups":["Staff","ICT"]}
     * </p>
     */
    ACCESS_USER_GROUPS("access.user-groups"),

    /**
     * Boolean: Y | N. When not present N is assumed.
     */
    ACCESS_INTERNAL("access.internal"),

    /**
     * Boolean: Y | N. When not present N is assumed.
     */
    ARCHIVE_DISABLE("archive.disable"),

    /**
     * Boolean: Y | N. When not present N is assumed.
     */
    JOURNAL_DISABLE("journal.disable"),

    /**
     * Is monochrome conversion performed client-side (locally)? Boolean:
     * {@code true|false}.
     */
    CLIENT_SIDE_MONOCHROME("filter.monochrome.client-side"),

    /**
     * A PPD extensions file in
     * {@link ConfigManager#SERVER_REL_PATH_CUSTOM_CUPS} with IPP mappings and
     * constraints.
     */
    CUSTOM_PPD_EXT_FILE("custom.ppd-ext-file"),

    /**
     * Enable as Job Ticket Printer. Boolean: Y | N
     */
    JOBTICKET_ENABLE("jobticket.enable"),

    /**
     * Printer Group with compatible redirect printers for JobTicket Printer.
     */
    JOBTICKET_PRINTER_GROUP("jobticket.printer-group"),

    /**
     * Preferred media-sources (JSON) for
     * {@link IppDictJobTemplateAttr#ORG_SAVAPAGE_ATTR_JOB_SHEETS}.
     */
    JOB_SHEETS_MEDIA_SOURCES("job-sheets.media-sources"),

    /**
     * <i>This is a prefix for media-source parameters and is not used as such:
     * note the <u>dot character</u> at the end.</i>
     * <p>
     * Usage example: {@code media-source.tray1}
     * </p>
     */
    PFX_MEDIA_SOURCE("media-source."),

    /**
     * <i>This is a prefix for cost parameters per media and is not used as
     * such: note the <u>dot character</u> at the end.</i>
     * <p>
     * Usage example: {@code cost.media.default} and
     * {@code cost.media.iso_a4_210x297mm}
     * </p>
     */
    PFX_COST_MEDIA("cost.media."),

    /**
     * <i>This is a prefix for IPP keywords and is not used as such: note the
     * <u>dot character</u> at the end.</i>
     */
    PFX_IPP_KEYWORD("ipp."),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,2,1,0,...,0,8,1}
     * </p>
     */
    PRINT_OUT_ROLLING_DAY_PAGES(PrinterAttrDao.STATS_ROLLING_PREFIX //
            + "-day.pages"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,2,1,0,...,0,8,1}
     * </p>
     */
    PRINT_OUT_ROLLING_DAY_SHEETS(PrinterAttrDao.STATS_ROLLING_PREFIX //
            + "-day.sheets"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,2,1,0,...,0,8,1}
     * </p>
     */
    PRINT_OUT_ROLLING_DAY_ESU(PrinterAttrDao.STATS_ROLLING_PREFIX //
            + "-day.esu"),

    /**
     * Date of {@link #SNMP_INFO}.
     */
    SNMP_DATE("snmp.date"),

    /**
     * SNMP information at {@link #SNMP_DATE}.
     */
    SNMP_INFO("snmp.info");

    /**
     *
     */
    private static class Lookup {

        /**
         *
         */
        private final Map<String, PrinterAttrEnum> enumLookup =
                new HashMap<String, PrinterAttrEnum>();

        /**
         *
         */
        public Lookup() {
            for (PrinterAttrEnum value : PrinterAttrEnum.values()) {
                enumLookup.put(value.dbName, value);
            }
        }

        /**
         *
         * @param key
         *            The key (name).
         * @return The enum.
         */
        public PrinterAttrEnum get(final String key) {
            return enumLookup.get(key);
        }
    }

    /**
         *
         */
    private final String dbName;

    /**
     * Ensure one-time initialization on class loading.
     */
    private static class LookupHolder {
        public static final Lookup INSTANCE = new Lookup();
    }

    /**
     * Gets the PrinterAttrEnum from the database name.
     *
     * @param dbName
     *            The database name
     * @return The {@link PrinterAttrEnum}.
     */
    public static PrinterAttrEnum asEnum(final String dbName) {
        return LookupHolder.INSTANCE.get(dbName);
    }

    /**
     *
     * @param dbName
     *            The database name.
     */
    PrinterAttrEnum(final String dbName) {
        this.dbName = dbName;
    }

    /**
     * Gets the (prefix) name used in the database.
     *
     * @return The database name.
     */
    public final String getDbName() {
        return this.dbName;
    }

}
