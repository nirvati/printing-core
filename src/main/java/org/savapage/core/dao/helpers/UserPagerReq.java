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

import org.savapage.core.SpException;
import org.savapage.core.dao.UserDao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Bean for mapping JSON page request.
 *
 * @author Rijk Ravestein
 *
 */
public class UserPagerReq extends AbstractPagerReq {

    /**
     * Reads the page request from the JSON string.
     *
     * @return The page request.
     */
    public static UserPagerReq read(final String data) {

        UserPagerReq req = null;

        if (data != null) {
            /*
             * Use passed JSON values
             */
            ObjectMapper mapper = new ObjectMapper();
            try {
                req = mapper.readValue(data, UserPagerReq.class);
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
            req = new UserPagerReq();
        }
        return req;
    }

    public static class Select {

        @JsonProperty("usergroup_id")
        private Long userGroupId;

        @JsonProperty("name_id_text")
        private String nameIdContainingText = null;

        @JsonProperty("email_text")
        private String emailContainingText = null;

        private Boolean admin = null;
        private Boolean person = null;
        private Boolean disabled = null;
        private Boolean deleted = null;

        public Long getUserGroupId() {
            return userGroupId;
        }

        public void setUserGroupId(Long userGroupId) {
            this.userGroupId = userGroupId;
        }

        public String getNameIdContainingText() {
            return nameIdContainingText;
        }

        public void setNameIdContainingText(String nameIdContainingText) {
            this.nameIdContainingText = nameIdContainingText;
        }

        public String getEmailContainingText() {
            return emailContainingText;
        }

        public void setEmailContainingText(String emailContainingText) {
            this.emailContainingText = emailContainingText;
        }

        public Boolean getAdmin() {
            return admin;
        }

        public void setAdmin(Boolean admin) {
            this.admin = admin;
        }

        public Boolean getPerson() {
            return person;
        }

        public void setPerson(Boolean person) {
            this.person = person;
        }

        public Boolean getDisabled() {
            return disabled;
        }

        public void setDisabled(Boolean disabled) {
            this.disabled = disabled;
        }

        public Boolean getDeleted() {
            return deleted;
        }

        public void setDeleted(Boolean deleted) {
            this.deleted = deleted;
        }

    }

    public static class Sort {

        public static final String FLD_ID = "id";
        public static final String FLD_EMAIL = "email";

        private String field = null;
        private Boolean ascending = true;

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        private boolean isIdSort() {
            return field != null && field.equalsIgnoreCase(FLD_ID);
        }

        private boolean isEmailSort() {
            return field != null && field.equalsIgnoreCase(FLD_EMAIL);
        }

        public UserDao.Field getSortField() {
            if (isIdSort()) {
                return UserDao.Field.USERID;
            }
            return UserDao.Field.EMAIL;
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
