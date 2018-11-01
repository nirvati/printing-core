/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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

import org.savapage.core.SpException;
import org.savapage.core.dao.UserGroupDao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Bean for mapping JSON page request.
 *
 * @author Rijk Ravestein
 *
 */
public final class UserGroupPagerReq extends AbstractPagerReq {

    /**
     * Reads the page request from the JSON string.
     *
     * @param json
     *            The JSON data.
     * @return The page request.
     */
    public static UserGroupPagerReq read(final String json) {

        UserGroupPagerReq req = null;

        if (json != null) {
            /*
             * Use passed JSON values
             */
            ObjectMapper mapper = new ObjectMapper();
            try {
                req = mapper.readValue(json, UserGroupPagerReq.class);
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
            req = new UserGroupPagerReq();
        }
        return req;
    }

    public static class Select {

        @JsonProperty("name_text")
        private String nameContainingText = null;

        public String getNameContainingText() {
            return nameContainingText;
        }

        public void setNameContainingText(String nameContainingText) {
            this.nameContainingText = nameContainingText;
        }

    }

    public static class Sort {

        public static final String FLD_NAME = "name";

        private String field = null;
        private Boolean ascending = true;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        private boolean isNameSort() {
            return field != null && field.equalsIgnoreCase(FLD_NAME);
        }

        public UserGroupDao.Field getSortField() {
            if (isNameSort()) {
                return UserGroupDao.Field.ID;
            }
            return UserGroupDao.Field.ID;
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

}
