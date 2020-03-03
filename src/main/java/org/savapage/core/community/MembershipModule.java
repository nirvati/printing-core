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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.math.NumberUtils;
import org.savapage.core.VersionInfo;

/**
 * The Member Card properties for this module. This class is used to issue a
 * Member Card file for this module, as well as interpreting an issued Member
 * Card file.
 *
 * @author Rijk Ravestein
 *
 */
public final class MembershipModule implements IMembershipModule {

    /**
     * The prefix of all Member Card properties.
     */
    private static final String PFX_MEMBERCARD_PROP = VersionInfo.MODULE + "-";

    /**
     * Property key for major version.
     */
    public static final String CARD_PROP_MEMBERCARD_VERSION_MAJOR =
            PFX_MEMBERCARD_PROP
                    + MemberCardManager.CARD_PROP_SAVAPAGE_VERSION_MAJOR;

    /**
     * Property key for minor version.
     */
    public static final String CARD_PROP_MEMBERCARD_VERSION_MINOR =
            PFX_MEMBERCARD_PROP
                    + MemberCardManager.CARD_PROP_SAVAPAGE_VERSION_MINOR;

    /**
     * Property key for revision version.
     */
    public static final String CARD_PROP_MEMBERCARD_VERSION_REVISION =
            PFX_MEMBERCARD_PROP
                    + MemberCardManager.CARD_PROP_SAVAPAGE_VERSION_REVISION;

    /**
     * Property key for member organization type.
     */
    public static final String CARD_MEMBER_TYPE = "member-type";

    /**
     * Property key for number of participants in the fellow organization.
     */
    public static final String CARD_MEMBER_PARTICIPANTS = "member-participants";

    /**
     * Property key for number of participants donated for.
     */
    public static final String CARD_MEMBER_PARTICIPANTS_DONATED =
            "member-participants-donated";

    @Override
    public Map<String, String> getEditableMemberCardProperties() {
        Map<String, String> map = new HashMap<String, String>();
        map.put(CARD_MEMBER_TYPE,
                CommunityMemberTypeEnum.EDUCATIONAL.toString());
        map.put(CARD_MEMBER_PARTICIPANTS_DONATED, null);
        map.put(CARD_MEMBER_PARTICIPANTS, null);
        return map;
    }

    /**
     *
     * @param value
     *            The {@link CommunityMemberTypeEnum} as String.
     * @return {@code true} when valid.
     */
    public static boolean isCommunityMemberTypeValid(final String value) {

        for (final CommunityMemberTypeEnum enumObj : CommunityMemberTypeEnum
                .values()) {
            if (enumObj.toString().equals(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param key
     *            The key.
     * @param value
     *            The value.
     * @return {@code true} when value is valid or key is unknown.
     */
    public static boolean checkEditableMemberCardProperty(final String key,
            final String value) {

        final boolean isValid;

        switch (key) {

        case CARD_MEMBER_TYPE:
            isValid = isCommunityMemberTypeValid(value);
            break;

        case CARD_MEMBER_PARTICIPANTS:
        case CARD_MEMBER_PARTICIPANTS_DONATED:
            isValid = NumberUtils.isDigits(value);
            break;

        default:
            isValid = true;
            break;
        }
        return isValid;
    }

    @Override
    public Map<String, String> getMemberCardProperties() {
        Map<String, String> map = new HashMap<String, String>();
        return map;
    }

    @Override
    public void checkMemberCardProperties(final Properties props)
            throws MemberCardException {
        // no code intended (yet)
    }

    @Override
    public String getProduct() {
        return VersionInfo.PRODUCT;
    }

    @Override
    public String getModule() {
        return VersionInfo.MODULE;
    }

    @Override
    public String getVersionMajor() {
        return VersionInfo.VERSION_A_MAJOR;
    }

    @Override
    public String getVersionMinor() {
        return VersionInfo.VERSION_B_MINOR;
    }

    @Override
    public String getVersionRevision() {
        return VersionInfo.VERSION_C_REVISION;
    }

    @Override
    public String getVersionBuild() {
        return VersionInfo.VERSION_E_BUILD;
    }
}
