/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.dao.impl;

import javax.persistence.Query;

import org.savapage.core.dao.DocInDao;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.tools.DbSimpleEntity;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocInDaoImpl extends GenericDaoImpl<DocIn>
        implements DocInDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM DocIn T";
    }

    @Override
    public int eraseUser(final User user) {
        final String jpql = "UPDATE " + DbSimpleEntity.DOC_IN
                + " SET originatorIp = null WHERE id IN" //
                + " (SELECT I.id FROM " + DbSimpleEntity.DOC_LOG + " L"
                + " JOIN " + DbSimpleEntity.DOC_IN + " I ON I.id = L.docIn"
                + " AND L.user = :user)";
        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("user", user.getId());
        return query.executeUpdate();
    }

}
