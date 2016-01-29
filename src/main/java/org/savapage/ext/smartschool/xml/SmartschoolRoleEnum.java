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
package org.savapage.ext.smartschool.xml;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum SmartschoolRoleEnum {

    /**
     *
     */
    STUDENT("student"),

    /**
     *
     */
    TEACHER("teacher");

    /**
     * The XML value.
     */
    private final String xmlValue;

    /**
     *
     * @param role
     *            The XML value;
     */
    private SmartschoolRoleEnum(final String role) {
        this.xmlValue = role;
    }

    /**
     * Maps the XML value to the enum.
     *
     * @param value
     *            The XML value;
     * @return {@code null} when no mapping found.
     */
    public static SmartschoolRoleEnum fromXmlValue(final String value) {
        if (value.equalsIgnoreCase("student")) {
            return SmartschoolRoleEnum.STUDENT;
        } else if (value.equalsIgnoreCase("teacher")) {
            return SmartschoolRoleEnum.TEACHER;
        }
        return null;
    }

    /**
     *
     * @return
     */
    public String getXmlValue() {
        return xmlValue;
    }

}
