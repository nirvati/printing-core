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

import java.lang.reflect.ParameterizedType;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;

import org.savapage.core.SpException;
import org.savapage.core.dao.GenericDao;
import org.savapage.core.jpa.Entity;

/**
 *
 * @author Rijk Ravestein
 *
 * @param <T>
 */
public abstract class GenericDaoImpl<T extends Entity>
        implements GenericDao<T> {

    /**
     *
     */
    protected static final int JPSQL_STRINGBUILDER_CAPACITY = 256;

    /**
     * <p>
     * {@link LockModeType#PESSIMISTIC_WRITE} is used so entity can not be read
     * or written by other transactions.
     * </p>
     * <p>
     * (Note: A {@link LockModeType#PESSIMISTIC_READ} allows other transactions
     * to read the entity, but not to make changes).
     * </p>
     */
    private static final LockModeType DAO_LOCK_MODE =
            LockModeType.PESSIMISTIC_WRITE;

    /**
     *
     */
    private final Class<T> entityClass;

    @SuppressWarnings("unchecked")
    public GenericDaoImpl() {
        this.entityClass = (Class<T>) ((ParameterizedType) getClass()
                .getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @Override
    public final T findById(final Long id) {
        return getEntityManager().find(entityClass, id);
    }

    @Override
    public final T create(final T entity) {
        getEntityManager().persist(entity);
        return entity;
    }

    @Override
    public final T update(final T entity) {
        return getEntityManager().merge(entity);
    }

    @Override
    public final boolean delete(final T entity) {
        getEntityManager().remove(entity);
        return true;
    }

    @Override
    public final T lock(final Long id) {
        return getEntityManager().find(entityClass, id, DAO_LOCK_MODE);
    }

    @Override
    public final boolean isLocked(final T entity) {
        return getEntityManager().contains(entity)
                && getEntityManager().getLockMode(entity).equals(DAO_LOCK_MODE);
    }

    @Override
    public final long count() {
        return ((Number) getEntityManager().createQuery(this.getCountQuery())
                .getSingleResult()).longValue();
    }

    /**
     * @return The JPA query string to count all rows in the table.
     */
    protected abstract String getCountQuery();

    /**
     *
     * @return The JPA {@link EntityManager}.
     */
    protected final EntityManager getEntityManager() {
        return DaoContextImpl.lazyEntityManager();
    }

    /**
     * Executes an update query, expecting one or zero rows updated/deleted.
     *
     * @param query
     *            The query.
     * @return {@code true} when row updated, {@code false} when not found.
     */
    protected final boolean executeSingleRowUpdate(final Query query) {

        final int nRows = query.executeUpdate();

        if (nRows > 1) {
            throw new SpException("More then one (1) row updated");
        }
        return nRows == 1;
    }

}