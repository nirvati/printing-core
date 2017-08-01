/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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

import org.savapage.core.dao.UserAttrDao;
import org.savapage.core.jpa.UserAttr;

/**
 * {@link UserAttr} names. See {@link UserAttr#setName(String)}.
 *
 * @author Rijk Ravestein
 *
 */
public enum UserAttrEnum {

    /**
     * A JSON value of {@link Map} with key {@link ACLRoleEnum} and value
     * {@link Boolean}. When a {@link ACLRoleEnum} key is not present the value
     * is indeterminate.
     */
    ACL_ROLES("acl.roles"),

    /**
     * OIDS for Role "User": A JSON value of a {@link Map} with key
     * {@link ACLOidEnum}. Value {@link Integer} is a bitwise OR of
     * {@link ACLPermissionEnum#getPermission()} values that hold the granted
     * access. When a {@link ACLOidEnum} key is not present in the map the
     * access is indeterminate.
     */
    ACL_OIDS_USER("acl.oids.user"),

    /**
     * OIDS for Role "Admin": A JSON value of a {@link Map} with key
     * {@link ACLOidEnum}. Value {@link Integer} is a bitwise OR of
     * {@link ACLPermissionEnum#getPermission()} values that hold the granted
     * access. When a {@link ACLOidEnum} key is not present in the map the
     * access is indeterminate.
     */
    ACL_OIDS_ADMIN("acl.oids.admin"),

    /**
     * The pending bitcoin address created for a {@link User} payment
     * transaction. This address is deleted when consumed, i.e. upon first
     * confirmation.
     */
    BITCOIN_PAYMENT_ADDRESS("bitcoin.payment.address"),

    /**
     * Password of an Internal user.
     */
    INTERNAL_PASSWORD("internal-password"),

    /**
     * Encrypted PIN optionally to be used in combination with ID number and
     * Card(s).
     */
    PIN("pin"),

    /**
     * User PGP Public Key ID in upper-case hex notation (without "0x" prefix).
     */
    PGP_PUBKEY_ID("pgp.pubkey.id"),

    /**
     *
     */
    PDF_PROPS("pdf-properties"),

    /**
     * The Encrypted PIN {@link UUID} used for identification when printing from
     * the internet. See {@link ReservedIppQueueEnum#IPP_PRINT_INTERNET}.
     */
    UUID("uuid"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,7,16,2,9,16}
     * </p>
     */
    PRINT_IN_ROLLING_WEEK_PAGES("print.in." + UserAttrDao.STATS_ROLLING + "-week.pages"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,70033,163344,22335511,9999332,16345}
     * </p>
     */
    PRINT_IN_ROLLING_WEEK_BYTES("print.in." + UserAttrDao.STATS_ROLLING + "-week.bytes"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,7,16,2,9,16}
     * </p>
     */
    PRINT_IN_ROLLING_MONTH_PAGES("print.in." + UserAttrDao.STATS_ROLLING + "-month.pages"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,70033,163344,22335511,9999332,16345}
     * </p>
     */
    PRINT_IN_ROLLING_MONTH_BYTES("print.in." + UserAttrDao.STATS_ROLLING + "-month.bytes"),
    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,7,16,2,9,16}
     * </p>
     */
    PRINT_OUT_ROLLING_WEEK_PAGES("print.out." + UserAttrDao.STATS_ROLLING + "-week.pages"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,7,16,2,9,16}
     * </p>
     */
    PRINT_OUT_ROLLING_WEEK_SHEETS("print.out." + UserAttrDao.STATS_ROLLING + "-week.sheets"),
    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,7,16,2,9,16}
     * </p>
     */
    PRINT_OUT_ROLLING_WEEK_ESU("print.out." + UserAttrDao.STATS_ROLLING + "-week.esu"),
    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,70033,163344,22335511,9999332,16345}
     * </p>
     */
    PRINT_OUT_ROLLING_WEEK_BYTES("print.out." + UserAttrDao.STATS_ROLLING + "-week.bytes"),
    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,7,16,2,9,16}
     * </p>
     */
    PRINT_OUT_ROLLING_MONTH_PAGES("print.out." + UserAttrDao.STATS_ROLLING + "-month.pages"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,7,16,2,9,16}
     * </p>
     */
    PRINT_OUT_ROLLING_MONTH_SHEETS("print.out." + UserAttrDao.STATS_ROLLING + "-month.sheets"),
    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,7,16,2,9,16}
     * </p>
     */
    PRINT_OUT_ROLLING_MONTH_ESU("print.out." + UserAttrDao.STATS_ROLLING + "-month.esu"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,70033,163344,22335511,9999332,16345}
     * </p>
     */
    PRINT_OUT_ROLLING_MONTH_BYTES("print.out." + UserAttrDao.STATS_ROLLING + "-month.bytes"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,7,16,2,9,16}
     * </p>
     */
    PDF_OUT_ROLLING_WEEK_PAGES("pdf.out." + UserAttrDao.STATS_ROLLING + "-week.pages"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,70033,163344,22335511,9999332,16345}
     * </p>
     */
    PDF_OUT_ROLLING_WEEK_BYTES("pdf.out." + UserAttrDao.STATS_ROLLING + "-week.bytes"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,7,16,2,9,16}
     * </p>
     */
    PDF_OUT_ROLLING_MONTH_PAGES("pdf.out." + UserAttrDao.STATS_ROLLING + "-month.pages"),

    /**
     * Statistic time series. Example:
     * <p>
     * {@code 1342562400000,70033,163344,22335511,9999332,16345}
     * </p>
     */
    PDF_OUT_ROLLING_MONTH_BYTES("pdf.out." + UserAttrDao.STATS_ROLLING + "-month.bytes");

    /**
     * Lookup {@link UserAttrEnum} by database name.
     */
    private static class Lookup {

        /**
         *
         */
        private final Map<String, UserAttrEnum> enumLookup =
                new HashMap<String, UserAttrEnum>();

        /**
         *
         */
        public Lookup() {
            for (UserAttrEnum value : UserAttrEnum.values()) {
                enumLookup.put(value.name, value);
            }
        }

        /**
         *
         * @param key
         *            The key (name).
         * @return The enum.
         */
        public UserAttrEnum get(final String key) {
            return enumLookup.get(key);
        }
    }

    /**
     * The name used in the database.
     */
    private final String name;

    /**
     * Ensure one-time initialization on class loading.
     */
    private static class LookupHolder {
        public static final Lookup INSTANCE = new Lookup();
    }

    /**
     *
     * @param name
     *            The database name.
     * @return The {@link UserAttrEnum}.
     */
    public static UserAttrEnum asEnum(final String name) {
        return LookupHolder.INSTANCE.get(name);
    }

    /**
     *
     * @param name
     *            The database name.
     */
    private UserAttrEnum(final String name) {
        this.name = name;
    }

    /**
     *
     * @return The database name.
     */
    public final String getName() {
        return this.name;
    }
}
