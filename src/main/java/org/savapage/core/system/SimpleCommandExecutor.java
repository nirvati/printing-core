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
package org.savapage.core.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import org.savapage.core.SpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple command executor using non-threaded writer/readers for stdin,
 * stdout/stderr streams. It has minimal overhead for simple commands that are
 * executed frequently and have limited stream content.
 *
 * @author Rijk Ravestein
 *
 */
public final class SimpleCommandExecutor implements ICommandExecutor {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(SimpleCommandExecutor.class);

    /**
     * List containing the program and its arguments.
     */
    private final List<String> command;

    /**
     * {@code stdout} from command.
     */
    private final StringBuilder stdoutBuilder = new StringBuilder();

    /**
     * {@code stderr} from command.
     */
    private final StringBuilder stderrBuilder = new StringBuilder();

    /**
     * Pass system command. For example ... here:
     *
     * <pre>
     * List&lt;String&gt; commands = new ArrayList&lt;String&gt;();
     * commands.add(&quot;/sbin/ping&quot;);
     * commands.add(&quot;-c&quot;);
     * commands.add(&quot;2&quot;);
     * commands.add(&quot;www.savapage.org&quot;);
     * SystemCommandExecutor commandExecutor =
     *         new SimpleCommandExecutor(commands);
     * commandExecutor.executeCommand();
     * </pre>
     *
     * @param commandInfo
     *            List containing the program and its arguments.
     */
    public SimpleCommandExecutor(final List<String> commandInfo) {
        if (commandInfo == null) {
            throw new SpException("Command missing.");
        }
        this.command = commandInfo;
    }

    @Override
    public int executeCommand() throws IOException, InterruptedException {

        int exitValue = EXIT_VALUE_ERROR;

        int i = 0;

        if (LOGGER.isTraceEnabled()) {
            for (String cmd : this.command) {
                LOGGER.trace("arg [{}] {}", i, cmd);
                i++;
            }
        }

        final ProcessBuilder pb = new ProcessBuilder(this.command);
        final Process p = pb.start();

        try (
                // Declare so it is auto closed.
                OutputStream stdIn = p.getOutputStream();

                BufferedReader stdOut = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));

                BufferedReader stdErr = new BufferedReader(
                        new InputStreamReader(p.getErrorStream()));) {

            /*
             * Block until process exits.
             */
            exitValue = p.waitFor();

            /*
             * Read stdout.
             */
            String s = null;
            i = 0;
            while ((s = stdOut.readLine()) != null) {
                if (i > 0) {
                    this.stdoutBuilder.append("\n");
                }
                this.stdoutBuilder.append(s);
                i++;
            }

            /*
             * Read stderr.
             */
            i = 0;
            while ((s = stdErr.readLine()) != null) {
                if (i > 0) {
                    this.stderrBuilder.append("\n");
                }
                this.stderrBuilder.append(s);
                i++;
            }
        }
        return exitValue;
    }

    @Override
    public String getStandardOutput() {
        return this.stdoutBuilder.toString();
    }

    @Override
    public String getStandardError() {
        return this.stderrBuilder.toString();
    }

}
