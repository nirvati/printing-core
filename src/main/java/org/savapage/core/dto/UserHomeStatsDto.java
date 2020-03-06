/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.dto;

import java.math.BigInteger;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */

@JsonInclude(Include.NON_NULL)
public class UserHomeStatsDto extends AbstractDto {

    @JsonInclude(Include.NON_NULL)
    public static class ScopeCount {

        private long count;

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }

    }

    @JsonInclude(Include.NON_NULL)
    public static class Scope extends ScopeCount {

        private BigInteger size;

        public BigInteger getSize() {
            return size;
        }

        public void setSize(BigInteger size) {
            this.size = size;
        }

    }
    @JsonInclude(Include.NON_NULL)
    public static class Stats {

        private ScopeCount users;
        private Scope inbox;
        private Scope outbox;

        public ScopeCount getUsers() {
            return users;
        }

        public void setUsers(ScopeCount users) {
            this.users = users;
        }

        public Scope getInbox() {
            return inbox;
        }

        public void setInbox(Scope inbox) {
            this.inbox = inbox;
        }

        public Scope getOutbox() {
            return outbox;
        }

        public void setOutbox(Scope outbox) {
            this.outbox = outbox;
        }

    }

    private Date date;
    private boolean cleaned;
    private Stats current;
    private Stats cleanup;


    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Stats getCurrent() {
        return current;
    }

    public void setCurrent(Stats current) {
        this.current = current;
    }

    public Stats getCleanup() {
        return cleanup;
    }

    public void setCleanup(Stats cleanup) {
        this.cleanup = cleanup;
    }

    public boolean isCleaned() {
        return cleaned;
    }

    public void setCleaned(boolean cleaned) {
        this.cleaned = cleaned;
    }

    @Override
    public String toString() {
        return "UserHomeStatsDto [current=" + current + ", cleanup=" + cleanup
                + ", cleaned=" + cleaned + "]";
    }

    public static UserHomeStatsDto create(final String json) {
        return create(UserHomeStatsDto.class, json);
    }

}
