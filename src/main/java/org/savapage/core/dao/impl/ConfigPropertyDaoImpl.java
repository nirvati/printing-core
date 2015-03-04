/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.dao.impl;

import java.util.Date;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.savapage.core.dao.ConfigPropertyDao;
import org.savapage.core.jpa.ConfigProperty;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class ConfigPropertyDaoImpl extends GenericDaoImpl<ConfigProperty>
        implements ConfigPropertyDao {

    @Override
    public ConfigProperty findByName(final String propertyName) {
        return readProperty(propertyName, null);
    }

    @Override
    public ConfigProperty findByNameInsert(final ConfigProperty prop) {
        return readProperty(prop.getPropertyName(), prop);
    }

    /**
     * Reads the property from database, when not found the value of
     * propToInsert determines how to proceed.
     *
     * @param propertyName
     *            The unique name of the property.
     * @param propToInsert
     *            When NOT {@code null}, ad-hoc insert of this property instance
     *            into the database when propertyName does not exist.
     *
     * @return The property instance from the database, or null when not found.
     */
    private ConfigProperty readProperty(final String propertyName,
            final ConfigProperty propToInsert) {

        final boolean lazyInsertRow = (propToInsert != null);

        /*
         * Find the property by unique name
         */
        final String jpql =
                "SELECT C FROM ConfigProperty C WHERE C.propertyName = :name";

        final Query query = getEntityManager().createQuery(jpql);
        query.setParameter("name", propertyName);

        ConfigProperty configProp = null;

        try {

            configProp = (ConfigProperty) query.getSingleResult();

        } catch (NoResultException e) {

            if (lazyInsertRow) {

                configProp = propToInsert;

                /*
                 * Fill standard attributes
                 */
                Date now = new Date();
                configProp.setModifiedBy(configProp.getCreatedBy());
                configProp.setCreatedDate(now);
                configProp.setModifiedDate(now);

                /*
                 * Insert
                 */
                getEntityManager().persist(configProp);
            }
        }

        return configProp;
    }

    @Override
    public long getListCount(final ListFilter filter) {
        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT COUNT(C.id) FROM ConfigProperty C");

        applyListFilter(jpql, filter);

        final Query query = createListQuery(jpql, filter);
        final Number countResult = (Number) query.getSingleResult();
        return countResult.longValue();
    }

    @Override
    public List<ConfigProperty> getListChunk(final ListFilter filter,
            final Integer startPosition, final Integer maxResults,
            final Field orderBy, final boolean sortAscending) {

        final StringBuilder jpql =
                new StringBuilder(JPSQL_STRINGBUILDER_CAPACITY);

        jpql.append("SELECT C FROM ConfigProperty C");

        applyListFilter(jpql, filter);

        //
        jpql.append(" ORDER BY ");

        if (orderBy == Field.NAME) {
            jpql.append("C.propertyName");
        } else {
            jpql.append("C.propertyName");
        }

        if (!sortAscending) {
            jpql.append(" DESC");
        }

        //
        final Query query = createListQuery(jpql, filter);

        //
        if (startPosition != null) {
            query.setFirstResult(startPosition);
        }
        if (maxResults != null) {
            query.setMaxResults(maxResults);
        }
        return query.getResultList();
    }

    /**
     * Applies the list filter to the JPQL string.
     *
     * @param jpql
     *            The {@link StringBuilder} to append to.
     * @param filter
     *            The filter.
     */
    private void applyListFilter(final StringBuilder jpql,
            final ListFilter filter) {

        final StringBuilder where = new StringBuilder();

        int nWhere = 0;

        if (filter.getContainingText() != null) {
            if (nWhere > 0) {
                where.append(" AND");
            }
            nWhere++;
            where.append(" lower(C.propertyName) like :containingText");
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

        if (filter.getContainingText() != null) {
            query.setParameter("containingText", "%"
                    + filter.getContainingText().toLowerCase() + "%");
        }

        return query;
    }

}
