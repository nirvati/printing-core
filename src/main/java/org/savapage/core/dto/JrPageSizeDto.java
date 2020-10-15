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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.dto;

/**
 * Jasper Report Page Size.
 *
 * @author Rijk Ravestein
 *
 */
public final class JrPageSizeDto extends AbstractDto {

    private static final Integer LETTER_PORTRAIT_WIDTH = Integer.valueOf(612);
    private static final Integer LETTER_PORTRAIT_HEIGHT = Integer.valueOf(792);

    private static final Integer A4_PORTRAIT_WIDTH = Integer.valueOf(595);
    private static final Integer A4_PORTRAIT_HEIGHT = Integer.valueOf(842);

    public static final JrPageSizeDto LETTER_PORTRAIT =
            new JrPageSizeDto(LETTER_PORTRAIT_WIDTH, LETTER_PORTRAIT_HEIGHT);

    public static final JrPageSizeDto LETTER_LANDSCAPE =
            new JrPageSizeDto(LETTER_PORTRAIT_HEIGHT, LETTER_PORTRAIT_WIDTH);

    public static final JrPageSizeDto A4_PORTRAIT =
            new JrPageSizeDto(A4_PORTRAIT_WIDTH, A4_PORTRAIT_HEIGHT);

    public static final JrPageSizeDto A4_LANDSCAPE =
            new JrPageSizeDto(A4_PORTRAIT_HEIGHT, A4_PORTRAIT_WIDTH);

    private JrPageSizeDto() {
    }

    private Integer width;
    private Integer height;

    private JrPageSizeDto(Integer width, Integer height) {
        this.width = width;
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

}
