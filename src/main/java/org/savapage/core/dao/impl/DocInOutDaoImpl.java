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

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.savapage.core.dao.DocInOutDao;
import org.savapage.core.jpa.DocInOut;
import org.savapage.core.jpa.DocLog;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocInOutDaoImpl extends GenericDaoImpl<DocInOut>
        implements DocInOutDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM DocInOut T";
    }

    @Override
    public DocLog findDocOutSource(final Long docOutId) {

        final String jpql = "SELECT L FROM DocInOut D " + "JOIN D.docIn I "
                + "JOIN I.docLog L " + "JOIN D.docOut O"
                + " WHERE O.id = :docOutId";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("docOutId", docOutId);

        DocLog docLog = null;

        try {
            docLog = (DocLog) query.getSingleResult();
        } catch (NoResultException e) {
            docLog = null;
        }

        return docLog;
    }

}
