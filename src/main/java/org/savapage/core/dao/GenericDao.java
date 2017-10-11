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
package org.savapage.core.dao;

import javax.persistence.EntityManager;

import org.savapage.core.jpa.Entity;

/**
 * Common DAO methods.
 *
 * http://stackoverflow.com/questions/12565973/java-ee-dao-dto-data-transfer-
 * object-design-patterns
 *
 * @author Rijk Ravestein
 *
 * @param <T>
 */
public interface GenericDao<T extends Entity> {

    /**
     * Locks pessimistic.
     * <p>
     * Use this method to force serialization among transactions attempting to
     * update the entity data.
     * </p>
     *
     * @param id
     *            The primary key.
     * @return The {@link Entity} instance.
     */
    T lock(Long id);

    /**
     * Checks if the {@link Entity} is locked (with {@link #lock(Long)}).
     *
     * @param entity
     *            The {@link Entity}.
     * @return {@code true} when locked.
     */
    boolean isLocked(T entity);

    /**
     *
     * @param id
     *            The primary key.
     * @return The {@link Entity} instance, or {@code null} when not found.
     */
    T findById(Long id);

    /**
     *
     * @param entity
     *            The {@link Entity}.
     * @return The {@link Entity} instance.
     */
    T create(T entity);

    /**
     *
     * @param entity
     *            The {@link Entity}.
     * @return The {@link Entity} instance.
     */
    T update(T entity);

    /**
     * Refresh the state of the instance from the database, overwriting changes
     * made to the entity, if any. See {@link EntityManager#refresh(Object)}
     *
     * @param entity
     *            The {@link Entity}.
     */
    void refresh(T entity);

    /**
     *
     * @param entity
     *            The {@link Entity}.
     * @return
     */
    boolean delete(T entity);

    /**
     * @return The number of rows.
     */
    long count();

}
