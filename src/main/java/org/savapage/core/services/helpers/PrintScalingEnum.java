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
package org.savapage.core.services.helpers;

import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;

/**
 * IPP print-scaling attribute values according to PWG5100.16.
 *
 * @author Rijk Ravestein
 *
 */
public enum PrintScalingEnum {

    /**
     * If the “ipp-attribute-fidelity” attribute is true or the document is
     * larger than the requested media, scale the document using the 'fit'
     * method if the margins are nonzero, otherwise scale using the 'fill'
     * method. If the “ipp-attribute-fidelity” attribute is false or unspecified
     * and the document is smaller than the requested media, scale using the
     * 'none' method.
     *
     */
    // Reserved for future use.
    // AUTO("auto"),

    /**
     * If the “ipp-attribute-fidelity” attribute is true or the document is
     * larger than the requested media, scale the document using the ‘fit’
     * method. Otherwise, scale using the ‘none’ method.
     */
    // Reserved for future use.
    // AUTO_FIT("auto-fit"),

    /**
     * Scale the document to fill the requested media size, preserving the
     * aspect ratio of the document data but potentially cropping portions of
     * the document.
     */
    // Reserved for future use.
    // FILL("fill"),

    /**
     * Scale the document to fit the printable area of the requested media size,
     * preserving the aspect ratio of the document data without cropping the
     * document.
     */
    FIT("fit"),

    /**
     * Do not scale the document to fit the requested media size. If the
     * document is larger than the requested media, center and clip the
     * resulting output. If the document is smaller than the requested media,
     * center the resulting output.
     */
    NONE("none");

    /**
     * The IPP attribute value.
     */
    private final String ippValue;

    /**
     * The IPP name.
     */
    public static final String IPP_NAME =
            IppDictJobTemplateAttr.ATTR_PRINT_SCALING;

    /**
     *
     * @param value
     *            The IPP value.
     */
    PrintScalingEnum(final String value) {
        this.ippValue = value;
    }

    /**
     * @return The IPP attribute value.
     */
    public String getIppValue() {
        return ippValue;
    }

    /**
     * Get enum from IPP value.
     *
     * @param ippValue
     *            The IPP value (can be {@code null}).
     * @return {@code null} if not found or when parameter is {@code null} .
     */
    public static PrintScalingEnum fromIppValue(final String ippValue) {
        if (ippValue != null) {
            for (final PrintScalingEnum wlk : PrintScalingEnum.values()) {
                if (ippValue.equals(wlk.getIppValue())) {
                    return wlk;
                }
            }
        }
        return null;
    }
}
