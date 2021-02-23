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

import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.savapage.core.dao.DocInOutDao;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocInOut;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintOut;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DocInOutDaoImpl extends GenericDaoImpl<DocInOut>
        implements DocInOutDao {

    /** */
    protected static final String QPARM_ID = "parm_id";

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM DocInOut T";
    }

    @Override
    public DocLog findDocOutSource(final Long docOutId) {

        final String jpql = "SELECT L FROM DocInOut D " + "JOIN D.docIn I "
                + "JOIN I.docLog L " + "JOIN D.docOut O" + " WHERE O.id = :"
                + QPARM_ID;

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter(QPARM_ID, docOutId);

        DocLog docLog = null;

        try {
            docLog = (DocLog) query.getSingleResult();
        } catch (NoResultException e) {
            docLog = null;
        }

        return docLog;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PrintOut> getPrintOutOfDocIn(final Long docInId) {

        final String jpql = "SELECT P FROM DocInOut D " + "JOIN D.docIn I "
                + "JOIN D.docOut O " + "JOIN O.printOut P " + " WHERE I.id = :"
                + QPARM_ID;

        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter(QPARM_ID, docInId);

        return query.getResultList();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DocIn> getDocInOfDocOut(final Long docOutId) {

        final String jpql = "SELECT I FROM DocInOut D " + "JOIN D.docIn I "
                + "JOIN D.docOut O " + " WHERE O.id = :" + QPARM_ID;

        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter(QPARM_ID, docOutId);

        return query.getResultList();
    }

}
