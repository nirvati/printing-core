/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.config;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

import javax.print.attribute.standard.MediaSizeName;

import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.validator.BooleanValidator;
import org.savapage.core.config.validator.CidrRangesValidator;
import org.savapage.core.config.validator.ConfigPropValidator;
import org.savapage.core.config.validator.CronExpressionDaysOfWeekValidator;
import org.savapage.core.config.validator.CronExpressionValidator;
import org.savapage.core.config.validator.CurrencyCodeValidator;
import org.savapage.core.config.validator.DecimalValidator;
import org.savapage.core.config.validator.EmailAddressValidator;
import org.savapage.core.config.validator.EnumSetValidator;
import org.savapage.core.config.validator.EnumValidator;
import org.savapage.core.config.validator.InternalFontFamilyValidator;
import org.savapage.core.config.validator.IpPortValidator;
import org.savapage.core.config.validator.LocaleValidator;
import org.savapage.core.config.validator.NotEmptyValidator;
import org.savapage.core.config.validator.NumberValidator;
import org.savapage.core.config.validator.UriValidator;
import org.savapage.core.config.validator.UrlValidator;
import org.savapage.core.config.validator.UserAuthModeSetValidator;
import org.savapage.core.config.validator.UuidValidator;
import org.savapage.core.config.validator.ValidationResult;
import org.savapage.core.config.validator.ValidationStatusEnum;
import org.savapage.core.crypto.OneTimeAuthToken;
import org.savapage.core.dao.enums.DeviceTypeEnum;
import org.savapage.core.dao.enums.ReservedIppQueueEnum;
import org.savapage.core.dao.impl.DaoBatchCommitterImpl;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.json.rpc.JsonRpcMethodName;
import org.savapage.core.services.helpers.DocLogScopeEnum;
import org.savapage.core.services.helpers.InboxSelectScopeEnum;
import org.savapage.core.services.helpers.PrintScalingEnum;
import org.savapage.core.services.helpers.UserAuthModeEnum;
import org.savapage.core.util.Messages;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface IConfigProp {

    /**
     * IMPORTANT: This is the maximum number of decimals (scale) used in the
     * database for SavaPage Financial.
     */
    int MAX_FINANCIAL_DECIMALS_IN_DB = 6;

    /**
     * .
     */
    String DEFAULT_SMARTSCHOOL_SIMULATION_USER = "smartschool.simulation";

    /**
     * .
     */
    String DEFAULT_SMARTSCHOOL_SIMULATION_STUDENT_1 =
            "smartschool.simulation.student-1";

    /**
     * .
     */
    String DEFAULT_SMARTSCHOOL_SIMULATION_STUDENT_2 =
            "smartschool.simulation.student-2";

    /**
     *
     */
    String DEFAULT_FINANCIAL_PRINTER_COST_DECIMALS = "4";

    /**
     *
     */
    String DEFAULT_FINANCIAL_USER_BALANCE_DECIMALS = "2";

    /**
     *
     */
    String DEFAULT_WEBAPP_WATCHDOG_HEARTBEAT_SECS = "3";

    /**
     *
     */
    String DEFAULT_WEBAPP_WATCHDOG_TIMEOUT_SECS = "10";

    /**
     *
     */
    String DEFAULT_BATCH_COMMIT_CHUNK_SIZE = "100";

    /**
     * Default number of rows in the result set for exporting tables for
     * database backup.
     */
    String DEFAULT_EXPORT_QUERY_MAX_RESULTS = "1000";

    /**
     *
     */
    InternalFontFamilyEnum DEFAULT_INTERNAL_FONT_FAMILY =
            InternalFontFamilyEnum.DEFAULT;

    String V_YES = "Y";
    String V_NO = "N";

    String V_ZERO = "0";

    /**
     * Is updatable with {@link JsonRpcMethodName#SET_CONFIG_PROPERTY}.
     */
    boolean API_UPDATABLE_ON = true;
    /**
     * <b>Not</b> updatable with {@link JsonRpcMethodName#SET_CONFIG_PROPERTY}.
     */
    boolean API_UPDATABLE_OFF = !API_UPDATABLE_ON;

    /**
     * Null value for numerics.
     */
    String V_NULL = "-1";

    String AUTH_METHOD_V_LDAP = "ldap";
    String AUTH_METHOD_V_UNIX = "unix";
    String AUTH_METHOD_V_NONE = "none";

    String AUTH_MODE_V_NAME = UserAuthModeEnum.NAME.toDbValue();
    String AUTH_MODE_V_ID = UserAuthModeEnum.ID.toDbValue();
    String AUTH_MODE_V_CARD_LOCAL = UserAuthModeEnum.CARD_LOCAL.toDbValue();
    String AUTH_MODE_V_CARD_IP = UserAuthModeEnum.CARD_IP.toDbValue();
    String AUTH_MODE_V_YUBIKEY = UserAuthModeEnum.YUBIKEY.toDbValue();

    String LDAP_TYPE_V_APPLE = "APPLE_OPENDIR";
    String LDAP_TYPE_V_OPEN_LDAP = "OPEN_LDAP";
    String LDAP_TYPE_V_E_DIR = "NOVELL_EDIRECTORY";
    String LDAP_TYPE_V_ACTIV = "ACTIVE_DIRECTORY";
    String LDAP_TYPE_V_G_SUITE = "G_SUITE";

    String PAPERSIZE_V_SYSTEM = "";
    String PAPERSIZE_V_A4 = MediaSizeName.ISO_A4.toString();
    String PAPERSIZE_V_LETTER = MediaSizeName.NA_LETTER.toString();

    /**
     *
     */
    String SMTP_SECURITY_V_NONE = "";

    /**
     * Set to Y to enable STARTTLS, or N to disable it. STARTTLS is for
     * connecting to an SMTP server port using a plain (non-encrypted)
     * connection, then elevating to an encrypted connection on the same port.
     */
    String SMTP_SECURITY_V_STARTTLS = "starttls";
    /**
     *
     */
    String SMTP_SECURITY_V_SSL = "ssl";

    /**
     *
     */
    String IMAP_SECURITY_V_NONE = "";

    /**
     *
     */
    String IMAP_SECURITY_V_STARTTLS = "starttls";

    /**
     *
     */
    String IMAP_SECURITY_V_SSL = "ssl";

    /**
     *
     */
    Integer IMAP_CONNECTION_TIMEOUT_V_DEFAULT = 10000;
    Integer IMAP_TIMEOUT_V_DEFAULT = 10000;

    Long IMAP_MAX_FILE_MB_V_DEFAULT = 5L;
    Integer IMAP_MAX_FILES_V_DEFAULT = 1;

    /** */
    Long WEBPRINT_MAX_FILE_MB_V_DEFAULT = 10L;

    /** */
    Long WEBAPP_PDFPGP_MAX_UPLOAD_FILE_MB_V_DEFAULT = 10L;

    /**
     *
     */
    Integer WEBAPP_MAX_IDLE_SECS_V_NONE = 0;

    String CARD_NUMBER_FORMAT_V_DEC = "DEC";
    String CARD_NUMBER_FORMAT_V_HEX = "HEX";

    String CARD_NUMBER_FIRSTBYTE_V_LSB = "LSB";
    String CARD_NUMBER_FIRSTBYTE_V_MSB = "MSB";

    /**
     *
     */
    Integer NUMBER_V_NONE = 0;

    /**
     * .
     */
    enum KeyType {
        /**
         * .
         */
        BIG_DECIMAL,

        /**
         * .
         */
        LOCALIZED_MULTI_LINE,

        /**
         * .
         */
        LOCALIZED_SINGLE_LINE,

        /**
         * .
         */
        MULTI_LINE,

        /**
         * .
         */
        SINGLE_LINE
    };

    /**
     * .
     */
    enum Key {

        /**
         * See {@link DaoBatchCommitterImpl}.
         */
        DB_BATCH_COMMIT_CHUNK_SIZE(//
                "db.batch.commit-chunk-size", NUMBER_VALIDATOR,
                DEFAULT_BATCH_COMMIT_CHUNK_SIZE),

        /**
         * The number of rows in the result set for exporting tables to a
         * database backup.
         */
        DB_EXPORT_QUERY_MAX_RESULTS(//
                "db.export.query-max-results", NUMBER_VALIDATOR,
                DEFAULT_EXPORT_QUERY_MAX_RESULTS),

        /**
         *
         */
        FINANCIAL_GLOBAL_CREDIT_LIMIT(//
                "financial.global.credit-limit", KeyType.BIG_DECIMAL, "0.00"),

        /**
         * ISO 4217 codes, like EUR, USD, JPY, etc. A <i>blank</i> Currency Code
         * is API updatable with {@link JsonRpcMethodName#SET_CONFIG_PROPERTY}
         * When set, it can only be "changed" by server command
         * {@link JsonRpcMethodName#CHANGE_BASE_CURRENCY}.
         */
        FINANCIAL_GLOBAL_CURRENCY_CODE(//
                "financial.global.currency-code", CURRENCY_VALIDATOR,
                API_UPDATABLE_ON),

        /**
         * A comma separated list of Point-of-Sale payment methods.
         */
        FINANCIAL_POS_PAYMENT_METHODS(//
                "financial.pos.payment-methods", API_UPDATABLE_OFF),

        /**
         *
         */
        FINANCIAL_POS_RECEIPT_HEADER(//
                "financial.pos.receipt-header", API_UPDATABLE_OFF),

        /**
         *
         */
        FINANCIAL_PRINTER_COST_DECIMALS(//
                "financial.printer.cost-decimals", ACCOUNTING_DECIMAL_VALIDATOR,
                DEFAULT_FINANCIAL_PRINTER_COST_DECIMALS, API_UPDATABLE_ON),

        /**
         * When "Y", PaperCut Personal Account is the leading account for
         * personal financial transactions and credit checks. Is active when
         * {@link #PAPERCUT_ENABLE} is "Y".
         */
        FINANCIAL_USER_ACCOUNT_PAPERCUT_ENABLE(//
                "financial.user.account.papercut.enable", BOOLEAN_VALIDATOR,
                V_NO, API_UPDATABLE_ON),

        /**
         *
         */
        FINANCIAL_USER_BALANCE_DECIMALS(//
                "financial.user.balance-decimals", ACCOUNTING_DECIMAL_VALIDATOR,
                DEFAULT_FINANCIAL_USER_BALANCE_DECIMALS, API_UPDATABLE_ON),

        /**
         * .
         */
        FINANCIAL_USER_TRANSFER_ENABLE(//
                "financial.user.transfers.enable", BOOLEAN_VALIDATOR, V_YES),

        /**
         * .
         */
        FINANCIAL_USER_TRANSFER_ENABLE_COMMENTS(//
                "financial.user.transfers.enable-comments", BOOLEAN_VALIDATOR,
                V_YES),

        /**
         * .
         */
        FINANCIAL_USER_TRANSFER_AMOUNT_MIN(//
                "financial.user.transfers.amount-min", KeyType.BIG_DECIMAL,
                "0.01"),

        /**
         * .
         */
        FINANCIAL_USER_TRANSFER_AMOUNT_MAX(//
                "financial.user.transfers.amount-max", KeyType.BIG_DECIMAL,
                "999999999.99"),

        /**
         * .
         */
        FINANCIAL_USER_TRANSFER_ENABLE_LIMIT_GROUP(//
                "financial.user.transfers.enable-limit-group",
                BOOLEAN_VALIDATOR, V_NO),

        /**
         * .
         */
        FINANCIAL_USER_TRANSFER_LIMIT_GROUP(//
                "financial.user.transfers.limit-group", API_UPDATABLE_OFF),

        /**
         * .
         */
        FINANCIAL_USER_VOUCHERS_ENABLE(//
                "financial.user.vouchers.enable", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        FINANCIAL_VOUCHER_CARD_HEADER(//
                "financial.voucher.card-header", KeyType.LOCALIZED_SINGLE_LINE),

        /**
         *
         */
        FINANCIAL_VOUCHER_CARD_FOOTER(//
                "financial.voucher.card-footer", API_UPDATABLE_OFF),

        /**
         *
         */
        FINANCIAL_VOUCHER_CARD_FONT_FAMILY(//
                "financial.voucher.card-font-family",
                INTERNAL_FONT_FAMILY_VALIDATOR,
                DEFAULT_INTERNAL_FONT_FAMILY.toString()),

        /**
         * URL of external user page with information about the Bitcoin
         * transaction hash. The value is in {@link MessageFormat} pattern where
         * {0} is the hash. E.g.
         *
         * <pre>
         * https://blockchain.info/tx-index/{0}
         * https://blockexplorer.com/tx/{0}
         * </pre>
         */
        FINANCIAL_BITCOIN_USER_PAGE_URL_PATTERN_TRX(//
                "financial.bitcoin.user-page.url-pattern.trx",
                URL_VALIDATOR_OPT, "https://blockchain.info/tx-index/{0}"),

        /**
         * URL of external user page with information about the Bitcoin address.
         * The value is in {@link MessageFormat} pattern where {0} is the
         * address. E.g.
         *
         * <pre>
         * https://blockchain.info/address/{0}
         * https://blockexplorer.com/address/{0}
         * </pre>
         */
        FINANCIAL_BITCOIN_USER_PAGE_URL_PATTERN_ADDRESS(//
                "financial.bitcoin.user-page.url-pattern.address",
                URL_VALIDATOR_OPT, "https://blockchain.info/address/{0}"),

        /**
         *
         */
        AUTH_MODE_NAME("auth-mode.name", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        AUTH_MODE_NAME_SHOW("auth-mode.name.show", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        AUTH_MODE_ID("auth-mode.id", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        AUTH_MODE_ID_PIN_REQUIRED(//
                "auth-mode.id.pin-required", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        AUTH_MODE_ID_IS_MASKED(//
                "auth-mode.id.is-masked", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        AUTH_MODE_ID_SHOW("auth-mode.id.show", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        AUTH_MODE_CARD_LOCAL("auth-mode.card-local", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        AUTH_MODE_YUBIKEY("auth-mode.yubikey", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        AUTH_MODE_YUBIKEY_SHOW(//
                "auth-mode.yubikey.show", BOOLEAN_VALIDATOR, V_YES),

        /**
         * .
         */
        AUTH_MODE_YUBIKEY_API_CLIENT_ID(//
                "auth-mode.yubikey.api.client-id", API_UPDATABLE_ON),

        /**
         * .
         */
        AUTH_MODE_YUBIKEY_API_SECRET_KEY(//
                "auth-mode.yubikey.api.secret-key", API_UPDATABLE_ON),

        /**
         *
         */
        AUTH_MODE_CARD_PIN_REQUIRED(//
                "auth-mode.card.pin-required", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        AUTH_MODE_CARD_SELF_ASSOCIATION(//
                "auth-mode.card.self-association", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Number of msecs after which an IP Card Number Detect event expires.
         */
        AUTH_MODE_CARD_IP_EXPIRY_MSECS(//
                "auth-mode.card-ip.expiry-msecs", NUMBER_VALIDATOR, "2000"),

        /**
         *
         */
        AUTH_MODE_CARD_LOCAL_SHOW(//
                "auth-mode.card-local.show", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        AUTH_MODE_DEFAULT(//
                "auth-mode-default", null, AUTH_MODE_V_NAME,
                new String[] { AUTH_MODE_V_NAME, AUTH_MODE_V_ID,
                        AUTH_MODE_V_CARD_LOCAL, AUTH_MODE_V_YUBIKEY },
                API_UPDATABLE_OFF),

        /**
         * Authentication method.
         */
        AUTH_METHOD(//
                "auth.method", null, AUTH_METHOD_V_NONE,
                new String[] { AUTH_METHOD_V_NONE, AUTH_METHOD_V_UNIX,
                        AUTH_METHOD_V_LDAP },
                API_UPDATABLE_ON),

        /**
         *
         */
        AUTH_LDAP_ADMIN_DN("auth.ldap.admin-dn", API_UPDATABLE_ON),

        /**
         *
         */
        AUTH_LDAP_ADMIN_PASSWORD("auth.ldap.admin-password", API_UPDATABLE_ON),

        /**
         *
         */
        AUTH_LDAP_BASE_DN("auth.ldap.basedn", API_UPDATABLE_ON),

        /**
         * LDAP Host name or IP address.
         */
        AUTH_LDAP_HOST("auth.ldap.host", "localhost", API_UPDATABLE_ON),

        /**
         * LDAP host IP port number.
         */
        AUTH_LDAP_PORT(//
                "auth.ldap.port", IP_PORT_VALIDATOR, "389", API_UPDATABLE_ON),

        /**
         * Use SSL for the LDAP connection.
         */
        AUTH_LDAP_USE_SSL(//
                "auth.ldap.use-ssl", BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_ON),

        /**
         * Trust self-signed certificate for LDAP SSL?
         */
        AUTH_LDAP_USE_SSL_TRUST_SELF_SIGNED(//
                "auth.ldap.use-ssl.trust-self-signed", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         *
         */
        CARD_NUMBER_FORMAT(//
                "card.number.format", null, CARD_NUMBER_FORMAT_V_HEX,
                new String[] { CARD_NUMBER_FORMAT_V_DEC,
                        CARD_NUMBER_FORMAT_V_HEX },
                API_UPDATABLE_OFF),

        /**
         *
         */
        CARD_NUMBER_FIRST_BYTE(//
                "card.number.first-byte", null, CARD_NUMBER_FIRSTBYTE_V_LSB,
                new String[] { CARD_NUMBER_FIRSTBYTE_V_LSB,
                        CARD_NUMBER_FIRSTBYTE_V_MSB },
                API_UPDATABLE_OFF),

        /**
         * IMPORTANT: the value of this key should be GT one (1) hour, since the
         * renewal is Quartz scheduled with Key.ScheduleHourly.
         */
        CUPS_IPP_SUBSCR_NOTIFY_LEASE_DURATION(//
                "cups.ipp.subscription.notify-lease-duration", NUMBER_VALIDATOR,
                "4200", API_UPDATABLE_ON),

        /**
         * Max number of IPP connections per CUPS server.
         */
        CUPS_IPP_MAX_CONNECTIONS(//
                "cups.ipp.max-connections", NUMBER_VALIDATOR, "10",
                API_UPDATABLE_ON),

        /**
         * Timeout in milliseconds until a IPP connection with local CUPS server
         * is established.
         */
        CUPS_IPP_LOCAL_CONNECT_TIMEOUT_MSEC(//
                "cups.ipp.local-connect-timeout-msec", NUMBER_VALIDATOR, "5000",
                API_UPDATABLE_ON),

        /**
         * Timeout in milliseconds to receive IPP data from local CUPS server
         * after the connection is established, i.e. maximum time of inactivity
         * between two data packets.
         */
        CUPS_IPP_LOCAL_SOCKET_TIMEOUT_MSEC(//
                "cups.ipp.local-socket-timeout-msec", NUMBER_VALIDATOR, "9000",
                API_UPDATABLE_ON),

        /**
         * Is access of remote CUPS enabled?
         */
        CUPS_IPP_REMOTE_ENABLED(//
                "cups.ipp.remote-enabled", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * Timeout in milliseconds until a IPP connection with remote CUPS
         * server is established.
         */
        CUPS_IPP_REMOTE_CONNECT_TIMEOUT_MSEC(//
                "cups.ipp.remote-connect-timeout-msec", NUMBER_VALIDATOR,
                "5000", API_UPDATABLE_ON),

        /**
         * Timeout in milliseconds to receive IPP data from remote CUPS server
         * after the connection is established, i.e. maximum time of inactivity
         * between two data packets.
         */
        CUPS_IPP_REMOTE_SOCKET_TIMEOUT_MSEC(//
                "cups.ipp.remote-socket-timeout-msec", NUMBER_VALIDATOR, "9000",
                API_UPDATABLE_ON),

        /**
         * Cancel CUPS job when stopped.
         */
        CUPS_JOBSTATE_CANCEL_IF_STOPPED_ENABLE(//
                "cups.job-state.cancel-if-stopped.enable", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),

        /**
         * Heartbeat (milliseconds) for performing a CUPS pull of job id status
         * while monitoring CUPS notifications. When CUPS notifies (push) job id
         * status in between, the heartbeat offset for this job id is reset to
         * zero.
         */
        CUPS_NOTIFIER_JOB_STATUS_PULL_HEARTBEAT_MSEC(//
                "cups.notifier.job-status-pull.heartbeat-msec",
                NUMBER_VALIDATOR, "30000"),

        /**
         *
         */
        DELETE_ACCOUNT_TRX_LOG(//
                "delete.account-trx-log", BOOLEAN_VALIDATOR, V_YES),

        /**
         * A value of {@code -1} is interpreted as {@code null}.
         */
        DELETE_ACCOUNT_TRX_DAYS(//
                "delete.account-trx-log.days", NUMBER_VALIDATOR, "365"),

        /**
         *
         */
        DELETE_APP_LOG("delete.app-log", BOOLEAN_VALIDATOR, V_YES),

        /**
         * A value of {@code -1} is interpreted as {@code null}.
         */
        DELETE_APP_LOG_DAYS("delete.app-log.days", NUMBER_VALIDATOR, "365"),

        /**
         *
         */
        DELETE_DOC_LOG("delete.doc-log", BOOLEAN_VALIDATOR, V_YES),

        /**
         * A value of {@code -1} is interpreted as {@code null}.
         */
        DELETE_DOC_LOG_DAYS("delete.doc-log.days", NUMBER_VALIDATOR, "365"),

        /**
         * The default port for {@link DeviceTypeEnum#CARD_READER}.
         */
        DEVICE_CARD_READER_DEFAULT_PORT(//
                "device.card-reader.default-port", NUMBER_VALIDATOR, "7772"),

        /**
         *
         */
        DOC_CONVERT_XPS_TO_PDF_ENABLED(//
                "doc.convert.xpstopdf-enabled", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         *
         */
        DOC_CONVERT_LIBRE_OFFICE_ENABLED(//
                "doc.convert.libreoffice-enabled", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * Max number of HTTP download connections.
         */
        DOWNLOAD_MAX_CONNECTIONS(//
                "download.max-connections", NUMBER_VALIDATOR, "10",
                API_UPDATABLE_ON),

        /**
         * Max number of HTTP download connections per route (host).
         */
        DOWNLOAD_MAX_CONNECTIONS_PER_ROUTE(//
                "download.max-connections-per-route", NUMBER_VALIDATOR, "2",
                API_UPDATABLE_ON),

        /**
         * Timeout in milliseconds until a download connection is established.
         */
        DOWNLOAD_CONNECT_TIMEOUT_MSEC(//
                "download.connect-timeout-msec", NUMBER_VALIDATOR, "5000",
                API_UPDATABLE_ON),

        /**
         * Timeout in milliseconds to receive data from remote host after the
         * connection is established, i.e. maximum time of inactivity between
         * two data packets.
         */
        DOWNLOAD_SOCKET_TIMEOUT_MSEC(//
                "download.socket-timeout-msec", NUMBER_VALIDATOR, "9000",
                API_UPDATABLE_ON),

        /**
         * Max number of RESTful client connections.
         */
        RESTFUL_CLIENT_MAX_CONNECTIONS(//
                "restful.client.max-connections", NUMBER_VALIDATOR, "100",
                API_UPDATABLE_ON),

        /**
         * Max number of RESTful client connections per route (host).
         */
        RESTFUL_CLIENT_MAX_CONNECTIONS_PER_ROUTE(//
                "restful.client.max-connections-per-route", NUMBER_VALIDATOR,
                "20", API_UPDATABLE_ON),

        /**
         * Timeout in milliseconds until a connection is established.
         */
        RESTFUL_CLIENT_CONNECT_TIMEOUT_MSEC(//
                "restful.client.connect-timeout-msec", NUMBER_VALIDATOR, "4000",
                API_UPDATABLE_ON),

        /**
         * Timeout in milliseconds to receive data from remote host after the
         * connection is established, i.e. maximum time of inactivity between
         * two data packets.
         */
        RESTFUL_CLIENT_READ_TIMEOUT_MSEC(//
                "restful.client.read-timeout-msec", NUMBER_VALIDATOR, "2000",
                API_UPDATABLE_ON),

        /**
         * Trust self-signed certificate of RESTful servers?
         */
        RESTFUL_CLIENT_SSL_TRUST_SELF_SIGNED(//
                "restful.client.trust-self-signed", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         *
         */
        ENV_CO2_GRAMS_PER_SHEET(//
                "environment.co2-grams-per-sheet", "5.1", API_UPDATABLE_OFF),

        /**
         *
         */
        ENV_SHEETS_PER_TREE(//
                "environment.sheets-per-tree", NUMBER_VALIDATOR, "8333"),

        /**
         *
         */
        ENV_WATT_HOURS_PER_SHEET(//
                "environment.watt-hours-per-sheet", "12.5", API_UPDATABLE_OFF),

        /**
         * Enable PaperCut Custom User Sync Integration (boolean).
         */
        EXT_PAPERCUT_USER_SYNC_ENABLE(//
                "ext.papercut.user.sync.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * PaperCut Custom User Sync Integration: Basic Authentication Username.
         */
        EXT_PAPERCUT_USER_SYNC_USERNAME(//
                "ext.papercut.user.sync.username", "", API_UPDATABLE_ON),

        /**
         * PaperCut Custom User Sync Integration: Basic Authentication Password.
         */
        EXT_PAPERCUT_USER_SYNC_PASSWORD(//
                "ext.papercut.user.sync.password", "", API_UPDATABLE_ON),

        /**
         * Client IP addresses (CIDR) that are allowed to use PaperCut Custom
         * User Sync Integration (when void, not a single client is allowed).
         */
        EXT_PAPERCUT_USER_SYNC_IP_ADDRESSES_ALLOWED(//
                "ext.papercut.user.sync.ip-addresses-allowed",
                CIDR_RANGES_VALIDATOR_OPT, API_UPDATABLE_OFF),

        /**
         * The base URL, i.e. "protocol://authority" <i>without</i> the path, of
         * the Web API callback interface (no trailing slash) (optional).
         */
        EXT_WEBAPI_CALLBACK_URL_BASE(//
                "ext.webapi.callback.url-base", URL_VALIDATOR_OPT, ""),

        /**
         * The URL of the User Web App used by the Web API to redirect to after
         * remote Web App dialog is done (optional).
         */
        EXT_WEBAPI_REDIRECT_URL_WEBAPP_USER(//
                "ext.webapi.redirect.url-webapp-user", URL_VALIDATOR_OPT, ""),

        /**
         * Google Cloud Print enabled (boolean).
         */
        GCP_ENABLE("gcp.enable", BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_OFF),

        /**
         * Max seconds to wait for a GCP connect.
         */
        GC_CONNECT_TIMEOUT_SECS(//
                "gcp.connect-timeout-secs", NUMBER_VALIDATOR, "5"),

        /**
         * Max seconds to wait for a GCP event.
         */
        GCP_EVENT_TIMEOUT_SECS("gcp.event-timeout-secs", NUMBER_VALIDATOR, "5"),

        /**
         *
         */
        GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_ENABLE(//
                "gcp.job-owner-unknown.cancel-mail.enable", BOOLEAN_VALIDATOR,
                V_YES),

        /**
         *
         */
        GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_BODY(//
                "gcp.job-owner-unknown.cancel-mail.body",
                KeyType.LOCALIZED_MULTI_LINE),

        /**
         *
         */
        GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_SUBJECT(//
                "gcp.job-owner-unknown.cancel-mail.subject",
                KeyType.LOCALIZED_SINGLE_LINE),

        /**
         * Do we have an Internet connection?
         */
        INFRA_INTERNET_CONNECTED(//
                "infra.internet-connected", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        INTERNAL_USERS_ENABLE(//
                "internal-users.enable", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        INTERNAL_USERS_CAN_CHANGE_PW(//
                "internal-users.user-can-change-password", BOOLEAN_VALIDATOR,
                V_YES),

        /**
         *
         */
        INTERNAL_USERS_NAME_PREFIX(//
                "internal-users.username-prefix", "guest-", API_UPDATABLE_OFF),

        /**
         *
         */
        INTERNAL_USERS_PW_LENGTH_MIN(//
                "internal-users.password-length-min", NUMBER_VALIDATOR, "6"),

        /**
         *
         */
        IPP_EXT_CONSTRAINT_BOOKLET_ENABLE(//
                "ipp.ext.constraint.booklet.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * This is a temporary solution: see Mantis #987.
         */
        IPP_JOB_NAME_SPACE_TO_UNDERSCORE_ENABLE(//
                "ipp.job-name.space-to-underscore.enable", BOOLEAN_VALIDATOR,
                V_NO, API_UPDATABLE_ON),

        /**
         * The base URL, i.e. "protocol://authority" <i>without</i> the path, of
         * the IPP Internet Printer URI (no trailing slash) (optional).
         */
        IPP_INTERNET_PRINTER_URI_BASE(//
                "ipp.internet-printer.uri-base", URI_VALIDATOR_OPT, ""),

        /** */
        IPP_ROUTING_ENABLE(//
                "ipp.routing.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * See this <a href=
         * "http://docs.oracle.com/javase/jndi/tutorial/ldap/search/batch.html"
         * >explanation</a> and this <a href=
         * "http://docs.oracle.com/javase/jndi/tutorial/ldap/search/src/Batchsize.java"
         * >sample code</a>.
         */
        LDAP_BATCH_SIZE("ldap.batchsize", NUMBER_VALIDATOR, "500"),

        /**
         * The LDAP field that contains the group members.
         */
        LDAP_SCHEMA_GROUP_MEMBER_FIELD(//
                "ldap.schema.group-member-field", API_UPDATABLE_ON),

        /**
         * The LDAP field that contains the group's name.
         */
        LDAP_SCHEMA_GROUP_NAME_FIELD(//
                "ldap.schema.group-name-field", API_UPDATABLE_ON),

        /**
         * The LDAP field that contains the group's full name.
         */
        LDAP_SCHEMA_GROUP_FULL_NAME_FIELD(//
                "ldap.schema.group-full-name-field", API_UPDATABLE_ON),

        /**
         * The LDAP search to retrieve the group. The <code>{0}</code> in the
         * search is replaced with {@code *} for all group searches. If no
         * search is defined, the default is
         * <code>([groupMemberField]={0})</code> , which means get all entries
         * with at least one member.
         * <p>
         * IMPORTANT: The search must include the <code>{0}</code> value.
         * </p>
         */
        LDAP_SCHEMA_GROUP_SEARCH("ldap.schema.group-search", API_UPDATABLE_ON),

        /**
         * The LDAP search to retrieve the users as member of a group. The {0}
         * in the expression is replaced with the distinguishedName (DN) of the
         * group. The extra {1} in the search is replaced with an optional
         * filter to fetch enabled users only.
         * <p>
         * Note: Active Directory only.
         * </p>
         * <p>
         * IMPORTANT: The search must include the <code>{0}</code> value.
         * </p>
         */
        LDAP_SCHEMA_USER_NAME_GROUP_SEARCH(//
                "ldap.schema.user-name-group-search", API_UPDATABLE_ON),

        /**
         * The LDAP search to retrieve the nested groups of a parent group. The
         * {0} in the expression is replaced with the distinguishedName (DN) of
         * the parent group.
         * <p>
         * Note: Active Directory only.
         * </p>
         * <p>
         * IMPORTANT: The search must include the <code>{0}</code> value.
         * </p>
         */
        LDAP_SCHEMA_NESTED_GROUP_SEARCH(//
                "ldap.schema.nested-group-search", API_UPDATABLE_ON),

        /**
         * The LDAP field that contains the Distinguished Name (DN).
         * <p>
         * Note: Active Directory only.
         * </p>
         */
        LDAP_SCHEMA_DN_FIELD("ldap.schema.dn-field", API_UPDATABLE_ON),

        /**
         * Boolean to allow or deny disabled users. When blank, LDAP default is
         * used.
         * <p>
         * Note: Active Directory only.
         * </p>
         */
        LDAP_ALLOW_DISABLED_USERS(//
                "ldap.disabled-users.allow", BOOLEAN_VALIDATOR_OPT,
                API_UPDATABLE_ON),

        /**
         * Boolean to indicate if filtering out disabled users is done locally
         * (by checking the userAccountControl attribute), or remotely (by AND
         * in userAccountControl in the LDAP query). When blank, LDAP default is
         * used.
         * <p>
         * Note: Active Directory only.
         * </p>
         */
        LDAP_FILTER_DISABLED_USERS_LOCALLY(//
                "ldap.disabled-users.local-filter", BOOLEAN_VALIDATOR_OPT,
                API_UPDATABLE_ON),

        /**
         * If {@code Y}, then the group member field contains the user's
         * username. If {@code N}, then the group member field contains the
         * user's DN.
         */
        LDAP_SCHEMA_POSIX_GROUPS("ldap.schema.posix-groups", API_UPDATABLE_ON),

        /**
         * LdapSchema* properties have "" default value.
         */
        LDAP_SCHEMA_TYPE(//
                "ldap.schema.type", null, LDAP_TYPE_V_OPEN_LDAP,
                new String[] { LDAP_TYPE_V_ACTIV, LDAP_TYPE_V_E_DIR,
                        LDAP_TYPE_V_APPLE, LDAP_TYPE_V_OPEN_LDAP,
                        LDAP_TYPE_V_G_SUITE },
                API_UPDATABLE_ON),

        /**
         * The LDAP field that contains the user's username.
         */
        LDAP_SCHEMA_USER_NAME_FIELD(//
                "ldap.schema.user-name-field", API_UPDATABLE_ON),

        /**
         * The LDAP search to retrieve the user. The <code>{0}</code> in the
         * search is replaced with {@code *} when listing all users, and
         * {@code [username]} when searching for a specific user. If no search
         * is defined, the default is <code>([userNameField]={0})</code>.
         * <p>
         * IMPORTANT: The search must include the <code>{0}</code> value.
         * </p>
         * <p>
         * NOTE: Active Directory Only. The extra {1} in the search is replaced
         * with an optional filter to fetch enabled users only.
         * </p>
         */
        LDAP_SCHEMA_USER_NAME_SEARCH(//
                "ldap.schema.user-name-search", API_UPDATABLE_ON),

        /**
         * The LDAP field that contains the user's full name.
         */
        LDAP_SCHEMA_USER_FULL_NAME_FIELD(//
                "ldap.schema.user-full-name-field", API_UPDATABLE_ON),

        /**
         * The LDAP field that contains the user's email address.
         */
        LDAP_SCHEMA_USER_EMAIL_FIELD(//
                "ldap.schema.user-email-field", API_UPDATABLE_ON),

        /**
         * The LDAP field that contains the user's department.
         */
        LDAP_SCHEMA_USER_DEPARTMENT_FIELD(//
                "ldap.schema.user-department-field", API_UPDATABLE_ON),

        /**
         * The LDAP field that contains the user's office location.
         */
        LDAP_SCHEMA_USER_OFFICE_FIELD(//
                "ldap.schema.user-office-field", API_UPDATABLE_ON),

        /**
         * The LDAP field that contains the user's Card Number.
         */
        LDAP_SCHEMA_USER_CARD_NUMBER_FIELD(//
                "ldap.schema.user-card-number-field", API_UPDATABLE_ON),

        /**
         *
         */
        LDAP_SCHEMA_USER_CARD_NUMBER_FIRST_BYTE(//
                "ldap.user-card-number.first-byte", null,
                CARD_NUMBER_FIRSTBYTE_V_LSB,
                new String[] { CARD_NUMBER_FIRSTBYTE_V_LSB,
                        CARD_NUMBER_FIRSTBYTE_V_MSB },
                API_UPDATABLE_OFF),

        /**
         *
         */
        LDAP_SCHEMA_USER_CARD_NUMBER_FORMAT(//
                "ldap.user-card-number.format", null, CARD_NUMBER_FORMAT_V_HEX,
                new String[] { CARD_NUMBER_FORMAT_V_DEC,
                        CARD_NUMBER_FORMAT_V_HEX },
                API_UPDATABLE_OFF),

        /**
         * The LDAP field that contains the user's ID Number.
         */
        LDAP_SCHEMA_USER_ID_NUMBER_FIELD(//
                "ldap.schema.user-id-number-field", API_UPDATABLE_ON),

        /**
         * Date on which this SavaPage instance was first installed. The
         * community role at this point is "Visitor", and the date defaults to
         * the start date of the visiting period.
         */
        COMMUNITY_VISITOR_START_DATE(//
                "community.visitor.start-date", API_UPDATABLE_OFF),

        /**
         * Is PaperCut integration enabled?
         */
        PAPERCUT_ENABLE("papercut.enable", BOOLEAN_VALIDATOR, V_NO),

        /**
         * PaperCut Database JDBC driver, like "org.postgresql.Driver".
         */
        PAPERCUT_DB_JDBC_DRIVER("papercut.db.jdbc-driver", API_UPDATABLE_ON),

        /**
         * PaperCut Database JDBC url.
         */
        PAPERCUT_DB_JDBC_URL("papercut.db.jdbc-url", API_UPDATABLE_ON),

        /**
         * PaperCut Database user.
         */
        PAPERCUT_DB_USER("papercut.db.user", API_UPDATABLE_ON),

        /**
         * PaperCut Database password.
         */
        PAPERCUT_DB_PASSWORD("papercut.db.password", API_UPDATABLE_ON),

        /**
         * PaperCut Server host.
         */
        PAPERCUT_SERVER_HOST(//
                "papercut.server.host", "localhost", API_UPDATABLE_ON),

        /**
         * PaperCut Server port.
         */
        PAPERCUT_SERVER_PORT("papercut.server.port", IP_PORT_VALIDATOR, "9191"),

        /**
         * PaperCut authentication token for Web Services.
         */
        PAPERCUT_SERVER_AUTH_TOKEN(//
                "papercut.webservices.auth-token", API_UPDATABLE_ON),

        /**
         * PaperCut XML-RPC path. E.g.{@code /rpc/api/xmlrpc}
         */
        PAPERCUT_XMLRPC_URL_PATH(//
                "papercut.xmlrpc.url-path", "/rpc/api/xmlrpc",
                API_UPDATABLE_OFF),

        /**
         *
         */
        API_JSONRPC_SECRET_KEY(//
                "api.jsonrpc.secret-key", API_UPDATABLE_OFF),

        /**
         * Client IP addresses (CIDR) that are allowed to use the JSON_RPC API
         * (when void, all client addresses are allowed).
         */
        API_JSONRPC_IP_ADDRESSES_ALLOWED(//
                "api.jsonrpc.ext.ip-addresses-allowed",
                CIDR_RANGES_VALIDATOR_OPT, API_UPDATABLE_OFF),

        /**
         * RESTful API: Basic Authentication Username.
         */
        API_RESTFUL_AUTH_USERNAME(//
                "api.restful.auth.username", "", API_UPDATABLE_ON),

        /**
         * RESTful API: Basic Authentication Password.
         */
        API_RESTFUL_AUTH_PASSWORD(//
                "api.restful.auth.password", "", API_UPDATABLE_ON),

        /**
         * Client IP addresses (CIDR) that are allowed to use RESTful API (when
         * void, all client addresses are allowed).
         */
        API_RESTFUL_IP_ADDRESSES_ALLOWED(//
                "api.restful.ip-addresses-allowed", CIDR_RANGES_VALIDATOR_OPT,
                API_UPDATABLE_OFF),

        /**
         * Admin Atom Feed: enable.
         */
        FEED_ATOM_ADMIN_ENABLE(//
                "feed.atom.admin.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * Tuesday-Saturday at 3:00.
         */
        FEED_ATOM_ADMIN_SCHEDULE(//
                "feed.atom.admin.schedule", CRON_EXPR_VALIDATOR,
                "0 0 3 ? * 3-7", API_UPDATABLE_OFF),

        /**
         * Admin Atom Feed: UUID as feed id.
         */
        FEED_ATOM_ADMIN_UUID(//
                "feed.atom.admin.uuid", UUID_VALIDATOR,
                UUID.randomUUID().toString(), API_UPDATABLE_ON),

        /**
         * Admin Atom Feed: Basic Authentication Username.
         */
        FEED_ATOM_ADMIN_USERNAME(//
                "feed.atom.admin.username", "", API_UPDATABLE_ON),

        /**
         * Admin Atom Feed: Basic Authentication Password.
         */
        FEED_ATOM_ADMIN_PASSWORD(//
                "feed.atom.admin.password", "", API_UPDATABLE_ON),

        /**
         *
         */
        PRINT_IMAP_ENABLE(//
                "print.imap.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_OFF),

        /**
         *
         */
        PRINT_IMAP_HOST("print.imap.host", API_UPDATABLE_OFF),

        /**
         * The port to connect to on the IMAP server.
         */
        PRINT_IMAP_PORT("print.imap.port", IP_PORT_VALIDATOR, "143"),

        /**
         * Socket connection timeout value in milliseconds. Default is infinite
         * timeout.
         */
        PRINT_IMAP_CONNECTION_TIMEOUT_MSECS(//
                "print.imap.connectiontimeout-msecs", NUMBER_VALIDATOR,
                IMAP_CONNECTION_TIMEOUT_V_DEFAULT.toString()),

        /**
         * Socket I/O timeout value in milliseconds. Default is infinite
         * timeout.
         */
        PRINT_IMAP_TIMEOUT_MSECS(//
                "print.imap.timeout-msecs", NUMBER_VALIDATOR,
                IMAP_TIMEOUT_V_DEFAULT.toString()),

        /**
         *
         */
        PRINT_IMAP_SECURITY(//
                "print.imap.security", IMAP_SECURITY_V_STARTTLS,
                API_UPDATABLE_OFF),

        /**
         * Username for IMAP authentication.
         */
        PRINT_IMAP_USER_NAME("print.imap.user", API_UPDATABLE_OFF),

        /**
         * Password for IMAP authentication.
         */
        PRINT_IMAP_PASSWORD("print.imap.password", API_UPDATABLE_OFF),

        /**
         * Produces extra IMAP related logging for troubleshooting.
         */
        PRINT_IMAP_DEBUG("print.imap.debug", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        PRINT_IMAP_INBOX_FOLDER(//
                "print.imap.folder.inbox", "Inbox", API_UPDATABLE_OFF),

        /**
         *
         */
        PRINT_IMAP_TRASH_FOLDER(//
                "print.imap.folder.trash", "Trash", API_UPDATABLE_OFF),

        /**
         *
         */
        PRINT_IMAP_SESSION_HEARTBEAT_SECS(//
                "print.imap.session.heartbeat-secs", NUMBER_VALIDATOR, "300"),

        /**
         *
         */
        PRINT_IMAP_SESSION_DURATION_SECS(//
                "print.imap.session.duration-secs", NUMBER_VALIDATOR, "3000"),

        /**
         *
         */
        PRINT_IMAP_MAX_FILE_MB(//
                "print.imap.max-file-mb", NUMBER_VALIDATOR,
                IMAP_MAX_FILE_MB_V_DEFAULT.toString(), API_UPDATABLE_ON),

        /**
         *
         */
        PRINT_IMAP_MAX_FILES(//
                "print.imap.max-files", NUMBER_VALIDATOR,
                IMAP_MAX_FILES_V_DEFAULT.toString(), API_UPDATABLE_ON),

        /**
        *
        */
        PRINTER_SNMP_ENABLE(//
                "printer.snmp.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         *
         */
        PRINTER_SNMP_READ_TRIGGER_MINS(//
                "printer.snmp.read.trigger-mins", NUMBER_VALIDATOR, "240",
                API_UPDATABLE_ON),

        /**
         *
         */
        PRINTER_SNMP_READ_RETRIES(//
                "printer.snmp.read.retries", NUMBER_VALIDATOR, "2",
                API_UPDATABLE_ON),

        /**
         *
         */
        PRINTER_SNMP_READ_TIMEOUT_MSECS(//
                "printer.snmp.read.timeout-msec", NUMBER_VALIDATOR, "1500",
                API_UPDATABLE_ON),

        /**
         *
         */
        PRINTER_SNMP_MARKER_PERC_WARN(//
                "printer.snmp.marker.percent.warn", NUMBER_VALIDATOR, "30",
                API_UPDATABLE_ON),

        /**
         *
         */
        PRINTER_SNMP_MARKER_PERC_ALERT(//
                "printer.snmp.marker.percent.alert", NUMBER_VALIDATOR, "10",
                API_UPDATABLE_ON),

        /**
         * Insert requesting users ad-hoc when printing from Smartschool to the
         * {@link ReservedIppQueueEnum#SMARTSCHOOL} queue.
         * <p>
         * WARNING: this option assumes that the user is TRUSTED.
         * </p>
         * <p>
         * NOTE: when 'false', the requesting user needs to exist in the
         * database before he is allowed to print.
         * </p>
         */
        SMARTSCHOOL_USER_INSERT_LAZY_PRINT(//
                "smartschool.user.insert.lazy-print", BOOLEAN_VALIDATOR, V_NO),

        /**
         * User used for simulation and testing.
         */
        SMARTSCHOOL_SIMULATION_USER(//
                "smartschool.simulation.user",
                DEFAULT_SMARTSCHOOL_SIMULATION_USER, API_UPDATABLE_OFF),

        /**
         * Student user 1 used for simulation and testing.
         */
        SMARTSCHOOL_SIMULATION_STUDENT_1(//
                "smartschool.simulation.student-1",
                DEFAULT_SMARTSCHOOL_SIMULATION_STUDENT_1, API_UPDATABLE_OFF),

        /**
         * Student user 2 used for simulation and testing.
         */
        SMARTSCHOOL_SIMULATION_STUDENT_2(//
                "smartschool.simulation.student-2",
                DEFAULT_SMARTSCHOOL_SIMULATION_STUDENT_2, API_UPDATABLE_OFF),

        /**
         * The simulation variant of
         * {@link #SMARTSCHOOL_SOAP_PRINT_POLL_HEARTBEATS}.
         */
        SMARTSCHOOL_SIMULATION_SOAP_PRINT_POLL_HEARTBEATS(//
                "smartschool.simulation.soap.print.poll.heartbeats",
                NUMBER_VALIDATOR, "15"),

        /**
         * Is PaperCut integration for Smartschool component enabled?
         */
        SMARTSCHOOL_PAPERCUT_ENABLE(//
                "smartschool.papercut.enable", BOOLEAN_VALIDATOR, V_NO),

        /**
         * The shared top-level account that must be present in PaperCut.
         * Several sub-accounts will be lazy created by SavaPage. Besides, any
         * PaperCut printer assigned to Smartschool will be configured to charge
         * to this account.
         */
        SMARTSCHOOL_PAPERCUT_ACCOUNT_SHARED_PARENT(//
                "smartschool.papercut.account.shared.parent", "Smartschool",
                API_UPDATABLE_OFF),

        /**
         * The sub-account of
         * {@link #SMARTSCHOOL_PAPERCUT_ACCOUNT_SHARED_PARENT} holding Print Job
         * transactions.
         */
        SMARTSCHOOL_PAPERCUT_ACCOUNT_SHARED_CHILD_JOBS(//
                "smartschool.papercut.account.shared.child.jobs", "Jobs",
                API_UPDATABLE_OFF),

        /**
         * This is one of the “Multiple Personal Accounts” in PaperCut and is
         * used by SavaPage to charge printing costs to individual persons.
         */
        SMARTSCHOOL_PAPERCUT_ACCOUNT_PERSONAL(//
                "smartschool.papercut.account.personal", "Smartschool",
                API_UPDATABLE_OFF),

        /**
         * The PaperCut account_type (like "USER-001", "USER-002") of the
         * {@link #SMARTSCHOOL_PAPERCUT_ACCOUNT_PERSONAL}. This is a technical
         * value determined by PaperCut. When a value is specified in this key
         * it is used to filter personal transactions in JDBC queries (CSV
         * downloads) for the Smartschool context.
         */
        SMARTSCHOOL_PAPERCUT_ACCOUNT_PERSONAL_TYPE(//
                "smartschool.papercut.account.personal-type",
                API_UPDATABLE_OFF),
        /**
         * Timeout in milliseconds until a Smartschool SOAP connection is
         * established.
         */
        SMARTSCHOOL_SOAP_CONNECT_TIMEOUT_MILLIS(//
                "smartschool.soap.connect-timeout-msec", NUMBER_VALIDATOR,
                "20000"),

        /**
         * Timeout in milliseconds to receive data from Smartschool SOAP
         * Service.
         */
        SMARTSCHOOL_SOAP_SOCKET_TIMEOUT_MILLIS(//
                "smartschool.soap.socket-timeout-msec", NUMBER_VALIDATOR,
                "20000"),

        /**
         * Is Smartschool (1) enabled?
         */
        SMARTSCHOOL_1_ENABLE(//
                "smartschool.1.enable", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Printer name for Smartschool direct proxy printing (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER(//
                "smartschool.1.soap.print.proxy-printer", API_UPDATABLE_OFF),

        /**
         * Printer name for Smartschool direct duplex proxy printing (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_DUPLEX(//
                "smartschool.1.soap.print.proxy-printer-duplex",
                API_UPDATABLE_OFF),

        /**
         * Printer name for Smartschool direct proxy grayscale printing (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE(//
                "smartschool.1.soap.print.proxy-printer-grayscale",
                API_UPDATABLE_OFF),

        /**
         * Printer name for Smartschool direct proxy grayscale duplex printing
         * (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE_DUPLEX(//
                "smartschool.1.soap.print.proxy-printer-grayscale-duplex",
                API_UPDATABLE_OFF),

        /**
         * {@code true} if costs are charged to individual students,
         * {@code false} if costs are charged to shared "Klas" accounts only.
         */
        SMARTSCHOOL_1_SOAP_PRINT_CHARGE_TO_STUDENTS(//
                "smartschool.1.soap.print.charge-to-students",
                BOOLEAN_VALIDATOR, V_YES),

        /**
         * SOAP endpoint URL of Smartschool Print Center (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_URL(//
                "smartschool.1.soap.print.endpoint.url", API_UPDATABLE_OFF),

        /**
         * Password of SOAP endpoint URL of Smartschool Print Center (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_PASSWORD(//
                "smartschool.1.soap.print.endpoint.password",
                API_UPDATABLE_OFF),

        /**
         * {@code true} if this module is a node Smartschool Print Cluster (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_NODE_ENABLE(//
                "smartschool.1.soap.print.node.enable", BOOLEAN_VALIDATOR,
                V_NO),

        /**
         * The unique node ID of this module in the Smartschool cluster (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_NODE_ID(//
                "smartschool.1.soap.print.node.id", API_UPDATABLE_OFF),

        /**
         * {@code true} if this node acts as Smartschool Print Center Proxy (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_NODE_PROXY_ENABLE(//
                "smartschool.1.soap.print.node.proxy.enable", BOOLEAN_VALIDATOR,
                V_NO),

        /**
         * SOAP endpoint URL of Smartschool Print Center Proxy (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_NODE_PROXY_ENDPOINT_URL(//
                "smartschool.1.soap.print.node.proxy.endpoint.url",
                API_UPDATABLE_OFF),

        /**
         * Is Smartschool (2) enabled?
         */
        SMARTSCHOOL_2_ENABLE("smartschool.2.enable", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Printer name for Smartschool direct proxy printing (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER(//
                "smartschool.2.soap.print.proxy-printer", API_UPDATABLE_OFF),

        /**
         * Printer name for Smartschool direct duplex proxy printing (1).
         */
        SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_DUPLEX(//
                "smartschool.2.soap.print.proxy-printer-duplex",
                API_UPDATABLE_OFF),

        /**
         * Printer name for Smartschool direct proxy grayscale printing (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE(//
                "smartschool.2.soap.print.proxy-printer-grayscale",
                API_UPDATABLE_OFF),

        /**
         * Printer name for Smartschool direct proxy grayscale duplex printing
         * (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER_GRAYSCALE_DUPLEX(//
                "smartschool.2.soap.print.proxy-printer-grayscale-duplex",
                API_UPDATABLE_OFF),

        /**
         * {@code true} if costs are charged to individual students,
         * {@code false} if costs are charged to shared "Klas" accounts only.
         */
        SMARTSCHOOL_2_SOAP_PRINT_CHARGE_TO_STUDENTS(//
                "smartschool.2.soap.print.charge-to-students",
                BOOLEAN_VALIDATOR, V_YES),

        /**
         * SOAP endpoint URL of Smartschool Print Center (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_URL(//
                "smartschool.2.soap.print.endpoint.url", API_UPDATABLE_OFF),

        /**
         * Password of SOAP endpoint URL of Smartschool Print Center (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_PASSWORD(//
                "smartschool.2.soap.print.endpoint.password",
                API_UPDATABLE_OFF),

        /**
         * {@code true} if this module is a node Smartschool Print Cluster (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_NODE_ENABLE(//
                "smartschool.2.soap.print.node.enable", BOOLEAN_VALIDATOR,
                V_NO),

        /**
         * The unique node ID of this module in the Smartschool cluster (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_NODE_ID(//
                "smartschool.2.soap.print.node.id", API_UPDATABLE_OFF),

        /**
         * {@code true} if this node acts as Smartschool Print Center Proxy (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_NODE_PROXY_ENABLE(//
                "smartschool.2.soap.print.node.proxy.enable", BOOLEAN_VALIDATOR,
                V_NO),

        /**
         * SOAP endpoint URL of Smartschool Print Center Proxy (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_NODE_PROXY_ENDPOINT_URL(//
                "smartschool.2.soap.print.node.proxy.endpoint.url",
                API_UPDATABLE_OFF),

        /**
         * The heartbeat (seconds) within a Smartschool print polling session.
         */
        SMARTSCHOOL_SOAP_PRINT_POLL_HEARTBEAT_SECS(//
                "smartschool.soap.print.poll.heartbeat-secs", NUMBER_VALIDATOR,
                "2"),

        /**
         * The number of heartbeats within a Smartschool print polling session
         * after which an actual poll to Smartschool is executed.
         * <p>
         * Smartschool has a rate-limit of one (1) poll per 2 minutes. When
         * limit is exceeded an error message is returned. Note: status updates
         * can be send unlimited.
         * </p>
         */
        SMARTSCHOOL_SOAP_PRINT_POLL_HEARTBEATS(//
                "smartschool.soap.print.poll.heartbeats", NUMBER_VALIDATOR,
                "61"),

        /**
         * The duration (seconds) of a Smartschool print polling session.
         */
        SMARTSCHOOL_SOAP_PRINT_POLL_SESSION_DURATION_SECS(//
                "smartschool.soap.print.poll.session.duration-secs",
                NUMBER_VALIDATOR, "3600"),

        /**
         * .
         */
        SOFFICE_ENABLE(//
                "soffice.enable", BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_ON),

        /**
         * The LibreOffice home location. When empty, a probe to likely
         * candidates is performed to retrieve the location.
         */
        SOFFICE_HOME("soffice.home", API_UPDATABLE_OFF),

        /**
         * A temporary profile directory is created for each UNO connection
         * process with its own defaults settings. With this config item you can
         * provide a profile directory containing customized settings instead.
         * This template directory will be copied to the temporary profile.
         */
        SOFFICE_PROFILE_TEMPLATE_DIR(//
                "soffice.profile.template-dir", API_UPDATABLE_OFF),

        /**
         * A comma/space separated list of TCP/IP ports to localhost LibreOffice
         * (UNO) connection instances to be launched by SavaPage.
         */
        SOFFICE_CONNECTION_PORTS(//
                "soffice.connection.ports", "2002,2003", API_UPDATABLE_OFF),

        /**
         * The number of executed tasks after which the UNO connection is
         * restarted. When {@code 0} (zero) the process is <i>never</i>
         * restarted.
         */
        SOFFICE_CONNECTION_RESTART_TASK_COUNT(//
                "soffice.connection.restart-task-count", NUMBER_VALIDATOR,
                "200"),

        /**
         * Wait time (milliseconds) for a UNO connection to become available for
         * task execution.
         */
        SOFFICE_TASK_QUEUE_TIMEOUT_MSEC(//
                "soffice.task.queue-timeout-msec", NUMBER_VALIDATOR, "10000"),

        /**
         * Wait time (milliseconds) for a conversion task to complete.
         */
        SOFFICE_TASK_EXEC_TIMEOUT_MSEC(//
                "soffice.task.exec-timeout-msec", NUMBER_VALIDATOR, "20000"),

        /**
         * Retry interval (milliseconds) for host process to respond.
         */
        SOFFICE_RESPOND_RETRY_MSEC(//
                "soffice.respond.retry-msec", NUMBER_VALIDATOR, "250"),

        /**
         * Wait time (milliseconds) for host process to respond (after retries).
         */
        SOFFICE_RESPOND_TIMEOUT_MSEC(//
                "soffice.respond.timeout-msec", NUMBER_VALIDATOR, "30000"),

        /**
         * Retry interval (milliseconds) for host process to start.
         */
        SOFFICE_START_RETRY_MSEC(//
                "soffice.start.retry-msec", NUMBER_VALIDATOR, "1000"),

        /**
         * Wait time (milliseconds) for host process to start (after retries).
         */
        SOFFICE_START_TIMEOUT_MSEC(//
                "soffice.start.timeout-msec", NUMBER_VALIDATOR, "120000"),

        /**
         *
         */
        REPORTS_PDF_INTERNAL_FONT_FAMILY(//
                "reports.pdf.font-family", INTERNAL_FONT_FAMILY_VALIDATOR,
                DEFAULT_INTERNAL_FONT_FAMILY.toString()),

        /**
         * Boolean (Default is false). If true, prevents use of the non-standard
         * AUTHENTICATE LOGIN command, instead using the plain LOGIN command.
         */
        MAIL_IMAP_AUTH_LOGIN_DISABLE(//
                "print.imap.auth.login.disable", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Boolean (Default is false). If true, prevents use of the AUTHENTICATE
         * PLAIN command.
         */
        MAIL_IMAP_AUTH_PLAIN_DISABLE(//
                "print.imap.auth.plain.disable", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Boolean (Default is false). If true, prevents use of the AUTHENTICATE
         * NTLM command.
         */
        MAIL_IMAP_AUTH_NTLM_DISABLE(//
                "print.imap.auth.ntlm.disable", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        MAIL_SMTP_HOST("mail.smtp.host", "localhost", API_UPDATABLE_ON),

        /**
         * The port to connect to on the SMTP server. Common ports include 25 or
         * 587 for STARTTLS, and 465 for SMTPS.
         */
        MAIL_SMTP_PORT("mail.smtp.port", IP_PORT_VALIDATOR, "465"),

        /**
         * STARTTLS is for connecting to an SMTP server port using a plain
         * (non-encrypted) connection, then elevating to an encrypted connection
         * on the same port.
         */
        MAIL_SMTP_SECURITY(//
                "mail.smtp.security", SMTP_SECURITY_V_SSL, API_UPDATABLE_OFF),

        /**
         * Username for SMTP authentication. Commonly an email address.
         */
        MAIL_SMTP_USER_NAME("mail.smtp.username", API_UPDATABLE_ON),

        /**
         * Password for SMTP authentication.
         */
        MAIL_SMTP_PASSWORD("mail.smtp.password", API_UPDATABLE_ON),

        /**
         * Produces extra SMTP related logging for troubleshooting.
         */
        MAIL_SMTP_DEBUG("mail.smtp.debug", BOOLEAN_VALIDATOR, V_NO),

        /**
         * .
         */
        MAIL_SMTP_MAX_FILE_KB(//
                "mail.smtp.max-file-kb", NUMBER_VALIDATOR, "1024"),

        /**
         * Value for SMTP property: <b>mail.smtp.connectiontimeout</b>
         * <p>
         * Timeout (in milliseconds) for establishing the SMTP connection.
         * </p>
         * <p>
         * This timeout is implemented by java.net.Socket.
         * </p>
         */
        MAIL_SMTP_CONNECTIONTIMEOUT(//
                "mail.smtp.connectiontimeout", NUMBER_VALIDATOR, "5000"),

        /**
         * Value for SMTP property: <b>mail.smtp.timeout</b>
         * <p>
         * The timeout (milliseconds) for sending the mail messages.
         * </p>
         * <p>
         * This timeout is implemented by java.net.Socket.
         * </p>
         */
        MAIL_SMTP_TIMEOUT("mail.smtp.timeout", NUMBER_VALIDATOR, "5000"),

        /**
         * Heartbeat (milliseconds) to poll the store-and-forward mail outbox
         * for new messages.
         */
        MAIL_OUTBOX_POLL_HEARTBEAT_MSEC("mail.outbox.poll.heartbeat-msec",
                NUMBER_VALIDATOR, "10000"),

        /**
         * Interval (milliseconds) between sending each message.
         */
        MAIL_OUTBOX_SEND_INTERVAL_MSEC("mail.outbox.send.interval-msec",
                NUMBER_VALIDATOR, "1000"),

        /**
         *
         */
        MAIL_FROM_ADDRESS(//
                "mail.from.address", NOT_EMPTY_VALIDATOR, API_UPDATABLE_ON),

        /**
         *
         */
        MAIL_FROM_NAME(//
                "mail.from.name", CommunityDictEnum.SAVAPAGE.getWord(),
                API_UPDATABLE_ON),

        /**
         *
         */
        MAIL_REPLY_TO_ADDRESS("mail.reply.to.address", API_UPDATABLE_ON),

        /**
         *
         */
        MAIL_REPLY_TO_NAME(//
                "mail.reply.to.name", "DO NOT REPLY", API_UPDATABLE_ON),

        /**
         * If "Y", mail is PGP/MIME signed (if PGP Secret Key is present).
         */
        MAIL_PGP_MIME_SIGN("mail.pgp.mime.sign", BOOLEAN_VALIDATOR, V_YES),

        /**
         * If "Y" <i>and</i> mail is PGP signed, it is also PGP encrypted, for
         * each recipients.
         */
        MAIL_PGP_MIME_ENCRYPT(//
                "mail.pgp.mime.encrypt", BOOLEAN_VALIDATOR, V_YES),

        /**
         * OpenPGP Public Key Server URL (optional).
         */
        PGP_PKS_URL("pgp.pks.url", URL_VALIDATOR_OPT),

        /**
         * The path of the custom template files, relative to
         * {@link ConfigManager#SERVER_REL_PATH_CUSTOM_TEMPLATE}.
         */
        CUSTOM_TEMPLATE_HOME("custom.template.home", API_UPDATABLE_ON),

        /**
         * The path of the custom Email template files, relative to
         * {@link ConfigManager#SERVER_REL_PATH_CUSTOM_TEMPLATE}.
         */
        CUSTOM_TEMPLATE_HOME_MAIL(//
                "custom.template.home.mail", API_UPDATABLE_ON),

        /**
         *
         */
        PRINT_IN_PDF_ENCRYPTED_ALLOW(//
                "print-in.pdf.encrypted.allow", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * Enable PDF "repair" option for print-in PDF documents (Web Print)
         * (boolean).
         */
        PRINT_IN_PDF_INVALID_REPAIR(//
                "print-in.pdf.invalid.repair", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * Enable {@code pdffonts} validation/repair for print-in PDF documents
         * (Web Print) (boolean).
         */
        PRINT_IN_PDF_FONTS_VERIFY(//
                "print-in.pdf.fonts.verify", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * Enable embedding of all fonts (including standard PDF fonts) if
         * non-embedded/non-standard fonts are present in print-in PDF document
         * (Web Print) (boolean).
         */
        PRINT_IN_PDF_FONTS_EMBED(//
                "print-in.pdf.fonts.embed", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * Enable/disable cleaning of print-in PDF documents (Web Print).
         */
        PRINT_IN_PDF_CLEAN(//
                "print-in.pdf.clean", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * Number of minutes after which a print-in job expires. When zero (0)
         * there is NO expiry.
         *
         */
        PRINT_IN_JOB_EXPIRY_MINS(//
                "print-in.job-expiry.mins", NUMBER_VALIDATOR, V_ZERO),

        /**
         * Enable Copy Job option for Job Ticket (boolean). When {@code true} a
         * job ticket for a copy job can be created.
         */
        JOBTICKET_COPIER_ENABLE(//
                "jobticket.copier.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * Enable "delivery data/time" option for Job Ticket (boolean).
         */
        JOBTICKET_DELIVERY_DATETIME_ENABLE(//
                "jobticket.delivery-datetime.enable", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * Enable "delivery time" option for Job Ticket (boolean).
         */
        JOBTICKET_DELIVERY_TIME_ENABLE(//
                "jobticket.delivery-time.enable", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * Default delivery time (days-of-week count after ticket creation).
         */
        JOBTICKET_DELIVERY_DAYS(//
                "jobticket.delivery-days", NUMBER_VALIDATOR, "1",
                API_UPDATABLE_ON),

        /**
         * Minimal delivery time (days-of-week count).
         */
        JOBTICKET_DELIVERY_DAYS_MIN(//
                "jobticket.delivery-days-min", NUMBER_VALIDATOR, "1",
                API_UPDATABLE_ON),

        /**
         * Delivery days of week.
         */
        JOBTICKET_DELIVERY_DAYS_OF_WEEK(//
                "jobticket.delivery-days-of-week",
                CRON_EXPR_DAY_OF_WEEK_VALIDATOR, "MON-FRI", API_UPDATABLE_ON),

        /**
         * Time of delivery on delivery day as minutes after midnight. For
         * instance: 8h30m = 8*60+30 = 510
         */
        JOBTICKET_DELIVERY_DAY_MINUTES(//
                "jobticket.delivery-day-minutes", NUMBER_VALIDATOR, "510",
                API_UPDATABLE_ON),

        /**
         * Enable notification by email to owner of job ticket when ticket is
         * completed (Boolean).
         */
        JOBTICKET_NOTIFY_EMAIL_COMPLETED_ENABLE(//
                "jobticket.notify-email.completed.enable", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),

        /**
         * Enable notification by email to owner of job ticket when ticket is
         * canceled (Boolean).
         */
        JOBTICKET_NOTIFY_EMAIL_CANCELED_ENABLE(//
                "jobticket.notify-email.canceled.enable", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),

        /**
         * Send job ticket email notification with content-type as "text/html".
         */
        JOBTICKET_NOTIFY_EMAIL_CONTENT_TYPE_HTML(//
                "jobticket.notify-email.content-type.html", BOOLEAN_VALIDATOR,
                V_NO, API_UPDATABLE_ON),

        /**
         * A comma separated list of Job Ticket domains to be applied as job
         * ticket number prefix. Each domain on the list is formatted as
         * "DOM/domain/n", where "DOM" is a unique N-letter upper-case mnemonic,
         * "/" is a fixed separator, "domain" is a case-sensitive single word
         * used in UI context, and n is a unique ID number.
         *
         * E.g. "A/DomainA/1,B/DomainB/2,C/DomainC/3". When "B" domain is
         * applied, a generated ticket number looks like "B/EE1-FA3E-6596".
         */
        JOBTICKET_DOMAINS("jobticket.domains", KeyType.MULTI_LINE),

        /**
         * Enable {@link IConfigProp.Key#JOBTICKET_DOMAINS} (boolean).
         */
        JOBTICKET_DOMAINS_ENABLE(//
                "jobticket.domains.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * Is domains required, when
         * {@link IConfigProp.Key#JOBTICKET_DOMAINS_ENABLE} ? (boolean).
         */
        JOBTICKET_DOMAINS_REQUIRED(//
                "jobticket.domains.required", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * A comma separated list of Job Ticket uses (applications) within a
         * domain to be applied as job ticket number prefix. Each use on the
         * list is formatted as "USE/usage", where "USE" is a unique N-letter
         * upper-case mnemonic, "/" is a fixed separator, and "usage" is a
         * case-sensitive single word used in UI context.
         *
         * E.g. "TE/Test,TA/Task,EX/Exam". When "EX" usage is applied, a
         * generated ticket number looks like "EX/EE1-FA3E-6596".
         */
        JOBTICKET_USES("jobticket.uses", KeyType.MULTI_LINE),

        /**
         * Enable {@link IConfigProp.Key#JOBTICKET_USES} (boolean).
         */
        JOBTICKET_USES_ENABLE(//
                "jobticket.uses.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * Is domains required, when
         * {@link IConfigProp.Key#JOBTICKET_USES_ENABLE} ? (boolean).
         */
        JOBTICKET_USES_REQUIRED(//
                "jobticket.uses.required", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * A comma separated list of Job Ticket tags to be applied as job ticket
         * number prefix. Each tag on the list is formatted as "TAG/word", where
         * "TAG" is a unique N-letter upper-case mnemonic, "/" is a fixed
         * separator, and "word" is a case-sensitive single word used in UI
         * context.
         *
         * E.g. "MATH/Maths,PHYS/Physics,CHEM/Chemistry". When "MATH" tag is
         * applied, a generated ticket number looks like "MATH/EE1-FA3E-6596".
         */
        JOBTICKET_TAGS("jobticket.tags", KeyType.MULTI_LINE),

        /**
         * Enable {@link IConfigProp.Key#JOBTICKET_TAGS} (boolean).
         */
        JOBTICKET_TAGS_ENABLE(//
                "jobticket.tags.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * Is tag required, when {@link IConfigProp.Key#JOBTICKET_TAGS_ENABLE} ?
         * (boolean).
         */
        JOBTICKET_TAGS_REQUIRED(//
                "jobticket.tags.required", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * Enable Doc Store (boolean).
         */
        DOC_STORE_ENABLE(//
                "doc.store.enable", BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_ON),

        /** */
        DOC_STORE_FREE_SPACE_LIMIT_MB(//
                "doc.store.free-space-limit-mb", NUMBER_VALIDATOR, "5000",
                API_UPDATABLE_ON),

        /** .--------------------------. */
        DOC_STORE_ARCHIVE_ENABLE(//
                "doc.store.archive.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_ARCHIVE_OUT_ENABLE(//
                "doc.store.archive.out.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_ARCHIVE_OUT_PRINT_ENABLE(//
                "doc.store.archive.out.print.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_ARCHIVE_OUT_PDF_ENABLE(//
                "doc.store.archive.out.pdf.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_ARCHIVE_IN_ENABLE(//
                "doc.store.archive.in.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_ARCHIVE_IN_PRINT_ENABLE(//
                "doc.store.archive.in.print.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_ARCHIVE_OUT_PRINT_DAYS_TO_KEEP(//
                "doc.store.archive.out.print.days-to-keep", NUMBER_VALIDATOR,
                "30", API_UPDATABLE_ON),
        /** */
        DOC_STORE_ARCHIVE_OUT_PDF_DAYS_TO_KEEP(//
                "doc.store.archive.out.pdf.days-to-keep", NUMBER_VALIDATOR,
                "30", API_UPDATABLE_ON),
        /** */
        DOC_STORE_ARCHIVE_IN_PRINT_DAYS_TO_KEEP(//
                "doc.store.archive.in.print.days-to-keep", NUMBER_VALIDATOR,
                "30", API_UPDATABLE_ON),

        /** .--------------------------. */
        DOC_STORE_JOURNAL_ENABLE(//
                "doc.store.journal.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_JOURNAL_OUT_ENABLE(//
                "doc.store.journal.out.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_JOURNAL_OUT_PRINT_ENABLE(//
                "doc.store.journal.out.print.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_JOURNAL_OUT_PDF_ENABLE(//
                "doc.store.journal.out.pdf.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_JOURNAL_IN_ENABLE(//
                "doc.store.journal.in.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_JOURNAL_IN_PRINT_ENABLE(//
                "doc.store.journal.in.print.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_JOURNAL_OUT_PRINT_DAYS_TO_KEEP(//
                "doc.store.journal.out.print.days-to-keep", NUMBER_VALIDATOR,
                "2", API_UPDATABLE_ON),
        /** */
        DOC_STORE_JOURNAL_OUT_PDF_DAYS_TO_KEEP(//
                "doc.store.journal.out.pdf.days-to-keep", NUMBER_VALIDATOR, "2",
                API_UPDATABLE_ON),
        /** */
        DOC_STORE_JOURNAL_IN_PRINT_DAYS_TO_KEEP(//
                "doc.store.journal.in.print.days-to-keep", NUMBER_VALIDATOR,
                "2", API_UPDATABLE_ON),

        /**
         * Enable Delegated Print (boolean).
         */
        PROXY_PRINT_DELEGATE_ENABLE(//
                "proxy-print.delegate.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * Enable delegated print account type {@link AccountTypeEnum#GROUP}
         * (boolean).
         */
        PROXY_PRINT_DELEGATE_ACCOUNT_GROUP_ENABLE(//
                "proxy-print.delegate.account.group.enable", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),

        /**
         * Enable delegated print account type {@link AccountTypeEnum#USER}
         * (boolean).
         */
        PROXY_PRINT_DELEGATE_ACCOUNT_USER_ENABLE(//
                "proxy-print.delegate.account.user.enable", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),

        /**
         * Enable preferred UserGroup IDs for delegated proxy print.
         */
        PROXY_PRINT_DELEGATE_GROUPS_PREFERRED_ENABLE(//
                "proxy-print.delegate.groups.preferred.enable",
                BOOLEAN_VALIDATOR, V_YES, API_UPDATABLE_ON),

        /**
         * Enable preferred Shared Account IDs for delegated proxy print.
         */
        PROXY_PRINT_DELEGATE_ACCOUNTS_PREFERRED_ENABLE(//
                "proxy-print.delegate.accounts.preferred.enable",
                BOOLEAN_VALIDATOR, V_YES, API_UPDATABLE_ON),

        /**
         * Enable delegated print account type {@link AccountTypeEnum#SHARED}
         * (boolean).
         */
        PROXY_PRINT_DELEGATE_ACCOUNT_SHARED_ENABLE(//
                "proxy-print.delegate.account.shared.enable", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),

        /**
         * Enable delegated User Group selection for print account type
         * {@link AccountTypeEnum#SHARED} (boolean).
         */
        PROXY_PRINT_DELEGATE_ACCOUNT_SHARED_GROUP_ENABLE(//
                "proxy-print.delegate.account.shared.group.enable",
                BOOLEAN_VALIDATOR, V_YES, API_UPDATABLE_ON),

        /**
         * Enable multiple delegated print copies (boolean).
         */
        PROXY_PRINT_DELEGATE_MULTIPLE_MEMBER_COPIES_ENABLE(//
                "proxy-print.delegate.multiple-member-copies.enable",
                BOOLEAN_VALIDATOR, V_YES, API_UPDATABLE_ON),

        /**
         * Enable direct input of group copies, bypassing calculation based on
         * number of members (boolean).
         */
        PROXY_PRINT_DELEGATE_GROUP_COPIES_ENABLE(//
                "proxy-print.delegate.group-copies.enable", BOOLEAN_VALIDATOR,
                V_NO, API_UPDATABLE_ON),

        /**
         * Number of minutes after which a PaperCut print is set to error when
         * the PaperCut print log is still not found.
         */
        PROXY_PRINT_PAPERCUT_PRINTLOG_MAX_MINS(// )
                "proxy-print.papercut.print-log.max-mins", NUMBER_VALIDATOR,
                "7200", API_UPDATABLE_ON),

        /**
         * Enable Personal Print integration with PaperCut (boolean).
         */
        PROXY_PRINT_PERSONAL_PAPERCUT_ENABLE(//
                "proxy-print.personal.papercut.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * Enable Delegated Print integration with PaperCut (boolean).
         */
        PROXY_PRINT_DELEGATE_PAPERCUT_ENABLE(//
                "proxy-print.delegate.papercut.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * The shared top-level account that must be present in PaperCut.
         * Several sub-accounts will be lazy created by SavaPage. Besides, any
         * PaperCut printer assigned to Delegated Print will be configured to
         * charge to this account.
         */
        PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_SHARED_PARENT(//
                "proxy-print.delegate.papercut.account.shared.parent",
                "SavaPage", API_UPDATABLE_OFF),

        /**
         * The sub-account of
         * {@link #PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_SHARED_PARENT} holding
         * Print Job transactions.
         */
        PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_SHARED_CHILD_JOBS(//
                "proxy-print.delegate.papercut.account.shared.child.jobs",
                "Jobs", API_UPDATABLE_OFF),

        /**
         * This is one of the “Multiple Personal Accounts” in PaperCut and is
         * used by SavaPage to charge Delegated Print costs to individual
         * persons.
         */
        PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_PERSONAL(//
                "proxy-print.delegate.papercut.account.personal", "SavaPage",
                API_UPDATABLE_OFF),

        /**
         * The PaperCut account_type (like "USER-001", "USER-002") of the
         * {@link #PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_PERSONAL}. This is a
         * technical value determined by PaperCut. When a value is specified in
         * this key it is used to filter personal transactions in JDBC queries
         * (CSV downloads) for the Delegated Print context.
         */
        PROXY_PRINT_DELEGATE_PAPERCUT_ACCOUNT_PERSONAL_TYPE(//
                "proxy-print.delegate.papercut.account.personal-type",
                API_UPDATABLE_OFF),

        /**
         * Enable non-secure proxy printing (Boolean).
         */
        PROXY_PRINT_NON_SECURE(//
                "proxy-print.non-secure", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        PROXY_PRINT_FAST_EXPIRY_MINS(//
                "proxy-print.fast-expiry-mins", NUMBER_VALIDATOR, "10"),

        /**
         *
         */
        PROXY_PRINT_HOLD_EXPIRY_MINS(//
                "proxy-print.hold-expiry-mins", NUMBER_VALIDATOR, "60"),

        /**
         *
         */
        PROXY_PRINT_DIRECT_EXPIRY_SECS(//
                "proxy-print.direct-expiry-secs", NUMBER_VALIDATOR, "20"),

        /**
         * Maximum number of pages allowed for a proxy print job.
         */
        PROXY_PRINT_MAX_PAGES("proxy-print.max-pages", NUMBER_VALIDATOR_OPT),

        /**
         * Restrict non-secure proxy printing to this Printer Group. See:
         * {@link PrinterGroup#getGroupName()}
         */
        PROXY_PRINT_NON_SECURE_PRINTER_GROUP(//
                "proxy-print.non-secure-printer-group", API_UPDATABLE_OFF),

        /**
         * Enable "remove graphics" option for Proxy Print (boolean).
         */
        PROXY_PRINT_REMOVE_GRAPHICS_ENABLE(//
                "proxy-print.remove-graphics.enable", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * Enable "repair" option for Proxy Print (boolean).
         *
         * @deprecated Use {@link #PRINT_IN_PDF_FONTS_EMBED} = Y.
         */
        @Deprecated
        PROXY_PRINT_REPAIR_ENABLE(//
                "proxy-print.repair.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * CRON expression: 10 minutes past midnight.
         */
        SCHEDULE_DAILY(//
                "schedule.daily", CRON_EXPR_VALIDATOR, "0 10 0 * * ?",
                API_UPDATABLE_OFF),

        /**
         * CRON expression: 12:55am each day (before 1am to miss DST
         * switch-overs).
         */
        SCHEDULE_DAILY_MAINT(//
                "schedule.daily-maintenance", CRON_EXPR_VALIDATOR,
                "0 55 0 * * ?", API_UPDATABLE_OFF),

        /**
         * CRON expression.
         */
        SCHEDULE_HOURLY(//
                "schedule.hourly", CRON_EXPR_VALIDATOR, "0 0 * * * ?",
                API_UPDATABLE_OFF),

        /**
         * CRON expression.
         */
        SCHEDULE_MONTHLY(//
                "schedule.monthly", CRON_EXPR_VALIDATOR, "0 30 0 1 * ?",
                API_UPDATABLE_OFF),

        /**
         * CRON expression: 20 minutes past midnight on Sunday morning.
         */
        SCHEDULE_WEEKLY(//
                "schedule.weekly", CRON_EXPR_VALIDATOR, "0 20 0 ? * 1",
                API_UPDATABLE_OFF),

        /**
         *
         */
        SCHEDULE_AUTO_SYNC_USER(//
                "schedule.auto-sync.user", BOOLEAN_VALIDATOR, V_YES),

        /**
         * All print-in documents (day).
         */
        STATS_PRINT_IN_ROLLING_DAY_DOCS(//
                "stats.print-in.rolling-day.docs", API_UPDATABLE_OFF),

        /**
         * All print-in PDF documents (day).
         */
        STATS_PRINT_IN_ROLLING_DAY_PDF(//
                "stats.print-in.rolling-day.pdf", API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_DAY_PDF_REPAIR(//
                "stats.print-in.rolling-day.pdf.repair", API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_DAY_PDF_REPAIR_FAIL(//
                "stats.print-in.rolling-day.pdf.repair.fail",
                API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_DAY_PDF_REPAIR_FONT(//
                "stats.print-in.rolling-day.pdf.repair.font",
                API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_DAY_PDF_REPAIR_FONT_FAIL(//
                "stats.print-in.rolling-day.pdf.repair.font.fail",
                API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_IN_ROLLING_DAY_PAGES(//
                "stats.print-in.rolling-day.pages", API_UPDATABLE_OFF),

        /**
         * All print-in documents (week).
         */
        STATS_PRINT_IN_ROLLING_WEEK_DOCS(//
                "stats.print-in.rolling-week.docs", API_UPDATABLE_OFF),

        /**
         * All print-in PDF documents (week).
         */
        STATS_PRINT_IN_ROLLING_WEEK_PDF(//
                "stats.print-in.rolling-week.pdf", API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_WEEK_PDF_REPAIR(//
                "stats.print-in.rolling-week.pdf.repair", API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_WEEK_PDF_REPAIR_FAIL(//
                "stats.print-in.rolling-week.pdf.repair.fail",
                API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_WEEK_PDF_REPAIR_FONT(//
                "stats.print-in.rolling-week.pdf.repair.font",
                API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_WEEK_PDF_REPAIR_FONT_FAIL(//
                "stats.print-in.rolling-week.pdf.repair.font.fail",
                API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_IN_ROLLING_WEEK_PAGES(//
                "stats.print-in.rolling-week.pages", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_IN_ROLLING_WEEK_BYTES(//
                "stats.print-in.rolling-week.bytes", API_UPDATABLE_OFF),

        /**
         * All print-in documents (month).
         */
        STATS_PRINT_IN_ROLLING_MONTH_DOCS(//
                "stats.print-in.rolling-month.docs", API_UPDATABLE_OFF),

        /**
         * All print-in PDF documents (month).
         */
        STATS_PRINT_IN_ROLLING_MONTH_PDF(//
                "stats.print-in.rolling-month.pdf", API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_MONTH_PDF_REPAIR(//
                "stats.print-in.rolling-month.pdf.repair", API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_MONTH_PDF_REPAIR_FAIL(//
                "stats.print-in.rolling-month.pdf.repair.fail",
                API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_MONTH_PDF_REPAIR_FONT(//
                "stats.print-in.rolling-month.pdf.repair.font",
                API_UPDATABLE_OFF),
        STATS_PRINT_IN_ROLLING_MONTH_PDF_REPAIR_FONT_FAIL(//
                "stats.print-in.rolling-month.pdf.repair.font.fail",
                API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_IN_ROLLING_MONTH_PAGES(//
                "stats.print-in.rolling-month.pages", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_IN_ROLLING_MONTH_BYTES(//
                "stats.print-in.rolling-month.bytes", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PDF_OUT_ROLLING_DAY_PAGES(//
                "stats.pdf-out.rolling-day.pages", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PDF_OUT_ROLLING_WEEK_PAGES(//
                "stats.pdf-out.rolling-week.pages", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PDF_OUT_ROLLING_WEEK_BYTES(//
                "stats.pdf-out.rolling-week.bytes", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PDF_OUT_ROLLING_MONTH_PAGES(//
                "stats.pdf-out.rolling-month.pages", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PDF_OUT_ROLLING_MONTH_BYTES(//
                "stats.pdf-out.rolling-month.bytes", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_DAY_PAGES(//
                "stats.print-out.rolling-day.pages", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_WEEK_PAGES(//
                "stats.print-out.rolling-week.pages", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_WEEK_SHEETS(//
                "stats.print-out.rolling-week.sheets", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_WEEK_ESU(//
                "stats.print-out.rolling-week.esu", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_WEEK_BYTES(//
                "stats.print-out.rolling-week.bytes", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_MONTH_PAGES(//
                "stats.print-out.rolling-month.pages", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_MONTH_SHEETS(//
                "stats.print-out.rolling-month.sheets", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_MONTH_ESU(//
                "stats.print-out.rolling-month.esu", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_MONTH_BYTES(//
                "stats.print-out.rolling-month.bytes", API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_TOTAL_RESET_DATE(//
                "stats.total.reset-date",
                String.valueOf(System.currentTimeMillis()), API_UPDATABLE_OFF),

        STATS_TOTAL_RESET_DATE_PRINT_IN(//
                "stats.total.reset-date.print-in",
                String.valueOf(System.currentTimeMillis()), API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_TOTAL_PDF_OUT_PAGES(//
                "stats.total.pdf-out.pages", V_ZERO, API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_TOTAL_PDF_OUT_BYTES(//
                "stats.total.pdf-out.bytes", V_ZERO, API_UPDATABLE_OFF),

        /**
        *
        */
        STATS_TOTAL_PRINT_IN_DOCS(//
                "stats.total.print-in.docs", V_ZERO, API_UPDATABLE_OFF),

        STATS_TOTAL_PRINT_IN_PDF(//
                "stats.total.print-in.pdf", V_ZERO, API_UPDATABLE_OFF),
        STATS_TOTAL_PRINT_IN_PDF_REPAIR(//
                "stats.total.print-in.pdf.repair", V_ZERO, API_UPDATABLE_OFF),
        STATS_TOTAL_PRINT_IN_PDF_REPAIR_FAIL(//
                "stats.total.print-in.pdf.repair.fail", V_ZERO,
                API_UPDATABLE_OFF),
        STATS_TOTAL_PRINT_IN_PDF_REPAIR_FONT(//
                "stats.total.print-in.pdf.repair.font", V_ZERO,
                API_UPDATABLE_OFF),
        STATS_TOTAL_PRINT_IN_PDF_REPAIR_FONT_FAIL(//
                "stats.total.print-in.pdf.repair.font.fail", V_ZERO,
                API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_TOTAL_PRINT_IN_PAGES(//
                "stats.total.print-in.pages", V_ZERO, API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_TOTAL_PRINT_IN_BYTES(//
                "stats.total.print-in.bytes", V_ZERO, API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_TOTAL_PRINT_OUT_PAGES(//
                "stats.total.print-out.pages", V_ZERO, API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_TOTAL_PRINT_OUT_SHEETS(//
                "stats.total.print-out.sheets", V_ZERO, API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_TOTAL_PRINT_OUT_ESU(//
                "stats.total.print-out.esu", V_ZERO, API_UPDATABLE_OFF),

        /**
         *
         */
        STATS_TOTAL_PRINT_OUT_BYTES(//
                "stats.total.print-out.bytes", V_ZERO, API_UPDATABLE_OFF),

        /**
         * Make a backup before a database schema upgrade.
         */
        SYS_BACKUP_BEFORE_DB_UPGRADE(//
                "system.backup.before-db-upgrade", BOOLEAN_VALIDATOR, V_YES),

        /**
         * Time in milliseconds when last backup was run.
         */
        SYS_BACKUP_LAST_RUN_TIME(//
                "system.backup.last-run-time", NUMBER_VALIDATOR, V_ZERO),

        /**
         *
         */
        SYS_BACKUP_DAYS_TO_KEEP(//
                "system.backup.days-to-keep", NUMBER_VALIDATOR, "30"),

        /**
         *
         */
        SYS_BACKUP_ENABLE_AUTOMATIC(//
                "system.backup.enable-automatic", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        SYS_DEFAULT_LOCALE(//
                "system.default-locale", LOCALE_VALIDATOR, API_UPDATABLE_ON),

        /**
         *
         */
        SYS_DEFAULT_PAPER_SIZE(//
                "system.default-papersize", PAPERSIZE_V_SYSTEM,
                API_UPDATABLE_OFF),

        /**
         * The DNS name of the server. Used to give user feedback for URL's,
         * e.g. URL's to use for IPP printing.
         */
        SYS_SERVER_DNS_NAME("system.server.dns-name", API_UPDATABLE_ON),

        /**
         * The major database schema version.
         * <p>
         * Do NOT set a value since it is present in installation database.
         * </p>
         */
        SYS_SCHEMA_VERSION("system.schema-version", API_UPDATABLE_OFF),

        /**
         * The minor database schema version.
         * <p>
         * This value is set in the installation database (since v0.9.3), but
         * defaults to "0" for pre v0.9.3 databases.
         * </p>
         */
        SYS_SCHEMA_VERSION_MINOR(//
                "system.schema-version-minor", V_ZERO, API_UPDATABLE_OFF),

        /**
         * Do NOT set a value since it is present in installation database.
         */
        SYS_SETUP_COMPLETED("system.setup-completed", API_UPDATABLE_OFF),

        /**
         * Enable CUPS job status synchronization at startup.
         */
        SYS_STARTUP_CUPS_SYNC_PRINT_JOBS_ENABLE(//
                "system.startup.cups.sync-print-jobs.enable", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),

        /**
         * When system is in maintenance mode, only admins can login to Web Apps
         * (regular users cannot).
         */
        SYS_MAINTENANCE("system.maintenance", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Heartbeat (seconds) to monitor system errors like deadlocked threads.
         */
        SYS_MONITOR_HEARTBEAT_SEC("system.monitor.heartbeat-sec",
                NUMBER_VALIDATOR, "120"),

        /**
         *
         */
        USER_CAN_CHANGE_PIN(//
                "user.can-change-pin", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * .
         */
        USER_PIN_LENGTH_MIN("user.pin-length-min", NUMBER_VALIDATOR, "4"),

        /**
         * .
         */
        USER_PIN_LENGTH_MAX(//
                "user.pin-length-max", NUMBER_VALIDATOR,
                NUMBER_V_NONE.toString()),

        /**
         *
         */
        USER_ID_NUMBER_LENGTH_MIN(//
                "user.id-number-length-min", NUMBER_VALIDATOR, "4"),

        /**
         * Insert users ad-hoc after successful authentication at the login
         * page.
         */
        USER_INSERT_LAZY_LOGIN(//
                "user.insert.lazy-login", BOOLEAN_VALIDATOR, V_YES),

        /**
         * Insert users ad-hoc when printing to a SavaPage printer.
         * <p>
         * WARNING: this option assumes that the user is TRUSTED.
         * </p>
         * <p>
         * NOTE: when 'false', the user needs to exist in the database before
         * any SavaPage printer can be used.
         * </p>
         */
        USER_INSERT_LAZY_PRINT(//
                "user.insert.lazy-print", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        USER_SOURCE_GROUP("user-source.group", API_UPDATABLE_ON),

        /**
         *
         */
        USER_SOURCE_UPDATE_USER_DETAILS(//
                "user-source.update-user-details", BOOLEAN_VALIDATOR, V_YES),

        /**
         * Client IP addresses (CIDR) that are allowed to use the User Client
         * App (when void, all client addresses are allowed).
         */
        CLIAPP_IP_ADDRESSES_ALLOWED(//
                "cliapp.ip-addresses-allowed", CIDR_RANGES_VALIDATOR_OPT,
                API_UPDATABLE_ON),

        /**
         * Enable Client App authentication for clients that are denied for
         * their IP address.
         */
        CLIAPP_AUTH_IP_ADDRESSES_DENIED_ENABLE(//
                "cliapp.auth.ip-addresses-denied.enable", BOOLEAN_VALIDATOR,
                V_NO),

        /**
         * (boolean) Trust the User Client App system account name as user
         * identification?
         */
        CLIAPP_AUTH_TRUST_USER_ACCOUNT(//
                "cliapp.auth.trust-user-account", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Secret administrator passkey of User Client App.
         */
        CLIAPP_AUTH_ADMIN_PASSKEY(//
                "cliapp.auth.admin-passkey", API_UPDATABLE_OFF),

        /**
         * Trust authenticated user in User Web App on same IP address as Client
         * App (Boolean, default TRUE).
         */
        CLIAPP_AUTH_TRUST_WEBAPP_USER_AUTH(//
                "cliapp.auth.trust-webapp-user-auth", BOOLEAN_VALIDATOR, V_YES),

        /**
         * The query string to be appended to the base URL when opening the User
         * Web App in response to a print-in event. Do <i>not</i> prefix the
         * value with a {@code '?'} or {@code '&'} character.
         */
        CLIAPP_PRINT_IN_URL_QUERY(//
                "cliapp.print-in.url-query", API_UPDATABLE_OFF),

        /**
         * Action button text on print-in action dialog for opening User Web
         * App.
         */
        CLIAPP_PRINT_IN_DIALOG_BUTTON_OPEN(//
                "cliapp.print-in.dialog.button-open", API_UPDATABLE_OFF),

        /**
         * .
         */
        ECO_PRINT_ENABLE("eco-print.enable", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Threshold for automatically creating an EcoPrint shadow file when PDF
         * arrives in SafePages inbox: if number of PDF pages is GT threshold
         * the shadow is not created.
         */
        ECO_PRINT_AUTO_THRESHOLD_SHADOW_PAGE_COUNT(//
                "eco-print.auto-threshold.page-count", NUMBER_VALIDATOR,
                V_ZERO),

        /**
         * .
         */
        ECO_PRINT_RESOLUTION_DPI(//
                "eco-print.resolution-dpi", NUMBER_VALIDATOR, "300"),

        /**
         * Discount percentage for EcoPrint proxy printing.
         */
        ECO_PRINT_DISCOUNT_PERC(//
                "eco-print.discount-percent", NUMBER_VALIDATOR, "15"),

        /**
         * (boolean) Show Document title in the DocLog.
         */
        WEBAPP_DOCLOG_SHOW_DOC_TITLE(//
                "webapp.doclog.show-doc-title", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        WEBAPP_NUMBER_UP_PREVIEW_ENABLE(//
                "webapp.number-up-preview.enable", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),
        /**
         *
         */
        WEBAPP_WATCHDOG_HEARTBEAT_SECS(//
                "webapp.watchdog.heartbeat-secs", NUMBER_VALIDATOR,
                DEFAULT_WEBAPP_WATCHDOG_HEARTBEAT_SECS),

        /**
         *
         */
        WEBAPP_WATCHDOG_TIMEOUT_SECS(//
                "webapp.watchdog.timeout-secs", NUMBER_VALIDATOR,
                DEFAULT_WEBAPP_WATCHDOG_TIMEOUT_SECS),

        /**
         * Admin WebApp: show technical info on dashboard?
         */
        WEBAPP_ADMIN_DASHBOARD_SHOW_TECH_INFO(//
                "webapp.admin.dashboard.show-tech-info", BOOLEAN_VALIDATOR,
                V_NO, API_UPDATABLE_ON),

        /**
         * Admin WebApp: show environmental effect on dashboard?
         */
        WEBAPP_ADMIN_DASHBOARD_SHOW_ENV_INFO(//
                "webapp.admin.dashboard.show-env-info", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),

        /**
         * Number of seconds after which cached Bitcoin wallet information
         * expires.
         */
        WEBAPP_ADMIN_BITCOIN_WALLET_CACHE_EXPIRY_SECS(//
                "webapp.admin.bitcoin.wallet.cache-expiry-secs",
                NUMBER_VALIDATOR, "3600"),

        /**
         * Enable PDF/PGP page in Admin WebApp.
         */
        WEBAPP_PDFPGP_ENABLE(//
                "webapp.pdfpgp.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
        *
        */
        WEBAPP_PDFPGP_MAX_UPLOAD_FILE_MB(//
                "webapp.pdfpgp.max-upload-file-mb", NUMBER_VALIDATOR,
                WEBAPP_PDFPGP_MAX_UPLOAD_FILE_MB_V_DEFAULT.toString()),

        /** */
        PDFPGP_VERIFICATION_ENABLE(//
                "pdfpgp.verification.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),
        /** */
        PDFPGP_VERIFICATION_HOST(//
                "pdfpgp.verification.host", API_UPDATABLE_ON),
        /** */
        PDFPGP_VERIFICATION_PORT(//
                "pdfpgp.verification.port", NUMBER_VALIDATOR_OPT,
                API_UPDATABLE_ON),

        /** */
        WEBAPP_JOBTICKETS_CANCEL_ALL_ENABLE(//
                "webapp.jobtickets.cancel-all.enable", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /** */
        WEBAPP_JOBTICKETS_PRINT_ALL_ENABLE(//
                "webapp.jobtickets.print-all.enable", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /** */
        WEBAPP_JOBTICKETS_CLOSE_ALL_ENABLE(//
                "webapp.jobtickets.close-all.enable", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /** */
        WEBAPP_JOBTICKETS_REOPEN_ENABLE(//
                "webapp.jobtickets.reopen.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * Number of job tickets to show in the list. A value of zero means all
         * available tickets are shown.
         */
        WEBAPP_JOBTICKETS_LIST_SIZE(//
                "webapp.jobtickets.list-size", NUMBER_VALIDATOR, "10"),

        /**
         * The minimum number of job tickets that can be shown in the list. A
         * value of zero means all available tickets are shown.
         */
        WEBAPP_JOBTICKETS_LIST_SIZE_MIN(//
                "webapp.jobtickets.list-size-min", NUMBER_VALIDATOR, "5"),

        /**
         * The maximum number of job tickets that can be shown in the list.
         */
        WEBAPP_JOBTICKETS_LIST_SIZE_MAX(//
                "webapp.jobtickets.list-size-max", NUMBER_VALIDATOR, "50"),

        /**
         * Trust authenticated user in Client App on same IP address as User Web
         * App (Boolean, default TRUE).
         */
        WEBAPP_USER_AUTH_TRUST_CLIAPP_AUTH(//
                "webapp.user.auth.trust-cliapp-auth", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        WEBAPP_USER_DOCLOG_SELECT_TYPE_DEFAULT_ORDER(//
                "webapp.user.doclog.select.type.default-order",
                new EnumSetValidator<>(DocLogScopeEnum.class),
                API_UPDATABLE_ON),

        /**
         * Is GDPR enabled in User Web App.
         */
        WEBAPP_USER_GDPR_ENABLE(//
                "webapp.user.gdpr.enable", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * Contact email address for GDPR (erase) requests.
         */
        WEBAPP_USER_GDPR_CONTACT_EMAIL(//
                "webapp.user.gdpr.contact.email", EMAIL_VALIDATOR_OPT,
                API_UPDATABLE_ON),

        /**
         * Max idle seconds after which automatic logout occurs.
         */
        WEBAPP_USER_MAX_IDLE_SECS(//
                "webapp.user.max-idle-secs", NUMBER_VALIDATOR,
                WEBAPP_MAX_IDLE_SECS_V_NONE.toString()),

        /**
         * Delete all print-in jobs at User WebApp logout.
         */
        WEBAPP_USER_LOGOUT_CLEAR_INBOX(//
                "webapp.user.logout.clear-inbox", BOOLEAN_VALIDATOR, V_NO),

        /**
         * User WebApp: The number of minutes before job expiration when a job
         * is signaled as nearing expiration. When zero (0) the expiration is
         * <i>not</i> signaled.
         */
        WEBAPP_USER_PRINT_IN_JOB_EXPIRY_SIGNAL_MINS(//
                "webapp.user.print-in.job-expiry.signal-mins", NUMBER_VALIDATOR,
                V_ZERO),

        /** */
        WEBAPP_USER_PROXY_PRINT_SCALING_MEDIA_MATCH_SHOW(//
                "webapp.user.proxy-print.scaling.media-match.show",
                BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_ON),

        /** */
        WEBAPP_USER_PROXY_PRINT_SCALING_MEDIA_CLASH_SHOW(//
                "webapp.user.proxy-print.scaling.media-clash.show",
                BOOLEAN_VALIDATOR, V_YES, API_UPDATABLE_ON),

        /** */
        WEBAPP_USER_PROXY_PRINT_SCALING_MEDIA_MATCH_DEFAULT(//
                "webapp.user.proxy-print.scaling.media-match.default",
                new EnumValidator<>(PrintScalingEnum.class),
                PrintScalingEnum.NONE.toString(), API_UPDATABLE_ON),

        /** */
        WEBAPP_USER_PROXY_PRINT_SCALING_MEDIA_CLASH_DEFAULT(//
                "webapp.user.proxy-print.scaling.media-clash.default",
                new EnumValidator<>(PrintScalingEnum.class),
                PrintScalingEnum.FIT.toString(), API_UPDATABLE_ON),

        /**
         * User WebApp: Max. copies for proxy printing.
         */
        WEBAPP_USER_PROXY_PRINT_MAX_COPIES(//
                "webapp.user.proxy-print.max-copies", NUMBER_VALIDATOR, "30",
                API_UPDATABLE_ON),

        /**
         * User WebApp: show archive scope (Boolean).
         */
        WEBAPP_USER_DOC_STORE_ARCHIVE_OUT_PRINT_PROMPT(//
                "webapp.user.doc.store.archive.out.print.prompt",
                BOOLEAN_VALIDATOR, V_YES, API_UPDATABLE_ON),

        /** */
        WEBAPP_USER_DATABASE_USER_ROW_LOCKING_ENABLED(//
                "webapp.user.database-user-row-locking.enabled",
                BOOLEAN_VALIDATOR, V_YES, API_UPDATABLE_ON),
        /**
         * User WebApp: enable a fixed inbox clearing scope after a proxy print
         * job is issued.
         */
        WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_ENABLE(//
                "webapp.user.proxy-print.clear-inbox.enable", BOOLEAN_VALIDATOR,
                V_NO, API_UPDATABLE_ON),

        /**
         * User WebApp: the fixed inbox clearing scope after proxy printing.
         */
        WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_SCOPE(//
                "webapp.user.proxy-print.clear-inbox.scope",
                new EnumValidator<>(InboxSelectScopeEnum.class),
                InboxSelectScopeEnum.ALL.toString(), API_UPDATABLE_ON),

        /**
         * User WebApp: show clearing scope (Boolean).
         */
        WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX_PROMPT(//
                "webapp.user.proxy-print.clear-inbox.prompt", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),

        /**
         * User WebApp: clear selected printer (including options) after proxy
         * printing.
         */
        WEBAPP_USER_PROXY_PRINT_CLEAR_PRINTER(//
                "webapp.user.proxy-print.clear-printer", BOOLEAN_VALIDATOR,
                V_NO, API_UPDATABLE_ON),

        /**
         * User WebApp: clear print delegate data after proxy printing.
         */
        WEBAPP_USER_PROXY_PRINT_CLEAR_DELEGATE(//
                "webapp.user.proxy-print.clear-delegate", BOOLEAN_VALIDATOR,
                V_NO, API_UPDATABLE_ON),

        /**
         * User WebApp: Can application of selected copies for delegates be
         * switched off in Print Dialog?
         */
        WEBAPP_USER_PROXY_PRINT_DELEGATE_COPIES_APPLY_SWITCH(//
                "webapp.user.proxy-print.delegate-copies-apply-switch",
                BOOLEAN_VALIDATOR, V_YES, API_UPDATABLE_ON),

        /**
         * User WebApp: Is userid hidden of Delegator User?
         */
        WEBAPP_USER_PROXY_PRINT_DELEGATOR_USER_HIDE_ID(//
                "webapp.user.proxy-print.delegator-user.hide-id",
                BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_ON),

        /**
         * User WebApp: Is group name (id) hidden of Delegator Group?
         */
        WEBAPP_USER_PROXY_PRINT_DELEGATOR_GROUP_HIDE_ID(//
                "webapp.user.proxy-print.delegator-group.hide-id",
                BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_ON),

        /**
         * User WebApp: enable the "Print documents separately" option for proxy
         * printing (Boolean). If {@code true} the option is enabled (shown).
         */
        WEBAPP_USER_PROXY_PRINT_SEPARATE_ENABLE(//
                "webapp.user.proxy-print.separate.enable", BOOLEAN_VALIDATOR,
                V_NO, API_UPDATABLE_ON),

        /**
         * User WebApp: the (default) "Print documents separately" option value
         * for proxy printing when "All Documents" are selected (Boolean). If
         * {@code true}, a separate proxy print job is created for each vanilla
         * inbox document. If {@code false}, one (1) proxy print job is printed
         * for a vanilla inbox.
         */
        WEBAPP_USER_PROXY_PRINT_SEPARATE(//
                "webapp.user.proxy-print.separate", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * URL with User WebApp help information.
         */
        WEBAPP_USER_HELP_URL(//
                "webapp.user.help.url", URL_VALIDATOR_OPT, API_UPDATABLE_ON),

        /**
         * User WebApp: show help URL in web app.
         */
        WEBAPP_USER_HELP_URL_ENABLE(//
                "webapp.user.help.url.enable", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * User WebApp: must text of navigation buttons on main window be shown?
         */
        WEBAPP_USER_MAIN_NAV_BUTTON_TEXT(//
                "webapp.user.main.nav-button-text",
                new EnumValidator<>(OnOffEnum.class), OnOffEnum.AUTO.toString(),
                API_UPDATABLE_ON),

        /**
         * User WebApp: show environmental effect?
         */
        WEBAPP_USER_SHOW_ENV_INFO(//
                "webapp.user.show-env-info", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * WebApp: enable (show) driver download in About Dialog?
         */
        WEBAPP_ABOUT_DRIVER_DOWNLOAD_ENABLE(//
                "webapp.about.driver-download.enable", BOOLEAN_VALIDATOR,
                V_YES),

        /**
         * Time limit (milliseconds) to capture the keystrokes of the card
         * number from a Local Card Reader.
         */
        WEBAPP_CARD_LOCAL_KEYSTROKES_MAX_MSECS(//
                "webapp.card-local.keystrokes-max-msecs", NUMBER_VALIDATOR,
                "500"),

        /**
         * Time limit (milliseconds) to capture the keystrokes of the YubiKey
         * OTP.
         */
        WEBAPP_YUBIKEY_KEYSTROKES_MAX_MSECS(//
                "webapp.yubikey.keystrokes-max-msecs", NUMBER_VALIDATOR,
                "1500"),

        /**
         * Time limit (seconds) for a user to associate a new Card to his
         * account. After the time limit the dialog is automatically closed.
         */
        WEBAPP_CARD_ASSOC_DIALOG_MAX_SECS(//
                "webapp.card-assoc.dialog-max-secs", NUMBER_VALIDATOR, "30"),

        /**
         * The custom jQuery Mobile Theme CSS file for the Admin Web App as
         * present in the {@code server/custom/web/themes/} folder.
         */
        WEBAPP_THEME_ADMIN(//
                Key.WEBAPP_THEME_PFX + "admin", API_UPDATABLE_ON),

        /**
         * The custom jQuery Mobile Theme CSS file for the Copy Shop Web App as
         * present in the {@code server/custom/web/themes/} folder.
         */
        WEBAPP_THEME_PRINTSITE(//
                Key.WEBAPP_THEME_PFX + "printsite", API_UPDATABLE_ON),

        /**
         * The custom jQuery Mobile Theme CSS file for the Job Tickets Web App
         * as present in the {@code server/custom/web/themes/} folder.
         */
        WEBAPP_THEME_JOBTICKETS(//
                Key.WEBAPP_THEME_PFX + "jobtickets", API_UPDATABLE_ON),

        /**
         * The custom jQuery Mobile Theme CSS file for the POS Web App as
         * present in the {@code server/custom/web/themes/} folder.
         */
        WEBAPP_THEME_POS(Key.WEBAPP_THEME_PFX + "pos", API_UPDATABLE_ON),

        /**
         * The custom jQuery Mobile Theme CSS file for the User Web App as
         * present in the {@code server/custom/web/themes/} folder.
         */
        WEBAPP_THEME_USER(Key.WEBAPP_THEME_PFX + "user", API_UPDATABLE_ON),

        /**
         * The custom CSS file for the Admin Web App as present in the
         * {@code server/custom/web/} folder.
         */
        WEBAPP_CUSTOM_ADMIN(Key.WEBAPP_CUSTOM_PFX + "admin", API_UPDATABLE_ON),

        /**
         * The custom CSS file for the Cop Shop Web App as present in the
         * {@code server/custom/web/} folder.
         */
        WEBAPP_CUSTOM_PRINTSITE(//
                Key.WEBAPP_CUSTOM_PFX + "printsite", API_UPDATABLE_ON),

        /**
         * The custom CSS file for the Job Tickets Web App as present in the
         * {@code server/custom/web/} folder.
         */
        WEBAPP_CUSTOM_JOBTICKETS(//
                Key.WEBAPP_CUSTOM_PFX + "jobtickets", API_UPDATABLE_ON),

        /**
         * The custom CSS file for the POS Web App as present in the
         * {@code server/custom/web/} folder.
         */
        WEBAPP_CUSTOM_POS(Key.WEBAPP_CUSTOM_PFX + "pos", API_UPDATABLE_ON),

        /**
         * The custom CSS file for the User Web App as present in the
         * {@code server/custom/web/} folder.
         */
        WEBAPP_CUSTOM_USER(Key.WEBAPP_CUSTOM_PFX + "user", API_UPDATABLE_ON),

        /**
         *
         */
        WEBAPP_HTML_ADMIN_ABOUT(//
                Key.WEBAPP_HTML_PFX + "admin.about", API_UPDATABLE_ON),
        /** */
        WEBAPP_HTML_PRINTSITE_ABOUT(//
                Key.WEBAPP_HTML_PFX + "printsite.about", API_UPDATABLE_ON),
        /** */
        WEBAPP_HTML_JOBTICKETS_ABOUT(//
                Key.WEBAPP_HTML_PFX + "jobtickets.about", API_UPDATABLE_ON),
        /** */
        WEBAPP_HTML_POS_ABOUT(//
                Key.WEBAPP_HTML_PFX + "pos.about", API_UPDATABLE_ON),
        /** */
        WEBAPP_HTML_USER_ABOUT(//
                Key.WEBAPP_HTML_PFX + "user.about", API_UPDATABLE_ON),

        /**
         *
         */
        WEBAPP_HTML_ADMIN_LOGIN(//
                Key.WEBAPP_HTML_PFX + "admin.login", API_UPDATABLE_ON),
        /** */
        WEBAPP_HTML_PRINTSITE_LOGIN(//
                Key.WEBAPP_HTML_PFX + "printsite.login", API_UPDATABLE_ON),
        /** */
        WEBAPP_HTML_JOBTICKETS_LOGIN(//
                Key.WEBAPP_HTML_PFX + "jobtickets.login", API_UPDATABLE_ON),
        /** */
        WEBAPP_HTML_POS_LOGIN(//
                Key.WEBAPP_HTML_PFX + "pos.login", API_UPDATABLE_ON),
        /** */
        WEBAPP_HTML_USER_LOGIN(//
                Key.WEBAPP_HTML_PFX + "user.login", API_UPDATABLE_ON),

        /**
         * .
         */
        WEBAPP_INTERNET_ENABLE(//
                Key.WEBAPP_INTERNET_PFX + "enable", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /** */
        WEBAPP_INTERNET_ADMIN_ENABLE(//
                Key.WEBAPP_INTERNET_PFX + "admin.enable", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),
        /** */
        WEBAPP_INTERNET_ADMIN_AUTH_MODE_ENABLE(//
                Key.WEBAPP_INTERNET_PFX + "admin.auth-mode.enable",
                BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_ON),
        /** */
        WEBAPP_INTERNET_ADMIN_AUTH_MODES(//
                Key.WEBAPP_INTERNET_PFX + "admin.auth-modes",
                AUTHMODE_SET_VALIDATOR, AUTH_MODE_V_NAME, API_UPDATABLE_OFF),

        /**
         * .
         */
        WEBAPP_INTERNET_JOBTICKETS_ENABLE(//
                Key.WEBAPP_INTERNET_PFX + "jobtickets.enable",
                BOOLEAN_VALIDATOR, V_YES, API_UPDATABLE_ON),
        /** */
        WEBAPP_INTERNET_JOBTICKETS_AUTH_MODE_ENABLE(//
                Key.WEBAPP_INTERNET_PFX + "jobtickets.auth-mode.enable",
                BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_ON),
        /** */
        WEBAPP_INTERNET_JOBTICKETS_AUTH_MODES(//
                Key.WEBAPP_INTERNET_PFX + "jobtickets.auth-modes",
                AUTHMODE_SET_VALIDATOR, AUTH_MODE_V_NAME, API_UPDATABLE_OFF),

        /**
         * .
         */
        WEBAPP_INTERNET_PRINTSITE_ENABLE(//
                Key.WEBAPP_INTERNET_PFX + "printsite.enable", BOOLEAN_VALIDATOR,
                V_NO, API_UPDATABLE_ON),
        /** */
        WEBAPP_INTERNET_PRINTSITE_AUTH_MODE_ENABLE(//
                Key.WEBAPP_INTERNET_PFX + "printsite.auth-mode.enable",
                BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_ON),
        /** */
        WEBAPP_INTERNET_PRINTSITE_AUTH_MODES(//
                Key.WEBAPP_INTERNET_PFX + "printsite.auth-modes",
                AUTHMODE_SET_VALIDATOR, AUTH_MODE_V_NAME, API_UPDATABLE_OFF),

        /**
         * .
         */
        WEBAPP_INTERNET_POS_ENABLE(//
                Key.WEBAPP_INTERNET_PFX + "pos.enable", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),
        /** */
        WEBAPP_INTERNET_POS_AUTH_MODE_ENABLE(//
                Key.WEBAPP_INTERNET_PFX + "pos.auth-mode.enable",
                BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_ON),
        /** */
        WEBAPP_INTERNET_POS_AUTH_MODES(//
                Key.WEBAPP_INTERNET_PFX + "pos.auth-modes",
                AUTHMODE_SET_VALIDATOR, AUTH_MODE_V_NAME, API_UPDATABLE_OFF),

        /**
         * .
         */
        WEBAPP_INTERNET_USER_ENABLE(//
                Key.WEBAPP_INTERNET_PFX + "user.enable", BOOLEAN_VALIDATOR,
                V_YES, API_UPDATABLE_ON),
        /** */
        WEBAPP_INTERNET_USER_AUTH_MODE_ENABLE(//
                Key.WEBAPP_INTERNET_PFX + "user.auth-mode.enable",
                BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_ON),
        /** */
        WEBAPP_INTERNET_USER_AUTH_MODES(//
                Key.WEBAPP_INTERNET_PFX + "user.auth-modes",
                AUTHMODE_SET_VALIDATOR, AUTH_MODE_V_NAME, API_UPDATABLE_OFF),

        /**
         * A comma/space separated list with {@link Locale#getLanguage()} codes
         * that are available for users to choose for their Web App locale. When
         * blank, all languages choices are offered to the user.
         */
        WEBAPP_LANGUAGE_AVAILABLE(//
                "webapp.language.available", API_UPDATABLE_ON),

        /**
         * .
         */
        WEB_LOGIN_AUTHTOKEN_ENABLE(//
                "web-login.authtoken.enable", BOOLEAN_VALIDATOR, V_YES),

        /**
         * Is web login via Trusted Third Party (TTP) enabled?
         */
        WEB_LOGIN_TTP_ENABLE(//
                "web-login.ttp.enable", BOOLEAN_VALIDATOR, V_YES),

        /**
         * Trusted Third Party API Key for Web Login.
         */
        WEB_LOGIN_TTP_API_KEY(//
                "web-login.ttp.apikey", API_UPDATABLE_OFF),
        //
        /**
         * Number of msecs after after which an {@link OneTimeAuthToken}
         * expires.
         */
        WEB_LOGIN_TTP_TOKEN_EXPIRY_MSECS(//
                "web-login.ttp.token.expiry-msecs", NUMBER_VALIDATOR, "5000"),

        /**
         * Inactivity timeout (minutes) for the admin web interface.
         */
        WEB_LOGIN_ADMIN_SESSION_TIMEOUT_MINS(//
                "web-login.admin.session-timeout-mins", NUMBER_VALIDATOR,
                "1440"),

        /**
         * Inactivity timeout (minutes) for the user web interface.
         */
        WEB_LOGIN_USER_SESSION_TIMEOUT_MINS(//
                "web-login.user.session-timeout-mins", NUMBER_VALIDATOR, "60"),

        /**
         * Enable Web Print.
         */
        WEB_PRINT_ENABLE(//
                "web-print.enable", BOOLEAN_VALIDATOR, V_NO, API_UPDATABLE_OFF),

        /**
         * Enable drag & drop zone for Web Print.
         */
        WEB_PRINT_DROPZONE_ENABLE(//
                "web-print.dropzone-enable", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        WEB_PRINT_MAX_FILE_MB(//
                "web-print.max-file-mb", NUMBER_VALIDATOR,
                WEBPRINT_MAX_FILE_MB_V_DEFAULT.toString()),

        /**
         * Enable graphics files for Web Print.
         */
        WEB_PRINT_GRAPHICS_ENABLE(//
                "web-print.graphics.enable", BOOLEAN_VALIDATOR, V_YES,
                API_UPDATABLE_ON),

        /**
         * A comma/space separated list of file extensions (without leading
         * point), that are excluded for Web Print. For example:
         * "rtf,html,ps,txt".
         */
        WEB_PRINT_FILE_EXT_EXCLUDE(//
                "web-print.file-ext.exclude", API_UPDATABLE_ON),

        /**
         *
         */
        WEB_PRINT_LIMIT_IP_ADDRESSES(//
                "web-print.limit-ip-addresses", CIDR_RANGES_VALIDATOR_OPT,
                API_UPDATABLE_ON),

        /**
         * Use X-Forwarded-For (XFF) HTTP header to retrieve Client IP address?
         */
        WEBSERVER_HTTP_HEADER_XFF_ENABLE(//
                "webserver.http.header.xff.enable", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         *
         */
        WEBSERVER_HTTP_HEADER_XFF_DEBUG(//
                "webserver.http.header.xff.debug", BOOLEAN_VALIDATOR, V_NO,
                API_UPDATABLE_ON),

        /**
         * If empty all XFF proxies are allowed.
         */
        WEBSERVER_HTTP_HEADER_XFF_PROXIES_ALLOWED(//
                "webserver.http.header.xff.proxies.allowed",
                CIDR_RANGES_VALIDATOR_OPT, API_UPDATABLE_ON);

        /**
         * Prefix for Web App theme keys.
         */
        public final static String WEBAPP_THEME_PFX = "webapp.theme.";

        /**
         * Prefix for Web App custom keys.
         */
        public final static String WEBAPP_CUSTOM_PFX = "webapp.custom.";

        /**
         * Prefix for Web App HTML keys.
         */
        public final static String WEBAPP_HTML_PFX = "webapp.html.";

        /**
         * Prefix for Web App keys when accessed from Internet.
         */
        public final static String WEBAPP_INTERNET_PFX = "webapp.internet.";

        /**
         *
         */
        private final Prop property;

        /**
         *
         * @param name
         *            The property name.
         * @param isApiUpdatable
         *            {@code true} if value can be updated with
         *            {@link JsonRpcMethodName#SET_CONFIG_PROPERTY}.
         */
        Key(final String name, final boolean isApiUpdatable) {
            this.property = this.createProperty(KeyType.SINGLE_LINE, name, null,
                    "", null, isApiUpdatable);
        }

        /**
         *
         * @param name
         *            The property name.
         * @param keyType
         *            Type of key.
         */
        Key(final String name, final KeyType keyType) {
            this.property = this.createProperty(keyType, name, null, "", null);
        }

        /**
         *
         * @param name
         *            The property name.
         * @param defaultValue
         *            The initial value.
         * @param isApiUpdatable
         *            {@code true} if value can be updated with
         *            {@link JsonRpcMethodName#SET_CONFIG_PROPERTY}.
         */
        Key(final String name, final String defaultValue,
                final boolean isApiUpdatable) {
            this.property = this.createProperty(KeyType.SINGLE_LINE, name, null,
                    defaultValue, null, isApiUpdatable);
        }

        /**
         *
         * @param name
         *            The property name.
         * @param validator
         *            The validator.
         */
        Key(final String name, final ConfigPropValidator validator) {
            this.property = this.createProperty(KeyType.SINGLE_LINE, name,
                    validator, "", null);
        }

        /**
         *
         * @param name
         *            The property name.
         * @param validator
         *            The validator.
         * @param isApiUpdatable
         *            {@code true} if value can be updated with
         *            {@link JsonRpcMethodName#SET_CONFIG_PROPERTY}.
         */
        Key(final String name, final ConfigPropValidator validator,
                final boolean isApiUpdatable) {
            this.property = this.createProperty(KeyType.SINGLE_LINE, name,
                    validator, "", null, isApiUpdatable);
        }

        /**
         *
         * @param name
         *            The property name.
         * @param keyType
         *            Type of key.
         * @param defaultValue
         *            The initial value.
         */
        Key(final String name, final KeyType keyType,
                final String defaultValue) {
            this.property = this.createProperty(keyType, name, null,
                    defaultValue, null);
        }

        /**
         *
         * @param name
         *            The property name.
         * @param keyType
         *            Type of key.
         * @param validator
         *            The validator.
         * @param defaultValue
         *            The initial value.
         */
        Key(final String name, final KeyType keyType,
                final ConfigPropValidator validator,
                final String defaultValue) {
            this.property = this.createProperty(keyType, name, validator,
                    defaultValue, null);
        }

        /**
         *
         * @param name
         *            The property name.
         * @param validator
         *            The validator.
         * @param defaultValue
         *            The initial value.
         */
        Key(final String name, final ConfigPropValidator validator,
                final String defaultValue) {
            this.property = this.createProperty(KeyType.SINGLE_LINE, name,
                    validator, defaultValue, null);
        }

        /**
         *
         * @param name
         *            The property name.
         * @param validator
         *            The validator.
         * @param defaultValue
         *            The initial value.
         * @param isApiUpdatable
         *            {@code true} if value can be updated with
         *            {@link JsonRpcMethodName#SET_CONFIG_PROPERTY}.
         */
        Key(final String name, final ConfigPropValidator validator,
                final String defaultValue, final boolean isApiUpdatable) {
            this.property = this.createProperty(KeyType.SINGLE_LINE, name,
                    validator, defaultValue, null, isApiUpdatable);
        }

        /**
         *
         * @param name
         *            The property name.
         * @param validator
         *            The validator.
         * @param defaultValue
         *            The initial value.
         * @param values
         *            List of possible values.
         * @param isApiUpdatable
         *            {@code true} if value can be updated with
         *            {@link JsonRpcMethodName#SET_CONFIG_PROPERTY}.
         */
        Key(final String name, final ConfigPropValidator validator,
                final String defaultValue, final String[] values,
                final boolean isApiUpdatable) {
            this.property = this.createProperty(KeyType.SINGLE_LINE, name,
                    validator, defaultValue, values, isApiUpdatable);
        }

        /**
         * Creates property.
         *
         * @param keyType
         *            Type of key.
         * @param name
         *            The property name.
         * @param validator
         *            The validator.
         * @param defaultValue
         *            The initial value.
         * @param values
         *            List of possible values.
         * @return The property.
         */
        private Prop createProperty(final KeyType keyType, final String name,
                final ConfigPropValidator validator, final String defaultValue,
                final String[] values) {

            switch (keyType) {

            case BIG_DECIMAL:
                return new BigDecimalProp(this, name, defaultValue);

            case LOCALIZED_MULTI_LINE:
                return new LocalizedProp(this, name, true);

            case LOCALIZED_SINGLE_LINE:
                return new LocalizedProp(this, name, false);

            case MULTI_LINE:
                return new MultiLineProp(this, name);

            case SINGLE_LINE:
                return new Prop(this, name, validator, defaultValue, values);

            default:
                throw new SpException("Oops [" + keyType + "] not handled");
            }
        }

        /**
         *
         * @param keyType
         *            Type of key.
         * @param name
         *            The property name.
         * @param validator
         *            The validator.
         * @param defaultValue
         *            The initial value.
         * @param values
         *            List of possible values.
         * @param isApiUpdatable
         *            {@code true} if value can be updated with
         *            {@link JsonRpcMethodName#SET_CONFIG_PROPERTY}.
         * @return The property.
         */
        private Prop createProperty(final KeyType keyType, final String name,
                final ConfigPropValidator validator, final String defaultValue,
                final String[] values, final boolean isApiUpdatable) {
            final Prop prop = this.createProperty(keyType, name, validator,
                    defaultValue, values);
            prop.setApiUpdatable(isApiUpdatable);
            return prop;
        }

        /**
         *
         * @return The property.
         */
        public Prop getProperty() {
            return property;
        }

    };

    /** */
    UserAuthModeSetValidator AUTHMODE_SET_VALIDATOR =
            new UserAuthModeSetValidator(false);

    /** */
    BooleanValidator BOOLEAN_VALIDATOR = new BooleanValidator(false);

    /** */
    BooleanValidator BOOLEAN_VALIDATOR_OPT = new BooleanValidator(true);

    /** */
    CidrRangesValidator CIDR_RANGES_VALIDATOR_OPT =
            new CidrRangesValidator(true);

    /**
     * URI is not required (may be empty).
     */
    EmailAddressValidator EMAIL_VALIDATOR_OPT = new EmailAddressValidator(true);

    /** */
    IpPortValidator IP_PORT_VALIDATOR = new IpPortValidator();

    /** */
    NumberValidator NUMBER_VALIDATOR = new NumberValidator(false);

    /** */
    NumberValidator NUMBER_VALIDATOR_OPT = new NumberValidator(true);

    /** */
    LocaleValidator LOCALE_VALIDATOR = new LocaleValidator();

    /** */
    CurrencyCodeValidator CURRENCY_VALIDATOR = new CurrencyCodeValidator(false);

    /** */
    NotEmptyValidator NOT_EMPTY_VALIDATOR = new NotEmptyValidator();

    /** */
    UrlValidator URL_VALIDATOR = new UrlValidator(false);

    /**
     * URL is not required (may be empty).
     */
    UrlValidator URL_VALIDATOR_OPT = new UrlValidator(true);

    /**
     * URI is not required (may be empty).
     */
    UriValidator URI_VALIDATOR_OPT = new UriValidator(true);

    /** */
    UuidValidator UUID_VALIDATOR = new UuidValidator(false);

    /** */
    NumberValidator ACCOUNTING_DECIMAL_VALIDATOR = new NumberValidator(0L,
            Integer.valueOf(MAX_FINANCIAL_DECIMALS_IN_DB).longValue(), false);

    /** */
    InternalFontFamilyValidator INTERNAL_FONT_FAMILY_VALIDATOR =
            new InternalFontFamilyValidator();

    /** */
    CronExpressionValidator CRON_EXPR_VALIDATOR = new CronExpressionValidator();
    /** */
    CronExpressionDaysOfWeekValidator CRON_EXPR_DAY_OF_WEEK_VALIDATOR =
            new CronExpressionDaysOfWeekValidator();

    /**
     * .
     */
    static class MultiLineProp extends Prop {

        MultiLineProp(final Key key, final String name) {
            super(key, name, null, "");
        }

        @Override
        public boolean isMultiLine() {
            return true;
        }
    }

    /**
     * .
     */
    static class Prop {

        final private Key key;
        final private String name;

        private String defaultValue = null;
        private String value = null;

        private String[] values = null;

        private ConfigPropValidator validator = null;
        private ValidationResult validationResult = null;

        /**
         * {@code true} if value can be updated by public API.
         */
        private boolean apiUpdatable;

        private Prop(final Key key, final String name) {
            this.key = key;
            this.name = name;
        }

        Prop(final Key key, final String name,
                final ConfigPropValidator validator) {
            this(key, name);
            this.validator = validator;
        }

        Prop(final Key key, final String name,
                final ConfigPropValidator validator,
                final String defaultValue) {
            this(key, name, validator);
            this.defaultValue = defaultValue;
        }

        Prop(final Key key, final String name,
                final ConfigPropValidator validator, final String defaultValue,
                String[] values) {
            this(key, name, validator, defaultValue);
            this.values = values;
        }

        /**
         *
         * @return {@code true} if value can be updated by public API.
         */
        public boolean isApiUpdatable() {
            return apiUpdatable;
        }

        /**
         *
         * @param updatable
         *            {@code true} if value can be updated by public API.
         */
        public void setApiUpdatable(final boolean updatable) {
            this.apiUpdatable = updatable;
        }

        /**
         *
         * @return
         */
        public boolean isMultiLine() {
            return false;
        }

        /**
         *
         * @return
         */
        public boolean isBigDecimal() {
            return false;
        }

        /**
         * Validates a candidate value using the validator of a property.
         *
         * @param value
         *            The candidate value.
         * @return The {@link ValidationResult}.
         */
        public static ValidationResult validate(final Prop prop,
                final String value) {
            ValidationResult validationResult = null;
            final ConfigPropValidator validator = prop.getValidator();
            if (validator == null) {
                validationResult = new ValidationResult(value);
            } else {
                validationResult = validator.validate(value);
            }
            return validationResult;
        }

        /**
         * Validates this property using the validator and private value.
         *
         * @return The {@link ValidationResult}.
         */
        public ValidationResult validate() {
            if (validator == null) {
                validationResult = new ValidationResult(value);
                if (value == null && defaultValue == null) {
                    validationResult
                            .setStatus(ValidationStatusEnum.ERROR_EMPTY);
                    validationResult.setMessage("value is required");
                }
            } else {
                validationResult = validator.validate(valueAsString());
            }
            return validationResult;
        }

        public String valueAsString() {
            if (value == null) {
                return defaultValue;
            }
            return value;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        public String[] getValues() {
            return values;
        }

        public void setValues(String[] values) {
            this.values = values;
        }

        public Key getKey() {
            return key;
        }

        public String getName() {
            return name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public ValidationResult getValidationResult() {
            return validationResult;
        }

        public ConfigPropValidator getValidator() {
            return validator;
        }

        public void setValidator(ConfigPropValidator validator) {
            this.validator = validator;
        }

    };

    /**
     *
     */
    static class BigDecimalProp extends Prop {

        final static DecimalValidator VALIDATOR = new DecimalValidator();

        BigDecimalProp(Key key, String name, String defaultValue) {
            super(key, name, VALIDATOR, defaultValue);
        }

        @Override
        public boolean isBigDecimal() {
            return true;
        }

    };

    /**
     * Uses the property name as entry key in messages.xml to get its value.
     */
    static class LocalizedProp extends Prop {

        private boolean multiLine = false;

        /**
         *
         * @param key
         *            The property {@link Key}.
         * @param name
         *            The unique text key as used in the database and in
         *            {@code messages.xml}.
         * @param multiLine
         *            If {@code true} this represents a multi-line text value.
         */
        public LocalizedProp(Key key, String name, boolean multiLine) {
            super(key, name, null, Messages.getMessage(LocalizedProp.class,
                    name, (String[]) null));
            this.multiLine = multiLine;
        }

        @Override
        public boolean isMultiLine() {
            return multiLine;
        }

    };

    /**
     *
     * @author rijk
     *
     */
    enum LdapType {

        /**
         * OpenLDAP.
         */
        OPEN_LDAP,

        /**
         * Apple Open Directory.
         */
        OPEN_DIR,

        /**
         * Novell eDirectory.
         */
        EDIR,

        /**
         * Microsoft Active Directory.
         */
        ACTD,

        /**
         * Google Cloud Directory (G Suite).
         */
        G_SUITE
    };

    /**
     *
     * @author rijk
     *
     */
    static class LdapProp {

        private LdapType ldapType;
        private Key key;
        private String value;

        @SuppressWarnings("unused")
        private LdapProp() {
        }

        public LdapProp(final LdapType ldapType, final Key key,
                final String value) {
            this.ldapType = ldapType;
            this.key = key;
            this.value = value;
        }

        public LdapType getLdapType() {
            return ldapType;
        }

        public void setLdapType(LdapType ldapType) {
            this.ldapType = ldapType;
        }

        public Key getKey() {
            return key;
        }

        public void setKey(Key key) {
            this.key = key;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    };

    /**
     * Array with all default LDAP properties for the different LDAP types.
     */
    LdapProp[] getLdapProps();

    /**
     * Initializes the component for basic dictionary functions.
     * <p>
     * Note: Database access SHOULD NOT be used for this action.
     * </p>
     *
     * @param defaultProps
     *            The default properties.
     */
    void init(Properties defaultProps);

    /**
     * Initializes the component to be fully runnable.
     * <p>
     * Note: Database access CAN BE used for this action.
     * </p>
     */
    void initRunnable();

    /**
     * Checks whether the value for a {@link Prop} key is valid.
     *
     * @param key
     *            The property key.
     * @param value
     *            The value.
     * @return The {@link ValidationResult}.
     */
    ValidationResult validate(Key key, String value);

    /**
     * Gets the runnable status of the configuration, i.e. is application ready
     * to run.
     *
     * @return
     */
    boolean isRunnable();

    /**
     * Calculates the runnable status of the configuration.
     *
     * @return <code>true</code if runnable.
     */
    boolean calcRunnable();

    /**
     * Updates the string value of a configuration key.
     *
     * @param key
     *            The key.
     * @param value
     *            The string value.
     * @param actor
     *            The actor, one of {@link Entity#ACTOR_ADMIN},
     *            {@link Entity#ACTOR_INSTALL} or {@link Entity#ACTOR_SYSTEM}.
     */
    void updateValue(Key key, String value, String actor);

    /**
     * Saves the string value of a configuration key. The key is lazy created.
     *
     * @param key
     *            The key.
     * @param value
     *            The string value.
     */
    void saveValue(Key key, String value);

    /**
     * Does this property represent a multi-line value?
     *
     * @return
     */
    boolean isMultiLine(Key key);

    /**
     * Does this property represent a {@link BigDecimal} value?
     *
     * @return
     */
    boolean isBigDecimal(Key key);

    /**
     *
     * @param key
     *            The key.
     * @return {@code true} if value of key can be updated with Public API, like
     *         JSON-RPC.
     */
    boolean isApiUpdatable(Key key);

    /**
     * Gets the value of a configuration key as string.
     *
     * @param key
     *            The key.
     * @return The string value.
     */
    String getString(Key key);

    /**
     * Gets the value of an LDAP configuration key as string.
     *
     * @param ldapType
     *            The LDAP type.
     * @param key
     *            The key.
     * @return The string value or {@code null} when not found.
     */
    String getString(LdapType ldapType, Key key);

    /**
     * Gets the value of an LDAP configuration key as Boolean.
     *
     * @param ldapType
     *            The LDAP type.
     * @param key
     *            The key.
     * @return The boolean value or {@code null} when not found.
     */
    Boolean getBoolean(LdapType ldapType, Key key);

    /**
     * Gets the value of a configuration key as double.
     *
     * @param key
     *            The key.
     * @return The double value.
     */
    double getDouble(Key key);

    /**
     * Gets the value of a configuration key as {@link BigDecimal}.
     *
     * @param key
     *            The key.
     * @return The {@link BigDecimal} value.
     */
    BigDecimal getBigDecimal(Key key);

    /**
     * Gets the value of a configuration key as long.
     *
     * @param key
     *            The key.
     * @return The long value.
     */
    long getLong(Key key);

    /**
     * Gets the value of a configuration key as int.
     *
     * @param key
     *            The key.
     * @return The int value.
     */
    int getInt(Key key);

    /**
     * Gets the value of a configuration key as {@link Integer}.
     *
     * @param key
     *            The key.
     * @return The int value or {@code null} when not specified.
     */
    Integer getInteger(Key key);

    /**
     * Gets the value of a configuration key as boolean.
     *
     * @param key
     *            The key.
     * @return The boolean value.
     */
    boolean getBoolean(Key key);

    /**
     * Gets the value (comma separated list) of a configuration key as
     * {@link Set} of values.
     *
     * @param key
     *            The key.
     * @return The {@link Set} of values.
     */
    Set<String> getSet(Key key);

    /**
     * Gets the string representation of the configuration key.
     *
     * @param key
     *            The enum representation of the key.
     * @return The string representation of the key.
     */
    String getKey(Key key);

    /**
     * Gets the enum of the configuration key.
     *
     * @param key
     *            The string representation of the key.
     * @return The enum representation of the key, or {@code null} when the key
     *         is not found.
     */
    Key getKey(String key);

    /**
     *
     * @param key
     *            The key.
     * @return {@code null} when not found.
     */
    Prop getProp(Key key);

    /**
     *
     * @param name
     *            The name.
     * @return {@code null} when not found.
     */
    Prop getProp(String name);

}
