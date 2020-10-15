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
package org.savapage.core.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class JobTicketProperties extends JsonAbstractBase {

    /**
     * Domain label ID.
     */
    private String domain;

    /**
     *
     * @return Domain label ID (can be {@code null}).
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @param domain
     *            Domain label ID (can be {@code null}).
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Creates an instance from JSON string.
     *
     * @param json
     *            JSON string
     * @return Object.
     * @throws Exception
     *             If syntax error.
     */
    public static JobTicketProperties create(final String json)
            throws Exception {
        return getMapper().readValue(json, JobTicketProperties.class);
    }
}
