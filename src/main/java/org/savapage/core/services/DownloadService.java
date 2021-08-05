/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.services;

import java.io.IOException;
import java.net.URL;

import javax.naming.LimitExceededException;

import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.jpa.User;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface DownloadService extends StatefulService {

    /**
     * Download URL source to user SafePages.
     *
     * @param source
     *            URL source.
     * @param originatorIp
     *            Client IP address.
     * @param user
     *            The user.
     * @param preferredFont
     *            Preferred font.
     * @param maxMB
     *            Max MB to download.
     * @return {@code true} when download succeeded, {@code false} if unknown
     *         content type.
     * @throws IOException
     *             If IO error.
     * @throws LimitExceededException
     *             If download exceeded max MB.
     */
    boolean download(URL source, String originatorIp, User user,
            InternalFontFamilyEnum preferredFont, int maxMB)
            throws IOException, LimitExceededException;

}
