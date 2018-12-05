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

import org.bouncycastle.openpgp.PGPSignature;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class PGPSignatureInfo {

    /** */
    private final PGPSignature signature;
    /** */
    private final boolean valid;

    /**
     *
     * @param sig
     *            The signature.
     * @param isValid
     *            If {@code true}, signature is valid.
     */
    public PGPSignatureInfo(final PGPSignature sig, final boolean isValid) {
        this.signature = sig;
        this.valid = isValid;
    }

    /**
     * @return The signature.
     */
    public PGPSignature getSignature() {
        return signature;
    }

    /**
     * @return {@code true} if signature is valid.
     */
    public boolean isValid() {
        return valid;
    }

}
