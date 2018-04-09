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
package org.savapage.core.snmp;

import org.snmp4j.mp.SnmpConstants;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum SnmpVersionEnum {

    /**
     * SNMP Version 1.
     */
    V1(SnmpConstants.version1, "1"),

    /**
     * SNMP Version 2c.
     */
    V2C(SnmpConstants.version2c, "2c");

    /** */
    private final int version;

    /** */
    private final String cmdLineOption;

    /**
     *
     * @param versionValue
     *            The version as defined in {@link SnmpConstants}.
     * @param cmdLineValue
     *            The value used as command line option.
     */
    SnmpVersionEnum(final int versionValue, final String cmdLineValue) {
        this.version = versionValue;
        this.cmdLineOption = cmdLineValue;
    }

    /**
     *
     * @return The version as defined in {@link SnmpConstants}.
     */
    public int getVersion() {
        return version;
    }

    /**
     *
     * @return The value used as command line option.
     */
    public String getCmdLineOption() {
        return cmdLineOption;
    }

    /**
     * Gets the {@link SnmpVersionEnum} value belonging to the command line
     * option value.
     *
     * @param option
     *            The command line option value.
     * @return {@code null} when not found.
     */
    public static SnmpVersionEnum enumFromCmdLineOption(final String option) {
        for (final SnmpVersionEnum value : SnmpVersionEnum.values()) {
            if (value.getCmdLineOption().equalsIgnoreCase(option)) {
                return value;
            }
        }
        return null;
    }

    /**
     *
     * @return The formatted command line options, like "1|2c|3".
     */
    public static String formattedCmdLineOptions() {

        final StringBuilder opt = new StringBuilder();

        int i = 0;
        for (final SnmpVersionEnum value : SnmpVersionEnum.values()) {
            if (i > 0) {
                opt.append("|");
            }
            opt.append(value.getCmdLineOption());
            i++;
        }
        return opt.toString();
    }

}
