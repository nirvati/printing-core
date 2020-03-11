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
package org.savapage.core.dao;

import org.savapage.core.dao.enums.IppQueueAttrEnum;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.IppQueueAttr;

/**
 *
 * @author Datraverse B.V.
 *
 */
public interface IppQueueAttrDao extends GenericDao<IppQueueAttr> {

    /**
     * The prefix of rolling statistics {@link IppQueueAttr} names.
     */
    String STATS_ROLLING_PREFIX = "stats.rolling";

    /**
     * Finds the {@link IppQueueAttr} by name.
     *
     * @param queueId
     *            The primary key of the {@link IppQueue}.
     * @param attrName
     *            The {@link IppQueueAttrEnum}.
     * @return The attribute or {@code null} when not found.
     */
    IppQueueAttr findByName(Long queueId, IppQueueAttrEnum attrName);

    /**
     * Deletes all rolling statistics of ALL {@link IppQueue} instances.
     */
    void deleteRollingStats();

}
