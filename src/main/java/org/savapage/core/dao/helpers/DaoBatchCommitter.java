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
package org.savapage.core.dao.helpers;

/**
 * A helper class for chopping up a large batch of database actions into a
 * sequence of committed transactions.
 *
 * @author Datraverse B.V.
 *
 */
public interface DaoBatchCommitter {

    /**
     *
     * @param test
     *            If {@code true} the {@link DaoBatchCommitter} will act in test
     *            mode, i.e. no commit but a rollback will be performed in all
     *            cases.
     */
    void setTest(boolean test);

    /**
     * Increments the batch item counter, starts a new the database transaction
     * when needed, and performs a {@link #commit()} when the counter reaches a
     * max value.
     *
     * @return The counter value after the increment.
     */
    int increment();

    /**
     * Commits the current transaction (if present).
     */
    void commit();

    /**
     * Rolls back the current transaction (if present).
     */
    void rollback();

}
