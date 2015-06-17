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

import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.helpers.DaoBatchCommitter;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class DaoBatchCommitterImpl implements DaoBatchCommitter {

    /**
     * The number of items the chunk holds before it gets committed.
     */
    private final int commitThreshold;

    /**
     *
     */
    private final DaoContext daoCtx;

    /**
     * The number of items in the chunk.
     */
    private int chunkItemCounter;

    /**
     * The total number of items incremented.
     */
    private int itemCounter;

    /**
     *
     */
    private boolean testMode = false;

    /**
     *
     * @param ctx
     *            The {@link DaoContext} .
     * @param commitThreshold
     *            The number of increments after which a commit takes place.
     */
    public DaoBatchCommitterImpl(final DaoContext ctx, final int commitThreshold) {

        this.commitThreshold = commitThreshold;
        this.daoCtx = ctx;
        this.chunkItemCounter = 0;
        this.itemCounter = 0;
        this.testMode = false;
    }

    @Override
    public int increment() {

        if (chunkItemCounter == 0) {
            beginTransaction();
        }

        if (++chunkItemCounter >= commitThreshold) {
            commit();
        }

        itemCounter++;

        return chunkItemCounter;
    }

    @Override
    public void commit() {
        if (testMode) {
            rollback();
        } else {
            chunkItemCounter = 0;
            daoCtx.commit();
        }
    }

    /**
     * Begins a database transaction when not already active.
     */
    private void beginTransaction() {
        if (!daoCtx.isTransactionActive()) {
            daoCtx.beginTransaction();
        }
    }

    @Override
    public void rollback() {
        chunkItemCounter = 0;
        daoCtx.rollback();
    }

    @Override
    public void setTest(final boolean test) {
        this.testMode = test;
    }

    @Override
    public boolean isTest() {
        return this.testMode;
    }

    @Override
    public int getCommitThreshold() {
        return this.commitThreshold;
    }

}
