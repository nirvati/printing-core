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
package org.savapage.core.print.proxy;

/**
 * Proxy Print Request base on the SafePages inbox.
 *
 * @author Rijk Ravestein
 *
 */
public final class ProxyPrintInboxReq extends AbstractProxyPrintReq {

    /**
     * All the SafePages.
     */
    public static final String PAGE_RANGES_ALL = "";

    /**
     *
     */
    private String pageRanges;

    /**
     * .
     */
    private final Integer pageRangesJobIndex;

    /**
     *
     * @param pageRangesJobIndex
     *            {@code null} when {@link #pageRanges} are related to the
     *            virtual inbox document. Otherwise the zero-based index of the
     *            job the {@link #pageRanges} belong to.
     */
    public ProxyPrintInboxReq(final Integer pageRangesJobIndex) {
        super(null);
        this.pageRangesJobIndex = pageRangesJobIndex;
    }

    /**
     *
     * @return
     */
    public String getPageRanges() {
        return pageRanges;
    }

    /**
     *
     * @param pageRanges
     */
    public void setPageRanges(String pageRanges) {
        this.pageRanges = pageRanges;
    }

    /**
     *
     * @return {@code null} when {@link #pageRanges} are related to the virtual
     *         inbox document, or the zero-based index of the job the
     *         {@link #pageRanges} belong to.
     */
    public Integer getPageRangesJobIndex() {
        return pageRangesJobIndex;
    }

}
