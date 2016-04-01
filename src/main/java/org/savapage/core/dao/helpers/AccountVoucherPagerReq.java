/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.dao.helpers;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.SpException;
import org.savapage.core.dao.AccountVoucherDao;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class AccountVoucherPagerReq extends AbstractPagerReq {

    public static class Select {

        private String batch = null;

        private String number = null;

        private String userId = null;

        private Long dateFrom = null;

        private Long dateTo = null;

        private Boolean used = null;
        private Boolean expired = null;

        public Long getDateFrom() {
            return dateFrom;
        }

        public void setDateFrom(Long dateFrom) {
            this.dateFrom = dateFrom;
        }

        public Long getDateTo() {
            return dateTo;
        }

        public void setDateTo(Long dateTo) {
            this.dateTo = dateTo;
        }

        /**
         * Gets the truncated day of dateFrom.
         */
        public Date dateFrom() {
            if (dateFrom != null) {
                return DateUtils.truncate(new Date(dateFrom),
                        Calendar.DAY_OF_MONTH);
            }
            return null;
        }

        /**
         * Gets the truncated next day of dateTo.
         *
         * @return
         */
        public Date dateTo() {
            if (dateTo != null) {
                return DateUtils.truncate(
                        new Date(dateTo + DateUtils.MILLIS_PER_DAY),
                        Calendar.DAY_OF_MONTH);
            }
            return null;
        }

        public String getBatch() {
            return batch;
        }

        public void setBatch(String batch) {
            this.batch = batch;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public Boolean getUsed() {
            return used;
        }

        public void setUsed(Boolean used) {
            this.used = used;
        }

        public Boolean getExpired() {
            return expired;
        }

        public void setExpired(Boolean expired) {
            this.expired = expired;
        }

    }

    public static class Sort {

        private AccountVoucherDao.Field field = null;
        private Boolean ascending = true;

        public AccountVoucherDao.Field getField() {
            return field;
        }

        public void setField(AccountVoucherDao.Field field) {
            this.field = field;
        }

        public Boolean getAscending() {
            return ascending;
        }

        public void setAscending(Boolean ascending) {
            this.ascending = ascending;
        }

    }

    private Select select;
    private Sort sort;

    public Select getSelect() {
        return select;
    }

    public void setSelect(Select select) {
        this.select = select;
    }

    public Sort getSort() {
        return sort;
    }

    public void setSort(Sort sort) {
        this.sort = sort;
    }

    /**
     * Reads the page request from the JSON string.
     *
     * @return The page request.
     */
    public static AccountVoucherPagerReq readReq(final String data) {

        AccountVoucherPagerReq req = null;

        if (data != null) {
            /*
             * Use passed JSON values
             */
            ObjectMapper mapper = new ObjectMapper();
            try {
                req = mapper.readValue(data, AccountVoucherPagerReq.class);
            } catch (IOException e) {
                throw new SpException(e.getMessage());
            }
        }
        /*
         * Check inputData separately, since JSON might not have delivered the
         * right parameters and the mapper returned null.
         */
        if (req == null) {
            /*
             * Use the defaults
             */
            req = new AccountVoucherPagerReq();
        }
        return req;
    }

}
