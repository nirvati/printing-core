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

    public static class Detail {

        /** */
        private Long total;

        /** */
        private Long a4;

        /** */
        private Long a3;

        /** */
        private Long simplex;

        /** */
        private Long duplex;

        /** */
        private Long grayscale;

        /** */
        private Long color;

        public Long getTotal() {
            return total;
        }

        public void setTotal(Long total) {
            this.total = total;
        }

        public Long getA4() {
            return a4;
        }

        public void setA4(Long a4) {
            this.a4 = a4;
        }

        public Long getA3() {
            return a3;
        }

        public void setA3(Long a3) {
            this.a3 = a3;
        }

        public Long getSimplex() {
            return simplex;
        }

        public void setSimplex(Long simplex) {
            this.simplex = simplex;
        }

        public Long getDuplex() {
            return duplex;
        }

        public void setDuplex(Long duplex) {
            this.duplex = duplex;
        }

        public Long getGrayscale() {
            return grayscale;
        }

        public void setGrayscale(Long grayscale) {
            this.grayscale = grayscale;
        }

        public Long getColor() {
            return color;
        }

        public void setColor(Long color) {
            this.color = color;
        }

    }

    /** */
    private String userId;

    /** */
    private String userName;

    /** */
    private BigDecimal amount;

    /** */
    private Long transactions;

    private Detail totalPagesSent;
    private Detail totalPagesPrinted;
    private Detail totalSheets;
    private Detail totalJobs;
    private Detail totalCopies;

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

    public Detail getTotalPagesSent() {
        return totalPagesSent;
    }

    public void setTotalPagesSent(Detail totalPagesSent) {
        this.totalPagesSent = totalPagesSent;
    }

    public Detail getTotalPagesPrinted() {
        return totalPagesPrinted;
    }

    public void setTotalPagesPrinted(Detail totalPagesPrinted) {
        this.totalPagesPrinted = totalPagesPrinted;
    }

    public Detail getTotalSheets() {
        return totalSheets;
    }

    public void setTotalSheets(Detail totalSheets) {
        this.totalSheets = totalSheets;
    }

    public Detail getTotalJobs() {
        return totalJobs;
    }

    public void setTotalJobs(Detail totalJobs) {
        this.totalJobs = totalJobs;
    }

    public Detail getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(Detail totalCopies) {
        this.totalCopies = totalCopies;
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
