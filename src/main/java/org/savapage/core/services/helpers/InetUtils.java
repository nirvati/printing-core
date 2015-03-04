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
package org.savapage.core.services.helpers;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class InetUtils {

    private InetUtils() {

    }

    /**
     * Checks if an IPv4 address in range of a collection of CIDRs.
     * <p>
     * Note: "127.0.0.1" is considered in range.
     * </p>
     *
     * @param cidrRanges
     *            CIDRs separated by any of the characters ' ' (space), ','
     *            (comma), ':' (colon) or ';' (semicolon).
     * @param ipAddr
     *            The IPv4 address.
     * @return {@code true} if in range of at least one (1) CIDR.
     */
    public static boolean isIp4AddrInCidrRanges(final String cidrRanges,
            final String ipAddr) {

        boolean inrange =
                StringUtils.isBlank(cidrRanges) || ipAddr.equals("127.0.0.1");

        if (!inrange) {

            for (final String cidr : StringUtils.split(cidrRanges, " ,;:")) {

                final SubnetUtils utils = new SubnetUtils(cidr);

                /*
                 * setInclusiveHostCount(true) makes CIDR 192.168.1.43/32 a
                 * range of just one (1) IP address.
                 */
                utils.setInclusiveHostCount(true);

                final SubnetUtils.SubnetInfo info = utils.getInfo();
                inrange = info.isInRange(ipAddr);

                if (inrange) {
                    break;
                }
            }
        }

        return inrange;
    }

}
