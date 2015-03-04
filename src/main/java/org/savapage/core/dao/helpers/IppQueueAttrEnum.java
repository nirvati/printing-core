/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.dao.helpers;

import java.util.HashMap;
import java.util.Map;

import org.savapage.core.dao.IppQueueAttrDao;
import org.savapage.core.jpa.IppQueueAttr;

/**
 * {@link IppQueueAttr} names. See {@link IppQueueAttr#setName(String)}.
 *
 * @author Datraverse B.V.
 */
public enum IppQueueAttrEnum {

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,2,1,4,0,0,2,0,..,0,8,1,0}
     * </p>
     */
    PRINT_IN_ROLLING_DAY_PAGES(IppQueueAttrDao.STATS_ROLLING_PREFIX
            + "-day.pages");

    /**
     *
     */
    private static class Lookup {

        /**
         *
         */
        private final Map<String, IppQueueAttrEnum> enumLookup =
                new HashMap<String, IppQueueAttrEnum>();

        /**
         *
         */
        public Lookup() {
            for (IppQueueAttrEnum value : IppQueueAttrEnum.values()) {
                enumLookup.put(value.dbName, value);
            }
        }

        /**
         *
         * @param key
         *            The key (name).
         * @return The enum.
         */
        public IppQueueAttrEnum get(final String key) {
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
     * Gets the IppQueueAttrEnum from the database name.
     *
     * @param dbName
     *            The database name
     * @return The {@link IppQueueAttrEnum}.
     */
    public static IppQueueAttrEnum asEnum(final String dbName) {
        return LookupHolder.INSTANCE.get(dbName);
    }

    /**
     *
     * @param dbName
     *            The database name.
     */
    private IppQueueAttrEnum(final String dbName) {
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
