/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
 * Authors: Rijk Ravestein.
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

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JobTicketLabelDto {

    private final String domain;
    private final String use;
    private final String tag;

    /**
     *
     * @param domain
     *            The domain (to be pre-pended to the generated ticket number).
     *            Can be {@code null} or empty.
     * @param use
     *            The use (to be pre-pended to the generated ticket number). Can
     *            be {@code null} or empty.
     * @param tag
     *            The tag (to be pre-pended to the generated ticket number). Can
     *            be {@code null} or empty.
     */
    public JobTicketLabelDto(final String domain, final String use,
            final String tag) {
        this.domain = domain;
        this.use = use;
        this.tag = tag;
    }

    /**
     * @return The domain (to be pre-pended to the generated ticket number). Can
     *         be {@code null} or empty.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return The use (to be pre-pended to the generated ticket number). Can be
     *         {@code null} or empty.
     */
    public String getUse() {
        return use;
    }

    /**
     * @return The tag (to be pre-pended to the generated ticket number). Can be
     *         {@code null} or empty.
     */
    public String getTag() {
        return tag;
    }

}
