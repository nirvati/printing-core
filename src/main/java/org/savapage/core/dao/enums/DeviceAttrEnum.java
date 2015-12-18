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

import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.DeviceAttrDao;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.DeviceAttr;

/**
 * {@link DeviceAttr} names. See {@link DeviceAttr#setName(String)}.
 *
 * @author Datraverse B.V.
 */
public enum DeviceAttrEnum {

    /**
     * Identical to {@link IConfigProp.Key#WEBAPP_USER_MAX_IDLE_SECS}.
     */
    WEBAPP_USER_MAX_IDLE_SECS(DeviceAttrDao.WEBAPP_USER_PREFIX
            + ".max-idle-secs"),

    /**
     *
     */
    PROXY_PRINT_AUTH_MODE("proxy-print.auth-mode"),

    /**
     * Identical to {@link IConfigProp.Key#AUTH_MODE_NAME}.
     */
    AUTH_MODE_NAME(DeviceAttrDao.AUTH_MODE_PREFIX + ".name"),

    /**
     * Identical to {@link IConfigProp.Key#AUTH_MODE_ID}.
     */
    AUTH_MODE_ID(DeviceAttrDao.AUTH_MODE_PREFIX + ".id"),

    /**
     * Identical to {@link IConfigProp.Key#AUTH_MODE_CARD_LOCAL}.
     */
    AUTH_MODE_CARD_LOCAL(DeviceAttrDao.AUTH_MODE_PREFIX + ".card-local"),

    /**
     * Identical to {@link IConfigProp.Key#AUTH_MODE_ID_IS_MASKED}.
     */
    AUTH_MODE_ID_IS_MASKED(DeviceAttrDao.AUTH_MODE_PREFIX + ".id.is-masked"),

    /**
     * Identical to {@link IConfigProp.Key#AUTH_MODE_ID_PIN_REQUIRED}.
     */
    AUTH_MODE_ID_PIN_REQ(DeviceAttrDao.AUTH_MODE_PREFIX + ".id.pin-required"),

    /**
     * Identical to {@link IConfigProp.Key#AUTH_MODE_CARD_PIN_REQUIRED}.
     */
    AUTH_MODE_CARD_PIN_REQ(DeviceAttrDao.AUTH_MODE_PREFIX
            + ".card.pin-required"),

    /**
     * Identical to {@link IConfigProp.Key#AUTH_MODE_CARD_SELF_ASSOCIATION}.
     */
    AUTH_MODE_CARD_SELF_ASSOC(DeviceAttrDao.AUTH_MODE_PREFIX
            + ".card.self-association"),

    /**
     *
     */
    AUTH_MODE_CARD_IP(DeviceAttrDao.AUTH_MODE_PREFIX + ".card-ip"),

    /**
     * Identical to {@link IConfigProp.Key#AUTH_MODE_DEFAULT}.
     */
    AUTH_MODE_DEFAULT(DeviceAttrDao.AUTH_MODE_PREFIX + "-default"),

    /**
     *
     */
    AUTH_MODE_IS_CUSTOM(DeviceAttrDao.AUTH_MODE_PREFIX + "-is-custom"),

    /**
     * When {@link Device.DeviceTypeEnum#TERMINAL} this is for the LOCAL
     * (Keyboard emulating) Card reader. When
     * {@link Device.DeviceTypeEnum#CARD_READER} this is for the Network Card
     * reader.
     * <p>
     * Value: LSB | MSB
     * </p>
     */
    CARD_NUMBER_FIRST_BYTE(DeviceAttrDao.CARD_NUMBER_PREFIX + ".first-byte"),

    /**
     * When {@link Device.DeviceTypeEnum#TERMINAL} this is for the LOCAL
     * (Keyboard emulating) Card reader. When
     * {@link Device.DeviceTypeEnum#CARD_READER} this is for the Network Card
     * reader.
     * <p>
     * Value: DEC | HEX
     * </p>
     */
    CARD_NUMBER_FORMAT(DeviceAttrDao.CARD_NUMBER_PREFIX + ".format"),

    /**
     * Reserved for future use.
     */
    CARD_NUMBER_VALIDITY_REGEX(DeviceAttrDao.CARD_NUMBER_PREFIX
            + ".validity-regex");

    /**
     *
     */
    private static class Lookup {

        /**
         *
         */
        private final Map<String, DeviceAttrEnum> enumLookup =
                new HashMap<String, DeviceAttrEnum>();

        /**
         *
         */
        public Lookup() {
            for (DeviceAttrEnum value : DeviceAttrEnum.values()) {
                enumLookup.put(value.dbName, value);
            }
        }

        /**
         *
         * @param key
         *            The key (name).
         * @return The enum.
         */
        public DeviceAttrEnum get(final String key) {
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
     * Gets the DeviceAttrEnum from the database name.
     *
     * @param dbName
     *            The database name
     * @return The {@link DeviceAttrEnum}.
     */
    public static DeviceAttrEnum asEnum(final String dbName) {
        return LookupHolder.INSTANCE.get(dbName);
    }

    /**
     *
     * @param dbName
     *            The database name.
     */
    private DeviceAttrEnum(final String dbName) {
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
