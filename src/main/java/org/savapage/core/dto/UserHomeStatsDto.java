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

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

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

        @JsonProperty("lh")
        private Scope letterheads;

        @JsonProperty("pgpring")
        private Scope pgpPubRing;

        private Scope unkown;

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

        public Scope getLetterheads() {
            return letterheads;
        }

        public void setLetterheads(Scope letterheads) {
            this.letterheads = letterheads;
        }

        public Scope getPgpPubRing() {
            return pgpPubRing;
        }

        public void setPgpPubRing(Scope pgpPubRing) {
            this.pgpPubRing = pgpPubRing;
        }

        public Scope getUnkown() {
            return unkown;
        }

        public void setUnkown(Scope unkown) {
            this.unkown = unkown;
        }

    }

    private Date date;

    @JsonProperty("rc")
    private int returnCode;

    private boolean cleaned;
    private Stats current;
    private Stats cleanup;

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * @return {@code 0}, if User Home was completely scanned.
     */
    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
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

    /**
     * @return {@code true} if User Home scanning was not prematurely
     *         terminated.
     */
    @JsonIgnore
    public boolean isFullyScanned() {
        return this.returnCode == 0;
    }

    /**
     * @return Calculates number of files scanned.
     */
    @JsonIgnore
    public long calcScannedFiles() {
        return sumFiles(this.current);
    }

    /**
     * @return Calculates number of file bytes scanned.
     */
    @JsonIgnore
    public BigInteger calcScannedBytes() {
        return sumBytes(this.current);
    }

    /**
     * @return Calculates number of files scanned.
     */
    @JsonIgnore
    public long calcCleanupFiles() {
        return sumFiles(this.cleanup);
    }

    /**
     * @return Calculates number of file bytes scanned.
     */
    @JsonIgnore
    public BigInteger calcCleanupBytes() {
        return sumBytes(this.cleanup);
    }
    /**
     * @param stats
     *            Stats.
     * @return Sums number of files from stats.
     */
    private static long sumFiles(final Stats stats) {

        long tot = 0L;

        if (stats.inbox != null) {
            tot += stats.inbox.getCount();
        }
        if (stats.outbox != null) {
            tot += stats.outbox.getCount();
        }
        if (stats.letterheads != null) {
            tot += stats.letterheads.getCount();
        }
        if (stats.pgpPubRing != null) {
            tot += stats.pgpPubRing.getCount();
        }
        return tot;
    }

    /**
     * @param stats
     *            Stats.
     * @return Sums number of bytes from stats.
     */
    private static BigInteger sumBytes(final Stats stats) {

        BigInteger tot = BigInteger.ZERO;

        if (stats.inbox != null) {
            tot = tot.add(stats.inbox.getSize());
        }
        if (stats.outbox != null) {
            tot = tot.add(stats.outbox.getSize());
        }
        if (stats.letterheads != null) {
            tot = tot.add(stats.letterheads.getSize());
        }
        if (stats.pgpPubRing != null) {
            tot = tot.add(stats.pgpPubRing.getSize());
        }
        return tot;
    }

    @Override
    public String toString() {
        return "UserHomeStatsDto [current=" + current + ", cleanup=" + cleanup
                + ", cleaned=" + cleaned + "]";
    }

    /**
     * @param json
     *            JSON string.
     * @return {@code null} if JSON is blank or invalid.
     */
    public static UserHomeStatsDto create(final String json) {
        if (!StringUtils.isBlank(json)) {
            try {
                return create(UserHomeStatsDto.class, json);
            } catch (Exception e) {
                // noop
            }
        }
        return null;
    }

}
