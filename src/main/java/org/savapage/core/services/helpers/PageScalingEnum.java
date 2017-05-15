/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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

/**
 * Page Scaling options according to PWG5100.16.
 *
 * @author Rijk Ravestein
 *
 */
public enum PageScalingEnum {

    /**
     * If the “ipp-attribute-fidelity” attribute is true or the document is
     * larger than the requested media, scale the document using the 'fit'
     * method if the margins are nonzero, otherwise scale using the 'fill'
     * method. If the “ipp-attribute-fidelity” attribute is false or unspecified
     * and the document is smaller than the requested media, scale using the
     * 'none' method.
     *
     */
    AUTO,

    /**
     * If the “ipp-attribute-fidelity” attribute is true or the document is
     * larger than the requested media, scale the document using the ‘fit’
     * method. Otherwise, scale using the ‘none’ method.
     */
    AUTO_FIT,

    /**
     * Scale the document to fill the requested media size, preserving the
     * aspect ratio of the document data but potentially cropping portions of
     * the document.
     */
    FILL,

    /**
     * Scale the document to fit the printable area of the requested media size,
     * preserving the aspect ratio of the document data without cropping the
     * document.
     */
    FIT,

    /**
     * Do not scale the document to fit the requested media size. If the
     * document is larger than the requested media, center and clip the
     * resulting output. If the document is smaller than the requested media,
     * center the resulting output.
     */
    NONE
}
