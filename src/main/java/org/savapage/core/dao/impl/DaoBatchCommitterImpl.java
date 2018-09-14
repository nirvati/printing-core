/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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

import java.time.Duration;

import org.savapage.core.dao.DaoContext;
import org.savapage.core.dao.helpers.DaoBatchCommitter;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DaoBatchCommitterImpl implements DaoBatchCommitter {

    /**
     * Indicator value to check if committer is opened.
     */
    private static final int CHUNK_ITEM_COUNTER_INIT = -1;

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
     *
     */
    private boolean testMode = false;

    /**
     *
     */
    private boolean pause = false;

    /**
     *
     */
    private boolean commitAtNextIncrement = false;

    /**
     * Time of last {@link #open()}.
     */
    private long timeOpen;

    /**
     *
     * @param ctx
     *            The {@link DaoContext} .
     * @param chunkSize
     *            The number of increments after which a commit takes place.
     */
    public DaoBatchCommitterImpl(final DaoContext ctx, final int chunkSize) {

        this.commitThreshold = chunkSize;
        this.daoCtx = ctx;
        this.testMode = false;
        this.pause = false;

        this.chunkItemCounter = CHUNK_ITEM_COUNTER_INIT;
    }

    /**
     * Resets the counters.
     */
    private void reset() {
        this.chunkItemCounter = 0;
        this.commitAtNextIncrement = false;
    }

    @Override
    public void lazyOpen() {
        if (this.isClosed()) {
            this.open();
        }
    }

    @Override
    public void open() {

        if (chunkItemCounter != CHUNK_ITEM_COUNTER_INIT) {
            throw new IllegalStateException(
                    "DaoBatchCommitter is already open.");
        }
        this.timeOpen = System.currentTimeMillis();
        /*
         * Check for active transaction, since one might be open already.
         */
        if (!daoCtx.isTransactionActive()) {
            daoCtx.beginTransaction();
        }
        this.reset();
    }

    @Override
    public Duration close() {
        if (testMode) {
            this.rollback(false);
        } else {
            this.commit(false);
        }
        this.chunkItemCounter = CHUNK_ITEM_COUNTER_INIT;
        return Duration.ofMillis(System.currentTimeMillis() - this.timeOpen);
    }

    @Override
    public boolean isClosed() {
        return this.chunkItemCounter == CHUNK_ITEM_COUNTER_INIT;
    }

    @Override
    public int increment() {

        if (chunkItemCounter == CHUNK_ITEM_COUNTER_INIT) {
            throw new IllegalStateException("DaoBatchCommitter is not opened.");
        }

        chunkItemCounter++;

        if (!this.pause && (commitAtNextIncrement
                || chunkItemCounter >= commitThreshold)) {
            this.commit();
        }
        return chunkItemCounter;
    }

    @Override
    public void commit() {
        if (testMode) {
            this.rollback(true);
        } else {
            this.commit(true);
        }
    }

    @Override
    public void rollback() {
        this.rollback(true);
    }

    /**
     * @param beginTrx
     *            when {@code true} a new transaction is begun after the commit.
     */
    private void commit(final boolean beginTrx) {
        daoCtx.commit();
        if (beginTrx) {
            daoCtx.beginTransaction();
        }
        this.reset();
    }

    /**
     * @param beginTrx
     *            when {@code true} a new transaction is begun after the commit.
     */
    private void rollback(final boolean beginTrx) {
        daoCtx.rollback();
        if (beginTrx) {
            daoCtx.beginTransaction();
        }
        this.reset();
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

    @Override
    public void commitAtNextIncrement() {
        commitAtNextIncrement = true;
    }

    @Override
    public void pause() {
        this.pause = true;
    }

    @Override
    public void resume() {
        this.pause = false;
    }

}
