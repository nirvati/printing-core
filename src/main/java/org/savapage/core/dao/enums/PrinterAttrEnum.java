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
package org.savapage.core.dao.enums;

import java.util.HashMap;
import java.util.Map;

import org.savapage.core.dao.PrinterAttrDao;
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
     * Is monochrome conversion performed client-side (locally)? Boolean:
     * {@code true|false}.
     */
    CLIENT_SIDE_MONOCHROME("filter.monochrome.client-side"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,2,1,0,...,0,8,1}
     * </p>
     */
    PRINT_OUT_ROLLING_DAY_PAGES(PrinterAttrDao.STATS_ROLLING_PREFIX + "-day.pages"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,2,1,0,...,0,8,1}
     * </p>
     */
    PRINT_OUT_ROLLING_DAY_SHEETS(PrinterAttrDao.STATS_ROLLING_PREFIX + "-day.sheets"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,2,1,0,...,0,8,1}
     * </p>
     */
    PRINT_OUT_ROLLING_DAY_ESU(PrinterAttrDao.STATS_ROLLING_PREFIX + "-day.esu");

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
    private PrinterAttrEnum(final String dbName) {
        this.dbName = dbName;
    }

    /**
     * Gets the name used in the database.
     *
     * @return The database name.
     */
    public final String getDbName() {
        return this.dbName;
    }

}
