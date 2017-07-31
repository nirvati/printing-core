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
package org.savapage.lib.pgp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Convenience wrapper for PGP Public Key..
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPPublicKeyInfo {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PGPPublicKeyInfo.class);

    /**
     * Public key with UIDs.
     */
    private final PGPPublicKey masterKey;

    /**
     * Public key for encryption.
     */
    private final PGPPublicKey encryptionKey;

    /**
     *
     * @param master
     *            Public key with UIDs.
     * @param encryption
     *            Public key for encryption.
     */
    public PGPPublicKeyInfo(final PGPPublicKey master,
            final PGPPublicKey encryption) {
        this.masterKey = master;
        this.encryptionKey = encryption;
    }

    /**
     * @return Public key with UIDs.
     */
    public PGPPublicKey getMasterKey() {
        return masterKey;
    }

    /**
     * @return Public key for encryption.
     */
    public PGPPublicKey getEncryptionKey() {
        return encryptionKey;
    }

    /**
     *
     * @return List of UIDs as {@link InternetAddress}.
     */
    public List<InternetAddress> getUids() {

        final List<InternetAddress> uids = new ArrayList<>();

        final Iterator<String> iterID = masterKey.getUserIDs();
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
