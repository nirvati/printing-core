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
package org.savapage.core.dao.helpers;

import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dto.AbstractDto;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class UserPrintOutTotalsReq extends AbstractDto {

    public enum GroupBy {

        /** */
        USER,

        /** */
        PRINTER_USER;

        /**
         * @param locale
         *            {@link Locale}.
         * @return i18n text.
         */
        public String uiText(final Locale locale) {
            switch (this) {
            case USER:
                return NounEnum.USER.uiText(locale);
            case PRINTER_USER:
                return String.format("%s, %s", NounEnum.PRINTER.uiText(locale),
                        NounEnum.USER.uiText(locale));
            default:
                throw new RuntimeException(
                        String.format("%s not supported", this.toString()));
            }
        }
    }

    public enum Aspect {
        /** */
        PAGES(PrintOutNounEnum.PAGE),
        /** */
        SHEETS(PrintOutNounEnum.SHEET),
        /** */
        JOBS(PrintOutNounEnum.JOB),
        /** */
        COPIES(PrintOutNounEnum.COPY);

        /** */
        private final PrintOutNounEnum noun;

        Aspect(final PrintOutNounEnum n) {
            this.noun = n;
        }

        /**
         * @param locale
         *            {@link Locale}.
         * @return i18n text.
         */
        public String uiText(final Locale locale) {
            return this.noun.uiText(locale, true);
        }
    }

    public enum Pages {
        /** */
        SENT(AdjectiveEnum.SENT),
        /** */
        PRINTED(AdjectiveEnum.PRINTED);

        /** */
        private final AdjectiveEnum adjective;

        Pages(final AdjectiveEnum adj) {
            this.adjective = adj;
        }

        /**
         * @param locale
         *            {@link Locale}.
         * @return i18n text.
         */
        public String uiText(final Locale locale) {
            return this.adjective.uiText(locale);
        }
    }

    private Long timeFrom;
    private Long timeTo;

    private GroupBy groupBy;
    private Aspect aspect;
    private Pages pages;

    /**
     * The list of User Group names.
     */
    private List<String> userGroups;

    public Long getTimeFrom() {
        return timeFrom;
    }

    public void setTimeFrom(Long timeFrom) {
        this.timeFrom = timeFrom;
    }

    public Long getTimeTo() {
        return timeTo;
    }

    public void setTimeTo(Long timeTo) {
        this.timeTo = timeTo;
    }

    public List<String> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(List<String> groups) {
        this.userGroups = groups;
    }

    public GroupBy getGroupBy() {
        return groupBy;
    }

    public void setGroupBy(GroupBy groupBy) {
        this.groupBy = groupBy;
    }

    public Aspect getAspect() {
        return aspect;
    }

    public void setAspect(Aspect aspect) {
        this.aspect = aspect;
    }

    public Pages getPages() {
        return pages;
    }

    public void setPages(Pages pages) {
        this.pages = pages;
    }

    public boolean isGroupedByPrinterUser() {
        return this.groupBy != null && this.groupBy == GroupBy.PRINTER_USER;
    }

    /**
     * @param json
     *            JSON string.
     * @return {@code null} if JSON is blank or invalid.
     */
    public static UserPrintOutTotalsReq create(final String json) {
        if (!StringUtils.isBlank(json)) {
            try {
                return create(UserPrintOutTotalsReq.class, json);
            } catch (Exception e) {
                // noop
            }
        }
        return null;
    }

}
