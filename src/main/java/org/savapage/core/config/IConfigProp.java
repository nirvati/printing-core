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
package org.savapage.core.config;

import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.dao.helpers.DeviceTypeEnum;
import org.savapage.core.dao.helpers.ReservedIppQueueEnum;
import org.savapage.core.dao.impl.DaoBatchCommitterImpl;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.services.helpers.UserAuth;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.Messages;

/**
 *
 * @author Datraverse B.V.
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
    String DEFAULT_COMMUNITY_HELPDESK_URL =
            "https://secure.datraverse.nl/helpdesk/";

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
     *
     */
    InternalFontFamilyEnum DEFAULT_INTERNAL_FONT_FAMILY =
            InternalFontFamilyEnum.DEFAULT;

    /**
     * @author Datraverse B.V.
     */
    enum KeyType {
        /**
         *
         */
        BIG_DECIMAL,
        /**
         *
         */
        LOCALIZED_MULTI_LINE,
        /**
         *
         */
        LOCALIZED_SINGLE_LINE,
        /**
         *
         */
        MULTI_LINE,
        /**
         *
         */
        SINGLE_LINE
    };

    /**
     *
     * @author Datraverse B.V.
     *
     */
    enum Key {

        /**
         * See {@link DaoBatchCommitterImpl}.
         */
        DB_BATCH_COMMIT_CHUNK_SIZE("db.batch.commit-chunk-size",
                NUMBER_VALIDATOR, DEFAULT_BATCH_COMMIT_CHUNK_SIZE),

        /**
         *
         */
        FINANCIAL_GLOBAL_CREDIT_LIMIT("financial.global.credit-limit",
                KeyType.BIG_DECIMAL, "0.00"),

        /**
         * A comma separated list of Point-of-Sale payment methods.
         */
        FINANCIAL_POS_PAYMENT_METHODS("financial.pos.payment-methods"),

        /**
         *
         */
        FINANCIAL_POS_RECEIPT_HEADER("financial.pos.receipt-header"),

        /**
         *
         */
        FINANCIAL_PRINTER_COST_DECIMALS("financial.printer.cost-decimals",
                ACCOUNTING_DECIMAL_VALIDATOR,
                DEFAULT_FINANCIAL_PRINTER_COST_DECIMALS),

        /**
         *
         */
        FINANCIAL_USER_BALANCE_DECIMALS("financial.user.balance-decimals",
                ACCOUNTING_DECIMAL_VALIDATOR,
                DEFAULT_FINANCIAL_USER_BALANCE_DECIMALS),

        /**
         *
         */
        FINANCIAL_VOUCHER_CARD_HEADER("financial.voucher.card-header",
                KeyType.LOCALIZED_SINGLE_LINE),

        /**
         *
         */
        FINANCIAL_VOUCHER_CARD_FOOTER("financial.voucher.card-footer"),

        /**
         *
         */
        FINANCIAL_VOUCHER_CARD_FONT_FAMILY(
                "financial.voucher.card-font-family",
                INTERNAL_FONT_FAMILY_VALIDATOR, DEFAULT_INTERNAL_FONT_FAMILY
                        .toString()),

        /**
         * E.g.: €.
         */
        USER_FIN_CURRENCY_SYMBOL_CUSTOM("user.fin.currency-symbol.custom"),

        /**
         *
         */
        USER_FIN_CURRENTCY_SYMBOL_SHOWS("user.fin.currency-symbol.show",
                BOOLEAN_VALIDATOR, V_YES),

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
        AUTH_MODE_ID_PIN_REQUIRED("auth-mode.id.pin-required",
                BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        AUTH_MODE_ID_IS_MASKED("auth-mode.id.is-masked", BOOLEAN_VALIDATOR,
                V_NO),

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
        AUTH_MODE_CARD_PIN_REQUIRED("auth-mode.card.pin-required",
                BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        AUTH_MODE_CARD_SELF_ASSOCIATION("auth-mode.card.self-association",
                BOOLEAN_VALIDATOR, V_NO),

        /**
         * Number of msecs after which an IP Card Number Detect event expires.
         */
        AUTH_MODE_CARD_IP_EXPIRY_MSECS("auth-mode.card-ip.expiry-msecs",
                NUMBER_VALIDATOR, "2000"),

        /**
         *
         */
        AUTH_MODE_CARD_LOCAL_SHOW("auth-mode.card-local.show",
                BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        AUTH_MODE_DEFAULT("auth-mode-default", null, AUTH_MODE_V_NAME,
                new String[] { AUTH_MODE_V_NAME, AUTH_MODE_V_ID,
                        AUTH_MODE_V_CARD_LOCAL }),

        /**
         * Authentication method.
         */
        AUTH_METHOD("auth.method", null, AUTH_METHOD_V_NONE, new String[] {
                AUTH_METHOD_V_NONE, AUTH_METHOD_V_UNIX, AUTH_METHOD_V_LDAP }),

        /**
         *
         */
        AUTH_LDAP_ADMIN_DN("auth.ldap.admin-dn"),

        /**
         *
         */
        AUTH_LDAP_ADMIN_PASSWORD("auth.ldap.admin-password"),

        /**
         *
         */
        AUTH_LDAP_BASE_DN("auth.ldap.basedn"),

        /**
         * LDAP Host name or IP address.
         */
        AUTH_LDAP_HOST("auth.ldap.host", "localhost"),

        /**
         * LDAP host IP port number.
         */
        AUTH_LDAP_PORT("auth.ldap.port", IP_PORT_VALIDATOR, "389"),

        /**
         * Use SSL for the LDAP connection.
         */
        AUTH_LDAP_USE_SSL("auth.ldap.use-ssl", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        CARD_NUMBER_FORMAT("card.number.format", null,
                CARD_NUMBER_FORMAT_V_HEX, new String[] {
                        CARD_NUMBER_FORMAT_V_DEC, CARD_NUMBER_FORMAT_V_HEX }),

        /**
         *
         */
        CARD_NUMBER_FIRST_BYTE("card.number.first-byte", null,
                CARD_NUMBER_FIRSTBYTE_V_LSB, new String[] {
                        CARD_NUMBER_FIRSTBYTE_V_LSB,
                        CARD_NUMBER_FIRSTBYTE_V_MSB }),

        /**
         * IMPORTANT: the value of this key should be GT one (1) hour, since the
         * renewal is Quartz scheduled with Key.ScheduleHourly.
         */
        CUPS_IPP_SUBSCR_NOTIFY_LEASE_DURATION(
                "cups.ipp.subscription.notify-lease-duration",
                NUMBER_VALIDATOR, "4200"),

        /**
         * Max number of IPP connections per CUPS server.
         */
        CUPS_IPP_MAX_CONNECTIONS("cups.ipp.max-connections", NUMBER_VALIDATOR,
                "10"),

        /**
         * Timeout in milliseconds until a IPP connection with local CUPS server
         * is established.
         */
        CUPS_IPP_LOCAL_CONNECT_TIMEOUT_MILLIS(
                "cups.ipp.local-connect-timeout-millis", NUMBER_VALIDATOR,
                "2000"),

        /**
         * Timeout in milliseconds to receive IPP data from local CUPS server
         * after the connection is established, i.e. maximum time of inactivity
         * between two data packets.
         */
        CUPS_IPP_LOCAL_SOCKET_TIMEOUT_MILLIS(
                "cups.ipp.local-socket-timeout-millis", NUMBER_VALIDATOR,
                "2000"),

        /**
         * Is access of remote CUPS enabled?
         */
        CUPS_IPP_REMOTE_ENABLED("cups.ipp.remote-enabled", BOOLEAN_VALIDATOR,
                V_NO),

        /**
         * Timeout in milliseconds until a IPP connection with remote CUPS
         * server is established.
         */
        CUPS_IPP_REMOTE_CONNECT_TIMEOUT_MILLIS(
                "cups.ipp.remote-connect-timeout-millis", NUMBER_VALIDATOR,
                "3000"),

        /**
         * Timeout in milliseconds to receive IPP data from remote CUPS server
         * after the connection is established, i.e. maximum time of inactivity
         * between two data packets.
         */
        CUPS_IPP_REMOTE_SOCKET_TIMEOUT_MILLIS(
                "cups.ipp.remote-socket-timeout-millis", NUMBER_VALIDATOR,
                "3000"),

        /**
         *
         */
        DELETE_ACCOUNT_TRX_LOG("delete.account-trx-log", BOOLEAN_VALIDATOR,
                V_YES),

        /**
         * A value of {@code -1} is interpreted as {@code null}.
         */
        DELETE_ACCOUNT_TRX_DAYS("delete.account-trx-log.days",
                NUMBER_VALIDATOR, "365"),

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
        DEVICE_CARD_READER_DEFAULT_PORT("device.card-reader.default-port",
                NUMBER_VALIDATOR, "7772"),

        /**
         *
         */
        DOC_CONVERT_XPS_TO_PDF_ENABLED("doc.convert.xpstopdf-enabled",
                BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        DOC_CONVERT_LIBRE_OFFICE_ENABLED("doc.convert.libreoffice-enabled",
                BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        ENV_CO2_GRAMS_PER_SHEET("environment.co2-grams-per-sheet", "5.1"),

        /**
         *
         */
        ENV_SHEETS_PER_TREE("environment.sheets-per-tree", NUMBER_VALIDATOR,
                "8333"),

        /**
         *
         */
        ENV_WATT_HOURS_PER_SHEET("environment.watt-hours-per-sheet", "12.5"),

        /**
         * The base URL, i.e. "protocol://authority" <i>without</i> the path, of
         * the Web API callback interface (no trailing slash).
         */
        EXT_WEBAPI_CALLBACK_URL_BASE("ext.webapi.callback.url-base",
                URL_VALIDATOR_OPT, ""),

        /**
         * The URL of the User Web App used by the Web API to redirect to after
         * remote Web App dialog is done.
         */
        EXT_WEBAPI_REDIRECT_URL_WEBAPP_USER(
                "ext.webapi.redirect.url-webapp-user", URL_VALIDATOR_OPT, ""),

        /**
         * Google Cloud Print enabled (boolean).
         */
        GCP_ENABLE("gcp.enable", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Max seconds to wait for a GCP connect.
         */
        GC_CONNECT_TIMEOUT_SECS("gcp.connect-timeout-secs", NUMBER_VALIDATOR,
                "5"),

        /**
         * Max seconds to wait for a GCP event.
         */
        GCP_EVENT_TIMEOUT_SECS("gcp.event-timeout-secs", NUMBER_VALIDATOR, "5"),

        /**
         *
         */
        GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_ENABLE(
                "gcp.job-owner-unknown.cancel-mail.enable", BOOLEAN_VALIDATOR,
                V_YES),

        /**
         *
         */
        GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_BODY(
                "gcp.job-owner-unknown.cancel-mail.body",
                KeyType.LOCALIZED_MULTI_LINE),

        /**
         *
         */
        GCP_JOB_OWNER_UNKNOWN_CANCEL_MAIL_SUBJECT(
                "gcp.job-owner-unknown.cancel-mail.subject",
                KeyType.LOCALIZED_SINGLE_LINE),

        /**
         * Do we have an Internet connection?
         */
        INFRA_INTERNET_CONNECTED("infra.internet-connected", BOOLEAN_VALIDATOR,
                V_YES),

        /**
         *
         */
        INTERNAL_USERS_ENABLE("internal-users.enable", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        INTERNAL_USERS_CAN_CHANGE_PW("internal-users.user-can-change-password",
                BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        INTERNAL_USERS_NAME_PREFIX("internal-users.username-prefix", "guest-"),

        /**
         *
         */
        INTERNAL_USERS_PW_LENGTH_MIN("internal-users.password-length-min",
                NUMBER_VALIDATOR, "6"),

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
        LDAP_SCHEMA_GROUP_MEMBER_FIELD("ldap.schema.group-member-field"),

        /**
         * The LDAP field that contains the group's name.
         */
        LDAP_SCHEMA_GROUP_NAME_FIELD("ldap.schema.group-name-field"),

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
        LDAP_SCHEMA_GROUP_SEARCH("ldap.schema.group-search"),

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
        LDAP_SCHEMA_USER_NAME_GROUP_SEARCH("ldap.schema.user-name-group-search"),

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
        LDAP_SCHEMA_NESTED_GROUP_SEARCH("ldap.schema.nested-group-search"),

        /**
         * The LDAP field that contains the Distinguished Name (DN).
         * <p>
         * Note: Active Directory only.
         * </p>
         */
        LDAP_SCHEMA_DN_FIELD("ldap.schema.dn-field"),

        /**
         * Boolean to allow or deny disabled users.
         * <p>
         * Note: Active Directory only.
         * </p>
         */
        LDAP_ALLOW_DISABLED_USERS("ldap.disabled-users.allow"),

        /**
         * Boolean to indicate if filtering out disabled users is done locally
         * (by checking the userAccountControl attribute), or remotely (by AND
         * in userAccountControl in the LDAP query).
         * <p>
         * Note: Active Directory only.
         * </p>
         */
        LDAP_FILTER_DISABLED_USERS_LOCALLY("ldap.disabled-users.local-filter"),

        /**
         * If {@code Y}, then the group member field contains the user's
         * username. If {@code N}, then the group member field contains the
         * user's DN.
         */
        LDAP_SCHEMA_POSIX_GROUPS("ldap.schema.posix-groups"),

        /**
         * LdapSchema* properties have "" default value.
         */
        LDAP_SCHEMA_TYPE("ldap.schema.type", null, LDAP_TYPE_V_OPEN_LDAP,
                new String[] { LDAP_TYPE_V_ACTIV, LDAP_TYPE_V_E_DIR,
                        LDAP_TYPE_V_APPLE, LDAP_TYPE_V_OPEN_LDAP }),

        /**
         * The LDAP field that contains the user's username.
         */
        LDAP_SCHEMA_USER_NAME_FIELD("ldap.schema.user-name-field"),

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
        LDAP_SCHEMA_USER_NAME_SEARCH("ldap.schema.user-name-search"),

        /**
         * The LDAP field that contains the user's full name.
         */
        LDAP_SCHEMA_USER_FULL_NAME_FIELD("ldap.schema.user-full-name-field"),

        /**
         * The LDAP field that contains the user's email address.
         */
        LDAP_SCHEMA_USER_EMAIL_FIELD("ldap.schema.user-email-field"),

        /**
         * The LDAP field that contains the user's department.
         */
        LDAP_SCHEMA_USER_DEPARTMENT_FIELD("ldap.schema.user-department-field"),

        /**
         * The LDAP field that contains the user's office location.
         */
        LDAP_SCHEMA_USER_OFFICE_FIELD("ldap.schema.user-office-field"),

        /**
         * The LDAP field that contains the user's Card Number.
         */
        LDAP_SCHEMA_USER_CARD_NUMBER_FIELD("ldap.schema.user-card-number-field"),

        /**
         *
         */
        LDAP_SCHEMA_USER_CARD_NUMBER_FIRST_BYTE(
                "ldap.user-card-number.first-byte", null,
                CARD_NUMBER_FIRSTBYTE_V_LSB, new String[] {
                        CARD_NUMBER_FIRSTBYTE_V_LSB,
                        CARD_NUMBER_FIRSTBYTE_V_MSB }),

        /**
         *
         */
        LDAP_SCHEMA_USER_CARD_NUMBER_FORMAT("ldap.user-card-number.format",
                null, CARD_NUMBER_FORMAT_V_HEX, new String[] {
                        CARD_NUMBER_FORMAT_V_DEC, CARD_NUMBER_FORMAT_V_HEX }),

        /**
         * The LDAP field that contains the user's ID Number.
         */
        LDAP_SCHEMA_USER_ID_NUMBER_FIELD("ldap.schema.user-id-number-field"),

        /**
         * Date on which this SavaPage instance was first installed. The
         * community role at this point is "Visitor", and the date defaults to
         * the start date of the visiting period.
         */
        COMMUNITY_VISITOR_START_DATE("community.visitor.start-date"),

        /**
         *
         */
        COMMUNITY_HELPDESK_URL("community.helpdesk.url", URL_VALIDATOR,
                DEFAULT_COMMUNITY_HELPDESK_URL),

        /**
         * PaperCut Database JDBC url.
         */
        PAPERCUT_DB_JDBC_URL("papercut.db.jdbc-url"),

        /**
         * PaperCut Database user.
         */
        PAPERCUT_DB_USER("papercut.db.user"),

        /**
         * PaperCut Database password.
         */
        PAPERCUT_DB_PASSWORD("papercut.db.password"),

        /**
         * PaperCut Server host.
         */
        PAPERCUT_SERVER_HOST("papercut.server.host", "localhost"),

        /**
         * PaperCut Server port.
         */
        PAPERCUT_SERVER_PORT("papercut.server.port", IP_PORT_VALIDATOR, "9191"),

        /**
         * PaperCut authentication token for Web Services.
         */
        PAPERCUT_SERVER_AUTH_TOKEN("papercut.webservices.auth-token"),

        /**
         * PaperCut XML-RPC path. E.g.{@code /rpc/api/xmlrpc}
         */
        PAPERCUT_XMLRPC_URL_PATH("papercut.xmlrpc.url-path", "/rpc/api/xmlrpc"),

        /**
         *
         */
        PRINT_IMAP_ENABLE("print.imap.enable", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        PRINT_IMAP_HOST("print.imap.host"),

        /**
         * The port to connect to on the IMAP server.
         */
        PRINT_IMAP_PORT("print.imap.port", IP_PORT_VALIDATOR, "143"),

        /**
         * Socket connection timeout value in milliseconds. Default is infinite
         * timeout.
         */
        PRINT_IMAP_CONNECTION_TIMEOUT_MSECS(
                "print.imap.connectiontimeout-msecs", NUMBER_VALIDATOR,
                IMAP_CONNECTION_TIMEOUT_V_DEFAULT.toString()),

        /**
         * Socket I/O timeout value in milliseconds. Default is infinite
         * timeout.
         */
        PRINT_IMAP_TIMEOUT_MSECS("print.imap.timeout-msecs", NUMBER_VALIDATOR,
                IMAP_TIMEOUT_V_DEFAULT.toString()),

        /**
         *
         */
        PRINT_IMAP_SECURITY("print.imap.security", IMAP_SECURITY_V_STARTTLS),

        /**
         * Username for IMAP authentication.
         */
        PRINT_IMAP_USER_NAME("print.imap.user"),

        /**
         * Password for IMAP authentication.
         */
        PRINT_IMAP_PASSWORD("print.imap.password"),

        /**
         * Produces extra IMAP related logging for troubleshooting.
         */
        PRINT_IMAP_DEBUG("print.imap.debug", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        PRINT_IMAP_INBOX_FOLDER("print.imap.folder.inbox", "Inbox"),

        /**
         *
         */
        PRINT_IMAP_TRASH_FOLDER("print.imap.folder.trash", "Trash"),

        /**
         *
         */
        PRINT_IMAP_SESSION_HEARTBEAT_SECS("print.imap.session.heartbeat-secs",
                NUMBER_VALIDATOR, "300"),

        /**
         *
         */
        PRINT_IMAP_SESSION_DURATION_SECS("print.imap.session.duration-secs",
                NUMBER_VALIDATOR, "3000"),

        /**
         *
         */
        PRINT_IMAP_MAX_FILE_MB("print.imap.max-file-mb", NUMBER_VALIDATOR,
                IMAP_MAX_FILE_MB_V_DEFAULT.toString()),

        /**
         *
         */
        PRINT_IMAP_MAX_FILES("print.imap.max-files", NUMBER_VALIDATOR,
                IMAP_MAX_FILES_V_DEFAULT.toString()),

        /**
         * Insert requesting users ad-hoc when printing from SmartSchool to the
         * {@link ReservedIppQueueEnum#SMARTSCHOOL} queue.
         * <p>
         * WARNING: this option assumes that the user is TRUSTED.
         * </p>
         * <p>
         * NOTE: when 'false', the requesting user needs to exist in the
         * database before he is allowed to print.
         * </p>
         */
        SMARTSCHOOL_USER_INSERT_LAZY_PRINT(
                "smartschool.user.insert.lazy-print", BOOLEAN_VALIDATOR, V_NO),

        /**
         * User used for simulation and testing.
         */
        SMARTSCHOOL_SIMULATION_USER("smartschool.simulation.user",
                DEFAULT_SMARTSCHOOL_SIMULATION_USER),

        /**
         * Student user 1 used for simulation and testing.
         */
        SMARTSCHOOL_SIMULATION_STUDENT_1("smartschool.simulation.student-1",
                DEFAULT_SMARTSCHOOL_SIMULATION_STUDENT_1),

        /**
         * Student user 2 used for simulation and testing.
         */
        SMARTSCHOOL_SIMULATION_STUDENT_2("smartschool.simulation.student-2",
                DEFAULT_SMARTSCHOOL_SIMULATION_STUDENT_2),

        /**
         * Is PaperCut integration for SmartSchool component enabled?
         */
        SMARTSCHOOL_PAPERCUT_ENABLE("smartschool.papercut.enable",
                BOOLEAN_VALIDATOR, V_NO),

        /**
         * Timeout in milliseconds until a SmartSchool SOAP connection is
         * established.
         */
        SMARTSCHOOL_SOAP_CONNECT_TIMEOUT_MILLIS(
                "smartschool.soap.connect-timeout-millis", NUMBER_VALIDATOR,
                "3000"),

        /**
         * Timeout in milliseconds to receive data from SmartSchool SOAP
         * Service.
         */
        SMARTSCHOOL_SOAP_SOCKET_TIMEOUT_MILLIS(
                "smartschool.soap.socket-timeout-millis", NUMBER_VALIDATOR,
                "3000"),

        /**
         * Is SmartSchool (1) enabled?
         */
        SMARTSCHOOL_1_ENABLE("smartschool.1.enable", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Printer name for SmartSchool direct proxy printing (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_PROXY_PRINTER(
                "smartschool.1.soap.print.proxy-printer"),

        /**
         * SOAP endpoint URL of SmartSchool Print Center (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_URL(
                "smartschool.1.soap.print.endpoint.url"),

        /**
         * Password of SOAP endpoint URL of SmartSchool Print Center (1).
         */
        SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_PASSWORD(
                "smartschool.1.soap.print.endpoint.password"),

        /**
         * Is SmartSchool (2) enabled?
         */
        SMARTSCHOOL_2_ENABLE("smartschool.2.enable", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Printer name for SmartSchool direct proxy printing (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_PROXY_PRINTER(
                "smartschool.2.soap.print.proxy-printer"),

        /**
         * SOAP endpoint URL of SmartSchool Print Center (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_URL(
                "smartschool.2.soap.print.endpoint.url"),

        /**
         * Password of SOAP endpoint URL of SmartSchool Print Center (2).
         */
        SMARTSCHOOL_2_SOAP_PRINT_ENDPOINT_PASSWORD(
                "smartschool.2.soap.print.endpoint.password"),

        /**
         * The heartbeat (seconds) within a SmartSchool print polling session.
         */
        SMARTSCHOOL_SOAP_PRINT_POLL_HEARTBEAT_SECS(
                "smartschool.soap.print.poll.heartbeat-secs", NUMBER_VALIDATOR,
                "2"),

        /**
         * The number of heartbeats within a SmartSchool print polling after
         * which an actual poll to SmartSchool is executed.
         * <p>
         * SmartSchool tech support advises a poll frequency between 60 and 120
         * seconds.
         * </p>
         */
        SMARTSCHOOL_SOAP_PRINT_POLL_HEARTBEATS(
                "smartschool.soap.print.poll.heartbeats", NUMBER_VALIDATOR,
                "60"),

        /**
         * The duration (seconds) of a SmartSchool print polling session.
         */
        SMARTSCHOOL_SOAP_PRINT_POLL_SESSION_DURATION_SECS(
                "smartschool.soap.print.poll.session.duration-secs",
                NUMBER_VALIDATOR, "3600"),

        /**
         *
         */
        REPORTS_PDF_INTERNAL_FONT_FAMILY("reports.pdf.font-family",
                INTERNAL_FONT_FAMILY_VALIDATOR, DEFAULT_INTERNAL_FONT_FAMILY
                        .toString()),

        /**
         * Boolean (Default is false). If true, prevents use of the non-standard
         * AUTHENTICATE LOGIN command, instead using the plain LOGIN command.
         */
        MAIL_IMAP_AUTH_LOGIN_DISABLE("print.imap.auth.login.disable",
                BOOLEAN_VALIDATOR, V_NO),

        /**
         * Boolean (Default is false). If true, prevents use of the AUTHENTICATE
         * PLAIN command.
         */
        MAIL_IMAP_AUTH_PLAIN_DISABLE("print.imap.auth.plain.disable",
                BOOLEAN_VALIDATOR, V_NO),

        /**
         * Boolean (Default is false). If true, prevents use of the AUTHENTICATE
         * NTLM command.
         */
        MAIL_IMAP_AUTH_NTLM_DISABLE("print.imap.auth.ntlm.disable",
                BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        MAIL_SMTP_HOST("mail.smtp.host", "localhost"),

        /**
         * The port to connect to on the SMTP server. Common ports include 25 or
         * 587 for STARTTLS, and 465 for SMTPS.
         */
        MAIL_SMTP_PORT("mail.smtp.port", IP_PORT_VALIDATOR, "25"),

        /**
         * STARTTLS is for connecting to an SMTP server port using a plain
         * (non-encrypted) connection, then elevating to an encrypted connection
         * on the same port.
         */
        MAIL_SMTP_SECURITY("mail.smtp.security", SMTP_SECURITY_V_NONE),

        /**
         * Username for SMTP authentication. Commonly an email address.
         */
        MAIL_SMTP_USER_NAME("mail.smtp.username"),

        /**
         * Password for SMTP authentication.
         */
        MAIL_SMTP_PASSWORD("mail.smtp.password"),

        /**
         * Produces extra SMTP related logging for troubleshooting.
         */
        MAIL_SMTP_DEBUG("mail.smtp.debug", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        MAIL_SMTP_MAX_FILE_KB("mail.smtp.max-file-kb", NUMBER_VALIDATOR, "1024"),

        /**
         * Value for SMTP property: <b>mail.smtp.connectiontimeout</b>
         * <p>
         * Timeout (in milliseconds) for establishing the SMTP connection.
         * </p>
         * <p>
         * This timeout is implemented by java.net.Socket.
         * </p>
         */
        MAIL_SMTP_CONNECTIONTIMEOUT("mail.smtp.connectiontimeout",
                NUMBER_VALIDATOR, "5000"),

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
         *
         */
        MAIL_FROM_ADDRESS("mail.from.address", NOT_EMPTY_VALIDATOR),

        /**
         *
         */
        MAIL_FROM_NAME("mail.from.name", CommunityDictEnum.SAVAPAGE.getWord()),

        /**
         *
         */
        MAIL_REPLY_TO_ADDRESS("mail.reply.to.address"),

        /**
         *
         */
        MAIL_REPLY_TO_NAME("mail.reply.to.name", "DO NOT REPLY"),

        /**
         *
         */
        PRINT_IN_ALLOW_ENCRYPTED_PDF("print-in.allow-encrypted-pdf",
                BOOLEAN_VALIDATOR, V_YES),

        /**
         * Enable non-secure proxy printing (Boolean).
         */
        PROXY_PRINT_NON_SECURE("proxy-print.non-secure", BOOLEAN_VALIDATOR,
                V_YES),

        /**
         *
         */
        PROXY_PRINT_FAST_EXPIRY_MINS("proxy-print.fast-expiry-mins",
                NUMBER_VALIDATOR, "10"),

        /**
          *
          */
        PROXY_PRINT_HOLD_EXPIRY_MINS("proxy-print.hold-expiry-mins",
                NUMBER_VALIDATOR, "60"),

        /**
          *
          */
        PROXY_PRINT_DIRECT_EXPIRY_SECS("proxy-print.direct-expiry-secs",
                NUMBER_VALIDATOR, "20"),

        /**
         * Restrict non-secure proxy printing to this Printer Group. See:
         * {@link PrinterGroup#getGroupName()}
         */
        PROXY_PRINT_NON_SECURE_PRINTER_GROUP(
                "proxy-print.non-secure-printer-group"),

        /**
         * CRON expression: 10 minutes past midnight.
         */
        SCHEDULE_DAILY("schedule.daily", "0 10 0 * * ?"),

        /**
         * CRON expression: 12:55am each day (before 1am to miss DST
         * switch-overs).
         */
        SCHEDULE_DAILY_MAINT("schedule.daily-maintenance", "0 55 0 * * ?"),

        /**
         * CRON expression.
         */
        SCHEDULE_HOURLY("schedule.hourly", "0 0 * * * ?"),

        /**
         * CRON expression.
         */
        SCHEDULE_MONTHLY("schedule.monthly", "0 30 0 1 * ?"),

        /**
         * CRON expression: 20 minutes past midnight on Sunday morning.
         */
        SCHEDULE_WEEKLY("schedule.weekly", "0 20 0 ? * 1"),

        /**
         *
         */
        SCHEDULE_AUTO_SYNC_USER("schedule.auto-sync.user", BOOLEAN_VALIDATOR,
                V_YES),

        /**
         *
         */
        STATS_PRINT_IN_ROLLING_DAY_PAGES("stats.print-in.rolling-day.pages"),

        /**
         *
         */
        STATS_PRINT_IN_ROLLING_WEEK_PAGES("stats.print-in.rolling-week.pages"),

        /**
         *
         */
        STATS_PRINT_IN_ROLLING_WEEK_BYTES("stats.print-in.rolling-week.bytes"),

        /**
         *
         */
        STATS_PRINT_IN_ROLLING_MONTH_PAGES("stats.print-in.rolling-month.pages"),

        /**
         *
         */
        STATS_PRINT_IN_ROLLING_MONTH_BYTES("stats.print-in.rolling-month.bytes"),

        /**
         *
         */
        STATS_PDF_OUT_ROLLING_DAY_PAGES("stats.pdf-out.rolling-day.pages"),

        /**
         *
         */
        STATS_PDF_OUT_ROLLING_WEEK_PAGES("stats.pdf-out.rolling-week.pages"),

        /**
         *
         */
        STATS_PDF_OUT_ROLLING_WEEK_BYTES("stats.pdf-out.rolling-week.bytes"),

        /**
         *
         */
        STATS_PDF_OUT_ROLLING_MONTH_PAGES("stats.pdf-out.rolling-month.pages"),

        /**
         *
         */
        STATS_PDF_OUT_ROLLING_MONTH_BYTES("stats.pdf-out.rolling-month.bytes"),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_DAY_PAGES("stats.print-out.rolling-day.pages"),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_WEEK_PAGES("stats.print-out.rolling-week.pages"),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_WEEK_SHEETS(
                "stats.print-out.rolling-week.sheets"),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_WEEK_ESU("stats.print-out.rolling-week.esu"),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_WEEK_BYTES("stats.print-out.rolling-week.bytes"),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_MONTH_PAGES(
                "stats.print-out.rolling-month.pages"),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_MONTH_SHEETS(
                "stats.print-out.rolling-month.sheets"),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_MONTH_ESU("stats.print-out.rolling-month.esu"),

        /**
         *
         */
        STATS_PRINT_OUT_ROLLING_MONTH_BYTES(
                "stats.print-out.rolling-month.bytes"),

        /**
         *
         */
        STATS_TOTAL_RESET_DATE("stats.total.reset-date", String
                .valueOf(new Date().getTime())),

        /**
         *
         */
        STATS_TOTAL_PDF_OUT_PAGES("stats.total.pdf-out.pages", "0"),

        /**
         *
         */
        STATS_TOTAL_PDF_OUT_BYTES("stats.total.pdf-out.bytes", "0"),

        /**
         *
         */
        STATS_TOTAL_PRINT_IN_PAGES("stats.total.print-in.pages", "0"),

        /**
         *
         */
        STATS_TOTAL_PRINT_IN_BYTES("stats.total.print-in.bytes", "0"),

        /**
         *
         */
        STATS_TOTAL_PRINT_OUT_PAGES("stats.total.print-out.pages", "0"),

        /**
         *
         */
        STATS_TOTAL_PRINT_OUT_SHEETS("stats.total.print-out.sheets", "0"),

        /**
         *
         */
        STATS_TOTAL_PRINT_OUT_ESU("stats.total.print-out.esu", "0"),

        /**
         *
         */
        STATS_TOTAL_PRINT_OUT_BYTES("stats.total.print-out.bytes", "0"),

        /**
         * Make a backup before a database schema upgrade.
         */
        SYS_BACKUP_BEFORE_DB_UPGRADE("system.backup.before-db-upgrade",
                BOOLEAN_VALIDATOR, V_YES),

        /**
         * Time in milliseconds when last backup was run.
         */
        SYS_BACKUP_LAST_RUN_TIME("system.backup.last-run-time",
                NUMBER_VALIDATOR, "0"),

        /**
         *
         */
        SYS_BACKUP_DAYS_TO_KEEP("system.backup.days-to-keep", NUMBER_VALIDATOR,
                "30"),

        /**
         *
         */
        SYS_BACKUP_ENABLE_AUTOMATIC("system.backup.enable-automatic",
                BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        SYS_DEFAULT_LOCALE("system.default-locale", LOCALE_VALIDATOR),

        /**
         *
         */
        SYS_DEFAULT_PAPER_SIZE("system.default-papersize", PAPERSIZE_V_SYSTEM),

        /**
         * The DNS name of the server. Used to give user feedback for URL's,
         * e.g. URL's to use for IPP printing.
         */
        SYS_SERVER_DNS_NAME("system.server.dns-name"),

        /**
         * The major database schema version.
         * <p>
         * Do NOT set a value since it is present in installation database.
         * </p>
         */
        SYS_SCHEMA_VERSION("system.schema-version"),

        /**
         * The minor database schema version.
         * <p>
         * This value is set in the installation database (since v0.9.3), but
         * defaults to "0" for pre v0.9.3 databases.
         * </p>
         */
        SYS_SCHEMA_VERSION_MINOR("system.schema-version-minor", "0"),

        /**
         * Do NOT set a value since it is present in installation database.
         */
        SYS_SETUP_COMPLETED("system.setup-completed"),

        /**
         *
         */
        USER_CAN_CHANGE_PIN("user.can-change-pin", BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        USER_PIN_LENGTH_MIN("user.pin-length-min", NUMBER_VALIDATOR, "4"),

        /**
         *
         */
        USER_PIN_LENGTH_MAX("user.pin-length-max", NUMBER_VALIDATOR,
                NUMBER_V_NONE.toString()),

        /**
         *
         */
        USER_ID_NUMBER_LENGTH_MIN("user.id-number-length-min",
                NUMBER_VALIDATOR, "4"),

        /**
         * Insert users ad-hoc after successful authentication at the login
         * page.
         */
        USER_INSERT_LAZY_LOGIN("user.insert.lazy-login", BOOLEAN_VALIDATOR,
                V_YES),

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
        USER_INSERT_LAZY_PRINT("user.insert.lazy-print", BOOLEAN_VALIDATOR,
                V_NO),

        /**
         *
         */
        USER_SOURCE_GROUP("user-source.group"),

        /**
         *
         */
        USER_SOURCE_UPDATE_USER_DETAILS("user-source.update-user-details",
                BOOLEAN_VALIDATOR, V_YES),

        /**
         * (boolean) Trust the User Client App system account name as user
         * identification?
         */
        CLIAPP_AUTH_TRUST_USER_ACCOUNT("cliapp.auth.trust-user-account",
                BOOLEAN_VALIDATOR, V_NO),

        /**
         * Secret administrator passkey of User Client App.
         */
        CLIAPP_AUTH_ADMIN_PASSKEY("cliapp.auth.admin-passkey"),

        /**
         * Trust authenticated user in User Web App on same IP address as Client
         * App (Boolean, default TRUE).
         */
        CLIAPP_AUTH_TRUST_WEBAPP_USER_AUTH(
                "cliapp.auth.trust-webapp-user-auth", BOOLEAN_VALIDATOR, V_YES),

        /**
         * Trust authenticated user in Client App on same IP address as User Web
         * App (Boolean, default TRUE).
         */
        WEBAPP_USER_AUTH_TRUST_CLIAPP_AUTH(
                "webapp.user.auth.trust-cliapp-auth", BOOLEAN_VALIDATOR, V_YES),

        /**
         * (boolean) Show Document title in the DocLog.
         */
        WEBAPP_DOCLOG_SHOW_DOC_TITLE("webapp.doclog.show-doc-title",
                BOOLEAN_VALIDATOR, V_YES),

        /**
         *
         */
        WEBAPP_WATCHDOG_HEARTBEAT_SECS("webapp.watchdog.heartbeat-secs",
                NUMBER_VALIDATOR, DEFAULT_WEBAPP_WATCHDOG_HEARTBEAT_SECS),

        /**
         *
         */
        WEBAPP_WATCHDOG_TIMEOUT_SECS("webapp.watchdog.timeout-secs",
                NUMBER_VALIDATOR, DEFAULT_WEBAPP_WATCHDOG_TIMEOUT_SECS),

        /**
         * Max idle seconds after which automatic logout occurs.
         */
        WEBAPP_USER_MAX_IDLE_SECS("webapp.user.max-idle-secs",
                NUMBER_VALIDATOR, WEBAPP_MAX_IDLE_SECS_V_NONE.toString()),

        /**
         * Admin WebApp: show technical info on dashboard?
         */
        WEBAPP_ADMIN_DASHBOARD_SHOW_TECH_INFO(
                "webapp.admin.dashboard.show-tech-info", BOOLEAN_VALIDATOR,
                V_NO),

        /**
         * User WebApp: Max. copies for proxy printing.
         */
        WEBAPP_USER_PROXY_PRINT_MAX_COPIES(
                "webapp.user.proxy-print.max-copies", NUMBER_VALIDATOR, "30"),

        /**
         * User WebApp: clear inbox after proxy printing.
         */
        WEBAPP_USER_PROXY_PRINT_CLEAR_INBOX(
                "webapp.user.proxy-print.clear-inbox", BOOLEAN_VALIDATOR, V_NO),

        /**
         * Time limit (milliseconds) to capture the keystrokes of the card
         * number from a Local Card Reader.
         */
        WEBAPP_CARD_LOCAL_KEYSTROKES_MAX_MSECS(
                "webapp.card-local.keystrokes-max-msecs", NUMBER_VALIDATOR,
                "500"),

        /**
         * Time limit (seconds) for a user to associate a new Card to his
         * account. After the time limit the dialog is automatically closed.
         */
        WEBAPP_CARD_ASSOC_DIALOG_MAX_SECS("webapp.card-assoc.dialog-max-secs",
                NUMBER_VALIDATOR, "30"),

        /**
         * Inactivity timeout (minutes) for the admin web interface.
         */
        WEB_LOGIN_ADMIN_SESSION_TIMOUT_MINS(
                "web-login.admin.session-timeout-mins", NUMBER_VALIDATOR,
                "1440"),

        /**
         * Inactivity timeout (minutes) for the user web interface.
         */
        WEB_LOGIN_USER_SESSION_TIMEOUT_MINS(
                "web-login.user.session-timeout-mins", NUMBER_VALIDATOR, "60"),

        /**
         *
         */
        WEB_PRINT_ENABLE("web-print.enable", BOOLEAN_VALIDATOR, V_NO),

        /**
         *
         */
        WEB_PRINT_MAX_FILE_MB("web-print.max-file-mb", NUMBER_VALIDATOR,
                WEBPRINT_MAX_FILE_MB_V_DEFAULT.toString()),

        /**
         *
         */
        WEB_PRINT_LIMIT_IP_ADDRESSES("web-print.limit-ip-addresses");

        /**
         *
         */
        private final Prop property;

        /**
         *
         * @param name
         */
        private Key(final String name) {
            this.property =
                    this.createProperty(KeyType.SINGLE_LINE, name, null, "",
                            null);
        }

        /**
         *
         * @param name
         * @param keyType
         */
        private Key(final String name, final KeyType keyType) {
            this.property = this.createProperty(keyType, name, null, "", null);
        }

        /**
         *
         * @param name
         * @param defaultValue
         */
        private Key(final String name, final String defaultValue) {
            this.property =
                    this.createProperty(KeyType.SINGLE_LINE, name, null,
                            defaultValue, null);
        }

        /**
         *
         * @param name
         * @param validator
         */
        private Key(final String name, final Validator validator) {
            this.property =
                    this.createProperty(KeyType.SINGLE_LINE, name, validator,
                            "", null);
        }

        /**
         *
         * @param name
         * @param keyType
         * @param defaultValue
         */
        private Key(final String name, final KeyType keyType,
                final String defaultValue) {
            this.property =
                    this.createProperty(keyType, name, null, defaultValue, null);
        }

        /**
         *
         * @param name
         * @param keyType
         * @param validator
         * @param defaultValue
         */
        private Key(final String name, final KeyType keyType,
                final Validator validator, final String defaultValue) {
            this.property =
                    this.createProperty(keyType, name, validator, defaultValue,
                            null);
        }

        /**
         *
         * @param name
         * @param validator
         * @param defaultValue
         */
        private Key(final String name, final Validator validator,
                final String defaultValue) {
            this.property =
                    this.createProperty(KeyType.SINGLE_LINE, name, validator,
                            defaultValue, null);
        }

        /**
         *
         * @param name
         * @param validator
         * @param defaultValue
         * @param values
         */
        private Key(final String name, final Validator validator,
                final String defaultValue, String[] values) {
            this.property =
                    this.createProperty(KeyType.SINGLE_LINE, name, validator,
                            defaultValue, values);
        }

        /**
         *
         * @param keyType
         * @param name
         * @param validator
         * @param defaultValue
         * @param values
         * @return
         */
        private Prop createProperty(final KeyType keyType, final String name,
                final Validator validator, final String defaultValue,
                String[] values) {

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
         * @return
         */
        public Prop getProperty() {
            return property;
        }

    };

    /**
     * Validation status.
     *
     * @author rijk
     *
     */
    enum ValidationStatusEnum {
        /**
         *
         */
        OK,
        /**
         *
         */
        ERROR_EMPTY,
        /**
         *
         */
        ERROR_ENUM,
        /**
         *
         */
        ERROR_SYNTAX,
        /**
         *
         */
        ERROR_YN,
        /**
         *
         */
        ERROR_LOCALE,
        /**
         *
         */
        ERROR_NOT_NUMERIC,
        /**
         *
         */
        ERROR_NOT_DECIMAL,
        /**
         *
         */
        ERROR_RANGE
    }

    /**
     *
     * @author rijk
     *
     */
    public static class ValidationResult {

        private String value = null;
        private String message = "";

        /**
         *
         */
        private ValidationStatusEnum status = ValidationStatusEnum.OK;

        public ValidationResult(final String value) {
            this.setValue(value);
            status = ValidationStatusEnum.OK;
        }

        public ValidationResult(final String value,
                final ValidationStatusEnum status, final String message) {
            this.status = status;
            this.message = message;
        }

        public boolean isValid() {
            return status == ValidationStatusEnum.OK;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public ValidationStatusEnum getStatus() {
            return status;
        }

        public void setStatus(ValidationStatusEnum status) {
            this.status = status;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    /**
     * Validation of a single value.
     */
    interface Validator {

        /**
         * Validate a value.
         *
         * @param value
         *            Value to be validated.
         * @return The newly created validation result;
         */
        ValidationResult validate(final String value);
    }

    /**
     *
     */
    static class NotEmptyValidator implements Validator {

        @Override
        public ValidationResult validate(String value) {
            ValidationResult res = new ValidationResult(value);
            if (StringUtils.isBlank(value)) {
                res.setStatus(ValidationStatusEnum.ERROR_EMPTY);
            }
            if (!res.isValid()) {
                res.setMessage("value cannot be empty");
            }
            return res;
        }
    }

    /**
    *
    */
    static class UrlValidator implements Validator {

        /**
         * {@code true} if value is optional.
         */
        private final boolean isOptional;

        /**
         * @param optional
         *            {@code true} if value is optional.
         */
        public UrlValidator(final boolean optional) {
            super();
            this.isOptional = optional;
        }

        @Override
        public ValidationResult validate(final String value) {

            ValidationResult res = new ValidationResult(value);

            if (!this.isOptional || !value.isEmpty()) {

                try {
                    new URL(value);
                } catch (MalformedURLException e) {
                    res.setStatus(ValidationStatusEnum.ERROR_SYNTAX);
                }
            }

            if (!res.isValid()) {
                res.setMessage("Invalid URL");
            }
            return res;
        }
    }

    /**
     *
     */
    static class BooleanValidator implements Validator {

        @Override
        public ValidationResult validate(String value) {
            ValidationResult res = new ValidationResult(value);
            boolean valid =
                    (value != null && (value.equals(V_YES) || value
                            .equals(V_NO)));
            if (!valid) {
                res.setStatus(ValidationStatusEnum.ERROR_YN);
            }
            if (!res.isValid()) {
                res.setMessage("value should be [" + V_YES + "] or [" + V_NO
                        + "]");
            }
            return res;
        }

    }

    /**
     *
     */
    static class LocaleValidator implements Validator {

        @Override
        public ValidationResult validate(String value) {
            ValidationResult res = new ValidationResult(value);

            try {
                if (!value.isEmpty()) {
                    new Locale.Builder().setLanguageTag(value).build();
                }
            } catch (Exception e) {
                res.setStatus(ValidationStatusEnum.ERROR_LOCALE);
                res.setMessage(e.getMessage());
            }
            return res;
        }
    }

    /**
     *
     */
    static class InternalFontFamilyValidator implements Validator {

        @Override
        public ValidationResult validate(String fontFamily) {
            ValidationResult res = new ValidationResult(fontFamily);
            if (StringUtils.isBlank(fontFamily)) {
                res.setStatus(ValidationStatusEnum.ERROR_EMPTY);
                res.setMessage("value must be a font family name");
            } else {
                if (!EnumUtils.isValidEnum(InternalFontFamilyEnum.class,
                        fontFamily)) {
                    res.setStatus(ValidationStatusEnum.ERROR_ENUM);
                    res.setMessage("font family enum is invalid");
                }
            }
            return res;
        }
    }

    /**
     *
     */
    static class DecimalValidator implements Validator {

        @Override
        public ValidationResult validate(String plainString) {
            ValidationResult res = new ValidationResult(plainString);
            if (StringUtils.isBlank(plainString)) {
                res.setStatus(ValidationStatusEnum.ERROR_EMPTY);
                res.setMessage("value must be a decimal");
            } else {
                if (!BigDecimalUtil.isValid(plainString)) {
                    res.setStatus(ValidationStatusEnum.ERROR_NOT_DECIMAL);
                    res.setMessage("decimal value is invalid");
                }
            }
            return res;
        }
    }

    /**
     *
     */
    static class NumberValidator implements Validator {

        private final Long minValue;
        private final Long maxValue;

        public NumberValidator() {
            this.minValue = null;
            this.maxValue = null;
        }

        public NumberValidator(Long minValue, Long maxValue) {
            this.minValue = minValue;
            this.maxValue = maxValue;
        }

        @Override
        public ValidationResult validate(String value) {

            ValidationResult res = new ValidationResult(value);

            if (StringUtils.isBlank(value)) {

                res.setStatus(ValidationStatusEnum.ERROR_EMPTY);
                res.setMessage("value must be a number");

            } else {

                if (StringUtils.isNumeric(value)) {

                    final Long longValue = Long.valueOf(value);

                    final boolean isValid =
                            (this.minValue == null || longValue >= this.minValue)
                                    && (this.maxValue == null || longValue <= this.maxValue);

                    if (!isValid) {
                        res.setStatus(ValidationStatusEnum.ERROR_RANGE);
                        res.setMessage("value is not in range");
                    }

                } else {

                    res.setStatus(ValidationStatusEnum.ERROR_NOT_NUMERIC);
                    res.setMessage("value must contain digits only");

                }
            }
            return res;
        }

    }

    /**
     * Just a wrapper for now.
     */
    static class IpPortValidator extends NumberValidator {
    }

    /**
     * .
     */
    BooleanValidator BOOLEAN_VALIDATOR = new BooleanValidator();
    /**
     * .
     */
    IpPortValidator IP_PORT_VALIDATOR = new IpPortValidator();
    /**
     * .
     */
    NumberValidator NUMBER_VALIDATOR = new NumberValidator();
    /**
     * .
     */
    LocaleValidator LOCALE_VALIDATOR = new LocaleValidator();

    /**
     * .
     */
    NotEmptyValidator NOT_EMPTY_VALIDATOR = new NotEmptyValidator();

    /**
     * .
     */
    UrlValidator URL_VALIDATOR = new UrlValidator(false);

    /**
     * URL is not required (may be empty).
     */
    UrlValidator URL_VALIDATOR_OPT = new UrlValidator(true);

    /**
     * .
     */
    NumberValidator ACCOUNTING_DECIMAL_VALIDATOR = new NumberValidator(0L,
            Integer.valueOf(MAX_FINANCIAL_DECIMALS_IN_DB).longValue());

    /**
     * .
     */
    InternalFontFamilyValidator INTERNAL_FONT_FAMILY_VALIDATOR =
            new InternalFontFamilyValidator();

    /**
     * @author Datraverse B.V.
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
     *
     * @author Datraverse B.V.
     *
     */
    static class Prop {

        final private Key key;
        final private String name;

        private String defaultValue = null;
        private String value = null;

        private String[] values = null;

        private Validator validator = null;
        private ValidationResult validationResult = null;

        private Prop(final Key key, final String name) {
            this.key = key;
            this.name = name;
        }

        Prop(final Key key, final String name, final Validator validator) {
            this(key, name);
            this.validator = validator;
        }

        Prop(final Key key, final String name, final Validator validator,
                final String defaultValue) {
            this(key, name, validator);
            this.defaultValue = defaultValue;
        }

        Prop(final Key key, final String name, final Validator validator,
                final String defaultValue, String[] values) {
            this(key, name, validator, defaultValue);
            this.values = values;
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
            final Validator validator = prop.getValidator();
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

        public Validator getValidator() {
            return validator;
        }

        public void setValidator(Validator validator) {
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

    final String V_YES = "Y";
    final String V_NO = "N";

    /**
     * Null value for numerics.
     */
    final String V_NULL = "-1";

    final String AUTH_METHOD_V_LDAP = "ldap";
    final String AUTH_METHOD_V_UNIX = "unix";
    final String AUTH_METHOD_V_NONE = "none";

    final String AUTH_MODE_V_NAME = UserAuth.MODE_NAME;
    final String AUTH_MODE_V_ID = UserAuth.MODE_ID;
    final String AUTH_MODE_V_CARD_LOCAL = UserAuth.MODE_CARD_LOCAL;
    final String AUTH_MODE_V_CARD_IP = UserAuth.MODE_CARD_IP;

    final String LDAP_TYPE_V_APPLE = "APPLE_OPENDIR";
    final String LDAP_TYPE_V_OPEN_LDAP = "OPEN_LDAP";
    final String LDAP_TYPE_V_E_DIR = "NOVELL_EDIRECTORY";
    final String LDAP_TYPE_V_ACTIV = "ACTIVE_DIRECTORY";

    final String PAPERSIZE_V_SYSTEM = "";
    final String PAPERSIZE_V_A4 = MediaSizeName.ISO_A4.toString();
    final String PAPERSIZE_V_LETTER = MediaSizeName.NA_LETTER.toString();

    /**
     *
     */
    final String SMTP_SECURITY_V_NONE = "";

    /**
     * Set to Y to enable STARTTLS, or N to disable it. STARTTLS is for
     * connecting to an SMTP server port using a plain (non-encrypted)
     * connection, then elevating to an encrypted connection on the same port.
     */
    final String SMTP_SECURITY_V_STARTTLS = "starttls";
    /**
     *
     */
    final String SMTP_SECURITY_V_SSL = "ssl";

    /**
     *
     */
    final String IMAP_SECURITY_V_NONE = "";

    /**
     *
     */
    final String IMAP_SECURITY_V_STARTTLS = "starttls";

    /**
     *
     */
    final String IMAP_SECURITY_V_SSL = "ssl";

    /**
     *
     */
    final Integer IMAP_CONNECTION_TIMEOUT_V_DEFAULT = 10000;
    final Integer IMAP_TIMEOUT_V_DEFAULT = 10000;

    final Long IMAP_MAX_FILE_MB_V_DEFAULT = 5L;
    final Integer IMAP_MAX_FILES_V_DEFAULT = 1;

    /**
     *
     */
    final Long WEBPRINT_MAX_FILE_MB_V_DEFAULT = 5L;

    /**
     *
     */
    final Integer WEBAPP_MAX_IDLE_SECS_V_NONE = 0;

    final String CARD_NUMBER_FORMAT_V_DEC = "DEC";
    final String CARD_NUMBER_FORMAT_V_HEX = "HEX";

    final String CARD_NUMBER_FIRSTBYTE_V_LSB = "LSB";
    final String CARD_NUMBER_FIRSTBYTE_V_MSB = "MSB";

    /**
     *
     */
    final Integer NUMBER_V_NONE = 0;

    /**
     *
     * @author rijk
     *
     */
    enum LdapType {

        /**
         * OpenLDAP
         */
        OPEN_LDAP,

        /**
         * Apple Open Directory
         */
        OPEN_DIR,
        /**
         * Novell eDirectory
         */
        EDIR,
        /**
         * Microsoft Active Directory
         */
        ACTD,
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
    void init(final Properties defaultProps);

    /**
     * Initializes the component to be fully runnable.
     * <p>
     * Note: Database access CAN BE used for this action.
     * </p>
     *
     * @param defaultProps
     *            The default properties.
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
    ValidationResult validate(final Key key, final String value);

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
    void updateValue(final Key key, final String value, final String actor);

    /**
     * Saves the string value of a configuration key. The key is lazy created.
     *
     * @param key
     *            The key.
     * @param value
     *            The string value.
     */
    void saveValue(final Key key, final String value);

    /**
     * Does this property represent a multi-line value?
     *
     * @return
     */
    boolean isMultiLine(final Key key);

    /**
     * Does this property represent a {@link BigDecimal} value?
     *
     * @return
     */
    boolean isBigDecimal(final Key key);

    /**
     * Gets the value of a configuration key as string.
     *
     * @param key
     *            The key.
     * @return The string value.
     */
    String getString(final Key key);

    /**
     * Gets the value of an LDAP configuration key as string.
     *
     * @param ldapType
     *            The LDAP type.
     * @param key
     *            The key.
     * @return The string value or {@code null} when not found.
     */
    String getString(final LdapType ldapType, final Key key);

    /**
     * Gets the value of an LDAP configuration key as Boolean.
     *
     * @param ldapType
     *            The LDAP type.
     * @param key
     *            The key.
     * @return The boolean value or {@code null} when not found.
     */
    Boolean getBoolean(final LdapType ldapType, final Key key);

    /**
     * Gets the value of a configuration key as double.
     *
     * @param key
     *            The key.
     * @return The double value.
     */
    double getDouble(final Key key);

    /**
     * Gets the value of a configuration key as {@link BigDecimal}.
     *
     * @param key
     *            The key.
     * @return The {@link BigDecimal} value.
     */
    BigDecimal getBigDecimal(final Key key);

    /**
     * Gets the value of a configuration key as long.
     *
     * @param key
     *            The key.
     * @return The long value.
     */
    long getLong(final Key key);

    /**
     * Gets the value of a configuration key as int.
     *
     * @param key
     *            The key.
     * @return The int value.
     */
    int getInt(final Key key);

    /**
     * Gets the value of a configuration key as boolean.
     *
     * @param key
     *            The key.
     * @return The boolean value.
     */
    boolean getBoolean(final Key key);

    /**
     * Gets the value (comma separated list) of a configuration key as
     * {@link Set} of values.
     *
     * @param key
     *            The key.
     * @return The {@link Set} of values.
     */
    Set<String> getSet(final Key key);

    /**
     * Gets the string representation of the configuration key.
     *
     * @param key
     *            The enum representation of the key.
     * @return The string representation of the key.
     */
    String getKey(final Key key);

    /**
     * Gets the enum of the configuration key.
     *
     * @param key
     *            The string representation of the key.
     * @return The enum representation of the key, or {@code null} when the key
     *         is not found.
     */
    Key getKey(final String key);

    /**
     *
     * @param key
     * @return
     */
    Prop getProp(Key key);

    /**
     *
     * @param name
     * @return {@code null} when not found.
     */
    Prop getProp(String name);

}
