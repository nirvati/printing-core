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

import org.savapage.core.jpa.Entity;
import org.savapage.core.jpa.User;

/**
 * A generic DOA for an {@link Entity} that can be erased, complying to GDPR
 * Data Erasure.
 *
 * @param <T>
 *            The Entity.
 *
 * @author Rijk Ravestein
 *
 */
public interface UserErasableDao<T extends Entity> extends GenericDao<T> {

    /**
     * Erases JPA objects for a {@link User}, complying to GDPR Data Erasure.
     *
     * @param user
     *            The {@link User}.
     * @return The number of erased objects.
     */
    int eraseUser(User user);
}
