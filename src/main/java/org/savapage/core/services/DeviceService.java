/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.services;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.enums.DeviceAttrEnum;
import org.savapage.core.dao.enums.ProxyPrintAuthModeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.DeviceAttr;
import org.savapage.core.rfid.RfidNumberFormat;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface DeviceService {

    /**
     * Encapsulation of Device attribute map.
     */
    class DeviceAttrLookup {

        /**
         *
         */
        private final Map<String, String> lookup;

        /**
         *
         * @param device
         *            The Device.
         */
        public DeviceAttrLookup(final Device device) {
            this.lookup = new HashMap<>();
            if (device.getAttributes() != null) {
                for (final DeviceAttr attr : device.getAttributes()) {
                    lookup.put(attr.getName(), attr.getValue());
                }
            }
        }

        /**
         * Gets the attribute value.
         *
         * @param key
         *            The attribute key.
         * @return The attribute value, or {@code null} when not found.
         */
        public String get(final DeviceAttrEnum key) {
            return lookup.get(key.getDbName());
        }

        /**
         * Gets the attribute value.
         *
         * @param key
         *            The attribute key.
         * @param dfault
         *            The default value.
         * @return The attribute value, or the default when not found.
         */
        public String get(final DeviceAttrEnum key, final String dfault) {
            String value = lookup.get(key.getDbName());
            if (value == null) {
                value = dfault;
            }
            return value;
        }

        /**
         * Checks if the value of the attribute key represents "true" value.
         *
         * @param key
         *            The attribute key.
         * @param dfault
         *            The default value.
         * @return {@code true} when key value represents true.
         */
        public boolean isTrue(final DeviceAttrEnum key, final boolean dfault) {

            final boolean bValue;
            final String value = this.get(key);

            if (value == null) {
                bValue = dfault;
            } else {
                bValue = value.equals(IConfigProp.V_YES);
            }

            return bValue;
        }

    }

    /**
     * Creates the {@link RfidNumberFormat} for a {@link Device}.
     *
     * @param device
     *            The device.
     * @param lookup
     *            The device attribute lookup.
     * @return The {@link RfidNumberFormat}.
     */
    RfidNumberFormat createRfidNumberFormat(Device device,
            DeviceAttrLookup lookup);

    /**
     * Gets the {@link ProxyPrintAuthModeEnum} for a card reader device.
     *
     * @param deviceId
     *            The primary key of the reader {@link Device}.
     * @return {@code null} if not present (device is not a card reader, or does
     *         not support proxy print authentication).
     */
    ProxyPrintAuthModeEnum getProxyPrintAuthMode(Long deviceId);

    /**
     * Collects the single printer name and printer names of the printer group
     * of a card reader.
     *
     * @param cardReader
     *            The card reader.
     * @return The set of printer names.
     */
    Set<String> collectPrinterNames(Device cardReader);

}
