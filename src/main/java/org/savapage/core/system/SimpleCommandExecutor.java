/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.system;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.savapage.core.SpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a simple command executor, using non-threaded readers
 * for input and error streams. The idea is to get less overhead for simple
 * commmands that are executed frequently.
 *
 * @author Datraverse B.V.
 *
 */
public class SimpleCommandExecutor implements ICommandExecutor {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(SimpleCommandExecutor.class);

    private final List<String> commandInformation;

    StringBuilder inputBuffer = new StringBuilder();
    StringBuilder errorBuffer = new StringBuilder();

    /**
     * Pass in the system command you want to run as a List of Strings, as shown
     * here:
     *
     * <pre>
     * List&lt;String&gt; commands = new ArrayList&lt;String&gt;();
     * commands.add(&quot;/sbin/ping&quot;);
     * commands.add(&quot;-c&quot;);
     * commands.add(&quot;5&quot;);
     * commands.add(&quot;www.google.com&quot;);
     * SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
     * commandExecutor.executeCommand();
     * </pre>
     *
     * @param commandInformation
     *            The command you want to run.
     */
    public SimpleCommandExecutor(final List<String> commandInformation) {
        if (commandInformation == null) {
            throw new SpException("The commandInformation is required.");
        }
        this.commandInformation = commandInformation;
    }

    /**
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public int executeCommand() throws IOException, InterruptedException { // @RRA:
        // added
        // method
        int exitValue = -99;

        int i = 0;
        for (String cmd : commandInformation) {
            LOGGER.trace("arg [" + i + "] " + cmd);
            i++;
        }

        ProcessBuilder pb = new ProcessBuilder(commandInformation);
        Process p = pb.start();

        BufferedReader stdInput =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

        BufferedReader stdError =
                new BufferedReader(new InputStreamReader(p.getErrorStream()));

        // TODO a better way to do this?
        exitValue = p.waitFor();

        /*
         * Read output from the command
         */
        String s = null;
        i = 0;
        while ((s = stdInput.readLine()) != null) {
            if (i > 0) {
                inputBuffer.append("\n");
            }
            inputBuffer.append(s);
            i++;
        }

        /*
         * Read any errors from the attempted command
         */
        i = 0;
        while ((s = stdError.readLine()) != null) {
            if (i > 0) {
                errorBuffer.append("\n");
            }
            errorBuffer.append(s);
            i++;
        }

        return exitValue;
    }

    /**
     * Get the standard output (stdout) from the command you just exec'd.
     */
    @Override
    public StringBuilder getStandardOutputFromCommand() {
        return inputBuffer;
    }

    /**
     * Get the standard error (stderr) from the command you just exec'd.
     */
    @Override
    public StringBuilder getStandardErrorFromCommand() {
        return errorBuffer;
    }

}
