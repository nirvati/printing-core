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

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Date;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Properties of the SavaPage Google Cloud Ready Printer.
 * <p>
 * This is a mix of properties stored in {@link #FILENAME_GCP_PROPERTIES},
 * cached properties retrieved via Google Cloud Print API, and constants.
 * </p>
 * <p>
 * See:
 * <a href= "https://developers.google.com/cloud-print/docs/devguide" >Developer
 * Guide for Printers and Connectors</a>.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public final class GcpPrinter {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(GcpPrinter.class);

    /**
     * Status of the Google Cloud Printer.
     */
    public static enum State {

        /**
         * Printer is not configured.
         */
        NOT_CONFIGURED,

        /**
         * Printer is configured but not found.
         */
        NOT_FOUND,

        /**
         * Printer is present but off-line.
         */
        OFF_LINE,

        /**
         * Printer is present and on-line.
         */
        ON_LINE
    }

    /**
     * Use local host as originator IP.
     */
    public static final String GOOGLE_CLOUD_PRINT_IP = "127.0.0.1";

    private static final String SETUP_URL =
            CommunityDictEnum.SAVAPAGE_WWW_DOT_ORG_URL.getWord();

    private static final String SUPPORT_URL = SETUP_URL;
    private static final String UPDATE_URL = SETUP_URL;

    private static final String PRINTER_MODEL = "Virtual Printer";

    /**
     * Basename of the properties file for the GCP Printer.
     */
    private static final String FILENAME_GCP_PROPERTIES = "gcp.properties";

    private static final String KEY_OAUTH_CLIENT_ID = "oauth.client.id";
    private static final String KEY_OAUTH_CLIENT_SECRET = "oauth.client.secret";

    private static final String KEY_GCP_PROXY = "gcp.proxy";
    private static final String KEY_GCP_PRINTER_UUID = "gcp.printer.uuid";

    private static final String KEY_GCP_REFRESH_TOKEN = "gcp.refresh-token";

    /**
     * Although persistence is not really needed, we store the ownerid for
     * documentation purposes.
     */
    private static final String KEY_GCP_OWNER_ID = "gcp.owner.id";

    /**
     *
     */
    private static final int ACCESS_TOKEN_EXPIRY_MARGIN_SECS = 60;

    /**
     *
     */
    private static String thePrinterName = CommunityDictEnum.SAVAPAGE.getWord();

    /**
     *
     */
    private static Properties theProperties = null;

    /**
     * Cached Access Token.
     */
    private static String theAccessToken;

    /**
     * Cache expiration date of {@link #theAccessToken}.
     */
    private static Date theAccessTokenExpiry;

    /**
     * If {@code null} the printer has {@link State#NOT_FOUND}.
     */
    private static String thePrinterId;

    private static String theXmppJid;

    /**
     * Defaults to {@code true}, since we intend to have the printer online when
     * enabled.
     */
    private static final boolean PRINTER_ONLINE_INIT = true;

    private static boolean thePrinterOnline = PRINTER_ONLINE_INIT;

    /**
     *
     */
    private GcpPrinter() {
    }

    /**
     * Gets the URL with printer setup instructions.
     *
     * @return The URL.
     */
    public static String getSetupUrl() {
        return SETUP_URL;
    }

    /**
     * URL that user should be directed to if they are in need of printer
     * support.
     *
     * @return The URL.
     */
    public static String getSupportUrl() {
        return SUPPORT_URL;
    }

    /**
     * URL that user should be directed to if printer needs a firmware update.
     *
     * @return The URL.
     */
    public static String getUpdateUrl() {
        return UPDATE_URL;
    }

    /**
     *
     * @return
     */
    synchronized public static State getState() {

        State state;

        if (!isConfigured()) {

            state = State.NOT_CONFIGURED;

        } else if (thePrinterId == null) {

            state = State.NOT_FOUND;

        } else if (thePrinterOnline) {

            state = State.ON_LINE;

        } else {

            state = State.OFF_LINE;
        }

        return state;
    }

    /**
     *
     * @param state
     * @return
     */
    public static String localized(boolean enabled, State state) {

        String msgKey;

        if (enabled) {
            switch (state) {
            case NOT_CONFIGURED:
                msgKey = "gcp-not-configured";
                break;
            case NOT_FOUND:
                msgKey = "gcp-not-found";
                break;
            case OFF_LINE:
                msgKey = "gcp-offline";
                break;
            case ON_LINE:
                msgKey = "gcp-online";
                break;
            default:
                throw new SpException(
                        "Unhandled GcpPrinter.Status [" + state + "]");
            }
        } else {
            msgKey = "gcp-disabled";
        }

        return Messages.getMessage(GcpPrinter.class, msgKey, null);
    }

    /**
     *
     * @return
     */
    synchronized public static void setOnline(boolean online) {
        thePrinterOnline = online;
    }

    /**
     * Is the printer online?
     *
     * @return
     */
    synchronized public static boolean isOnline() {
        return getState() == State.ON_LINE;
    }

    /**
     * Is the printer present?
     *
     * @return
     */
    synchronized public static boolean isPresent() {
        final State state = getState();
        return (state == State.ON_LINE || state == State.OFF_LINE);
    }

    /**
     *
     * @return
     */
    synchronized public static boolean isConfigured() {

        boolean isConfigured = true;

        for (String key : new String[] { KEY_OAUTH_CLIENT_ID,
                KEY_OAUTH_CLIENT_SECRET, KEY_GCP_PROXY,
                KEY_GCP_PRINTER_UUID }) {
            isConfigured =
                    isConfigured && StringUtils.isNotBlank(getProperty(key));
        }
        return isConfigured;
    }

    /**
     *
     * @param oauthId
     * @param oauthSecret
     */
    synchronized public static void storeOauthProps(String oauthId,
            String oauthSecret) {

        if (!getOAuthClientId().equals(oauthId)
                || !getOAuthClientSecret().equals(oauthSecret)) {
            setProperty(KEY_OAUTH_CLIENT_ID, oauthId);
            setProperty(KEY_OAUTH_CLIENT_SECRET, oauthSecret);
            store();
        }
    }

    /**
     *
     */
    synchronized public static boolean isAccessTokenExpired() {
        final long now = System.currentTimeMillis();
        return theAccessToken == null || theAccessTokenExpiry == null
                || theAccessTokenExpiry.getTime() <= now;
    }

    /**
     *
     */
    synchronized public static String getAccessToken() {
        return theAccessToken;
    }

    /**
     *
     */
    synchronized public static Date getAccessTokenExpiry() {
        return theAccessTokenExpiry;
    }

    /**
     *
     * @param accessToken
     * @param expiry
     */
    synchronized public static void getAccessTokenData(
            Mutable<String> accessToken, Mutable<Date> expiry) {

        if (theAccessToken != null && theAccessTokenExpiry != null) {
            accessToken.setValue(theAccessToken);
            expiry.setValue(new Date(theAccessTokenExpiry.getTime()));
        }
    }

    /**
     *
     * @param rsp
     */
    synchronized public static void store(GcpPollForAuthCodeRsp rsp) {
        setProperty(KEY_GCP_OWNER_ID, rsp.getUserEmail());
        store();

        thePrinterId = rsp.getPrinterId();

    }

    /**
     * Caches data from the {@link GcpRefreshAccessTokenRsp}.
     *
     * @param rsp
     *            The response.
     * @throws GcpAuthException
     */
    synchronized public static void cache(GcpRefreshAccessTokenRsp rsp)
            throws GcpAuthException {

        theAccessToken = null;
        theAccessTokenExpiry = null;

        if (!rsp.isSuccess()) {
            throw new GcpAuthException(rsp.getError());
        }

        cacheAccessToken(rsp.getAccessToken(), rsp.getExpiresIn());
    }

    /**
     *
     * @param rsp
     * @throws GcpAuthException
     */
    synchronized public static void store(GcpGetAuthTokensRsp rsp)
            throws GcpAuthException {

        theAccessToken = null;
        theAccessTokenExpiry = null;

        if (!rsp.isSuccess()) {
            throw new GcpAuthException(rsp.getError());
        }

        setProperty(KEY_GCP_REFRESH_TOKEN, rsp.getRefreshToken());
        store();

        cacheAccessToken(rsp.getAccessToken(), rsp.getExpiresIn());
    }

    /**
     *
     * @param token
     * @param expiresIn
     */
    private static void cacheAccessToken(String token, Integer expiresIn) {
        /*
         * Transient cache.
         */
        theAccessToken = token;
        theAccessTokenExpiry = DateUtils.addSeconds(new Date(),
                expiresIn - ACCESS_TOKEN_EXPIRY_MARGIN_SECS);
    }

    /**
     *
     * @param rsp
     * @throws GcpPrinterNotFoundException
     *             When the Google Cloud printer is not found.
     */
    synchronized public static void store(GcpPrinterDetailsRsp rsp)
            throws GcpPrinterNotFoundException {

        thePrinterId = null;

        if (rsp.isSuccess() && !rsp.isPrinterPresent()) {
            throw new GcpPrinterNotFoundException(
                    "No Google Cloud Printer found.");
        }
        thePrinterId = rsp.getId();
        thePrinterName = rsp.getDisplayName(); // rsp.getName();
        theXmppJid = rsp.getUser();

        String ownerId = getProperty(KEY_GCP_OWNER_ID);

        if (StringUtils.isBlank(ownerId) || !ownerId.equals(rsp.getOwnerId())) {
            setProperty(KEY_GCP_OWNER_ID, rsp.getOwnerId());
            store();
        }
    }

    /**
     * Resets the state of the printer by clearing the cache and re-reading the
     * {@link #FILENAME_GCP_PROPERTIES}.
     *
     * @return
     */
    public synchronized static void reset() {
        theProperties = read();
        clearCache();
    }

    private static void clearCache() {
        theAccessToken = null;
        theAccessTokenExpiry = null;
        thePrinterId = null;
        thePrinterOnline = PRINTER_ONLINE_INIT;
        theXmppJid = null;
    }

    /**
     *
     * @return
     */
    private synchronized static Properties getProperties() {
        if (theProperties == null) {
            theProperties = read();
        }
        return theProperties;
    }

    /**
     *
     * @param key
     * @return
     */
    private static String getProperty(final String key) {
        return getProperties().getProperty(key);
    }

    /**
     *
     * @param key
     * @param value
     */
    private static void setProperty(final String key, final String value) {
        getProperties().setProperty(key, value);
    }

    /**
     *
     * @return
     */
    private static String getPropFilePath() {
        return ConfigManager.getServerHome() + "/" + FILENAME_GCP_PROPERTIES;

    }

    /**
     *
     * @throws IOException
     */
    private static void store() {

        File fileProp = new File(getPropFilePath());

        Writer writer = null;

        try {
            writer = new FileWriter(fileProp);

            getProperties().store(writer, getPropertyFileComments());

            writer.close();
            writer = null;

            final FileSystem fs = FileSystems.getDefault();
            final Set<PosixFilePermission> permissions =
                    EnumSet.of(OWNER_READ, OWNER_WRITE);

            Files.setPosixFilePermissions(
                    fs.getPath(fileProp.getAbsolutePath()), permissions);

        } catch (IOException e) {
            throw new SpException(e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Creates a comment header text for the stored
     * {@link #FILENAME_GCP_PROPERTIES}.
     *
     * @return
     */
    private static String getPropertyFileComments() {

        final String line = "---------------------------"
                + "-------------------------------";

        return line + "\n " + CommunityDictEnum.SAVAPAGE.getWord()
                + " Google Cloud Ready Printer"
                + "\n Keep the content of this file at a secure place.\n"
                + line;
    }

    /**
     * Reads the properties from file {@link #FILENAME_GCP_PROPERTIES}, lazy
     * creates the file when not found.
     * <p>
     * Also initializes and generates missing values to file, like:
     * <ul>
     * <li>{@link GcpPrinter#KEY_GCP_PROXY}</li>
     * <li>{@link GcpPrinter#KEY_GCP_PRINTER_UUID}</li>
     * <ul>
     * </p>
     */
    private static Properties read() {

        Properties props = new Properties();
        File fileProp = new File(getPropFilePath());

        InputStream istr = null;
        Writer writer = null;

        try {

            boolean saveProps = !fileProp.exists();

            String oAuthClientId = null;
            String oAuthClientSecret = null;
            String gcpProxy = null;
            String gcpPrinterUuid = null;

            if (!saveProps) {

                istr = new java.io.FileInputStream(fileProp);
                props.load(istr);
                istr.close();
                istr = null;

                oAuthClientId = props.getProperty(KEY_OAUTH_CLIENT_ID);
                oAuthClientSecret = props.getProperty(KEY_OAUTH_CLIENT_SECRET);
                gcpProxy = props.getProperty(KEY_GCP_PROXY);
                gcpPrinterUuid = props.getProperty(KEY_GCP_PRINTER_UUID);
            }

            if (oAuthClientId == null) {
                props.setProperty(KEY_OAUTH_CLIENT_ID, "");
                saveProps = true;
            }

            if (oAuthClientSecret == null) {
                props.setProperty(KEY_OAUTH_CLIENT_SECRET, "");
                saveProps = true;
            }

            if (gcpProxy == null) {
                gcpProxy = UUID.randomUUID().toString();
                props.setProperty(KEY_GCP_PROXY, gcpProxy);
                saveProps = true;
            }

            if (gcpPrinterUuid == null) {
                gcpPrinterUuid = UUID.randomUUID().toString();
                props.setProperty(KEY_GCP_PRINTER_UUID, gcpPrinterUuid);
                saveProps = true;
            }

            if (saveProps) {
                writer = new FileWriter(fileProp);
                props.store(writer, getPropertyFileComments());

                writer.close();
                writer = null;

                final FileSystem fs = FileSystems.getDefault();
                final Set<PosixFilePermission> permissions =
                        EnumSet.of(OWNER_READ, OWNER_WRITE);

                Files.setPosixFilePermissions(
                        fs.getPath(fileProp.getAbsolutePath()), permissions);

            }

        } catch (IOException e) {

            throw new SpException(e);

        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
            try {
                if (istr != null) {
                    istr.close();
                }
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        return props;
    }

    /**
     * Gets the OAUTH Client ID. A fixed value of SavaPage as Printer
     * Manufacturer or a customer owned ID.
     * <p>
     * See: <a href=
     * "https://developers.google.com/cloud-print/docs/devguide#authcodeobtain"
     * >Obtaining the OAuth2 authorization_code</a>
     * </p>
     * <p>
     * Client IDs don't need to be unique per printer: <i>in fact we expect one
     * client ID per printer manufacturer</i>.
     * </p>
     * <p>
     * The client ID can be obtained as explained
     * <a href="https://code.google.com/apis/console">here</a>.
     * </p>
     * <p>
     * Also see <a href="https://cloud.google.com/console#/project">Google Cloud
     * Console</a>.
     * </p>
     *
     * @return {@code null} when not set.
     */
    public static String getOAuthClientId() {
        return getProperty(KEY_OAUTH_CLIENT_ID);
    }

    /**
     * Gets the secret belonging to {@link #getOAuthClientId()}.
     *
     * @return {@code null} when not set.
     */
    public static String getOAuthClientSecret() {
        return getProperty(KEY_OAUTH_CLIENT_SECRET);
    }

    /**
     * Gets the <i>unique</i> Proxy ID of this Printer instance.
     * <p>
     * This is a unique attribute of the printer (such as a MAC address) and is
     * the only piece of information, besides the Auth token, that the printer
     * needs to retain state with.
     * </p>
     *
     * @return
     */
    public static String getProxyId() {
        return getProperty(KEY_GCP_PROXY);
    }

    /**
     * Unique printer identification generated by SavaPage.
     *
     * @return
     */
    public static String getPrinterUuid() {
        return getProperty(KEY_GCP_PRINTER_UUID);
    }

    /**
     * Unique printer identification generated by Google Cloud Print.
     *
     */
    public synchronized static String getGcpPrinterId() {
        return thePrinterId;
    }

    /**
     * @return {@code null} when not found.
     */
    public static String getGcpRefreshToken() {
        return getProperty(KEY_GCP_REFRESH_TOKEN);
    }

    /**
     * @return
     */
    public static String getOwnerId() {
        return getProperty(KEY_GCP_OWNER_ID);
    }

    /**
     *
     */
    public synchronized static void setPrinterName(final String printerName) {
        thePrinterName = printerName;
    }

    /**
     *
     * @return
     */
    public synchronized static String getPrinterName() {
        return thePrinterName;
    }

    /**
     *
     */
    public synchronized static String getGcpXmppJid() {
        return theXmppJid;
    }

    /**
     *
     * @return
     */
    public static String getManufacturer() {
        return CommunityDictEnum.SAVAPAGE.getWord();
    }

    /**
     *
     * @return
     */
    public static String getModel() {
        return PRINTER_MODEL;
    }

    /**
     *
     * @return
     */
    public static String getFirmwareVersion() {
        return ConfigManager.getAppVersion();
    }

}
