/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.dao.helpers;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.SpException;
import org.savapage.core.dao.AppLogDao;
import org.savapage.core.dao.enums.AppLogLevelEnum;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class AppLogPagerReq extends AbstractPagerReq {

    public static class Select {

        @JsonProperty("text")
        private String containingText = null;

        private AppLogLevelEnum level = null;

        @JsonProperty("date_from")
        private Long dateFrom = null;

        @JsonProperty("date_to")
        private Long dateTo = null;

        public AppLogLevelEnum getLevel() {
            return level;
        }

        public void setLevel(AppLogLevelEnum level) {
            this.level = level;
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

        private AppLogDao.Field field = null;
        private Boolean ascending = true;

        public AppLogDao.Field getField() {
            return field;
        }

        public void setField(AppLogDao.Field field) {
            this.field = field;
        }

        public Boolean getAscending() {
            return ascending;
        }

        public void setAscending(Boolean ascending) {
            this.ascending = ascending;
        }

        public AppLogDao.Field getSortField() {
            return field;
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
    public static AppLogPagerReq readReq(final String data) {

        AppLogPagerReq req = null;

        if (data != null) {
            /*
             * Use passed JSON values
             */
            ObjectMapper mapper = new ObjectMapper();
            try {
                req = mapper.readValue(data, AppLogPagerReq.class);
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
            req = new AppLogPagerReq();
        }
        return req;
    }

}
