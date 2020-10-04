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
package org.savapage.core.dao.enums;

import org.savapage.core.jpa.DocLog;

/**
 * Supplier of print data.
 * <ul>
 * <li>Do NOT changes enum names since they are stored in the database.</li>
 * <li>Enum name must fit the size of the
 * {@link DocLog#setExternalSupplier(String)} column.</li>
 * </ul>
 *
 * @author Rijk Ravestein
 *
 */
public enum ExternalSupplierEnum {

    /**
     * Microsoft Azure Active Directory (Azure AD)
     */
    AZURE("Azure", "microsoft.png"),

    /**
     * G.
     */
    GOOGLE("Google", "google.png"),

    /**
     * IPP Client.
     */
    IPP_CLIENT("IPP", "savapage.png"),

    /**
     * <a href="https://www.keycloak.org/about">www.keycloak.org</a>.
     */
    KEYCLOAK("Keycloak", "keycloak.png"),

    /**
     * SavaPage in role as external supplier.
     */
    SAVAPAGE("SavaPage", "savapage.png"),

    /**
     * Smartschool.
     */
    SMARTSCHOOL("Smartschool", "smartschool.jpg");

    /**
     * Text to display in user interface.
     */
    private final String uiText;

    /**
     * Image file name.
     */
    private final String imageFileName;

    /**
     *
     * @param txt
     *            Text to display in user interface.
     * @param img
     *            Image file name.
     */
    ExternalSupplierEnum(final String txt, final String img) {
        this.uiText = txt;
        this.imageFileName = img;
    }

    /**
     * @return Text to display in user interface.
     */
    public String getUiText() {
        return uiText;
    }

    /**
     * @return The image filename.
     */
    public String getImageFileName() {
        return imageFileName;
    }

}
