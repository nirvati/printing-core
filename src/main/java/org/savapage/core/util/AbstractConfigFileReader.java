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
package org.savapage.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

/**
 * A reader for a flat text file with configuration lines. "#" comments are
 * filtered and content lines are notified one by one.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractConfigFileReader {

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
     *
     */
    protected File configFile;

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

                if (strLine.isEmpty()) {
                    continue;
                }
                if (strLine.charAt(0) == '#') {
                    continue;
                }

                this.onConfigLine(lineNr, strLine);
            }
        } finally {
            if (br != null) {
                IOUtils.closeQuietly(br);
            }
        }
        this.onEof();
    }
}
