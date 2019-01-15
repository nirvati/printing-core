/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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
package org.savapage.core.dao.enums;

/**
 * Supplier of print data.
 *
 * @author Rijk Ravestein
 *
 */
public enum ExternalSupplierEnum {

    /**
     * G.
     */
    GOOGLE("Google", "google.png"),

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
     * @param uiText
     *            Text to display in user interface.
     * @param imageFileName
     *            Image file name.
     */
    private ExternalSupplierEnum(final String uiText,
            final String imageFileName) {
        this.uiText = uiText;
        this.imageFileName = imageFileName;
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
