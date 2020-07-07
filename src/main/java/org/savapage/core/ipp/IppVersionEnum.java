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
package org.savapage.core.ipp;

/**
 * IPP version.
 *
 * @author Rijk Ravestein
 *
 */
public enum IppVersionEnum {

    /** */
    V_1_0(IppVersionEnum.MAJOR_VER_1, 0, "1.0", true),

    /** */
    V_1_1(IppVersionEnum.MAJOR_VER_1, 1, "1.1", true),

    /**
     * PWG 5100.12. Required for IPP Everywhere (PWG 5100.14).
     */
    V_2_0(IppVersionEnum.MAJOR_VER_2, 0, "2.0", true),

    /** PWG 5100.12. */
    V_2_1(IppVersionEnum.MAJOR_VER_2, 1, "2.1", false),

    /** PWG 5100.12. */
    V_2_2(IppVersionEnum.MAJOR_VER_2, 2, "2.2", false);

    /** */
    public static final int MAJOR_VER_1 = 1;
    /** */
    public static final int MAJOR_VER_2 = 2;

    /**
     * IPP major version.
     */
    private final int major;

    /**
     * IPP minor version.
     */
    private final int minor;

    /**
     * IPP version type2 keyword.
     */
    private final String keyword;

    /**
     * {@code true} if supported by SavaPage IPP Printer.
     */
    private final boolean supported;

    /**
     * @param vMajor
     *            IPP major version.
     * @param vMinor
     *            IPP minor version.
     * @param vKeyword
     *            IPP version type2 keyword.
     * @param vSupported
     *            {@code true} if supported by SavaPage IPP Printer.
     */
    IppVersionEnum(final int vMajor, final int vMinor, final String vKeyword,
            final boolean vSupported) {
        this.major = vMajor;
        this.minor = vMinor;
        this.keyword = vKeyword;
        this.supported = vSupported;
    }

    /**
     * @return IPP major version.
     */
    public int getVersionMajor() {
        return this.major;
    }

    /**
     * @return IPP minor version.
     */
    public int getVersionMinor() {
        return this.minor;
    }

    /**
     * @return IPP version as type2 keyword.
     */
    public String getVersionKeyword() {
        return this.keyword;
    }

    /**
     * @return {@code true} if supported by SavaPage IPP Printer.
     */
    public boolean isSupported() {
        return this.supported;
    }

    /**
     * @return {@code true} if IPP/2.x
     */
    public boolean isIPPversion2() {
        return isIPPversion2(this.major);
    }

    /**
     * @param major
     *            IPP major version.
     * @return {@code true} if IPP/2.x
     */
    public static boolean isIPPversion2(final int major) {
        return major == MAJOR_VER_2;
    }

    /**
     * @param vMajor
     *            IPP major version.
     * @param vMinor
     *            IPP minor version.
     * @return {@code true} if supported by SavaPage IPP Printer.
     */
    public static boolean isSupported(final int vMajor, final int vMinor) {
        for (final IppVersionEnum version : IppVersionEnum.values()) {
            if (version.isSupported() && version.major == vMajor
                    && version.minor == vMinor) {
                return true;
            }
        }
        return false;
    }
}
