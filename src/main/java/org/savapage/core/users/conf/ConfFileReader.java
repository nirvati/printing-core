/*
 * This file is part of the SavaPage project <http://savapage.org>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.users.conf;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ConfFileReader {

    /**
     * Notifies a key-value pair.
     *
     * @param key
     *            The key.
     * @param value
     *            The value.
     */
    public abstract void onItem(String key, String value);

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

        BufferedReader br = null;

        try {
            br = new BufferedReader(new FileReader(file));
            String strLine;

            while ((strLine = br.readLine()) != null) {

                strLine = strLine.trim();

                if (strLine.isEmpty()) {
                    continue;
                }
                if (strLine.charAt(0) == '#') {
                    continue;
                }
                /*
                 * The key and value may be separated by '=', ':' or TAB (a
                 * tab-delimited file).
                 */
                final String[] words = StringUtils.split(strLine, ":=\t");

                if (words.length != 2) {
                    continue;
                }

                this.onItem(words[0].trim(), words[1].trim());
            }
        } finally {
            if (br != null) {
                IOUtils.closeQuietly(br);
            }
        }
    }
}
