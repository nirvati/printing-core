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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.util.encoders.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract Convenience wrapper for PGP Key.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class PGPKeyInfo {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PGPKeyInfo.class);

    /**
     * @return Human readable Key ID (upper-case, with "0x" prefix).
     */
    public abstract String formattedKeyID();

    /**
     * @return Human readable FingerPrint (upper-case, chunks of 4, separated
     *         with a space).
     */
    public abstract String formattedFingerPrint();

    /**
     * @return List of UIDs as {@link InternetAddress}.
     */
    public abstract List<InternetAddress> getUids();

    /**
     * @param id
     *            The Key ID.
     * @return Human readable Key ID (upper-case, with "0x" prefix).
     */
    public static final String formattedKeyID(final long id) {
        return String.format("0x%s", Long.toHexString(id).toUpperCase());
    }

    /**
     * @param fingerprint
     *            The fingerprint.
     * @return Human readable FingerPrint (upper-case, chunks of 4, separated
     *         with a space).
     */
    protected final String formattedFingerPrint(final byte[] fingerprint) {

        final int chunkWidth = 4;

        final StringBuilder builder = new StringBuilder();

        char[] charArray =
                new String(Hex.encode(fingerprint)).toUpperCase().toCharArray();

        for (int ch = 0; ch < charArray.length; ch++) {
            if (ch > 0 && ch % chunkWidth == 0) {
                builder.append(' ');
            }
            builder.append(charArray[ch]);
        }
        return builder.toString();
    }

    /**
     * @param publicKey
     *            The public key.
     * @return List of UIDs as {@link InternetAddress}.
     */
    protected final List<InternetAddress>
    getUids(final PGPPublicKey publicKey) {

        final List<InternetAddress> uids = new ArrayList<>();

        final Iterator<String> iterID = publicKey.getUserIDs();
        while (iterID.hasNext()) {
            try {
                uids.add(new InternetAddress(iterID.next()));
            } catch (AddressException e) {
                LOGGER.warn(e.getMessage());
            }
        }
        return uids;
    }

}
