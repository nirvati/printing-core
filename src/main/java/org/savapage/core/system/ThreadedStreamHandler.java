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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* LOCAL REVISION HISTORY
 *
 * Changes to the original code.
 *
 *
 * 2012-01-30
 *
 * Changed 'adminPassword' to 'myStdIn'
 * Changed 'sudoIsRequested' to 'hasStdInput'
 * Every reference to 'sudo' are removed/changed to stdin
 *
 * 2011-12-29
 *
 * See @RRA marks for changes to the original code
 *
 * The link is ORIGINAL TEXT is incorrect, and should be:
 * http://www.devdaily.com/java/java-exec-processbuilder-process-1
 *
 * See also:
 *
 * http://www.devdaily.com/java/java-exec-processbuilder-process-2
 * http://www.devdaily.com/java/java-exec-processbuilder-process-3
 */

/* ORIGINAL TEXT
 *
 * This class is intended to be used with the SystemCommandExecutor class to let
 * users execute system commands from Java applications.
 *
 * This class is based on work that was shared in a JavaWorld article named
 * "When System.exec() won't". That article is available at this url:
 *
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html
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
 * Please ee the following page for the LGPL license:
 * http://www.gnu.org/licenses/lgpl.txt
 *
 */

/**
 * @author alvin j. alexander, devdaily.com
 */
class ThreadedStreamHandler extends Thread {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ThreadedStreamHandler.class);

    InputStream inputStream;
    String myStdIn;
    OutputStream outputStream;
    PrintWriter printWriter;
    StringBuilder outputBuffer = new StringBuilder();
    private boolean hasStdIn = false;

    /**
     * A simple constructor for when the stdin is not necessary. This
     * constructor will just run the command you provide, without writing to
     * stdin before the command.
     *
     * @param inputStream
     */
    ThreadedStreamHandler(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * Use this constructor when you want to write to stdin. The outputStream
     * must not be null.
     *
     * @param inputStream
     * @param outputStream
     * @param stdIn
     */
    ThreadedStreamHandler(InputStream inputStream, OutputStream outputStream,
            String stdIn) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.printWriter = new PrintWriter(outputStream);
        this.myStdIn = stdIn;
        this.hasStdIn = true;
    }

    @Override
    public void run() {
        // Write to stdin BEFORE the command which depends on the stdin is
        // issued.
        if (hasStdIn) {
            // doSleep(500); // why?
            printWriter.println(myStdIn);
            printWriter.flush();
        }

        BufferedReader bufferedReader = null;
        try {
            bufferedReader =
                    new BufferedReader(new InputStreamReader(inputStream));
            String line = null;
            int i = 0; // @RRA: added
            while ((line = bufferedReader.readLine()) != null) {
                if (i > 0) {
                    outputBuffer.append("\n");
                }
                outputBuffer.append(line);
                i++;
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    // ignore this one
                }
            }
        }
    }

    public StringBuilder getOutputBuffer() {
        return outputBuffer;
    }

}
