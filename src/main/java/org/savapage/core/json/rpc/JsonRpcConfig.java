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
package org.savapage.core.json.rpc;

import org.savapage.core.community.MemberCard;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class JsonRpcConfig {

    /** */
    public static final String RPC_VERSION = "2.0";

    /** */
    public static final String INTERNET_MEDIA_TYPE = "application/json";

    /** */
    public static final String CHAR_ENCODING = "UTF-8";

    /** */
    public static final String TYPE_INFO_PROPERTY = "@type";

    /**
     * The identification of the internal JSON-RPC. This value is encrypted with
     * the SavaPage private key to generate the API key.
     * <p>
     * IMPORTANT: changing this value invalidates any previously issued API KEY.
     * </p>
     */
    public static final String API_INTERNAL_ID = "savapage-internal";

    /**
     * Checks is the apiKey is valid.
     *
     * @param apiId
     *            The content that was signed.
     * @param apiKey
     *            The apiKey signature.
     * @return {@code true} when valid
     */
    public static boolean isApiKeyValid(final String apiId,
            final String apiKey) {
        boolean isValid = true;
        try {
            MemberCard.instance().validateContent(apiId, apiKey);
        } catch (Exception e) {
            isValid = false;
        }
        return isValid;
    }

}
