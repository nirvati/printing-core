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
package org.savapage.core.dto;

import java.math.BigDecimal;
import java.util.Date;

/**
 * User PrintOut total.
 *
 * @author Rijk Ravestein
 *
 */
public class UserPrintOutTotalDto extends AbstractDto {

    /** */
    private String userId;

    /** */
    private String userName;

    /** */
    private BigDecimal amount;

    /** */
    private Long transactions;

    /** */
    private Long copies;

    /** */
    private Long pages;

    /** */
    private Long pagesA4;

    /** */
    private Long pagesA3;

    /** */
    private Long pagesSinglex;

    /** */
    private Long pagesDuplex;

    /** */
    private Long pagesGrayscale;

    /** */
    private Long pagesColor;

    /** */
    private String userGroup;

    /** */
    private Date dateFrom;

    /** */
    private Date dateTo;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getTransactions() {
        return transactions;
    }

    public void setTransactions(Long transactions) {
        this.transactions = transactions;
    }

    public Long getCopies() {
        return copies;
    }

    public void setCopies(Long copies) {
        this.copies = copies;
    }

    public Long getPages() {
        return pages;
    }

    public void setPages(Long pages) {
        this.pages = pages;
    }

    public Long getPagesA4() {
        return pagesA4;
    }

    public void setPagesA4(Long pagesA4) {
        this.pagesA4 = pagesA4;
    }

    public Long getPagesA3() {
        return pagesA3;
    }

    public void setPagesA3(Long pagesA3) {
        this.pagesA3 = pagesA3;
    }

    public Long getPagesSinglex() {
        return pagesSinglex;
    }

    public void setPagesSinglex(Long pagesSinglex) {
        this.pagesSinglex = pagesSinglex;
    }

    public Long getPagesDuplex() {
        return pagesDuplex;
    }

    public void setPagesDuplex(Long pagesDuplex) {
        this.pagesDuplex = pagesDuplex;
    }

    public Long getPagesGrayscale() {
        return pagesGrayscale;
    }

    public void setPagesGrayscale(Long pagesGrayscale) {
        this.pagesGrayscale = pagesGrayscale;
    }

    public Long getPagesColor() {
        return pagesColor;
    }

    public void setPagesColor(Long pagesColor) {
        this.pagesColor = pagesColor;
    }

    public String getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(String klas) {
        this.userGroup = klas;
    }

    public Date getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(Date dateFrom) {
        this.dateFrom = dateFrom;
    }

    public Date getDateTo() {
        return dateTo;
    }

    public void setDateTo(Date dateTo) {
        this.dateTo = dateTo;
    }

}
