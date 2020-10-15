/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

import java.util.HashSet;
import java.util.Set;

import org.savapage.core.dao.DeviceAttrDao;
import org.savapage.core.dao.enums.DeviceAttrEnum;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dao.enums.ProxyPrintAuthModeEnum;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.DeviceAttr;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.PrinterGroupMember;
import org.savapage.core.rfid.RfidNumberFormat;
import org.savapage.core.services.DeviceService;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DeviceServiceImpl extends AbstractService
        implements DeviceService {

    @Override
    public RfidNumberFormat createRfidNumberFormat(final Device device,
            final DeviceAttrLookup lookup) {

        final boolean useDeviceAttr;

        if (deviceDAO().isTerminal(device)) {
            useDeviceAttr =
                    lookup.isTrue(DeviceAttrEnum.AUTH_MODE_IS_CUSTOM, false);
        } else {
            useDeviceAttr = true;
        }

        final RfidNumberFormat rfidNumberFormat;

        if (useDeviceAttr) {

            final RfidNumberFormat.FirstByte firstByte;
            final RfidNumberFormat.Format format;

            if (lookup
                    .get(DeviceAttrEnum.CARD_NUMBER_FIRST_BYTE,
                            DeviceAttrDao.VALUE_CARD_NUMBER_LSB)
                    .equals(DeviceAttrDao.VALUE_CARD_NUMBER_LSB)) {
                firstByte = RfidNumberFormat.FirstByte.LSB;
            } else {
                firstByte = RfidNumberFormat.FirstByte.MSB;
            }

            if (lookup
                    .get(DeviceAttrEnum.CARD_NUMBER_FORMAT,
                            DeviceAttrDao.VALUE_CARD_NUMBER_HEX)
                    .equals(DeviceAttrDao.VALUE_CARD_NUMBER_HEX)) {
                format = RfidNumberFormat.Format.HEX;
            } else {
                format = RfidNumberFormat.Format.DEC;
            }

            rfidNumberFormat = new RfidNumberFormat(format, firstByte);

        } else {

            rfidNumberFormat = new RfidNumberFormat();
        }

        return rfidNumberFormat;

    }

    @Override
    public ProxyPrintAuthModeEnum getProxyPrintAuthMode(final Long deviceId) {

        final DeviceAttr attr = deviceAttrDAO().findByName(deviceId,
                DeviceAttrEnum.PROXY_PRINT_AUTH_MODE);

        ProxyPrintAuthModeEnum authMode;

        if (attr == null) {
            authMode = null;
        } else {
            try {
                authMode = ProxyPrintAuthModeEnum.valueOf(attr.getValue());
            } catch (Exception e) {
                authMode = null;
            }
        }
        return authMode;
    }

    @Override
    public Set<String> collectPrinterNames(final Device cardReader) {

        final Set<String> printerNames = new HashSet<>();

        final Printer targetPrinter = cardReader.getPrinter();
        final PrinterGroup targetPrinterGroup = cardReader.getPrinterGroup();

        if (targetPrinter != null) {
            printerNames.add(targetPrinter.getPrinterName());
        }

        if (targetPrinterGroup != null) {

            for (final PrinterGroupMember member : targetPrinterGroup
                    .getMembers()) {
                printerNames.add(member.getPrinter().getPrinterName());
            }

        }

        return printerNames;
    }

    @Override
    public Device getHostTerminal(final String remoteAddr) {
        return deviceDAO().findByHostDeviceType(remoteAddr,
                DeviceTypeEnum.TERMINAL);
    }

}
