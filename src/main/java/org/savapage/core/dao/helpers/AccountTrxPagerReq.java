/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
import org.savapage.core.dao.AccountTrxDao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Bean for mapping JSON page request.
 *
 * @author Datraverse B.V.
 */
public class AccountTrxPagerReq extends AbstractPagerReq {

    /**
     *
     */
    private Select select;

    /**
     *
     */
    private Sort sort;

    /**
     * Reads the page request from the JSON string.
     *
     * @return The page request.
     */
    public static AccountTrxPagerReq read(final String data) {

        AccountTrxPagerReq req = null;

        if (data != null) {
            /*
             * Use passed JSON values
             */
            ObjectMapper mapper = new ObjectMapper();
            try {
                req = mapper.readValue(data, AccountTrxPagerReq.class);
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
            req = new AccountTrxPagerReq();
        }
        return req;
    }

    public static class Select {

        @JsonProperty("user_id")
        private Long userId = null;

        @JsonProperty("account_id")
        private Long accountId = null;

        @JsonProperty("text")
        private String containingText = null;

        @JsonProperty("trxType")
        private AccountTrxTypeEnum trxType = null;

        @JsonProperty("date_from")
        private Long dateFrom = null;

        @JsonProperty("date_to")
        private Long dateTo = null;

        @JsonProperty("user_id")
        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getAccountId() {
            return accountId;
        }

        public void setAccountId(Long accountId) {
            this.accountId = accountId;
        }

        public AccountTrxTypeEnum getTrxType() {
            return trxType;
        }

        public void setTrxType(AccountTrxTypeEnum trxType) {
            this.trxType = trxType;
        }

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
                return DateUtils.truncate(new Date(dateTo
                        + DateUtils.MILLIS_PER_DAY), Calendar.DAY_OF_MONTH);
            }
            return null;
        }

        public String getContainingText() {
            return containingText;
        }

        public void setContainingText(String containingText) {
            this.containingText = containingText;
        }

    }

    public static class Sort {

        private AccountTrxDao.Field field = null;
        private Boolean ascending = true;

        public AccountTrxDao.Field getField() {
            return field;
        }

        public void setField(AccountTrxDao.Field field) {
            this.field = field;
        }

        public Boolean getAscending() {
            return ascending;
        }

        public void setAscending(Boolean ascending) {
            this.ascending = ascending;
        }

    }

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

}
