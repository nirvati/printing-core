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
package org.savapage.core.system; // @RRA

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

import org.savapage.core.SpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * System Command executor that prevents deadlock by reading stdout and stderr
 * input streams in separate threads, before waiting for the command process to
 * finish.
 *
 * <p>
 * Adapted from <a href=
 * "https://alvinalexander.com/java/java-exec-processbuilder-process-1">Java
 * exec - execute system processes with Java ProcessBuilder and Process</a> by
 * Alvin Alexander.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public final class SystemCommandExecutor implements ICommandExecutor {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SystemCommandExecutor.class);

    /**
     * Thread that reads an input stream, and optionally writes to output stream
     * first.
     *
     * @author Rijk Ravestein
     */
    private static final class ThreadedInputStreamReader extends Thread {

        /**
         * The {@link InputStream} to read.
         */
        private final InputStream inputStream;

        /** */
        private final String stdinValue;

        /** */
        private PrintWriter stdinWriter;

        /**
         * Where input is appended on.
         */
        private final StringBuilder outputBuilder = new StringBuilder();

        /**
         * Constructor when {@code stdin} is not needed.
         *
         * @param istr
         *            The {@link InputStream} to read.
         */
        ThreadedInputStreamReader(final InputStream istr) {
            this.inputStream = istr;
            this.stdinValue = null;
        }

        /**
         * Constructor when {@code stdin} is needed.
         *
         * @param istr
         *            The {@link InputStream} to read.
         * @param ostrStdIn
         *            The {@link OutputStream} to write {@code stdin}.
         * @param stdin
         *            The {@code stdin} value.
         */
        ThreadedInputStreamReader(final InputStream istr,
                final OutputStream ostrStdIn, final String stdin) {

            this.inputStream = istr;
            this.stdinWriter = new PrintWriter(ostrStdIn);
            this.stdinValue = stdin;
        }

        @Override
        public void run() {
            /*
             * Write to stdin first.
             */
            if (this.stdinValue != null) {
                this.stdinWriter.println(this.stdinValue);
                this.stdinWriter.flush();
            }

            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(this.inputStream));) {

                String line = null;
                int i = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    if (i > 0) {
                        this.outputBuilder.append("\n");
                    }
                    this.outputBuilder.append(line);
                    i++;
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        /**
         * @return The output read.
         */
        public String getOutput() {
            return this.outputBuilder.toString();
        }
    }

    /**
     * List containing the program and its arguments.
     */
    private final List<String> command;

    /** */
    private final String stdinString;

    /**
     * Handler of data piped (input stream) from the {@code stdout} of the
     * {@link Process} object.
     */
    private ThreadedInputStreamReader stdoutHandler;

    /**
     * Handler of data piped (input stream) from the {@code stderr} of the
     * {@link Process} object.
     */
    private ThreadedInputStreamReader stderrHandler;

    /**
     * System command.
     *
     * <p>
     * For example:
     * </p>
     *
     * <pre>
     * List&lt;String&gt; commands = new ArrayList&lt;String&gt;();
     * commands.add(&quot;/sbin/ping&quot;);
     * commands.add(&quot;-c&quot;);
     * commands.add(&quot;2&quot;);
     * commands.add(&quot;www.savapage.org&quot;);
     * SystemCommandExecutor commandExecutor =
     *         new SystemCommandExecutor(commands);
     * commandExecutor.executeCommand();
     * </pre>
     *
     * @param commandInfo
     *            List containing the program and its arguments.
     */
    public SystemCommandExecutor(final List<String> commandInfo) {
        if (commandInfo == null) {
            throw new SpException("Command missing.");
        }
        this.command = commandInfo;
        this.stdinString = null;
    }

    /**
     * System command with {@code stdin} as string.
     *
     * @param commandInfo
     *            List containing the program and its arguments.
     * @param stdIn
     *            Single string to be used as {@code stdin}. Use {@code '\n'}
     *            character for line feed.
     *
     */
    public SystemCommandExecutor(final List<String> commandInfo,
            final String stdIn) {
        if (commandInfo == null) {
            throw new SpException("Command missing.");
        }
        this.command = commandInfo;
        this.stdinString = stdIn;
    }

    @Override
    public int executeCommand() throws IOException, InterruptedException {

        int exitValue = EXIT_VALUE_ERROR;

        final ProcessBuilder pb = new ProcessBuilder(command);
        final Process process = pb.start();

        try (OutputStream ostrStdIn = process.getOutputStream();
                InputStream istrStdOut = process.getInputStream();
                InputStream istrStdErr = process.getErrorStream();) {

            /*
             * Read stdout and stderr input streams in separate threads BEFORE
             * process.waitFor() to prevent deadlock.
             *
             * When reading of stdout and stderr input streams is done AFTER
             * process.waitFor(), all in a single thread, a deadlock occurs when
             * the command produces abundant stdout and stderr, and the input
             * stream buffer(s) is (are) full. Since the corresponding streams
             * are not read, the command process is blocked, waiting for this
             * thread to continue reading.
             *
             * This thread in turn waits for the command process to finish
             * (which it won't because it waits for this thread, etc. Ergo, this
             * is a classical deadlock situation.
             *
             * Therefore, we continually read from the command input streams in
             * separate threads, to ensure that they don't block. We start the
             * reader BEFORE process.waitFor().
             */
            this.stdoutHandler = new ThreadedInputStreamReader(istrStdOut,
                    ostrStdIn, this.stdinString);
            this.stderrHandler = new ThreadedInputStreamReader(istrStdErr);

            /*
             * Start the threads for reading stdout and stderr.
             */
            this.stdoutHandler.start();
            this.stderrHandler.start();

            /*
             * Block until process exits.
             */
            exitValue = process.waitFor();

            /*
             * Because waitFor() finished we can stop the threads.
             */
            this.stdoutHandler.interrupt();
            this.stderrHandler.interrupt();

            this.stdoutHandler.join();
            this.stderrHandler.join();
        }
        return exitValue;
    }

    @Override
    public String getStandardOutput() {
        return this.stdoutHandler.getOutput();
    }

    @Override
    public String getStandardError() {
        return this.stderrHandler.getOutput();
    }

}
