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
package org.savapage.core.dao.impl;

import javax.persistence.Query;

import org.savapage.core.dao.CostChangeDao;
import org.savapage.core.jpa.CostChange;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.tools.DbSimpleEntity;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CostChangeDaoImpl extends GenericDaoImpl<CostChange>
        implements CostChangeDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM CostChange T";
    }

    @Override
    public int eraseUser(final User user) {
        final String jpql = "UPDATE " + DbSimpleEntity.COST_CHANGE
                + " SET reqReason = null, chgReason = null "
                + " WHERE reqUser = :user";
        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("user", user.getId());
        return query.executeUpdate();
    }

}
