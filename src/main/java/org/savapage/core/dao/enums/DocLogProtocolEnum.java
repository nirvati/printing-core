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
package org.savapage.core.dao.enums;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Datraverse B.V.
 *
 */
public enum DocLogProtocolEnum {

    /**
    *
    */
    IPP("IPP"),

    /**
    *
    */
    RAW("RAW"),

    /**
    *
    */
    HTTP("HTTP"),

    /**
    *
    */
    FTP("FTP"),

    /**
    *
    */
    LPR("LPR"),

    /**
    *
    */
    SMARTSCHOOL("SMARTSCHOOL"),

    /**
    *
    */
    SMTP("SMTP"),

    /**
    *
    */
    IMAP("IMAP"),

    /**
    *
    */
    GCP("GCP");

    /**
   *
   */
    private static class Lookup {

        /**
       *
       */
        private final Map<String, DocLogProtocolEnum> enumLookup =
                new HashMap<String, DocLogProtocolEnum>();

        /**
       *
       */
        public Lookup() {
            for (DocLogProtocolEnum value : DocLogProtocolEnum.values()) {
                enumLookup.put(value.dbName, value);
            }
        }

        /**
         *
         * @param key
         *            The key (name).
         * @return The enum.
         */
        public DocLogProtocolEnum get(final String key) {
            return enumLookup.get(key);
        }
    }

    /**
     * Ensure one-time initialization on class loading.
     */
    private static class LookupHolder {
        /**
        *
        */
        public static final Lookup INSTANCE = new Lookup();
    }

    /**
    *
    */
    private final String dbName;

    /**
     *
     * @param dbName
     */
    private DocLogProtocolEnum(final String dbName) {
        this.dbName = dbName;
    }

    /**
     * Gets the DocLogProtocolEnum from the database name.
     *
     * @param dbName
     *            The database name
     * @return The {@link DocLogProtocolEnum} or {@code null} when not found.
     */
    public static DocLogProtocolEnum asEnum(final String dbName) {
        return LookupHolder.INSTANCE.get(dbName);
    }

    /**
     * @return The value as used in the database.
     */
    public String getDbName() {
        return this.dbName;
    }

}
