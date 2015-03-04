/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.dao;

import java.util.List;

import org.savapage.core.dao.helpers.DeviceTypeEnum;
import org.savapage.core.jpa.Device;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface DeviceDao extends GenericDao<Device> {

    /**
     * Field identifiers used for select and sort.
     */
    enum Field {
        /**
         * Device name.
         */
        NAME
    }

    /**
     *
     */
    class ListFilter {

        private String containingText;
        private Boolean disabled;
        private DeviceTypeEnum deviceType;

        public String getContainingText() {
            return containingText;
        }

        public void setContainingText(String containingText) {
            this.containingText = containingText;
        }

        public Boolean getDisabled() {
            return disabled;
        }

        public void setDisabled(Boolean disabled) {
            this.disabled = disabled;
        }

        public DeviceTypeEnum getDeviceType() {
            return deviceType;
        }

        public void setDeviceType(DeviceTypeEnum deviceType) {
            this.deviceType = deviceType;
        }

    }

    /**
     * Counts the number of devices according to filter.
     *
     * @param filter
     *            The filter.
     * @return The count.
     */
    long getListCount(final ListFilter filter);

    /**
     * Gets a chunk of devices.
     *
     * @param filter
     *            The filter.
     * @param startPosition
     *            The zero-based start position of the chunk related to the
     *            total number of rows. If {@code null} the chunk starts with
     *            the first row.
     * @param maxResults
     *            The maximum number of rows in the chunk. If {@code null}, then
     *            ALL (remaining rows) are returned.
     * @param orderBy
     *            The sort field.
     * @param sortAscending
     *            {@code true} when sorted ascending.
     * @return The chunk.
     */
    List<Device> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending);

    /**
     * Finds the {@link Device} by name, when not found null is returned.
     *
     * @param deviceName
     *            The unique name of the Device.
     * @return The instance, or {@code null} when not found.
     */
    Device findByName(final String deviceName);

    /**
     * Reads the row from database, when not found null is returned.
     *
     * @param deviceName
     *            The unique name of the Device.
     * @return The instance, or {@code null} when not found.
     */

    /**
     * Finds the {@link Device} by host name and {@link DeviceTypeEnum}, when
     * not found null is returned.
     *
     * @param hostname
     *            The host name.
     * @param deviceType
     *            The {@link DeviceTypeEnum}.
     * @return The instance, or {@code null} when not found.
     */
    Device findByHostDeviceType(final String hostname,
            final DeviceTypeEnum deviceType);

    /**
     * Creates or updates the attribute value of a Device.
     *
     * @param device
     *            The Device.
     * @param key
     *            The attribute key.
     * @param value
     *            The attribute value.
     */
    void writeAttribute(Device device, String key, String value);

    /**
     * Checks if {@link Device} is a {@link DeviceTypeEnum#CARD_READER} (this is
     * a convenience method: no database access).
     *
     * @param device
     *            The {@link Device}.
     * @return {@code true} a card reader.
     */
    boolean isCardReader(Device device);

    /**
     * Checks if {@link Device} is a {@link DeviceTypeEnum#TERMINAL} (this is a
     * convenience method: no database access).
     *
     * @param device
     *            The {@link Device}.
     * @return {@code true} when a terminal.
     */
    boolean isTerminal(Device device);

    /**
     * Checks if printers are defined to be accessed via this terminal (this is
     * a convenience method: no database access).
     *
     * @param device
     *            The {@link Device}.
     * @return {@code true} when a printer or printer group is associated with
     *         this terminal.
     */
    boolean hasPrinterRestriction(Device device);

}
