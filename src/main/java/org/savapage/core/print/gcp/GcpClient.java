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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.savapage.core.dao.helpers.DocLogProtocolEnum;
import org.savapage.core.dao.helpers.ReservedIppQueueEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.print.server.DocContentPrintException;
import org.savapage.core.print.server.DocContentPrintReq;
import org.savapage.core.print.server.DocContentPrintRsp;
import org.savapage.core.print.server.PrintInResultEnum;
import org.savapage.core.services.QueueService;
import org.savapage.core.services.ServiceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class GcpClient {

    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(GcpClient.class);

    /**
     *
     */
    private static final QueueService QUEUE_SERVICE = ServiceContext
            .getServiceFactory().getQueueService();

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link GcpClient#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final GcpClient INSTANCE = new GcpClient();
    }

    /**
     *
     */
    private static final String GCP_VERSION = "2.0";

    /**
     * Base URL for the GCP API.
     */
    private static final String CLOUDPRINT_URL =
            "https://www.google.com/cloudprint";

    private static final String CLOUDPRINT_URL_REGISTER = CLOUDPRINT_URL
            + "/register?anonymous=True";

    private static final String CLOUDPRINT_URL_FETCH = CLOUDPRINT_URL
            + "/fetch";

    private static final String CLOUDPRINT_URL_CONTROL = CLOUDPRINT_URL
            + "/control";

    private static final String CLOUDPRINT_URL_LIST = CLOUDPRINT_URL + "/list";

    private static final String CLOUDPRINT_URL_UPDATE = CLOUDPRINT_URL
            + "/update";

    /**
     *
     */
    private static final String OAUTH2_TOKEN_URL =
            "https://accounts.google.com/o/oauth2/token";

    /**
     *
     */
    private static final String OAUTH2_TOKEN_SCOPE =
            "https://www.googleapis.com/auth/cloudprint";

    /**
     * Capabilities of the SavaPage Printer in Cloud Device Description (CDD)
     * format.
     */
    private static final String CAPABILITIES_CDD = "{"
            + "    \"version\": \"1.0\"," + "    \"printer\": {"
            + "      \"supported_content_type\": ["
            + "        {\"content_type\": \""
            + DocContent.MIMETYPE_PDF
            + "\", \"min_version\": \"1.5\"}"
            + "      ],"
            + "      \"vendor_capability\": [],"
            + "      \"color\": {"
            + "        \"option\": ["
            + "          {\"type\": \"STANDARD_COLOR\", \"is_default\": true},"
            + "        ]"
            + "      },"
            + "      \"copies\": {"
            + "        \"default\": 1,"
            + "        \"max\": 1"
            + "      },"
            + "      \"media_size\": {"
            + "        \"option\": ["
            + "          {"
            + "            \"name\": \"ISO_A4\","
            + "            \"width_microns\": 210000,"
            + "            \"height_microns\": 297000,"
            + "            \"is_default\": true"
            + "          },"
            + "          {"
            + "            \"name\": \"NA_LETTER\","
            + "            \"width_microns\": 215900,"
            + "            \"height_microns\": 279400"
            + "          }"
            + "        ]," + "      }" + "    }" + "}    ";

    /**
     * State "ONLINE" in Cloud Device State (CDS) format.
     */
    private static final String CDS_ONLINE = "{\"version\": \"1.0\", "
            + "\"cloud_connection_state\": \"ONLINE\", "
            + "\"printer\": {\"state\": \"IDLE\"} }";

    /**
     *
     */
    private static final String SEMANTIC_STATE_DIFF_DONE = "{\"state\": "
            + "{\"type\": \"DONE\"}}";

    /**
     *
     */
    private static final String SEMANTIC_STATE_DIFF_CANCELLED = "{ \"state\": "
            + "{ \"type\": \"ABORTED\", " + "\"user_action_cause\": "
            + "{\"action_code\": \"CANCELLED\"} }, \"pages_printed\": 0 }";

    /**
     *
     */
    private GcpClient() {
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static GcpClient instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Registers Google Cloud Printer as anonymous user, using the properties
     * from the singleton {@link GcpPrinter}.
     * <p>
     * Output:
     * <ul>
     * <li>{@code automated_invite_url}: to be used programmatically. E.g.
     * {@code https://www.google.com/cloudprint/confirm?token\u003dTUs4s}</li>
     * <li>{@code polling_url} :</li>
     * </ul>
     * </p>
     *
     * @param printerName
     *            The name of the GCP printer.
     * @throws IOException
     */
    public GcpRegisterPrinterRsp registerPrinter() throws IOException {

        Map<String, String> fields = new HashMap<>();

        /*
         * Required parameters.
         */
        fields.put("printer", GcpPrinter.getPrinterName());
        fields.put("proxy", GcpPrinter.getProxyId());

        /*
         * Required parameters for GCP 2.0.
         */
        fields.put("uuid", GcpPrinter.getPrinterUuid());
        fields.put("manufacturer", GcpPrinter.getManufacturer());
        fields.put("model", GcpPrinter.getModel());
        fields.put("gcp_version", GCP_VERSION);
        fields.put("setup_url", GcpPrinter.getSetupUrl());
        fields.put("support_url", GcpPrinter.getSetupUrl());
        fields.put("update_url", GcpPrinter.getUpdateUrl());

        fields.put("firmware", GcpPrinter.getFirmwareVersion());
        fields.put("semantic_state", CDS_ONLINE);
        fields.put("use_cdd", "true");
        fields.put("capabilities", CAPABILITIES_CDD);

        /*
         *
         */
        final HttpPost httppost = new HttpPost(CLOUDPRINT_URL_REGISTER);
        httppost.setEntity(encodeMultiPart(fields));

        final ByteArrayOutputStream bos = postRequest(httppost);

        return new GcpRegisterPrinterRsp(bos.toByteArray());

    }

    /**
     * @param polling_url
     *
     * @throws IOException
     */
    public GcpPollForAuthCodeRsp pollForAuthCode(final String polling_url)
            throws IOException {

        final HttpPost httppost =
                new HttpPost(polling_url + GcpPrinter.getOAuthClientId());

        final ByteArrayOutputStream bos = postRequest(httppost);
        return new GcpPollForAuthCodeRsp(bos.toByteArray());
    }

    /**
     * Obtains the OAuth2 authorization tokens.
     * <p>
     * If this request succeeds, a refresh token and short-lived access token
     * will be returned via JSON.
     * </p>
     * <p>
     * You can then use the access token to make API calls by attaching the
     * following Authorization HTTP header to each of your API calls:
     * Authorization: OAuth YOUR_ACCESS_TOKEN.
     * </p>
     *
     * @throws IOException
     */
    public GcpGetAuthTokensRsp getAuthTokens(final String authorization_code)
            throws IOException {

        final Map<String, String> fields = new HashMap<>();

        fields.put("code", authorization_code);
        fields.put("client_id", GcpPrinter.getOAuthClientId());
        fields.put("client_secret", GcpPrinter.getOAuthClientSecret());
        fields.put("redirect_uri", "oob");
        fields.put("grant_type", "authorization_code");
        fields.put("scope", OAUTH2_TOKEN_SCOPE);

        final HttpEntity entity = encodeMultiPart(fields);

        final HttpPost httppost = new HttpPost(OAUTH2_TOKEN_URL);
        httppost.setEntity(entity);

        final ByteArrayOutputStream bos = postRequest(httppost);

        return new GcpGetAuthTokensRsp(bos.toByteArray());
    }

    /**
     * Gets the access token. When expired the token is refreshed. See
     * {@link #refreshAccessToken()}.
     *
     * @return
     * @throws GcpAuthException
     * @throws IOException
     */
    public String getAccessToken() throws GcpAuthException, IOException {
        /*
         * Refresh access token?
         */
        if (GcpPrinter.isAccessTokenExpired()) {
            /*
             * Note: next statement throws an exception when authentication
             * fails.
             */
            GcpPrinter.cache(refreshAccessToken());
        }
        return GcpPrinter.getAccessToken();
    }

    /**
     * Refreshes the access token: which can be retrieved with
     * {@link #getAccessToken()}.
     *
     * @throws GcpAuthException
     */
    private GcpRefreshAccessTokenRsp refreshAccessToken() throws IOException,
            GcpAuthException {

        /*
         * INVARIANT: Refresh Token MUST be available.
         */
        final String refeshToken = GcpPrinter.getGcpRefreshToken();
        if (StringUtils.isBlank(refeshToken)) {
            throw new GcpAuthException("No Refresh Token available");
        }

        final Map<String, String> fields = new HashMap<>();

        fields.put("refresh_token", refeshToken);
        fields.put("client_id", GcpPrinter.getOAuthClientId());
        fields.put("client_secret", GcpPrinter.getOAuthClientSecret());
        fields.put("grant_type", "refresh_token");

        final HttpEntity entity = encodeMultiPart(fields);

        final HttpPost httppost = new HttpPost(OAUTH2_TOKEN_URL);
        httppost.setEntity(entity);

        return new GcpRefreshAccessTokenRsp(postRequest(httppost).toByteArray());
    }

    /**
     * Whenever the printer comes online or receives notification of a waiting
     * job, it should use the /fetch interface to grab any available jobs in the
     * queue.
     *
     * @throws IOException
     * @throws GcpAuthException
     */
    public GcpFetchRsp fetchJobs() throws IOException, GcpAuthException {

        final Map<String, String> fields = new HashMap<>();
        fields.put("printerid", GcpPrinter.getGcpPrinterId());

        final HttpPost httppost = new HttpPost(CLOUDPRINT_URL_FETCH);
        httppost.setHeader("Authorization", "OAuth " + this.getAccessToken());
        httppost.setEntity(encodeMultiPart(fields));

        return new GcpFetchRsp(postRequest(httppost).toByteArray());

    }

    /**
     * Prints the {@link GcpJob}.
     * <p>
     * The document type served is specified via the accept header of the
     * request made to the URL of the job file, i.e. {@link GcpJob#getFileUrl()}
     * . Not specifying this header will result in a PDF printout.
     * </p>
     *
     * @param user
     * @param job
     * @return The {@link PrintInResultEnum}.
     * @throws IOException
     * @throws ClientProtocolException
     * @throws DocContentPrintException
     * @throws IllegalStateException
     * @throws GcpAuthException
     * @throws Exception
     */
    public PrintInResultEnum printJob(final User user, final GcpJob job)
            throws ClientProtocolException, IOException, IllegalStateException,
            DocContentPrintException, GcpAuthException {

        final HttpPost httppost = new HttpPost(job.getFileUrl());
        httppost.setHeader("Authorization", "OAuth " + this.getAccessToken());

        final HttpClientBuilder builder = HttpClientBuilder.create();
        final CloseableHttpClient httpClient = builder.build();

        DocContentPrintRsp printRsp = null;

        try {

            final HttpResponse httpResponse = httpClient.execute(httppost);

            /*
             * Mantis #294
             *
             * Since, PDF is the only mimetype we allow and requested for this
             * download, we force to PDF, irrespective of what GCP says the
             * contentType is.
             */
            final DocContentTypeEnum contentType =
                    DocContent.getContentTypeFromMime(DocContent.MIMETYPE_PDF);

            final DocContentPrintReq docContentPrintReq =
                    new DocContentPrintReq();

            docContentPrintReq.setContentType(contentType);
            docContentPrintReq.setFileName(job.getId());
            docContentPrintReq.setOriginatorEmail(job.getOwnerId());
            docContentPrintReq
                    .setOriginatorIp(GcpPrinter.GOOGLE_CLOUD_PRINT_IP);
            docContentPrintReq.setPreferredOutputFont(null);
            docContentPrintReq.setProtocol(DocLogProtocolEnum.GCP);
            docContentPrintReq.setTitle(job.getTitle());

            printRsp =
                    QUEUE_SERVICE.printDocContent(ReservedIppQueueEnum.GCP
                            .getUrlPath(), user, docContentPrintReq,
                            httpResponse.getEntity().getContent());

        } finally {
            httpClient.close();
        }

        return printRsp.getResult();
    }

    /**
     * Sets the job state to DONE.
     *
     * @param jobid
     *            The job id.
     * @return The {@link GcpControlRsp}.
     * @throws IOException
     * @throws GcpAuthException
     */
    public GcpControlRsp controlJobDone(final String jobid) throws IOException,
            GcpAuthException {
        return controlJob(jobid, SEMANTIC_STATE_DIFF_DONE);
    }

    /**
     *
     * @param jobid
     *            The job id.
     * @return The {@link GcpControlRsp}.
     * @throws IOException
     * @throws GcpAuthException
     */
    public GcpControlRsp controlJobCancel(final String jobid)
            throws IOException, GcpAuthException {
        return controlJob(jobid, SEMANTIC_STATE_DIFF_CANCELLED);
    }

    /**
     *
     * @param jobid
     * @param semanticStateDiff
     *            JSON String value for {@code semantic_state_diff}.
     * @return
     * @throws IOException
     * @throws GcpAuthException
     */
    private GcpControlRsp controlJob(final String jobid,
            final String semanticStateDiff) throws IOException,
            GcpAuthException {

        final Map<String, String> fields = new HashMap<>();
        fields.put("jobid", jobid);
        fields.put("semantic_state_diff", semanticStateDiff);

        final HttpPost httppost = new HttpPost(CLOUDPRINT_URL_CONTROL);

        httppost.setHeader("Authorization", "OAuth " + this.getAccessToken());
        httppost.setEntity(encodeMultiPart(fields));

        return new GcpControlRsp(postRequest(httppost).toByteArray());
    }

    /**
     *
     * @param httppost
     * @return
     * @throws IOException
     */
    private ByteArrayOutputStream postRequest(final HttpPost httppost)
            throws IOException {

        ByteArrayOutputStream bos = null;

        final HttpClientBuilder builder = HttpClientBuilder.create();
        final CloseableHttpClient httpClient = builder.build();

        try {

            final HttpResponse httpResponse = httpClient.execute(httppost);

            bos = new ByteArrayOutputStream();
            httpResponse.getEntity().writeTo(bos);

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(bos.toString());
            }

        } finally {
            /*
             * Mantis #487: release the connection.
             */
            httppost.reset();
            //
            httpClient.close();
        }

        return bos;
    }

    /**
     * As a first step whenever the printer comes online, it should check in
     * with the CloudPrint service and sync the printer's status and
     * capabilities with the listing in the cloud. This way the client does not
     * need to maintain state with regard to what has been registered, and needs
     * to retain only the Auth token provided when the user authenticated.
     *
     * @throws IOException
     * @throws GcpAuthException
     */
    public GcpPrinterDetailsRsp getPrinterDetails() throws IOException,
            GcpAuthException {

        Map<String, String> fields = new HashMap<>();
        /*
         * Identification of the proxy, as submitted while registering the
         * printer.
         */
        fields.put("proxy", GcpPrinter.getProxyId());

        final HttpPost httppost = new HttpPost(CLOUDPRINT_URL_LIST);
        httppost.setHeader("Authorization", "OAuth " + this.getAccessToken());
        httppost.setEntity(encodeMultiPart(fields));

        final ByteArrayOutputStream bos = postRequest(httppost);

        return new GcpPrinterDetailsRsp(bos.toByteArray());
    }

    /**
     * UNDER CONSTRUCTION.
     *
     * @param printerid
     * @param accessToken
     * @throws IOException
     * @throws GcpAuthException
     */
    public void setPrinterOnline() throws IOException, GcpAuthException {

        final HttpPost httppost = new HttpPost(CLOUDPRINT_URL_UPDATE);
        httppost.setHeader("Authorization", "OAuth " + this.getAccessToken());

        final Map<String, String> fields = new HashMap<>();
        fields.put("printerid", GcpPrinter.getGcpPrinterId());
        fields.put("semantic_state", CDS_ONLINE);

        httppost.setEntity(encodeMultiPart(fields));

        postRequest(httppost);
    }

    /**
     * Encodes list of parameters for HTTP multipart format.
     *
     * @param fields
     *            List of tuples containing name and value of parameters.
     *
     * @return The entity to be sent as data for the HTTP post request.
     */
    private HttpEntity encodeMultiPart(Map<String, String> fields) {

        MultipartEntityBuilder mpeBuilder = MultipartEntityBuilder.create();

        mpeBuilder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        for (Entry<String, String> field : fields.entrySet()) {
            mpeBuilder.addPart(field.getKey(), new StringBody(field.getValue(),
                    ContentType.DEFAULT_TEXT));
        }

        return mpeBuilder.build();
    }

}
