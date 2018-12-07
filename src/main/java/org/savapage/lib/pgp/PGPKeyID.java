/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.lib.pgp;

import java.math.BigInteger;

/**
 * Wrapper for PGP Key ID.
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPKeyID {

    /** */
    private final long id;

    /** */
    private static final int HEX_RADIX = 16;

    /**
     *
     * @param hexKey
     *            Key ID in hex notation <i>without</i> "0x" prefix.
     */
    public PGPKeyID(final String hexKey) {
        this.id = new BigInteger(hexKey, HEX_RADIX).longValue();
    }

    /**
     * @param key
     *            Key ID.
     */
    public PGPKeyID(final long key) {
        this.id = key;
    }

    /**
     * @return The Key ID.
     */
    public long getId() {
        return id;
    }

    /**
     * @return Upper-case hex notation <i>without</i> "0x" prefix.
     */
    public String toHex() {
        return toHex(this.id);
    }

    /**
     * @param id
     *            The Key ID.
     * @return Upper-case hex notation <i>without</i> "0x" prefix.
     */
    public static String toHex(final long id) {
        return Long.toHexString(id).toUpperCase();
    }

    /**
     * @param id
     *            The Key ID.
     * @return Human readable Key ID (upper-case, <i>with</i> "0x" prefix).
     */
    public static String formattedKeyID(final long id) {
        return String.format("0x%s", toHex(id));
    }

}
