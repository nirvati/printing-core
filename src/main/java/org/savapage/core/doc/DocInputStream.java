/*
 * This file is part of the SavaPage project <http://savapage.org>.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.doc;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.mutable.MutableLong;

/**
 * Our own BufferedInputStream, since we need to know the number of bytes read.
 *
 * @author Rijk Ravestein
 *
 */
public class DocInputStream extends BufferedInputStream {

    private final MutableLong bytesRead = new MutableLong(0L);

    public DocInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int i = super.read();
        if (i > 0) {
            bytesRead.increment();
        }
        return i;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int i = super.read(b, off, len);
        if (i > 0) {
            bytesRead.add(i);
        }
        return i;
    }

    /**
     * Returns the number of bytes read.
     *
     * @return
     */
    public long getBytesRead() {
        return bytesRead.longValue();
    }

}
