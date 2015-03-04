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
package org.savapage.core.system; // @RRA

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import org.savapage.core.SpException;

/* LOCAL REVISION HISTORY
 *
 * Changes to the original code.
 *
 * 2011-12-29
 *
 * Changed the name "adminPassword" to "myStdIn"
 *
 * 2011-12-29
 *
 * The link is ORIGINAL TEXT is incorrect, and should be:
 * http://www.devdaily.com/java/java-exec-processbuilder-process-1
 *
 */

/* ORIGINAL TEXT
 *
 *
 * This class can be used to execute a system command from a Java application.
 * See the documentation for the public methods of this class for more
 * information.
 *
 * Documentation for this class is available at this URL:
 *
 * http://devdaily.com/java/java-processbuilder-process-system-exec
 *
 *
 * Copyright 2010 alvin j. alexander, devdaily.com.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Please see the following page for the LGPL license:
 * http://www.gnu.org/licenses/lgpl.txt
 *
 */

/**
 *
 * @author alvin j. alexander, devdaily.com
 */
public class SystemCommandExecutor implements ICommandExecutor { // @RRA added
                                                                 // implements

    private final List<String> commandInformation;
    private final String myStdIn;
    private ThreadedStreamHandler inputStreamHandler;
    private ThreadedStreamHandler errorStreamHandler;

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
    public SystemCommandExecutor(final List<String> commandInformation) {
        if (commandInformation == null) {
            throw new SpException("The commandInformation is required.");
        }
        this.commandInformation = commandInformation;
        this.myStdIn = null;
    }

    /**
     * Pass in the system command you want to run as a List of Strings, and the
     * stdin as string.
     *
     * WARNING: when executing the 'sudo' command, with password string in stdIn
     * parameter, the command will hang when the given password is wrong.
     *
     * @param commandInformation
     * @param stdIn
     *            Single string to be used as stdin: you can use the '\n'
     *            character for line feed.
     *
     */
    public SystemCommandExecutor(final List<String> commandInformation,
            final String stdIn) {
        if (commandInformation == null) {
            throw new SpException("The commandInformation is required.");
        }
        this.commandInformation = commandInformation;
        this.myStdIn = stdIn;
    }

    /**
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    public int executeSimple() throws IOException, InterruptedException { // @RRA:
                                                                          // added
                                                                          // method
        int exitValue = -99;

        ProcessBuilder pb = new ProcessBuilder(commandInformation);
        Process p = pb.start();

        BufferedReader bistr =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

        BufferedReader bestr =
                new BufferedReader(new InputStreamReader(p.getErrorStream()));

        // TODO a better way to do this?
        exitValue = p.waitFor();

        /*
         * Read output from the command
         */
        String myLastline = "";
        String s = null;
        while ((s = bistr.readLine()) != null) {
            myLastline = s;
        }

        /*
         * Read any errors from the attempted command
         */
        while ((s = bestr.readLine()) != null) {
            // myLogger.error(s);
        }

        return exitValue;
    }

    /**
     *
     * @return
     * @throws IOException
     * @throws InterruptedException
     */
    @Override
    public int executeCommand() throws IOException, InterruptedException {
        int exitValue = -99;

        try {
            ProcessBuilder pb = new ProcessBuilder(commandInformation);
            Process process = pb.start();

            // you need this if you're going to write something to the command's
            // input stream
            // (such as when invoking the 'sudo' command, and it prompts you for
            // a password).
            OutputStream stdOutput = process.getOutputStream();

            // i'm currently doing these on a separate line here in case i need
            // to set them to null
            // to get the threads to stop.
            // see
            // http://java.sun.com/j2se/1.5.0/docs/guide/misc/threadPrimitiveDeprecation.html
            InputStream inputStream = process.getInputStream();
            InputStream errorStream = process.getErrorStream();

            // these need to run as java threads to get the standard output and
            // error from the command.
            // the inputstream handler gets a reference to our stdOutput in case
            // we need to write
            // something to it, such as with the sudo command
            inputStreamHandler =
                    new ThreadedStreamHandler(inputStream, stdOutput, myStdIn);
            errorStreamHandler = new ThreadedStreamHandler(errorStream);

            // TODO the inputStreamHandler has a nasty side-effect of hanging if
            // the given password is wrong; fix it
            inputStreamHandler.start();
            errorStreamHandler.start();

            // TODO a better way to do this?
            exitValue = process.waitFor();

            // TODO a better way to do this?
            inputStreamHandler.interrupt();
            errorStreamHandler.interrupt();
            inputStreamHandler.join();
            errorStreamHandler.join();
        } catch (IOException e) {
            // TODO deal with this here, or just throw it?
            throw e;
        } catch (InterruptedException e) {
            // generated by process.waitFor() call
            // TODO deal with this here, or just throw it?
            throw e;
        } finally {
        }
        return exitValue;
    }

    /**
     * Get the standard output (stdout) from the command you just exec'd.
     */
    @Override
    public StringBuilder getStandardOutputFromCommand() {
        return inputStreamHandler.getOutputBuffer();
    }

    /**
     * Get the standard error (stderr) from the command you just exec'd.
     */
    @Override
    public StringBuilder getStandardErrorFromCommand() {
        return errorStreamHandler.getOutputBuffer();
    }

}
