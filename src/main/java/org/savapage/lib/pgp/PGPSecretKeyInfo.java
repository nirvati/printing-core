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

import java.util.List;

import javax.mail.internet.InternetAddress;

import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPSecretKey;

/**
 * Convenience wrapper for PGP Secret Key.
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPSecretKeyInfo extends PGPKeyInfo {

    /**
     * Secret key.
     */
    private final PGPSecretKey secretKey;

    /**
     * Private Key.
     */
    private final PGPPrivateKey privateKey;

    /**
     *
     * @param secKey
     *            Secret key.
     *
     * @param privKey
     *            Private key.
     */
    public PGPSecretKeyInfo(final PGPSecretKey secKey,
            final PGPPrivateKey privKey) {
        this.secretKey = secKey;
        this.privateKey = privKey;

        this.getSecretKey().getPublicKey();
    }

    /**
     * @return Secret key.
     */
    public PGPSecretKey getSecretKey() {
        return secretKey;
    }

    /**
     * @return Private key.
     */
    public PGPPrivateKey getPrivateKey() {
        return privateKey;
    }

    @Override
    public String formattedKeyID() {
        return formattedKeyID(this.getSecretKey().getKeyID());
    }

    @Override
    public String formattedFingerPrint() {
        return formattedFingerPrint(
                this.getSecretKey().getPublicKey().getFingerprint());
    }

    @Override
    public List<InternetAddress> getUids() {
        return getUids(this.secretKey.getPublicKey());
    }
}
