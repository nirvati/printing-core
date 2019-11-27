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
 *  "message": "De afdruktaak is bijgewerkt.",
 *  "xsrf_token": "AIp06Djm4fGW_BjzGf1dwNM-MvwSJ_C_lA:1386065586970",
 *  "request": {
 *   "time": "0",
 *   "users": [
 *    "0bddd2cb5b14c9b12429a0f821313a06@cloudprint.googleusercontent.com"
 *   ],
 *   "params": {
 *    "semantic_state_diff": [
 *     "{\"state\": {\"type\": \"DONE\"}}"
 *    ],
 *    "jobid": [
 *     "261c9320-c7a1-5ce9-d7ec-216dfd93c581"
 *    ]
 *   },
 *   "user": "0bddd2cb5b14c9b12429a0f821313a06@cloudprint.googleusercontent.com"
 *  },
 *  "job": {
 *   "tags": [
 *    "^shared"
 *   ],
 *   "createTime": "1386008477836",
 *   "printerName": "",
 *   "updateTime": "1386065587043",
 *   "status": "DONE",
 *   "ownerId": "rijkr@datraverse.nl",
 *   "ticketUrl": "https://www.google.com/cloudprint/ticket?format\u003dxps\u0026output\u003dxml\u0026jobid\u003d261c9320-c7a1-5ce9-d7ec-216dfd93c581",
 *   "printerid": "417f65c0-92a6-c61c-f65e-b5ce5a64d1c8",
 *   "semanticState": {
 *    "state": {
 *     "type": "DONE"
 *    },
 *    "delivery_attempts": 9,
 *    "version": "1.0"
 *   },
 *   "contentType": "application/pdf",
 *   "fileUrl": "https://www.google.com/cloudprint/download?id\u003d261c9320-c7a1-5ce9-d7ec-216dfd93c581",
 *   "id": "261c9320-c7a1-5ce9-d7ec-216dfd93c581",
 *   "message": "",
 *   "title": "Standaard ToDo",
 *   "errorCode": "",
 *   "numberOfPages": 1
 *  }
 * }
 * </pre>
 *
 * @deprecated See Mantis #1094.
 *
 * @author Rijk Ravestein
 *
 */
@Deprecated
public class GcpControlRsp extends AbstractGcpApiRsp {

    /**
     *
     * @param content
     * @throws IOException
     */
    public GcpControlRsp(byte[] content) throws IOException {
        super(content);
    }

    @Override
    protected void onSuccess(JsonNode root) {
    }

}
