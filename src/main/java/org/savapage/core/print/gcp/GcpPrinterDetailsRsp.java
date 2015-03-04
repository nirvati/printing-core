/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
 *  "xsrf_token": "AIp06DgL0cXMsRpn0BZVkY_OvxU8BjMZOA:1387310073367",
 *  "request": {
 *   "time": "0",
 *   "users": [
 *    "e486ab5d96b0ba4ffa098834fc8785d2@cloudprint.googleusercontent.com"
 *   ],
 *   "params": {
 *    "proxy": [
 *     "6d213a48-4a5c-4941-87f8-853915fdac48"
 *    ]
 *   },
 *   "user": "e486ab5d96b0ba4ffa098834fc8785d2@cloudprint.googleusercontent.com"
 *  },
 *  "printers": [
 *   {
 *    "tags": [
 *    ],
 *    "createTime": "1387223516646",
 *    "firmware": "0.9.4",
 *    "model": "Virtual Printer",
 *    "accessTime": "1387223516646",
 *    "updateUrl": "http://savapage.org",
 *    "updateTime": "1387223516646",
 *    "status": "",
 *    "ownerId": "datradrive@gmail.com",
 *    "gcpVersion": "2.0",
 *    "supportUrl": "http://savapage.org",
 *    "capsHash": "",
 *    "isTosAccepted": false,
 *    "type": "GOOGLE",
 *    "id": "61af5d31-e08a-1f73-d392-5efbea7d54af",
 *    "description": "",
 *    "manufacturer": "SavaPage",
 *    "proxy": "6d213a48-4a5c-4941-87f8-853915fdac48",
 *    "name": "SavaPage",
 *    "defaultDisplayName": "",
 *    "uuid": "7e345d5c-ddea-45b2-819d-9206d008bd41",
 *    "displayName": "SavaPage",
 *    "setupUrl": "http://savapage.org"
 *   }
 *  ]
 * }
 * </pre>
 *
 * @author Datraverse B.V.
 *
 */
public class GcpPrinterDetailsRsp extends AbstractGcpApiRsp {

    public static final String KEY_PRINTERS = "printers";

    private static final String KEY_ID = "id";

    private static final String KEY_OWNERID = "ownerId";

    private static final String KEY_REQUEST = "request";

    private static final String KEY_USER = "user";

    private static final String KEY_NAME = "name";

    private static final String KEY_DISPLAYNAME = "displayName";

    /**
     */
    private String ownerId;

    /**
     */
    private String name;

    /**
     */
    private String displayName;

    /**
     */
    private String id;

    /**
     */
    private String user;

    /**
     *
     * @param content
     * @throws IOException
     */
    public GcpPrinterDetailsRsp(byte[] content) throws IOException {
        super(content);
    }

    /**
     * NOTE: When the printer is deleted in the Google Cloud Print Console, we
     * have success, but the "printers" array is EMPTY.
     *
     * @param root
     *            The root node of the response.
     */
    @Override
    protected void onSuccess(JsonNode root) {

        final JsonNode printers = root.get(KEY_PRINTERS);

        if (printers != null && printers.size() > 0) {

            final JsonNode printer = root.get(KEY_PRINTERS).get(0);

            this.ownerId = printer.get(KEY_OWNERID).textValue();
            this.name = printer.get(KEY_NAME).textValue();
            this.displayName = printer.get(KEY_DISPLAYNAME).textValue();
            this.id = printer.get(KEY_ID).textValue();
        }

        this.user = root.get(KEY_REQUEST).get(KEY_USER).textValue();
    }

    /**
     *
     * @return
     */
    public boolean isPrinterPresent() {
        return (this.id != null);
    }

    /**
     * Gets the owner of the printer.
     *
     * @return {@code null} when printer is NOT present.
     */
    public String getOwnerId() {
        return ownerId;
    }

    /**
     *
     * @return {@code null} when printer is NOT present.
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return {@code null} when printer is NOT present.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     *
     * @return {@code null} when printer is NOT present.
     */
    public String getId() {
        return id;
    }

    /**
     * The Jabber ID for the XMPP session.
     *
     * @return {@code null} when printer is NOT present.
     */
    public String getUser() {
        return user;
    }
}
