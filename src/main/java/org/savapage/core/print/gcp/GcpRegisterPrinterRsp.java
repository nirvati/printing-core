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
 * @deprecated See Mantis #1094.
 *
 * @author Rijk Ravestein
 *
 */
@Deprecated
public class GcpRegisterPrinterRsp extends AbstractGcpApiRsp {

    // "polling_url":
    // "https://www.google.com/cloudprint/getauthcode?printerid\u003d417f65c0-92a6-c61c-f65e-b5ce5a64d1c8\u0026oauth_client_id\u003d",
    public static final String KEY_POLLING_URL = "polling_url";

    // "invite_url": "https://www.google.com/cloudprint/claimprinter.html",
    private static final String KEY_INVITE_URL = "invite_url";

    // "invite_page_url":
    // "https://www.google.com/cloudprint/regtokenpage?t\u003dTUs4s\u0026dpi\u003d300\u0026pagesize\u003d210000,297000",
    private static final String KEY_INVITE_PAGE_URL = "invite_page_url";

    // "complete_invite_url": "http://goo.gl/printer/aGqk",
    private static final String KEY_COMPLETE_INVITE_URL = "complete_invite_url";

    // "automated_invite_url":
    // "https://www.google.com/cloudprint/confirm?token\u003dTUs4s",
    private static final String KEY_AUTOMATED_INVITE_URL =
            "automated_invite_url";

    // "oauth_scope": "https://www.googleapis.com/auth/cloudprint",
    private static final String KEY_OAUTH_SCOPE = "oauth_scope";

    // "token_duration": "898",
    public static final String KEY_TOKEN_DURATION = "token_duration";

    // "registration_token": "TUs4s",
    private static final String KEY_REGISTRATION_TOKEN = "registration_token";

    private String pollingUrl;
    private String inviteUrl;
    private String invitePageUrl;
    private String completeInviteUrl;
    private String automatedInviteUrl;
    private String oauthScope;
    private Integer tokenDuration;
    private String registrationToken;

    /**
     *
     * @param content
     * @throws IOException
     */
    public GcpRegisterPrinterRsp(byte[] content) throws IOException {
        super(content);
    }

    @Override
    protected void onSuccess(JsonNode root) {

        this.pollingUrl = root.get(KEY_POLLING_URL).textValue();
        this.invitePageUrl = root.get(KEY_INVITE_PAGE_URL).textValue();
        this.inviteUrl = root.get(KEY_INVITE_URL).textValue();
        this.completeInviteUrl = root.get(KEY_COMPLETE_INVITE_URL).textValue();
        this.automatedInviteUrl =
                root.get(KEY_AUTOMATED_INVITE_URL).textValue();
        this.oauthScope = root.get(KEY_OAUTH_SCOPE).textValue();
        this.tokenDuration = root.get(KEY_TOKEN_DURATION).asInt();
        this.registrationToken = root.get(KEY_REGISTRATION_TOKEN).textValue();
    }

    public String getPollingUrl() {
        return pollingUrl;
    }

    public String getInvitePageUrl() {
        return invitePageUrl;
    }

    public String getCompleteInviteUrl() {
        return completeInviteUrl;
    }

    public String getAutomatedInviteUrl() {
        return automatedInviteUrl;
    }

    public String getOauthScope() {
        return oauthScope;
    }

    public Integer getTokenDuration() {
        return tokenDuration;
    }

    public String getRegistrationToken() {
        return registrationToken;
    }

    public String getInviteUrl() {
        return inviteUrl;
    }

}
