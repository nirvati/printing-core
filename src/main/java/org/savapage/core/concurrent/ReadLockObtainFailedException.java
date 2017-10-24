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

/**
 * An checked exception thrown when read lock could not be acquired for
 * {@link ReadWriteLockEnum}.
 *
 * @author Rijk Ravestein
 *
 */
public class ReadLockObtainFailedException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * The lock that caused the exception.
     */
    private final ReadWriteLockEnum rwLock;

    /**
     * Constructs a new {@link ReadLockObtainFailedException} without a message.
     *
     * @param theLock
     *            The lock that caused the exception.
     *
     */
    public ReadLockObtainFailedException(final ReadWriteLockEnum theLock) {
        super("");
        this.rwLock = theLock;
    }

    /**
     * @return The lock that caused the exception.
     */
    public ReadWriteLockEnum getRwLock() {
        return rwLock;
    }

}
