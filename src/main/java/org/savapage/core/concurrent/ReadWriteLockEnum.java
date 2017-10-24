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
package org.savapage.core.concurrent;

import org.savapage.core.job.CupsSyncPrintJobs;
import org.savapage.core.job.DocLogClean;
import org.savapage.core.jpa.DocIn;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PrintOut;

/**
 *
 * @author Rijk Ravestein
 *
 */
public enum ReadWriteLockEnum {

    /**
     * This R/W lock is used to serialize update access to the database.
     * <p>
     * E.g. when making an on-line database backup we set the write Lock. When
     * committing to the database we set the read lock.
     * </p>
     */
    DATABASE_READONLY("db-read-only", 60 * 1000L),

    /**
     * This R/W lock is used to serialize access to {@link DocIn} statistics.
     * <p>
     * When updating statistics we use the writeLock(). When reading statistics
     * we use the readLock().
     * </p>
     */
    DOC_IN_STATS("doc_in_stats", 10 * 1000L),

    /**
     * This R/W lock is used to serialize access to {@link DocOut} statistics.
     * <p>
     * When updating statistics we use the writeLock(). When reading statistics
     * we use the readLock().
     * </p>
     */
    DOC_OUT_STATS("doc_out_stats", 10 * 1000L),

    /**
     * This R/W lock is used to serialize access to the public Letterhead store.
     * <p>
     * With this lock we want to prevent reading of the letterhead store while a
     * create, update or delete action is in progress (vice versa).
     * </p>
     * <p>
     * Consider this case:
     * </p>
     * <p>
     * When moving a public letterhead file to the public letterhead location it
     * might be picked up by a read action that lazy adds an entry to the
     * letterhead JSON store (with default attributes), before the create action
     * moves the letterhead job from the private to the public store.
     * </p>
     * <p>
     * When read and update are not serialized, the public store might end-up
     * with 2 entries for the same letterhead (one default and one copied from
     * the private store).
     * </p>
     * <p>
     * The vice-versa case is also possible of course.
     * </p>
     */
    LETTERHEAD_STORE("letterhead", 60 * 1000L),

    /**
     * This R/W lock is used to serialize access to history of the
     * {@link PrintOut} database table.
     * <p>
     * With this lock we want to prevent that {@link CupsSyncPrintJobs} and
     * {@link DocLogClean} run at the same time.
     * </p>
     */
    PRINT_OUT_HISTORY("print_out_history", 60 * 1000L);

    /**
     * .
     */
    private TimedReadWriteLock rwLock;

    /**
     * Constructor.
     *
     * @param name
     *            The unique name for the lock (used for reporting).
     * @param maxWait
     *            Max wait milliseconds after which an error is reported if a
     *            thread is still locked.
     */
    private ReadWriteLockEnum(final String name, final long maxWait) {
        this.rwLock = new TimedReadWriteLock(name, maxWait);
    }

    /**
     * Acquires the read lock only if it is free at the time of invocation.
     *
     * @throws ReadLockObtainFailedException
     *             When read lock could not be acquired.
     */
    public void tryReadLock() throws ReadLockObtainFailedException {
        if (!this.rwLock.tryReadLock()) {
            throw new ReadLockObtainFailedException(this);
        }
    }

    /**
     * Locks or unlocks a read lock.
     *
     * @param lock
     *            true - lock for read, false - unlock for read.
     */
    public void setReadLock(final boolean lock) {
        this.rwLock.setReadLock(lock);
    }

    /**
     * Locks or unlocks a write lock.
     *
     * @param lock
     *            true - lock for write, false - unlock for write.
     */
    public void setWriteLock(final boolean lock) {
        this.rwLock.setWriteLock(lock);
    }

}
