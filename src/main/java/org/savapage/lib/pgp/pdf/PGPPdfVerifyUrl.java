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
package org.savapage.lib.pgp.pdf;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.lang3.StringUtils;
import org.savapage.lib.pgp.PGPSecretKeyInfo;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPPdfVerifyUrl {

    /**
     * Used in this class to set mountPage().
     */
    public static final String MOUNT_PATH_WEBAPP = "/pdf/verify";

    /** */
    public static final int URL_POSITION_PGP_KEY_ID = 0;

    /** */
    public static final int URL_POSITION_PGP_MESSAGE = 1;

    /** */
    public static final String HTTPS_PROTOCOL = "https";

    /** */
    private static final String BEGIN_PGP_MESSAGE =
            "-----BEGIN PGP MESSAGE-----\n";

    /** */
    private static final String END_PGP_MESSAGE =
            "\n-----END PGP MESSAGE-----\n";

    /** */
    private static final String REGEX_PGP_VERSION = "Version.*\n\n";

    /** */
    private static final String PGP_VERSION_DUMMY = "Version: DUMMY v1.0\n\n";

    /**
     *
     */
    private final URL urlBase;

    /**
     *
     * @param host
     *            the name of the host.
     * @param port
     *            the port number on the host.
     */
    public PGPPdfVerifyUrl(final String host, final int port) {
        try {
            this.urlBase =
                    new URL(HTTPS_PROTOCOL, host, port, MOUNT_PATH_WEBAPP);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     *
     * @param secKeyInfo
     *            The secret key to sign with.
     * @param pgpMsg
     *            The PGP message.
     * @return The verification URL.
     */
    public URL build(final PGPSecretKeyInfo secKeyInfo, final byte[] pgpMsg) {
        try {
            return new URL(String.format("%s/%s/%s",
                    this.urlBase.toExternalForm(), secKeyInfo.formattedKeyID(),
                    URLEncoder.encode(stripPgpMsg(pgpMsg), "ASCII")));
        } catch (MalformedURLException | UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     *
     * @param pgpMsg
     *            The full PGP message.
     * @return PGP message body.
     */
    private String stripPgpMsg(final byte[] pgpMsg) {
        final String msg = new String(pgpMsg);
        return StringUtils.removePattern(StringUtils.stripEnd(
                StringUtils.stripStart(msg, BEGIN_PGP_MESSAGE),
                END_PGP_MESSAGE), REGEX_PGP_VERSION);
    }

    /**
     *
     * @param pgpMsgBody
     *            PGP message body.
     * @return The full PGP message.
     */
    public static String assemblePgpMsg(final String pgpMsgBody) {
        return new StringBuilder().append(BEGIN_PGP_MESSAGE)
                .append(PGP_VERSION_DUMMY).append(pgpMsgBody)
                .append(END_PGP_MESSAGE).toString();
    }

}
