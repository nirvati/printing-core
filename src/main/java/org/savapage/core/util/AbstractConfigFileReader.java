/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * A reader for a flat text file with configuration lines. "#" comments are
 * filtered and content lines are notified one by one.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractConfigFileReader {

    /**
     * The prefix character for a comment line.
     */
    private static final char COMMENT_PFX_CHAR = '#';

    /**
     * .
     */
    protected File configFile;

    /**
     * Notifies a configuration line.
     *
     * @param line
     *            The 1-based line number.
     * @param content
     *            The line content.
     */
    protected abstract void onConfigLine(int line, String content);

    /**
     * Notifies start of reading.
     */
    protected abstract void onInit();

    /**
     * Notifies end of reading.
     */
    protected abstract void onEof();

    /**
     * Gets the line continuation character.
     *
     * @return {@code null} when not defined.
     */
    protected abstract Character getLineContinuationChar();

    /**
     *
     * @return
     */
    protected File getConfigFile() {
        return this.configFile;
    }

    /**
     * @param file
     *            The file to read.
     * @throws IOException
     *             When File IO errors.
     */
    public final void read(final File file) throws IOException {

        if (!file.isFile()) {
            return;
        }

        final String continueSuffix;

        if (this.getLineContinuationChar() == null) {
            continueSuffix = null;
        } else {
            continueSuffix = this.getLineContinuationChar().toString();
        }

        this.configFile = file;

        this.onInit();

        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(file));
            String strLine;
            int lineNr = 0;

            while ((strLine = br.readLine()) != null) {

                lineNr++;

                strLine = strLine.trim();

                // Skip empty line.
                if (strLine.isEmpty()) {
                    continue;
                }

                // Skip comment.
                if (strLine.charAt(0) == COMMENT_PFX_CHAR) {
                    continue;
                }

                // A regular line
                if (continueSuffix == null
                        || !strLine.endsWith(continueSuffix)) {
                    this.onConfigLine(lineNr, strLine);
                    continue;
                }

                /*
                 * Collect broken line.
                 */
                final StringBuilder lineBuilder = new StringBuilder();

                lineBuilder.append(
                        StringUtils.removeEnd(strLine, continueSuffix).trim());

                // Read next part.
                Boolean statusWlk =
                        readBrokenLine(br, lineBuilder, continueSuffix);

                if (statusWlk != null) {
                    lineNr++;
                }

                while (statusWlk != null && statusWlk.booleanValue()) {
                    statusWlk = readBrokenLine(br, lineBuilder, continueSuffix);
                    if (statusWlk != null) {
                        lineNr++;
                    }
                }

                if (lineBuilder.length() > 0) {
                    this.onConfigLine(lineNr, lineBuilder.toString());
                }

                // EOF
                if (statusWlk == null) {
                    break;
                }

            }
        } finally {
            if (br != null) {
                IOUtils.closeQuietly(br);
            }
        }
        this.onEof();
    }

    /**
     * Reads line part of a line broken by continuation suffixes.
     *
     * @param br
     *            The {@link BufferedReader}.
     * @param lineBuilder
     *            The {@link StringBuilder} containing the collected line so
     *            far.
     * @param continueSuffix
     *            The line continuation character.
     * @return {@code null} if EOF. {@link Boolean#TRUE} when read line has
     *         continuation suffix. {@link Boolean#FALSE} when line collection
     *         is finished.
     * @throws IOException
     *             When IO errors.
     */
    private Boolean readBrokenLine(final BufferedReader br,
            final StringBuilder lineBuilder, final String continueSuffix)
            throws IOException {

        String strLine = br.readLine();

        if (strLine == null) {
            return null;
        }
        if (strLine.isEmpty()) {
            return Boolean.FALSE;
        }
        if (strLine.charAt(0) == COMMENT_PFX_CHAR) {
            return Boolean.TRUE;
        }

        strLine = strLine.trim();

        if (strLine.endsWith(continueSuffix)) {
            final String part =
                    StringUtils.removeEnd(strLine, continueSuffix).trim();
            if (!part.isEmpty()) {
                lineBuilder.append(' ').append(part);
            }
            return Boolean.TRUE;
        }

        lineBuilder.append(' ').append(strLine);
        return Boolean.FALSE;
    }
}
