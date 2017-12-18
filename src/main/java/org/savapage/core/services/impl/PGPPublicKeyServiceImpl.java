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
package org.savapage.core.services.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.services.PGPPublicKeyService;
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

    @Override
    public PGPPublicKeyInfo lookup(final String hexKeyID)
            throws PGPBaseException {

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ByteArrayInputStream bis = null;

        try {

            final URL url = ConfigManager.instance()
                    .getPGPPublicKeyDownloadUrl(hexKeyID);

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
            IOUtils.closeQuietly(bos);
            IOUtils.closeQuietly(bis);
        }
        return null;
    }

}
