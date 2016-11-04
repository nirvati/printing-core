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

import java.io.File;

import org.savapage.core.util.DateUtil;

/**
 * Configuration parameters.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class SOfficeSettings {

    /**
     * The name of the required subdir of the office profile directory.
     */
    private static final String PROFILE_DIR_USER_SUBDIR = "user";

    /**
     * The retry timeout in milliseconds.
     */
    public static final long DEFAULT_PROCESS_RETRY_TIMEOUT_MSEC =
            30 * DateUtil.DURATION_MSEC_SECOND;

    /**
    *
    */
    public static final long DEFAULT_TASK_EXECUTION_TIMEOUT =
            20 * DateUtil.DURATION_MSEC_SECOND;

    /**
    *
    */
    public static final long DEFAULT_TASK_QUEUE_TIMEOUT =
            10 * DateUtil.DURATION_MSEC_SECOND;

    /**
     * The number of executed tasks after which the process is restarted. When
     * {@code 0} (zero) the process is <i>never</i>restarted.
     */
    public static final int DEFAULT_TASKCOUNT_FOR_PROCESS_RESTART = 200;

    /**
     *
     */
    private File officeLocation = SOfficeHelper.getOfficeLocation();

    /**
     * A temporary profile dir is created for each OOo process, to avoid
     * interfering with e.g. another OOo instance being used by the user.
     * <p>
     * By default this temporary profile will be a new one created by OOo with
     * its own defaults settings, and relies on the {@code -nofirststartwizard}
     * command line option.
     * </p>
     * <p>
     * You may want to provide a templateProfileDir containing customized
     * settings instead. This template dir will be copied to the temporary
     * profile, so OOo will use the same settings while still keeping the OOo
     * instances separate.
     * </p>
     * <p>
     * The profile can be customized in OOo by selecting the Tools > Options
     * menu item. Settings that may be worth customizing for automated
     * conversions include e.g.
     * <ul>
     * <li>Load/Save > General: you may e.g. want to disable
     * "Save URLs relative to internet" for security reasons</li>
     * <li>Load/Save > Microsoft Office: these options affect conversions of
     * embedded documents, e.g. an Excel table contained in a Word document. If
     * not enabled, the embedded table will likely be lost when converting the
     * Word document to another format.</li>
     * </ul>
     * </p>
     */
    private File templateProfileDir = null;

    /**
     *
     */
    private File workDir = new File(System.getProperty("java.io.tmpdir"));

    /**
     * Wait time (milliseconds) for a UNO connection to become available for
     * task execution.
     */
    private long taskQueueTimeout = DEFAULT_TASK_QUEUE_TIMEOUT;

    /**
     * Wait time (milliseconds) for a conversion task to complete.
     */
    private long taskExecutionTimeout = DEFAULT_TASK_EXECUTION_TIMEOUT;

    /**
     * The number of executed tasks after which the UNO connection is restarted.
     * When {@code 0} (zero) the process is <i>never</i> restarted.
     */
    private int tasksCountForProcessRestart =
            DEFAULT_TASKCOUNT_FOR_PROCESS_RESTART;

    /**
     * Wait time (milliseconds) before doing a retry of a host process command.
     */
    private long processRetryTimeout = DEFAULT_PROCESS_RETRY_TIMEOUT_MSEC;

    /**
     *
     * @return
     */
    public final File getTemplateProfileDir() {
        return templateProfileDir;
    }

    public final void setTemplateProfileDir(final File templateProfileDir) {
        this.templateProfileDir = templateProfileDir;
    }

    /**
     *
     * @return Wait time (milliseconds) for a UNO connection to become available
     *         for task execution.
     */
    public final long getTaskQueueTimeout() {
        return taskQueueTimeout;
    }

    /**
     *
     * @param timeout
     *            Wait time (milliseconds) for a UNO connection to become
     *            available for task execution.
     */
    public final void setTaskQueueTimeout(final long timeout) {
        this.taskQueueTimeout = timeout;
    }

    /**
     *
     * @return Wait time (milliseconds) for a conversion task to complete.
     */
    public final long getTaskExecutionTimeout() {
        return taskExecutionTimeout;
    }

    /**
     *
     * @param timeout
     *            Wait time (milliseconds) for a conversion task to complete.
     */
    public final void setTaskExecutionTimeout(final long timeout) {
        this.taskExecutionTimeout = timeout;
    }

    public final int getTasksCountForProcessRestart() {
        return tasksCountForProcessRestart;
    }

    public final void
            setTasksCountForProcessRestart(final int maxTasksPerProcess) {
        this.tasksCountForProcessRestart = maxTasksPerProcess;
    }

    /**
     * @return Wait time (milliseconds) before doing a retry of a host process
     *         command.
     */
    public final long getProcessRetryTimeout() {
        return processRetryTimeout;
    }

    /**
     * @param retryTimeout
     *            Wait time (milliseconds) before doing a retry of a host
     *            process command.
     */
    public final void setProcessRetryTimeout(final long retryTimeout) {
        this.processRetryTimeout = retryTimeout;
    }

    public final File getOfficeLocation() {
        return officeLocation;
    }

    public final void setOfficeLocation(final File officeLocation) {
        this.officeLocation = officeLocation;
    }

    public final File getWorkDir() {
        return workDir;
    }

    public final void setWorkDir(final File dir) {
        this.workDir = dir;
    }

    /**
     * @throws IllegalStateException
     *             if not valid.
     */
    public final void validate() {
        /*
         * INVARIANT: office location must be known.
         */
        if (officeLocation == null) {
            throw new IllegalStateException("SOffice location is not set.");
        }

        if (templateProfileDir != null
                && !isValidProfileDir(templateProfileDir)) {
            throw new IllegalStateException("templateProfileDir doesn't appear "
                    + "to contain a user profile: " + templateProfileDir);
        }

        if (!workDir.isDirectory()) {
            throw new IllegalStateException(
                    "workDir doesn't exist or is not a directory: " + workDir);
        }
    }

    /**
     * Checks if profile directory is valid.
     *
     * @param profileDir
     *            The profile directory.
     * @return {@code true} when valid.
     */
    private static boolean isValidProfileDir(final File profileDir) {
        return new File(profileDir, PROFILE_DIR_USER_SUBDIR).isDirectory();
    }
}
