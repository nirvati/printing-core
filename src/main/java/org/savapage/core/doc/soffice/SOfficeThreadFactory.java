/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.doc.soffice;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A {@link ThreadFactory} that creates threads with a custom thread name.
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeThreadFactory implements ThreadFactory {

    /**
     * The index counter.
     */
    private static final AtomicInteger THREAD_INDEX = new AtomicInteger(0);

    /**
     * The base name of the thread.
     */
    private final String baseName;

    /**
     * {@code true} is thread is a daemon.
     */
    private final boolean daemon;

    /**
     * A thread as daemon.
     *
     * @param threadName
     *            The base name of the thread.
     */
    public SOfficeThreadFactory(final String threadName) {
        this(threadName, true);
    }

    /**
     *
     * @param threadName
     *            The base name of the thread.
     * @param isDaemon
     *            {@code true} is thread is a daemon.
     */
    public SOfficeThreadFactory(final String threadName,
            final boolean isDaemon) {
        this.baseName = threadName;
        this.daemon = isDaemon;
    }

    @Override
    public Thread newThread(final Runnable runnable) {

        final Thread thread = new Thread(runnable, String.format("%s-%d",
                this.baseName, THREAD_INDEX.getAndIncrement()));

        thread.setDaemon(daemon);
        return thread;
    }

}
