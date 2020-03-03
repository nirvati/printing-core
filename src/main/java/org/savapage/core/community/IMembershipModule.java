/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.community;

import java.util.Map;
import java.util.Properties;

/**
 * Interface for a Membership module. A module needs an implementation of this
 * class to issue a Member Card file for the module, as well as for
 * interpreting an issued Member Card file.
 *
 * @author Rijk Ravestein
 *
 */
public interface IMembershipModule {

    /**
     * @return The product name.
     */
    String getProduct();

    /**
     * @return The module name.
     */
    String getModule();

    /**
     * @return The major version.
     */
    String getVersionMajor();

    /**
     * @return The minor version.
     */
    String getVersionMinor();

    /**
     * @return The version revision.
     */
    String getVersionRevision();

    /**
     * @return The build version.
     */
    String getVersionBuild();

    /**
     * Gets a map with editable membership properties (used when issuing a
     * Member Card file).
     *
     * @return Map with key (property name) and default value (when {@code null}
     *         , no default is available).
     */
    Map<String, String> getEditableMemberCardProperties();

    /**
     * @return The Member Card property map.
     */
    Map<String, String> getMemberCardProperties();

    /**
     *
     * @param props
     *            The membership properties.
     * @throws MemberCardException
     */
    void checkMemberCardProperties(Properties props)
            throws MemberCardException;

}
