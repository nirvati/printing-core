/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.config;

import java.util.Date;

import org.savapage.core.util.DateUtil;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SslCertInfo {

    /** */
    private static final long DAYS_IN_MONTH = 30;

    /** */
    private static final long DAYS_IN_YEAR = 365;

    /** */
    private final String issuerCN;
    /** */
    private final Date creationDate;
    /** */
    private final Date notAfter;
    /** */
    private final boolean selfSigned;

    @SuppressWarnings("unused")
    private SslCertInfo() {
        this.issuerCN = null;
        this.creationDate = null;
        this.notAfter = null;
        this.selfSigned = false;
    }

    public SslCertInfo(final String issuerCN, final Date creationDate,
            final Date notAfter, final boolean selfSigned) {
        this.issuerCN = issuerCN;
        this.creationDate = creationDate;
        this.notAfter = notAfter;
        this.selfSigned = selfSigned;
    }

    public String getIssuerCN() {
        return issuerCN;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public Date getNotAfter() {
        return notAfter;
    }

    public boolean isSelfSigned() {
        return selfSigned;
    }

    /**
     * Checks if notAfter date is within a year.
     *
     * @param dateRef
     *            Reference date
     * @return {@code true} when warning is applicable.
     */
    public boolean isNotAfterWithinYear(final Date dateRef) {
        final long delta = this.getNotAfter().getTime() - dateRef.getTime();
        return delta < DateUtil.DURATION_MSEC_DAY * DAYS_IN_YEAR;
    }

    /**
     * Checks if notAfter date is within a month.
     *
     * @param dateRef
     *            Reference date
     * @return {@code true} when warning is applicable.
     */
    public boolean isNotAfterWithinMonth(final Date dateRef) {
        final long delta = this.getNotAfter().getTime() - dateRef.getTime();
        return delta < DateUtil.DURATION_MSEC_DAY * DAYS_IN_MONTH;
    }

}
