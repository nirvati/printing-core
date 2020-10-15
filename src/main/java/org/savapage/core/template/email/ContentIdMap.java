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
package org.savapage.core.template.email;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.activation.DataSource;

/**
 * Content-ID map.
 *
 * @author Rijk Ravestein
 *
 */
public final class ContentIdMap {

    /**
     * Content-ID.
     */
    private final Map<String, DataSource> map;

    /**
     *
     * @param locale
     */
    public ContentIdMap() {
        this.map = new HashMap<>();
    }

    /**
     * @return The CID map.
     */
    public Map<String, DataSource> getMap() {
        return map;
    }

    /**
     * Adds a {@link DataSource} and assigns a Content-ID.
     *
     * @param src
     *            The {@link DataSource}.
     * @return The Content-ID.
     */
    public String addSource(final DataSource src) {
        final String cid = UUID.randomUUID().toString();
        this.map.put(cid, src);
        return cid;
    }

    /**
     * Adds the CID map.
     *
     * @param cidMap
     *            The {@link ContentIdMap} to add.
     */
    public void addAll(final ContentIdMap cidMap) {
        this.getMap().putAll(cidMap.getMap());
    }
}
