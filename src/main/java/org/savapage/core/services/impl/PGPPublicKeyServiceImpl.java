/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2019 Datraverse B.V.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.apache.commons.io.FileUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.UserHomePathEnum;
import org.savapage.core.dao.enums.UserAttrEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.UserAttr;
import org.savapage.core.services.PGPPublicKeyService;
import org.savapage.core.services.ServiceContext;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHelper;
import org.savapage.lib.pgp.PGPKeyID;
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
     * @param keyID
     *            Key ID.
     * @return The URL to download the public ASCII armored key, or {@code null}
     *         when unknown.
     * @throws PGPBaseException
     *             When PKS is not configured.
     * @throws MalformedURLException
     *             If URL template is ill-formed.
     */
    private static URL getPublicKeyDownloadUrl(final PGPKeyID keyID)
            throws PGPBaseException, MalformedURLException {

        final URL url = getPksUrl();

        if (url == null) {
            throw new PGPBaseException("No Public Key Server configured.");
        }

        return new URL(MessageFormat.format(URL_PKS_LOOKUP_GET_SEARCH, url,
                keyID.toHex()));
    }

    @Override
    public String getPublicKeyPreviewUrlTpl() {

        final URL url = getPksUrl();

        if (url == null) {
            return null;
        }
        return url.toString().concat(PATH_PKS_LOOKUP_VINDEX_SEARCH);
    }

    /**
     * Downloads public key from Public Key Server and writes to output stream.
     *
     * @param urlPks
     *            URL of PKS.
     * @param keyID
     *            Key ID.
     * @param ostr
     *            Output stream.
     * @throws PGPBaseException
     *             If error.
     */
    private void lookup(final URL urlPks, final PGPKeyID keyID,
            final OutputStream ostr) throws PGPBaseException {
        try {
            PGPHelper.downloadPublicKey(urlPks, ostr);
        } catch (MalformedURLException e) {
            throw new PGPBaseException(e.getMessage(), e);
        } catch (UnknownHostException e) {
            LOGGER.error(String.format("Unknown host [%s]", e.getMessage()));
            throw new PGPBaseException(e.getMessage(), e);
        } catch (IOException e) {
            LOGGER.warn(e.getMessage());
        }
    }

    /**
     *
     * @param user
     *            The user.
     * @param keyID
     *            Key ID
     * @return The ring entry file.
     */
    private File getRingEntry(final User user, final PGPKeyID keyID) {
        final Path path = Paths.get(
                ConfigManager.getUserHomeDir(user.getUserId()),
                UserHomePathEnum.PGP_PUBRING.getPath(),
                keyID.toHex().concat(".").concat(PGPHelper.FILENAME_EXT_ASC));
        return path.toFile();
    }

    @Override
    public PGPPublicKeyInfo lazyAddRingEntry(final User user,
            final PGPKeyID keyID) throws PGPBaseException {

        final File file = getRingEntry(user, keyID);

        if (!file.exists()) {

            try {
                final URL urlPks = getPublicKeyDownloadUrl(keyID);

                FileUtils.forceMkdirParent(file);

                try (OutputStream ostr = new FileOutputStream(file)) {
                    lookup(urlPks, keyID, ostr);
                    ostr.close();
                }
            } catch (IOException e) {
                file.delete();
                throw new PGPBaseException(e.getMessage());
            }
        }
        return readRingEntry(file);
    }

    /**
     *
     * @param file
     *            The entry file
     * @return Key info.
     * @throws PGPBaseException
     *             If error.
     */
    private PGPPublicKeyInfo readRingEntry(final File file)
            throws PGPBaseException {
        try (InputStream istr = new FileInputStream(file)) {
            return PGPHelper.instance().readPublicKey(istr);
        } catch (IOException e) {
            throw new PGPBaseException(e.getMessage());
        }
    }

    @Override
    public PGPPublicKeyInfo readRingEntry(final User user, final PGPKeyID keyID)
            throws PGPBaseException {

        final File file = getRingEntry(user, keyID);

        if (!file.exists()) {
            return null;
        }
        return readRingEntry(file);
    }

    @Override
    public PGPPublicKeyInfo readRingEntry(final User user)
            throws PGPBaseException {

        final UserAttr pgpPubAttr = userAttrDAO().findByName(user.getId(),
                UserAttrEnum.PGP_PUBKEY_ID);

        if (pgpPubAttr != null) {
            return readRingEntry(user, new PGPKeyID(pgpPubAttr.getValue()));
        }
        return null;
    }

    @Override
    public PGPPublicKeyInfo readRingEntry(final String userid)
            throws PGPBaseException {

        final User user = ServiceContext.getDaoContext().getUserDao()
                .findActiveUserByUserId(userid);

        if (user == null) {
            throw new IllegalArgumentException(userid.concat(" : not found."));
        }
        return this.readRingEntry(user);
    }

    @Override
    public boolean deleteRingEntry(final User user, final PGPKeyID keyID) {
        return getRingEntry(user, keyID).delete();
    }

}
