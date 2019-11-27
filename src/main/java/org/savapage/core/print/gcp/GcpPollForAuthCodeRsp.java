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
 * Sample response:
 * </p>
 *
 * <pre>
 * {
 *  "success": true,
 *  "xmpp_jid": "4f6ab4d3ea9fc9fb0796eb4c1e3b86ae@cloudprint.googleusercontent.com",
 *  "request": {
 *   "time": "0",
 *   "params": {
 *    "oauth_client_id": [
 *     "566026169770.apps.googleusercontent.com"
 *    ],
 *    "printerid": [
 *     "f0f9fb2d-f331-6a60-88c8-7b36418c9e48"
 *    ]
 *   }
 *  },
 *  "confirmation_page_url": "https://www.google.com/cloudprint/regconfirmpage?printername\u003dSavaPage+Test+Printer\u0026email\u003ddatradrive@gmail.com\u0026dpi\u003d300\u0026pagesize\u003d210000,297000",
 *  "user_email": "datradrive@gmail.com",
 *  "authorization_code": "4/jrOiXHQMDUVswgnmP8rCsVz5LLhU.khvp9sXMoMMYYKs_1NgQtmU7HcVmhQI"
 * }
 * </pre>
 *
 * @deprecated See Mantis #1094.
 *
 * @author Rijk Ravestein
 *
 */
@Deprecated
public class GcpPollForAuthCodeRsp extends AbstractGcpApiRsp {

    public static final String KEY_XMPP_JID = "xmpp_jid";

    private static final String KEY_USER_EMAIL = "user_email";

    private static final String KEY_AUTHORIZATION_CODE = "authorization_code";

    private static final String KEY_CONFIRMATION_PAGE_URL =
            "confirmation_page_url";

    public static final String KEY_REQUEST = "request";
    public static final String KEY_PARAMS = "params";
    public static final String KEY_PRINTERID = "printerid";

    /**
     * The Jabber ID or email address that needs to be used with Google Talk to
     * subscribe for print notifications. This needs to be retained in the
     * printer memory forever.
     */
    // private String xmppJid;

    /**
     * The email address of the user that claimed the registration_token at the
     * previous step.
     */
    private String userEmail;

    /**
     * The OAuth2 authorization_code to be used to get OAuth2 refresh_token and
     * access_token.
     */
    private String authorizationCode;

    /**
     * The url of a printable page that confirms the user that the printer has
     * been registered to him/herself. The same notes relative to retrieving the
     * invite_page_url above apply here.
     */
    private String confirmationPageUrl;

    /**
     *
     */
    private String printerId;

    /**
     *
     * @param content
     * @throws IOException
     */
    public GcpPollForAuthCodeRsp(byte[] content) throws IOException {
        super(content);
    }

    @Override
    protected void onSuccess(JsonNode root) {

        // this.xmppJid = root.get(KEY_XMPP_JID).textValue();
        this.userEmail = root.get(KEY_USER_EMAIL).textValue();
        this.authorizationCode = root.get(KEY_AUTHORIZATION_CODE).textValue();
        this.confirmationPageUrl =
                root.get(KEY_CONFIRMATION_PAGE_URL).textValue();

        this.printerId = root.get(KEY_REQUEST).get(KEY_PARAMS)
                .get(KEY_PRINTERID).get(0).textValue();

    }

    /**
     * The Jabber ID or email address that needs to be used with Google Talk to
     * subscribe for print notifications. This needs to be retained in the
     * printer memory forever.
     *
     * @return
     */
    // public String getXmppJid() {
    // return xmppJid;
    // }

    /**
     * The email address of the user that claimed the registration_token.
     *
     * @return
     */
    public String getUserEmail() {
        return userEmail;
    }

    /**
     * The OAuth2 authorization_code to be used to get OAuth2 refresh_token and
     * access_token.
     *
     * @return
     */
    public String getAuthorizationCode() {
        return authorizationCode;
    }

    /**
     *
     * @return
     */
    public String getConfirmationPageUrl() {
        return confirmationPageUrl;
    }

    public String getPrinterId() {
        return printerId;
    }

}
