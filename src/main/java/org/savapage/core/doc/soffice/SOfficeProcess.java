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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.savapage.core.util.RetryException;
import org.savapage.core.util.RetryExecutor;
import org.savapage.core.util.RetryTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux soffice process wrapper. Uses the {@code ps} and {@code kill} host
 * commands.
 *
 * @author Rijk Ravestein
 *
 */
public final class SOfficeProcess {

    /**
     * The {@code ps} host command with parameters.
     */
    private static final String[] PS_COMMAND =
            new String[] { "/bin/ps", "-e", "-o", "pid,args" };

    /**
     * The 'ps' output line pattern.
     */
    private static final Pattern PS_OUTPUT_LINE =
            Pattern.compile("^\\s*(\\d+)\\s+(.*)$");

    /**
     *
     */
    private static final long PID_UNKNOWN = -1;

    /**
     *
     */
    private static final long PID_NOT_FOUND = -2;

    /**
     *
     */
    private final File officeHome;

    /**
     *
     */
    private final SOfficeUnoUrl unoUrl;

    /**
     *
     */
    private final File templateProfileDir;

    /**
     *
     */
    private final File instanceProfileDir;

    /**
     *
     */
    private Process process;

    /**
     *
     */
    private long pid = PID_UNKNOWN;

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SOfficeProcess.class);

    /**
     * Attempts to get the exit code of the office process.
     */
    private class SExitCodeRetryExecutor extends RetryExecutor {

        /**
         * The exit code of the process.
         */
        private int exitCode;

        @Override
        protected void attempt() throws RetryException, Exception {
            try {
                this.exitCode = process.exitValue();
            } catch (IllegalThreadStateException illegalThreadStateException) {
                throw new RetryException(illegalThreadStateException);
            }
        }

        /**
         *
         * @return The exit code of the process.
         */
        public int getExitCode() {
            return this.exitCode;
        }

    }

    /**
     * Constructor.
     *
     * @param officeHome
     * @param unoUrl
     * @param templateProfileDir
     * @param workDir
     */
    public SOfficeProcess(final File officeHome, final SOfficeUnoUrl unoUrl,
            final File templateProfileDir, final File workDir) {
        this.officeHome = officeHome;
        this.unoUrl = unoUrl;
        this.templateProfileDir = templateProfileDir;
        this.instanceProfileDir = getInstanceProfileDir(workDir, unoUrl);
    }

    /**
     *
     * @throws IOException
     */
    public void start() throws IOException {
        start(false);
    }

    /**
     *
     * @return The PID of the host process.
     */
    public long getPid() {
        return this.pid;
    }

    /**
     * Finds the PID of this process by using the host 'ps' command.
     *
     * @param query
     *            The query parameters.
     * @return The PID or {@link #PID_NOT_FOUND}.
     * @throws IOException
     *             When
     */
    private static long findPid(final SOfficeProcessQuery query)
            throws IOException {

        final String regex = Pattern.quote(query.getCommand()) + ".*"
                + Pattern.quote(query.getArgument());

        final Pattern commandPattern = Pattern.compile(regex);

        /*
         * Run the 'ps' command.
         */
        final Process psProcess = new ProcessBuilder(PS_COMMAND).start();
        final List<String> lines =
                IOUtils.readLines(psProcess.getInputStream());

        /*
         * Find the PID.
         */
        for (final String line : lines) {
            final Matcher lineMatcher = PS_OUTPUT_LINE.matcher(line);
            if (lineMatcher.matches()) {
                final String command = lineMatcher.group(2);
                final Matcher commandMatcher = commandPattern.matcher(command);
                if (commandMatcher.find()) {
                    return Long.parseLong(lineMatcher.group(1));
                }
            }
        }
        return PID_NOT_FOUND;
    }

    /**
     * Kills the host process.
     *
     * @param pid
     *            The PID.
     * @throws IOException
     *             if error.
     */
    private static void kill(final long pid) throws IOException {
        if (pid <= 0) {
            throw new IllegalArgumentException("invalid pid: " + pid);
        }
        new ProcessBuilder(
                new String[] { "/bin/kill", "-KILL", Long.toString(pid) })
                        .start();
    }

    /**
     *
     * @param restart
     *            {@code true} if this is a restart.
     * @throws IOException
     *             when start fails.
     */
    public void start(final boolean restart) throws IOException {

        final SOfficeProcessQuery processQuery = new SOfficeProcessQuery(
                SOfficeHelper.SOFFICE_BIN, unoUrl.getAcceptString());

        long existingPid = findPid(processQuery);

        if (!(existingPid == PID_NOT_FOUND || existingPid == PID_UNKNOWN)) {

            final String msg =
                    String.format("Process '%s'(pid %d) already running",
                            unoUrl.getAcceptString(), existingPid);

            if (restart) {
                throw new IllegalStateException(msg);
            }

            LOGGER.warn(String.format("%s: restart.", msg));

            kill(existingPid);

            existingPid = PID_NOT_FOUND;
        }

        if (!restart) {
            prepareInstanceProfileDir();
        }

        final List<String> command = new ArrayList<String>();

        final File executable = SOfficeHelper.getOfficeExecutable(officeHome);

        command.add(executable.getAbsolutePath());
        command.add("-accept=" + unoUrl.getAcceptString() + ";urp;");
        command.add("-env:UserInstallation="
                + SOfficeHelper.toUrl(instanceProfileDir));
        command.add("-headless");
        command.add("-nocrashreport");
        command.add("-nodefault");
        command.add("-nofirststartwizard");
        command.add("-nolockcheck");
        command.add("-nologo");
        command.add("-norestore");
        ProcessBuilder processBuilder = new ProcessBuilder(command);

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(String.format(
                    "Starting connection [%s] with profile dir [%s]", unoUrl,
                    instanceProfileDir));
        }
        this.process = processBuilder.start();

        this.pid = findPid(processQuery);

        if (pid == PID_NOT_FOUND) {
            throw new IllegalStateException(String.format(
                    "Process with connection '%s' started "
                            + "but its PID could not be found.",
                    unoUrl.getAcceptString()));
        }

        if (LOGGER.isInfoEnabled()) {

            final String pidSuffix;

            if (pid != PID_UNKNOWN) {
                pidSuffix = String.format(" (pid %d)", pid);
            } else {
                pidSuffix = "";
            }

            LOGGER.info(String.format("Started process%s", pidSuffix));
        }
    }

    /**
     * Gets the profile directory if this process instance.
     *
     * @param parentDir
     *            The parent directory.
     * @param unoUrl
     *            The UNO url.
     * @return The profile directory.
     */
    private static File getInstanceProfileDir(final File parentDir,
            final SOfficeUnoUrl unoUrl) {

        final String dirName = String.format(".savage_soffice_%s",
                unoUrl.getAcceptString().replace(',', '_').replace('=', '-'));

        return new File(parentDir, dirName);
    }

    /**
     * Prepares the profile directory.
     *
     * @throws SOfficeException
     *             when preparation fails.
     */
    private void prepareInstanceProfileDir() throws SOfficeException {

        if (this.instanceProfileDir.exists()) {
            LOGGER.warn(String.format(
                    "Profile dir [%s] already exists and will be deleted.",
                    this.instanceProfileDir));
            deleteProfileDir();
        }

        if (this.templateProfileDir != null) {
            try {
                FileUtils.copyDirectory(this.templateProfileDir,
                        this.instanceProfileDir);
            } catch (IOException e) {
                throw new SOfficeException("Failed to copy profile dir.", e);
            }
        }
    }

    /**
     * Deletes the profile directory.
     */
    public void deleteProfileDir() {

        if (instanceProfileDir == null) {
            return;
        }

        try {
            FileUtils.deleteDirectory(instanceProfileDir);
        } catch (IOException ioException) {
            final File oldProfileDir =
                    new File(instanceProfileDir.getParentFile(),
                            instanceProfileDir.getName() + ".old."
                                    + System.currentTimeMillis());
            if (instanceProfileDir.renameTo(oldProfileDir)) {
                LOGGER.warn("could not delete profileDir: "
                        + ioException.getMessage() + "; renamed it to "
                        + oldProfileDir);
            } else {
                LOGGER.error("could not delete profileDir: "
                        + ioException.getMessage());
            }
        }
    }

    /**
     *
     * @return {@code true} when process is running.
     */
    public boolean isRunning() {
        if (this.process == null) {
            return false;
        }
        return getExitCode() == null;
    }

    /**
     *
     * @return The exit code of the process.
     */
    public Integer getExitCode() {
        try {
            return this.process.exitValue();
        } catch (IllegalThreadStateException exception) {
            return null;
        }
    }

    /**
     * Retries to get the exit code.
     *
     * @param retryInterval
     *            The retry interval.
     * @param retryTimeout
     *            The retry timeout.
     * @return The exit code of the process.
     * @throws RetryTimeoutException
     *             when timeout.
     */
    public int getExitCode(final long retryInterval, final long retryTimeout)
            throws RetryTimeoutException {

        try {

            final SExitCodeRetryExecutor retryable =
                    new SExitCodeRetryExecutor();

            retryable.execute(retryInterval, retryTimeout);

            return retryable.getExitCode();

        } catch (RetryTimeoutException e) {
            throw e;

        } catch (Exception e) {
            throw new SOfficeException("Could not get process exit code.", e);
        }
    }

    /**
     * Kills the host {@link Process} and retries to get the exit code.
     *
     * @param retryInterval
     *            The retry interval.
     * @param retryTimeout
     *            The retry timeout.
     * @return the exit code.
     * @throws RetryTimeoutException
     *             if timeout getting the exit code.
     * @throws IOException
     *             when kill fails.
     */
    public int terminateByForce(final long retryInterval,
            final long retryTimeout) throws RetryTimeoutException, IOException {

        if (LOGGER.isInfoEnabled()) {

            final String pidSuffix;

            if (this.pid != PID_UNKNOWN) {
                pidSuffix = String.format(" (pid %d)", this.pid);
            } else {
                pidSuffix = "";
            }

            LOGGER.info(String.format("Terminating process by force: '%s'%s",
                    unoUrl.toString(), pidSuffix));
        }

        kill(this.pid);

        return getExitCode(retryInterval, retryTimeout);
    }
}
