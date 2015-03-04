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
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 * <p>
 * Sample response: NO jobs available. <b>NOTE</b>: This message is also
 * returned when the printer has been removed.
 * </p>
 *
 * <pre>
 * {
 *  "success": false,
 *  "message": "Geen afdruktaak beschikbaar op de opgegeven printer.",
 *  "request": {
 *   "time": "0",
 *   "users": [
 *    "e486ab5d96b0ba4ffa098834fc8785d2@cloudprint.googleusercontent.com"
 *   ],
 *   "params": {
 *    "printerid": [
 *     "61af5d31-e08a-1f73-d392-5efbea7d54af"
 *    ]
 *   },
 *   "user": "e486ab5d96b0ba4ffa098834fc8785d2@cloudprint.googleusercontent.com"
 *  },
 *  "errorCode": 413
 * } *
 * </pre>
 *
 * <p>
 * Sample response: jobs AVAILABLE.
 * </p>
 *
 * <pre>
 * {
 *  "success": true,
 *  "jobs": [
 *   {
 *    "tags": [
 *     "^shared"
 *    ],
 *    "createTime": "1386008477836",
 *    "printerName": "SavaPage-Test-Printer",
 *    "updateTime": "1386015404565",
 *    "status": "QUEUED",
 *    "ownerId": "rijkr@datraverse.nl",
 *    "ticketUrl": "https://www.google.com/cloudprint/ticket?format\u003dxps\u0026output\u003dxml\u0026jobid\u003d261c9320-c7a1-5ce9-d7ec-216dfd93c581",
 *    "printerid": "417f65c0-92a6-c61c-f65e-b5ce5a64d1c8",
 *    "semanticState": {
 *     "state": {
 *      "type": "QUEUED"
 *     },
 *     "delivery_attempts": 9,
 *     "version": "1.0"
 *    },
 *    "printerType": "GOOGLE",
 *    "contentType": "application/pdf",
 *    "fileUrl": "https://www.google.com/cloudprint/download?id\u003d261c9320-c7a1-5ce9-d7ec-216dfd93c581",
 *    "id": "261c9320-c7a1-5ce9-d7ec-216dfd93c581",
 *    "message": "",
 *    "title": "Standaard ToDo",
 *    "errorCode": "",
 *    "numberOfPages": 1
 *   },
 *   {
 *   ...
 *   }
 *  ],
 *  "xsrf_token": "AIp06DgWwXpnh6vhPPLm4tSZqCIbRKqUMg:1386060664928",
 *  "request": {
 *   "time": "0",
 *   "users": [
 *    "0bddd2cb5b14c9b12429a0f821313a06@cloudprint.googleusercontent.com"
 *   ],
 *   "params": {
 *    "printerid": [
 *     "417f65c0-92a6-c61c-f65e-b5ce5a64d1c8"
 *    ]
 *   },
 *   "user": "0bddd2cb5b14c9b12429a0f821313a06@cloudprint.googleusercontent.com"
 *  }
 * }
 * </pre>
 *
 * @author Datraverse B.V.
 *
 */
public class GcpFetchRsp extends AbstractGcpApiRsp {

    private static final String KEY_JOBS = "jobs";

    private static final String KEY_JOB_FILEURL = "fileUrl";
    private static final String KEY_JOB_ID = "id";
    private static final String KEY_JOB_CONTENTTYPE = "contentType";
    private static final String KEY_JOB_OWNERID = "ownerId";
    private static final String KEY_JOB_TITLE = "title";
    private static final String KEY_JOB_NUMBER_OF_PAGES = "numberOfPages";

    private List<GcpJob> jobs;

    /**
     *
     * @param content
     * @throws IOException
     */
    public GcpFetchRsp(byte[] content) throws IOException {
        super(content);
    }

    @Override
    protected void onSuccess(JsonNode root) {

        jobs = new ArrayList<>();

        JsonNode jsonJobs = root.get(KEY_JOBS);
        int maxJobs = jsonJobs.size();

        for (int i = 0; i < maxJobs; i++) {

            JsonNode jsonJob = jsonJobs.get(i);
            GcpJob job = new GcpJob();

            /*
             *
             */
            job.setFileUrl(jsonJob.get(KEY_JOB_FILEURL).textValue());
            job.setId(jsonJob.get(KEY_JOB_ID).textValue());
            job.setContentType(jsonJob.get(KEY_JOB_CONTENTTYPE).textValue());
            job.setOwnerId(jsonJob.get(KEY_JOB_OWNERID).textValue());
            job.setTitle(jsonJob.get(KEY_JOB_TITLE).textValue());
            job.setNumberOfPages(jsonJob.get(KEY_JOB_NUMBER_OF_PAGES)
                    .intValue());

            /*
             *
             */
            this.jobs.add(job);
        }
    }

    /**
     *
     * @return List with fetched jobs (can be empty).
     */
    public List<GcpJob> getJobs() {
        return jobs;
    }

}
