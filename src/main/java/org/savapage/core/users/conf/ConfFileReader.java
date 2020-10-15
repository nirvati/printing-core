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
package org.savapage.core.users.conf;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.util.AbstractConfigFileReader;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class ConfFileReader extends AbstractConfigFileReader {

    /**
     * Notifies a key-value pair.
     *
     * @param key
     *            The key.
     * @param value
     *            The value.
     */
    protected abstract void onItem(String key, String value);

    /**
     * Notifies start of reading.
     */
    @Override
    protected final void onInit() {
    }

    /**
     * Notifies end of reading.
     */
    @Override
    protected final void onEof() {
    }

    @Override
    protected final void onConfigLine(final int lineNr, final String strLine) {
        /*
         * The key and value may be separated by '=', ':' or TAB (a
         * tab-delimited file).
         */
        final String[] words = StringUtils.split(strLine, ":=\t");

        if (words.length == 2) {
            this.onItem(words[0].trim(), words[1].trim());
        }
    }

    @Override
    protected final Character getLineContinuationChar() {
        return null;
    }

}
