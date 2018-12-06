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
package org.savapage.core.services.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.MessageFormat;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.services.PGPPublicKeyService;
import org.savapage.core.util.IOHelper;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHelper;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPPublicKeyServiceImpl extends AbstractService
        implements PGPPublicKeyService {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PGPPublicKeyServiceImpl.class);

    /**
     * The URL path of an PKS to preview the content of PGP Public Key as
     * template: {0} is to be replaced with hexKeyID, without "0x" prefix.
     */
    private static final String PATH_PKS_LOOKUP_VINDEX_SEARCH =
            "/pks/lookup?op=vindex&search=0x{0}";

    /**
     * An URL template to get (download) the PGP Public Key: {0} is the URL opf
     * the PKS, {1} is to be replaced with hexKeyID, without "0x" prefix.
     */
    private static final String URL_PKS_LOOKUP_GET_SEARCH =
            "{0}/pks/lookup?op=get&search=0x{1}";

    /**
     *
     * @return URL of PKS (can be {@code null}).
     */
    private static URL getPksUrl() {
        try {
            return ConfigManager.instance().getPGPPublicKeyServerUrl();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Gets the URL to download the PGP Public Key.
     *
     * @param hexKeyID
     *            Hexadecimal KeyID, without "0x" prefix.
     * @return The URL to download the public ASCII armored key, or {@code null}
     *         when unknown.
     * @throws MalformedURLException
     *             If URL template is ill-formed.
     */
    public URL getPublicKeyDownloadUrl(final String hexKeyID)
            throws MalformedURLException {

        final URL url = getPksUrl();

        if (url == null) {
            return null;
        }
        return new URL(
                MessageFormat.format(URL_PKS_LOOKUP_GET_SEARCH, url, hexKeyID));
    }

    @Override
    public String getPublicKeyPreviewUrlTpl() {

        final URL url = getPksUrl();

        if (url == null) {
            return null;
        }
        return String.format("%s%s", url, PATH_PKS_LOOKUP_VINDEX_SEARCH);
    }

    /**
     * Gets the URL of Web Page to preview the content of the PGP Public Key.
     *
     * @param hexKeyID
     *            Hexadecimal KeyID, without "0x" prefix.
     * @return The URL to preview the public key, or {@code null} when unknown.
     * @throws MalformedURLException
     *             If URL template is ill-formed.
     */
    public URL getPublicKeyPreviewUrl(final String hexKeyID)
            throws MalformedURLException {

        final String value = this.getPublicKeyPreviewUrlTpl();
        if (value == null) {
            return null;
        }
        return new URL(MessageFormat.format(value, hexKeyID));
    }

    @Override
    public PGPPublicKeyInfo lookup(final String hexKeyID)
            throws PGPBaseException {

        ByteArrayInputStream bis = null;

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();) {

            final URL url = this.getPublicKeyDownloadUrl(hexKeyID);

            if (url != null) {
                PGPHelper.downloadPublicKey(url, bos);
                bis = new ByteArrayInputStream(bos.toByteArray());
                return PGPHelper.instance().readPublicKey(bis);
            }

        } catch (MalformedURLException e) {
            throw new PGPBaseException(e.getMessage(), e);
        } catch (UnknownHostException e) {
            LOGGER.error(String.format("Unknown host [%s]", e.getMessage()));
            throw new PGPBaseException(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        } finally {
            IOHelper.closeQuietly(bis);
        }
        return null;
    }

}
