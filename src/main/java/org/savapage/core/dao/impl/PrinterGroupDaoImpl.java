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

import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.PrinterGroupDao;
import org.savapage.core.jpa.PrinterGroup;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PrinterGroupDaoImpl extends GenericDaoImpl<PrinterGroup>
        implements PrinterGroupDao {

    @Override
    protected String getCountQuery() {
        return "SELECT COUNT(T.id) FROM PrinterGroup T";
    }

    @Override
    public int prunePrinterGroups() {

        final String groupName;

        if (ConfigManager.instance()
                .isConfigValue(Key.PROXY_PRINT_NON_SECURE)) {
            groupName = ConfigManager.instance()
                    .getConfigValue(Key.PROXY_PRINT_NON_SECURE_PRINTER_GROUP);
        } else {
            groupName = null;
        }

        final String jpqlMain = "DELETE FROM PrinterGroup P WHERE";

        final String jpqlWhere = " (SELECT COUNT(M) FROM P.members M) = 0"
                + " AND (SELECT COUNT(D) FROM P.devices D) = 0";

        final Query query;

        if (StringUtils.isNotBlank(groupName)) {

            final String qry =
                    jpqlMain + " P.groupName != :groupName AND " + jpqlWhere;

            query = this.getEntityManager().createQuery(qry);
            query.setParameter("groupName", groupName.toLowerCase());

        } else {
            query = this.getEntityManager().createQuery(jpqlMain + jpqlWhere);
        }

        return ((Number) query.executeUpdate()).intValue(); // DELETE
    }

    @Override
    public PrinterGroup readOrAdd(final String groupName,
            final String displayName, final String requestingUser,
            final Date requestDate) {

        PrinterGroup printerGroup = findByName(groupName);

        if (printerGroup == null) {
            /*
             * Lazy insert Printer Group.
             */
            printerGroup = new PrinterGroup();

            printerGroup.setCreatedBy(requestingUser);
            printerGroup.setCreatedDate(requestDate);
            printerGroup.setGroupName(groupName.toLowerCase());
            printerGroup.setDisplayName(displayName);

            this.create(printerGroup);

        } else {

            if (!printerGroup.getDisplayName().equals(displayName)) {

                /*
                 * Update with new display name.
                 */
                printerGroup.setModifiedBy(requestingUser);
                printerGroup.setModifiedDate(requestDate);
                printerGroup.setDisplayName(displayName);

                this.update(printerGroup);
            }
        }

        return printerGroup;
    }

    @Override
    public PrinterGroup findByName(final String groupName) {

        final String jpql = "SELECT P FROM PrinterGroup P "
                + "WHERE P.groupName = :groupName";

        final Query query = getEntityManager().createQuery(jpql);

        query.setParameter("groupName", groupName.toLowerCase());

        PrinterGroup group;

        try {
            group = (PrinterGroup) query.getSingleResult();
        } catch (NoResultException e) {
            group = null;
        }

        return group;
    }

    /**
     * Applies the list filter to the JPQL string.
     *
     * @param jpql
     *            The JPA query string.
     * @param filter
     *            The {@link ListFilter}.
     */
    private void applyListFilter(final StringBuilder jpql,
            final ListFilter filter) {

        StringBuilder where = new StringBuilder();

        int nWhere = 0;

        if (filter.getContainingNameText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" P.groupName like :containingNameText");
        }

        if (nWhere > 0) {
            jpql.append(" WHERE ").append(where.toString());
        }
    }

    /**
     * Creates the List Query and sets the filter parameters.
     *
     * @param jpql
     *            The JPA query string.
     * @param filter
     *            The {@link ListFilter}.
     * @return The {@link Query}.
     */
    private Query createListQuery(final StringBuilder jpql,
            final ListFilter filter) {

        final Query query = getEntityManager().createQuery(jpql.toString());

        if (filter.getContainingNameText() != null) {
            query.setParameter("containingNameText",
                    String.format("%%%s%%", filter.getContainingNameText()));
        }

        return query;
    }

    @Override
    public long getListCount(final ListFilter filter) {
        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(P.id) FROM PrinterGroup P");

        applyListFilter(jpql, filter);

        final Query query = createListQuery(jpql, filter);
        final Number countResult = (Number) query.getSingleResult();

        return countResult.longValue();
    }

    @Override
    public List<PrinterGroup> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT P FROM PrinterGroup P");

        applyListFilter(jpql, filter);

        //
        jpql.append(" ORDER BY P.groupName");

        if (!sortAscending) {
            jpql.append(" DESC");
        }

        final Query query = createListQuery(jpql, filter);

        if (startPosition != null) {
            query.setFirstResult(startPosition);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList();
    }
}
