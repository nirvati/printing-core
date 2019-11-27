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
package org.savapage.core.print.gcp;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>
 * Sample SUCCESS response:
 * </p>
 *
 * <pre>
 * {
 *   "access_token" : "ya29.1.AADtN_X39G5Z7N16Yc22nsrD7_YOpEDVF8gcPNv3M0_aji3rag-ieEoGFQ4QpxKY6w",
 *   "token_type" : "Bearer",
 *   "expires_in" : 3600,
 *   "refresh_token" : "1/2RM6xtqO9-50CcogD214Qvs8Dp8pdW6lo2sK7y554iw"
 * }
 * </pre>
 *
 * Sample ERROR response:
 *
 * <pre>
 * { "error" : "invalid_client" }
 * </pre>
 *
 * @deprecated See Mantis #1094.
 *
 * @author Rijk Ravestein
 *
 */
@Deprecated
public class GcpGetAuthTokensRsp extends AbstractGcpApiRsp {

    public static final String KEY_ACCESS_TOKEN = "access_token";

    private static final String KEY_EXPIRES_IN = "expires_in";

    private static final String KEY_REFRESH_TOKEN = "refresh_token";

    private static final String KEY_ERROR = "error";

    /**
     */
    private String accessToken;

    /**
     */
    private Integer expiresIn;

    /**
     */
    private String refreshToken;

    /**
     */
    private String error;

    /**
     *
     * @param content
     * @throws IOException
     */
    public GcpGetAuthTokensRsp(byte[] content) throws IOException {
        super(content);
    }

    @Override
    protected boolean isSuccess(JsonNode root) {
        this.error = null;
        if (root.has(KEY_ERROR)) {
            this.error = root.get(KEY_ERROR).textValue();
        }
        return (this.error == null);
    }

    @Override
    protected void onSuccess(JsonNode root) {
        this.accessToken = root.get(KEY_ACCESS_TOKEN).textValue();
        this.refreshToken = root.get(KEY_REFRESH_TOKEN).textValue();
        this.expiresIn = root.get(KEY_EXPIRES_IN).intValue();
    }

    public String getAccessToken() {
        return accessToken;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     *
     * @return {@code null} when no error was encountered.
     */
    public String getError() {
        return error;
    }

}
