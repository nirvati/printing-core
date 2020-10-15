/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.services;

import org.savapage.core.doc.soffice.SOfficeBusyException;
import org.savapage.core.doc.soffice.SOfficeConfig;
import org.savapage.core.doc.soffice.SOfficeException;
import org.savapage.core.doc.soffice.SOfficeTask;
import org.savapage.core.doc.soffice.SOfficeTaskTimeoutException;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface SOfficeService extends StatefulService {

    @Override
    void start();

    @Override
    void shutdown();

    /**
     * Starts the service. Method is idempotent.
     *
     * @param config
     *            The configuration.
     */
    void start(SOfficeConfig config);

    /**
     * Restarts the service.
     *
     * @param config
     *            The configuration.
     * @throws SOfficeException
     *             if error.
     */
    void restart(SOfficeConfig config) throws SOfficeException;

    /**
     * Executes an {@link SOfficeTask}.
     *
     * @param task
     *            The task.
     * @throws SOfficeBusyException
     *             if task is rejected because service is too busy.
     * @throws SOfficeTaskTimeoutException
     *             When task did not complete within time.
     */
    void execute(SOfficeTask task)
            throws SOfficeBusyException, SOfficeTaskTimeoutException;

    /**
     *
     * @return true when running.
     */
    boolean isRunning();

}
