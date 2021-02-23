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

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.dao.IAttrDao;
import org.savapage.core.dao.IppQueueAttrDao;
import org.savapage.core.dao.enums.IppQueueAttrEnum;
import org.savapage.core.jpa.IppQueueAttr;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppQueueAttrDaoImpl extends GenericDaoImpl<IppQueueAttr>
        implements IppQueueAttrDao, IAttrDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM IppQueueAttr T";
    }

    /**
     * This SQL LIKE value is used to DELETE all rolling statistics.
     */
    private static final String SQL_LIKE_STATS_ROLLING =
            STATS_ROLLING_PREFIX + "%";

    @Override
    public IppQueueAttr findByName(final Long queueId,
            final IppQueueAttrEnum attrName) {

        final String jpql = "SELECT A FROM IppQueueAttr A JOIN A.queue Q "
                + "WHERE Q.id = :queueId AND A.name = :name";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("queueId", queueId);
        query.setParameter("name", attrName.getDbName());

        IppQueueAttr attr;

        try {
            attr = (IppQueueAttr) query.getSingleResult();
        } catch (NoResultException e) {
            attr = null;
        }

        return attr;

    }

    @Override
    public void deleteRollingStats() {
        final String jpql = "DELETE IppQueueAttr A WHERE A.name LIKE :name";
        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("name", SQL_LIKE_STATS_ROLLING);
        query.executeUpdate();
    }

    @Override
    public boolean getBooleanValue(final IppQueueAttr attr) {
        return attr != null && StringUtils.defaultString(attr.getValue(), V_NO)
                .equalsIgnoreCase(V_YES);
    }

    @Override
    public String getDbBooleanValue(final boolean value) {
        if (value) {
            return V_YES;
        }
        return V_NO;
    }

}
