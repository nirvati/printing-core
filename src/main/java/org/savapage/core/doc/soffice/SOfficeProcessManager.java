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

import java.net.ConnectException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.savapage.core.util.RetryException;
import org.savapage.core.util.RetryExecutor;
import org.savapage.core.util.RetryTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.star.frame.XDesktop;
import com.sun.star.lang.DisposedException;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeProcessManager {

    /**
     * If user installation files do not already exist, the first time
     * LibreOffice starts it will create the files and then immediately quit
     * with exit code 81.
     * <ul>
     * <li><a href=
     * "http://code.google.com/p/jodconverter/issues/ detail?id=84" >
     * JODConverter issue #84</a></li>
     * <li><a href="https://github.com/dagwieers/unoconv/issues/192">Unoconv
     * issue #192</a></li>
     * </ul>
     */
    private static final Integer EXIT_CODE_USER_INSTALLATION_FILES_CREATED =
            Integer.valueOf(81);

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SOfficeProcessManager.class);

    /**
     *
     */
    private final SOfficeProcessSettings settings;

    /**
     *
     */
    private final SOfficeProcess process;

    /**
     *
     */
    private final SOfficeConnection connection;

    /**
     *
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            new SOfficeThreadFactory("SOfficeProcessThread"));

    /**
     * Constructor.
     *
     * @param procSettings
     *            The settings.
     * @throws SOfficeException
     *             when invalid settings.
     */
    public SOfficeProcessManager(final SOfficeProcessSettings procSettings)
            throws SOfficeException {

        this.settings = procSettings;

        this.process = new SOfficeProcess(procSettings.getOfficeLocation(),
                procSettings.getUnoUrl(), procSettings.getTemplateProfileDir(),
                procSettings.getWorkDir());

        this.connection = new SOfficeConnection(procSettings.getUnoUrl());
    }

    /**
     *
     * @return the connection.
     */
    public SOfficeConnection getConnection() {
        return this.connection;
    }

    /**
     * Starts the process and connects.
     *
     * @throws SOfficeException
     *             if error.
     */
    public void startAndWait() throws SOfficeException {

        final Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                doStartProcessAndConnect(settings.getProcessStartRetry(),
                        settings.getProcessStartTimeout());
            }
        });

        try {
            future.get();
        } catch (Exception exception) {
            throw new SOfficeException("Failed to start and connect",
                    exception);
        }
    }

    /**
     *
     * @throws SOfficeException
     */
    public void stopAndWait() throws SOfficeException {

        final Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                doStopProcess();
            }
        });
        try {
            future.get();
        } catch (Exception exception) {
            throw new SOfficeException("failed to start and connect",
                    exception);
        }
    }

    /**
     *
     */
    public void restartAndWait() {

        final Future<?> future = executor.submit(new Runnable() {
            @Override
            public void run() {
                doStopProcess();
                doStartProcessAndConnect(settings.getProcessRespondRetry(),
                        settings.getProcessRespondTimeout());
            }
        });
        try {
            future.get();
        } catch (Exception exception) {
            throw new SOfficeException("failed to restart", exception);
        }
    }

    /**
     *
     */
    public void restartDueToTaskTimeout() {

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                doTerminateProcess();
                // will cause unexpected disconnection and subsequent restart
            }
        });
    }

    /**
     *
     */
    public void restartDueToLostConnection() {

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    doEnsureProcessExited();
                    doStartProcessAndConnect(settings.getProcessRespondRetry(),
                            settings.getProcessRespondTimeout());
                } catch (SOfficeException e) {
                    LOGGER.error("Process restart failed.", e);
                }
            }
        });
    }

    /**
     *
     * @param retryInterval
     *            The retry interval.
     * @param retryTimeout
     *            The timeout.
     * @throws SOfficeException
     *             if error.
     */
    private void doStartProcessAndConnect(final long retryInterval,
            final long retryTimeout) throws SOfficeException {

        try {
            process.start();

            new RetryExecutor() {

                @Override
                protected void attempt() throws RetryException, Exception {
                    try {
                        connection.connect();
                    } catch (ConnectException connectException) {

                        final Integer exitCode = process.getExitCode();

                        if (exitCode == null) {
                            /*
                             * Process is running; retry later.
                             */
                            throw new RetryException(connectException);

                        } else if (exitCode.equals(
                                EXIT_CODE_USER_INSTALLATION_FILES_CREATED)) {
                            /*
                             * Restart and retry later...
                             */
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("Office user installation files"
                                        + " created: restarting...");
                            }
                            process.start(true);

                            throw new RetryException(connectException);

                        } else {

                            throw new SOfficeException(String.format(
                                    "Office process died with exit code %d.",
                                    exitCode.intValue()));
                        }
                    }
                }
            }.execute(retryInterval, retryTimeout);

        } catch (RetryTimeoutException e) {
            throw new SOfficeException(
                    "Could not establish connection (timeout).");
        } catch (Exception e) {
            throw new SOfficeException("Error establishing connection.", e);
        }
    }

    /**
     * Terminates the UNO connection and waits till the host OS process exits.
     */
    private void doStopProcess() {
        try {
            final XDesktop desktop = SOfficeHelper.unoCast(XDesktop.class,
                    this.connection.getService(
                            SOfficeHelper.UNO_SERVICE_FRAME_DESKTOP));
            desktop.terminate();
        } catch (DisposedException disposedException) {
            // expected
        } catch (Exception e) {
            // in case we can't get hold of the desktop
            doTerminateProcess();
        }
        doEnsureProcessExited();
    }

    /**
     * Waits till the host OS process exits.
     *
     * @throws SOfficeException
     *             if error.
     */
    private void doEnsureProcessExited() throws SOfficeException {
        try {
            final int exitCode =
                    process.getExitCode(settings.getProcessRespondRetry(),
                            settings.getProcessRespondTimeout());

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("Process exited with code %d.",
                        exitCode));
            }

        } catch (RetryTimeoutException e) {
            doTerminateProcess();
        }
        process.deleteProfileDir();
    }

    /**
     *
     * @throws SOfficeException
     *             if error.
     */
    private void doTerminateProcess() throws SOfficeException {
        try {
            int exitCode = this.process.terminateByForce(
                    settings.getProcessRespondRetry(),
                    settings.getProcessRespondTimeout());

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format(
                        "Process forcibly terminated with exit code %d.",
                        exitCode));
            }
        } catch (Exception e) {
            throw new SOfficeException("Could not forcibly terminate process.",
                    e);
        }
    }

    /**
     *
     * @return {@code true} when connected.
     */
    boolean isConnected() {
        return connection.isConnected();
    }

    /**
     *
     * @return The {@link SOfficeProcess}.
     */
    public SOfficeProcess getProcess() {
        return process;
    }

}
