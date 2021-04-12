/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2021 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2021 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.dao.helpers;

import org.savapage.core.jpa.IppQueue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppQueueHelper {

    /** */
    private static final String PREFIX = "/";

    /**
     * Prevent public instantiation.
     */
    private IppQueueHelper() {
    }

    /**
     * The UI name for {@link IppQueue#getUrlPath()}.
     *
     * @param queue
     *            {@link IppQueue}.
     * @return UI name.
     */
    public static String uiPath(final IppQueue queue) {
        if (queue == null) {
            return "?";
        }
        final String urlPath = queue.getUrlPath();
        if (urlPath.equals(PREFIX)) {
            return PREFIX;
        }
        return PREFIX.concat(urlPath);
    }

}
