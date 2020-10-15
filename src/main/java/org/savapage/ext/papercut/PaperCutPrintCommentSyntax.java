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
package org.savapage.ext.papercut;

/**
 * Syntax for formatting SavaPage Print info in PaperCut Account Transaction
 * comment.
 *
 * @author Rijk Ravestein
 *
 */
public final class PaperCutPrintCommentSyntax {

    /**
     * .
     */
    public static final String JOB_NAME_INFO_SEPARATOR = ".";

    /**
     * Symbol for a dummy klas.
     */
    public static final String DUMMY_KLAS = "-";

    /**
     * .
     */
    private static final String FIELD_SEPARATOR_CHAR = "|";

    /**
     * .
     */
    public static final String FIELD_SEPARATOR =
            " " + FIELD_SEPARATOR_CHAR + " ";

    /**
     * .
     */
    public static final String FIELD_SEPARATOR_FIRST = "";

    /**
     * .
     */
    public static final String FIELD_SEPARATOR_LAST = "";

    /**
     * .
     */
    public static final char USER_CLASS_SEPARATOR = '@';

    /**
     * Duplex.
     */
    public static final String INDICATOR_DUPLEX_ON = "D";

    /**
     * Singlex.
     */
    public static final String INDICATOR_DUPLEX_OFF = "S";

    /**
     * Color.
     */
    public static final String INDICATOR_COLOR_ON = "C";

    /**
     * Grayscale.
     */
    public static final String INDICATOR_COLOR_OFF = "G";

    /**
     * Hide instantiation.
     */
    private PaperCutPrintCommentSyntax() {

    }

}
