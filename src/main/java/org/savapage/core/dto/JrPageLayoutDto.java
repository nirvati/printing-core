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

import net.sf.jasperreports.engine.type.PrintOrderEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class JrPageLayoutDto extends AbstractDto {

    private JrPageSizeDto pageSize = JrPageSizeDto.A4_PORTRAIT;

    private Integer columnWidth;
    private Integer columnCount;
    private PrintOrderEnum printOrder = PrintOrderEnum.HORIZONTAL;
    private Integer columnSpacing;
    private Integer leftMargin;
    private Integer rightMargin;
    private Integer topMargin;
    private Integer bottomMargin;

    public JrPageSizeDto getPageSize() {
        return pageSize;
    }

    public void setPageSize(JrPageSizeDto pageSize) {
        this.pageSize = pageSize;
    }

    public Integer getColumnWidth() {
        return columnWidth;
    }

    public void setColumnWidth(Integer columnWidth) {
        this.columnWidth = columnWidth;
    }

    public Integer getColumnCount() {
        return columnCount;
    }

    public void setColumnCount(Integer columnCount) {
        this.columnCount = columnCount;
    }

    public PrintOrderEnum getPrintOrder() {
        return printOrder;
    }

    public void setPrintOrder(PrintOrderEnum printOrder) {
        this.printOrder = printOrder;
    }

    public Integer getColumnSpacing() {
        return columnSpacing;
    }

    public void setColumnSpacing(Integer columnSpacing) {
        this.columnSpacing = columnSpacing;
    }

    public Integer getLeftMargin() {
        return leftMargin;
    }

    public void setLeftMargin(Integer leftMargin) {
        this.leftMargin = leftMargin;
    }

    public Integer getRightMargin() {
        return rightMargin;
    }

    public void setRightMargin(Integer rightMargin) {
        this.rightMargin = rightMargin;
    }

    public Integer getTopMargin() {
        return topMargin;
    }

    public void setTopMargin(Integer topMargin) {
        this.topMargin = topMargin;
    }

    public Integer getBottomMargin() {
        return bottomMargin;
    }

    public void setBottomMargin(Integer bottomMargin) {
        this.bottomMargin = bottomMargin;
    }

}
