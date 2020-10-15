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

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Financial information meant for display purposes, formatted according to a
 * {@link Locale}.
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public class FinancialDisplayInfoDto extends AbstractDto {

    public static class Stats {

        private String count;
        private String sum;
        private String min;
        private String max;
        private String avg;

        public String getCount() {
            return count;
        }

        public void setCount(String count) {
            this.count = count;
        }

        public String getSum() {
            return sum;
        }

        public void setSum(String sum) {
            this.sum = sum;
        }

        public String getMin() {
            return min;
        }

        public void setMin(String min) {
            this.min = min;
        }

        public String getMax() {
            return max;
        }

        public void setMax(String max) {
            this.max = max;
        }

        public String getAvg() {
            return avg;
        }

        public void setAvg(String avg) {
            this.avg = avg;
        }

    }

    /**
     * The locale (languageTag) of the amount strings (e.g. {@code en-US}) See
     * {@link Locale#toLanguageTag()}.
     */
    private String locale;

    private Stats userDebit;
    private Stats userCredit;

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Stats getUserDebit() {
        return userDebit;
    }

    public void setUserDebit(Stats userDebit) {
        this.userDebit = userDebit;
    }

    public Stats getUserCredit() {
        return userCredit;
    }

    public void setUserCredit(Stats userCredit) {
        this.userCredit = userCredit;
    }

}
