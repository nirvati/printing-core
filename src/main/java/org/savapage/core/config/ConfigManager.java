/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import javax.mail.internet.InternetAddress;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.savapage.common.SystemPropertyEnum;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.VersionInfo;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerRegistry;
import org.savapage.core.circuitbreaker.CircuitDamagingException;
import org.savapage.core.circuitbreaker.CircuitNonTrippingException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.concurrent.ReadLockObtainFailedException;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.IConfigProp.LdapTypeEnum;
import org.savapage.core.config.IConfigProp.Prop;
import org.savapage.core.config.validator.EnumSetValidator;
import org.savapage.core.config.validator.ValidationResult;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.doc.store.DocStoreBranchEnum;
import org.savapage.core.doc.store.DocStoreTypeEnum;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.i18n.SystemModeEnum;
import org.savapage.core.ipp.client.IppClient;
import org.savapage.core.jmx.CoreConfig;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.jpa.ConfigProperty;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.tools.DatabaseTypeEnum;
import org.savapage.core.jpa.tools.DbConfig;
import org.savapage.core.jpa.tools.DbConnectionPoolEnum;
import org.savapage.core.jpa.tools.DbTools;
import org.savapage.core.jpa.tools.DbUpgManager;
import org.savapage.core.jpa.tools.DbVersionInfo;
import org.savapage.core.pdf.PdfDocumentFonts;
import org.savapage.core.print.proxy.ProxyPrintJobStatusMonitor;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceFactory;
import org.savapage.core.services.helpers.SOfficeConfigProps;
import org.savapage.core.system.SystemInfo;
import org.savapage.core.users.ActiveDirectoryUserSource;
import org.savapage.core.users.CustomUserSource;
import org.savapage.core.users.IExternalUserAuthenticator;
import org.savapage.core.users.IUserSource;
import org.savapage.core.users.LdapUserSource;
import org.savapage.core.users.NoUserSource;
import org.savapage.core.users.UnixUserSource;
import org.savapage.core.users.conf.UserAliasList;
import org.savapage.core.util.CurrencyUtil;
import org.savapage.core.util.FileSystemHelper;
import org.savapage.core.util.InetUtils;
import org.savapage.ext.google.GoogleLdapClient;
import org.savapage.ext.google.GoogleLdapUserSource;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPHelper;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.savapage.lib.pgp.PGPSecretKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class ConfigManager {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(ConfigManager.class);

    /** */
    private static boolean shutdownInProgress = false;

    /** */
    private RunModeEnum runMode = null;

    /** */
    private static final String SERVER_DATA_CONF_USERNAME_ALIASES_TXT =
            "username-aliases.txt";

    /**
     * Path of SAVAPAGE.ppd (case sensitive!) relative to
     * {@link ConfigManager#getClientHome().
     */
    private static final String REL_PATH_SAVAPAGE_PPD_FILE = "SAVAPAGE.ppd";

    private static final String INTERNAL_ADMIN_PASSWORD_DEFAULT = "admin";

    private static final String PROP_INTERNAL_ADMIN_PASSWORD = "admin.password";

    /**
     * The default standard port of the Web server.
     */
    private static String theDefaultServerPort;

    /**
     * The default SSL port of the Web server.
     */
    private static String theDefaultServerSslPort;

    /** */
    private static SslCertInfo sslCertInfo;

    /**
     *
     */
    @SuppressWarnings("unchecked")
    private static final Class<? extends Exception>[] CIRCUIT_NON_TRIPPING_EXCEPTIONS =
            new Class[] { CircuitNonTrippingException.class };

    @SuppressWarnings("unchecked")
    private static final Class<? extends Exception>[] CIRCUIT_DAMAGING_EXCEPTIONS =
            new Class[] { CircuitDamagingException.class };

    /**
     * Captured system locale before it is changed.
     */
    private static final Locale SERVER_HOST_LOCALE = Locale.getDefault();

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link ConfigManager#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final ConfigManager INSTANCE = new ConfigManager();
    }

    /**
     * Basename of the properties file of the web server.
     */
    private static final String FILENAME_SERVER_PROPERTIES =
            "server.properties";

    private static volatile Properties theServerProps = null;

    public static final String SERVER_PROP_APP_DIR_TMP = "app.dir.tmp";
    public static final String SERVER_PROP_APP_DIR_SAFEPAGES =
            "app.dir.safepages";
    public static final String SERVER_PROP_APP_DIR_LETTERHEADS =
            "app.dir.letterheads";

    public static final String SERVER_PROP_APP_DIR_DOC_STORE_ARCHIVE =
            "app.dir.doc.store.archive";
    public static final String SERVER_PROP_APP_DIR_DOC_STORE_JOURNAL =
            "app.dir.doc.store.journal";

    private static final String SERVER_PROP_DB_TYPE = "database.type";

    /**
     * The JDBC driver, like "org.postgresql.Driver".
     */
    private static final String SERVER_PROP_DB_DRIVER = "database.driver";
    /**
     * The hibernate dialect: e.g.
     */
    private static final String SERVER_PROP_DB_HIBERNATE_DIALECT =
            "database.hibernate.dialect";
    private static final String SERVER_PROP_DB_URL = "database.url";
    private static final String SERVER_PROP_DB_USER = "database.user";
    private static final String SERVER_PROP_DB_PASS = "database.password";

    /*
     *
     */
    public static final String SERVER_PROP_CUPS_NOTIFIER = "cups.notifier";
    private static final String DEFAULT_CUPS_NOTIFIER = "savapage";

    private static final String SERVER_PROP_CUPS_SERVER_PORT =
            "cups.server.port";

    private static final String DEFAULT_CUPS_HOST = InetUtils.LOCAL_HOST;
    private static final String DEFAULT_CUPS_PORT = "631";

    public static final String SERVER_PROP_PRINTER_RAW_PORT =
            "server.print.port.raw";
    public static final String PRINTER_RAW_PORT_DEFAULT = "9100";

    private static final String SERVER_PROP_OPENPGP_PUBLICKEY_FILE =
            "pgp.publickey.file";

    private static final String SERVER_PROP_OPENPGP_SECRETKEY_FILE =
            "pgp.secretkey.file";

    private static final String SERVER_PROP_OPENPGP_SECRETKEY_PASSPHRASE =
            "pgp.secretkey.passphrase";

    /** */
    private static final String SERVER_PROP_START_CLEANUP_DOCLOG =
            "start.cleanup-doclog";

    /** */
    private static final String SERVER_PROP_START_CLEANUP_USERHOME =
            "start.cleanup-userhome";

    /** */
    private static final String SERVER_PROP_SYSTEM_CLEANUP_USERHOME_TEST =
            "system.cleanup-userhome.test";

    // ========================================================================
    // Undocumented ad-hoc properties for testing purposes.
    // ========================================================================
    private static final String SERVER_PROP_IPP_TRUST_IP_USER =
            "ipp.trust.ip-user";

    // ========================================================================

    /** */
    private static final String APP_OWNER =
            CommunityDictEnum.DATRAVERSE_BV.getWord();

    /**
     * Depth of a user home directory relative to root of all home directories.
     */
    private static final int USER_HOME_DEPTH_FROM_ROOT = 3;

    /**
     *
     */
    private static final Map<String, String> theTrustedUsersByIP =
            new HashMap<>();

    /** */
    private ProxyPrintService myPrintProxy;

    /** */
    private DatabaseTypeEnum myDatabaseType;

    /** */
    private Map<DbConnectionPoolEnum, String> dbConnectionPoolProps;

    /**
     * For convenience we use ConfigPropImp instead of ConfigProp (because of
     * easy Eclipse hyperlinking).
     */
    private final ConfigPropImpl myConfigProp = new ConfigPropImpl();

    /** */
    private Properties myPropsAdmin = null;

    /** */
    private EntityManagerFactory myEmf = null;

    /** */
    private final CircuitBreakerRegistry circuitBreakerRegistry =
            new CircuitBreakerRegistry();

    /** */
    private DbVersionInfo myDbVersionInfo = null;

    /** */
    private final Object myDbVersionInfoMutex = new Object();

    /** */
    private final Object createJobTicketsHomeMutex = new Object();

    /** */
    private PGPSecretKeyInfo pgpSecretKeyInfo;

    /** */
    private PGPPublicKeyInfo pgpPublicKeyInfo;

    /** */
    private final DbConfig.JdbcInfo jdbcInfo = new DbConfig.JdbcInfo();

    /** */
    private final DbConfig.HibernateInfo hibernateInfo =
            new DbConfig.HibernateInfo();

    /**
     * The SSL URL of the Admin WebApp.
     */
    private static URL theWebAppAdminSslUrl;

    /**
     * The SSL URL of the User WebApp.
     */
    private static URL theWebAppUserSslUrl;

    /**
     * Home of the IPP printer-icons.
     */
    private static String theIppPrinterIconsUrlPath;

    /** */
    private ConfigManager() {
        runMode = null;
    }

    /**
     * Redirects Java Logging to Log4j.
     */
    public static void initJavaUtilLogging() {
        java.util.logging.LogManager.getLogManager().reset();
        SLF4JBridgeHandler.install();
    }

    /**
     * Sets the default standard port of the Web server.
     *
     * @param port
     *            The port.
     */
    public static void setDefaultServerPort(final String port) {
        theDefaultServerPort = port;
    }

    /**
     * Sets the default SSL port of the Web server.
     *
     * @param port
     *            The port.
     */
    public static void setDefaultServerSslPort(final String port) {
        theDefaultServerSslPort = port;
    }

    /**
     *
     * @param info
     *            The SSL certificate info.
     */
    public static void setSslCertInfo(final SslCertInfo info) {
        sslCertInfo = info;
    }

    /**
     *
     * @return The {@link SslCertInfo}, or {@code null}. when alias is not
     *         found.
     */
    public static SslCertInfo getSslCertInfo() {
        return sslCertInfo;
    }

    /**
     * Gets the database commit chunk size for batch processing.
     *
     * @return The chunk size.
     */
    public static int getDaoBatchChunkSize() {
        return instance().getConfigInt(Key.DB_BATCH_COMMIT_CHUNK_SIZE,
                Integer.valueOf(IConfigProp.DEFAULT_BATCH_COMMIT_CHUNK_SIZE));
    }

    /**
     * Gets the singleton instance.
     *
     * @return The singleton.
     */
    public static ConfigManager instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Checks if application is properly initialized.
     *
     * @return {@code true} when properly initialized.
     */
    public boolean isInitialized() {
        return (runMode != null);
    }

    /**
     * @return JDBC info.
     */
    public DbConfig.JdbcInfo getJdbcInfo() {
        return this.jdbcInfo;
    }

    /**
     * @return Hibernate info.
     */
    public DbConfig.HibernateInfo getHibernateInfo() {
        return this.hibernateInfo;
    }

    /**
     * @return {@code null} when not (properly) configured.
     */
    public PGPSecretKeyInfo getPGPSecretKeyInfo() {
        return this.pgpSecretKeyInfo;
    }

    /**
     * @return {@code null} when not (properly) configured.
     */
    public PGPPublicKeyInfo getPGPPublicKeyInfo() {
        return this.pgpPublicKeyInfo;
    }

    /**
     * @return {@code true} when OpenPGP Key Pair is present.
     */
    public boolean hasOpenPGP() {
        return this.pgpPublicKeyInfo != null && this.pgpSecretKeyInfo != null;
    }

    /**
     * Gets the {@link Locale} of the SavaPage host system.
     * <p>
     * Note: {@link IConfigProp.Key#SYS_DEFAULT_LOCALE} holds the
     * {@link Locale#toLanguageTag()} override of this Locale.
     * </p>
     *
     * @return
     */
    public static Locale getServerHostLocale() {
        return SERVER_HOST_LOCALE;
    }

    /**
     * Gets the currency symbol of the SavaPage host system. See
     * {@link #getServerHostLocale()}.
     *
     * @return The currency symbol or an empty string if the country of the
     *         locale is not a valid ISO 3166 country code.
     */
    public static String getServerHostCurrencySymbol() {
        try {
            return Currency.getInstance(SERVER_HOST_LOCALE).getSymbol();
        } catch (IllegalArgumentException e1) {
            return "";
        }
    }

    public static String getDefaultCurrencySymbol() {
        try {
            return Currency.getInstance(getDefaultLocale()).getSymbol();
        } catch (IllegalArgumentException e1) {
            return "";
        }
    }

    /**
     * Gets the default {@link Locale} for this application.
     *
     * @return The {@link Locale}.
     */
    public static Locale getDefaultLocale() {
        final String languageTag =
                ConfigManager.instance().getConfigValue(Key.SYS_DEFAULT_LOCALE);

        if (StringUtils.isNotBlank(languageTag)) {
            Locale.Builder builder = new Locale.Builder();
            return builder.setLanguageTag(languageTag.trim()).build();
        } else {
            return getServerHostLocale();
        }
    }

    /**
     * Checks if the password is valid for a user according the checksum
     * applied.
     *
     * @param checkSum
     *            The checksum.
     * @param username
     * @param password
     * @return <code>true</code> if this is a valid password for the user.
     */
    public boolean isUserPasswordValid(final String checkSum,
            final String username, final String password) {
        return CryptoUser.isUserPasswordValid(checkSum, username, password);
    }

    /**
     * Checks if uid belongs to the internal admin.
     *
     * @param uid
     * @return
     */
    public static boolean isInternalAdmin(final String uid) {
        return uid.equals(UserDao.INTERNAL_ADMIN_USERID);
    }

    /**
     * Checks if internal Derby database is used.
     *
     * @return
     */
    public static boolean isDbInternal() {
        return instance().myDatabaseType == DatabaseTypeEnum.Internal;
    }

    /**
     * Checks if the internal admin password is valid.
     *
     * @param uid
     *            The internal administrator.
     * @param password
     *            The password
     * @return The admin user or {@code null} when uid/password combination is
     *         invalid.
     */
    public User isInternalAdminValid(final String uid, final String password) {

        User user = null;

        if (isInternalAdmin(uid)) {

            loadAdminProperties();

            if (isUserPasswordValid(
                    myPropsAdmin.getProperty(PROP_INTERNAL_ADMIN_PASSWORD), uid,
                    password)) {
                user = createInternalAdminUser();
            }
        }
        return user;
    }

    /**
     * Creates a dummy {@link User} object for the reserved
     * {@link #INTERNAL_ADMIN_USERID}.
     *
     * @return The User object.
     */
    public static User createInternalAdminUser() {
        User user = new User();
        user.setUserId(UserDao.INTERNAL_ADMIN_USERID);
        user.setAdmin(true);
        return user;
    }

    /**
     * Checks whether the internal admin has the default password.
     *
     * @return
     */
    public boolean doesInternalAdminHasDefaultPassword() {
        return isInternalAdminValid(UserDao.INTERNAL_ADMIN_USERID,
                INTERNAL_ADMIN_PASSWORD_DEFAULT) != null;
    }

    /**
     *
     * @return
     */
    public boolean isDbBackupBeforeUpg() {
        return isConfigValue(Key.SYS_BACKUP_BEFORE_DB_UPGRADE);
    }

    /**
     * Gets the {@link CircuitBreaker} instance.
     * <p>
     * Note: the instance is lazy created.
     * </p>
     *
     * @param breakerEnum
     *            The type of breaker.
     * @return he {@link CircuitBreaker} instance.
     */
    public static CircuitBreaker
            getCircuitBreaker(final CircuitBreakerEnum breakerEnum) {

        return instance().circuitBreakerRegistry.getOrCreateCircuitBreaker(
                breakerEnum.toString(), breakerEnum.getFailureThreshHold(),
                breakerEnum.getMillisUntilRetry(),
                CIRCUIT_NON_TRIPPING_EXCEPTIONS, CIRCUIT_DAMAGING_EXCEPTIONS,
                breakerEnum.getBreakerListener());
    }

    /**
     * Tells if lazy insert of user is permitted when authenticated at WebApp
     * logon.
     *
     * @return <code>true</code>, if lazy insert is permitted,
     *         <code>false</code> if not.
     */
    public boolean isUserInsertLazyLogin() {
        return myConfigProp.getBoolean(IConfigProp.Key.USER_INSERT_LAZY_LOGIN);
    }

    /**
     * Tells if lazy insert of user is permitted when printing a job on a
     * trusted queue.
     * <p>
     * <b>Note</b>: lazy insert is NOT permitted when
     * {@link IConfigProp#AUTH_METHOD_V_NONE} is chosen as
     * {@link IConfigProp.Key#AUTH_METHOD} whatever the value of
     * {@link IConfigProp.Key#USER_INSERT_LAZY_PRINT} is.
     * </p>
     *
     * @return <code>true</code>, if lazy insert is permitted,
     *         <code>false</code> if not.
     */
    public boolean isUserInsertLazyPrint() {
        return !myConfigProp.getString(IConfigProp.Key.AUTH_METHOD)
                .equals(IConfigProp.AUTH_METHOD_V_NONE)
                && myConfigProp
                        .getBoolean(IConfigProp.Key.USER_INSERT_LAZY_PRINT);
    }

    /**
     *
     * @return
     */
    public EntityManagerFactory getEntityManagerFactory() {
        return myEmf;
    }

    /**
     * Gets the SavaPage PPD file.
     *
     * @return The PPD file.
     */
    public static File getPpdFile() {
        return new File(
                getClientHome() + File.separator + REL_PATH_SAVAPAGE_PPD_FILE);
    }

    /**
     * Dynamically gets the {@link SystemPropertyEnum#SAVAPAGE_SERVER_HOME}
     * system property as passed to the JVM or set internally.
     * <p>
     * IMPORTANT: do NOT cache the value because we want to have the freedom to
     * change the system property on runtime.
     * </p>
     *
     * @return
     */
    public static String getServerHome() {
        return SystemPropertyEnum.SAVAPAGE_SERVER_HOME.getValue();
    }

    /**
     * @return The {@link SystemPropertyEnum#SAVAPAGE_CLIENT_HOME} system
     *         property as passed to the JVM or set internally. If the property
     *         is not found the "../client" path relative to
     *         {@link #getServerHome()} is returned.
     */
    public static String getClientHome() {
        String clientHome = SystemPropertyEnum.SAVAPAGE_CLIENT_HOME.getValue();
        if (clientHome == null) {
            clientHome = String.format("%s/../client", getServerHome());
        }
        return clientHome;
    }

    /**
     * @return The SafePages home path.
     */
    public static String getSafePagesHomeDir() {

        String homeSafePages = null;

        if (theServerProps != null) {
            homeSafePages =
                    theServerProps.getProperty(SERVER_PROP_APP_DIR_SAFEPAGES);
        }

        if (homeSafePages == null) {
            homeSafePages =
                    String.format("%s%c%s", getServerHome(), File.separatorChar,
                            ServerPathEnum.SAFEPAGES_DEFAULT.getPath());
        }

        return homeSafePages;
    }

    /**
     * @return Depth of a user home directory relative to root of all home
     *         directories.
     */
    public static int getUserHomeDepthFromRoot() {
        return USER_HOME_DEPTH_FROM_ROOT;
    }

    /**
     * Returns the location where the user's SafePages are stored.
     * <p>
     * The SafePages home of all users defaults to the
     * {@link #SERVER_REL_PATH_SAFEPAGES_DEFAULT} relative to $(server.home).
     * Each user's home is a subdirectory in this location with path
     * {@code x/y/user} where {@code x} and {@code y} are the first characters
     * of the md5sum of the {@code user}. See
     * {@link #USER_HOME_DEPTH_FROM_ROOT}.
     * </p>
     *
     * @param user
     *            The user id.
     * @return The directory with the user's SafePages.
     */
    public static String getUserHomeDir(final String user) {

        final String homeSafePages = getSafePagesHomeDir();
        final String md5hex = DigestUtils.md5Hex(user).toLowerCase();

        return String.format("%s%c%c%c%c%c%s", homeSafePages,
                File.separatorChar, md5hex.charAt(0), File.separatorChar,
                md5hex.charAt(1), File.separatorChar, user);
    }

    /**
     *
     * @return
     */
    public static String getDefaultCupsHost() {
        return DEFAULT_CUPS_HOST;
    }

    /**
     *
     * @return
     */
    public static String getCupsPort() {
        if (theServerProps == null) {
            return DEFAULT_CUPS_PORT;
        }
        return theServerProps.getProperty(SERVER_PROP_CUPS_SERVER_PORT,
                DEFAULT_CUPS_PORT);
    }

    /**
     * @return
     */
    public static boolean isCleanUpDocLogAtStart() {
        return theServerProps != null && BooleanUtils.toBooleanObject(
                theServerProps.getProperty(SERVER_PROP_START_CLEANUP_DOCLOG,
                        Boolean.TRUE.toString()))
                .booleanValue();
    }

    /**
     * @return
     */
    public static boolean isCleanUpUserHomeAtStart() {
        return theServerProps != null && BooleanUtils.toBooleanObject(
                theServerProps.getProperty(SERVER_PROP_START_CLEANUP_USERHOME,
                        Boolean.TRUE.toString()))
                .booleanValue();
    }

    /**
     * @return
     */
    public static boolean isCleanUpUserHomeTest() {
        return theServerProps != null
                && BooleanUtils.toBooleanObject(theServerProps.getProperty(
                        SERVER_PROP_SYSTEM_CLEANUP_USERHOME_TEST,
                        Boolean.FALSE.toString())).booleanValue();
    }

    /**
     *
     * @return
     */
    public static boolean isCleanUpDocStoreAtStart() {
        return isCleanUpDocLogAtStart();
    }

    /**
     * @return IPP printer UUID.
     */
    public static String getIppPrinterUuid() {
        return instance().getConfigValue(Key.IPP_PRINTER_ATTR_PRINTER_UUID);
    }

    /**
     * Gets the password of the database user.
     *
     * @return the password or an empty string when not specified or not found.
     */
    public static String getDbUserPassword() {
        if (theServerProps == null) {
            return "";
        }
        return theServerProps.getProperty(SERVER_PROP_DB_PASS, "");
    }

    /**
     * @return Location where the public letterheads are stored.
     */
    public static String getLetterheadDir() {

        String dir =
                theServerProps.getProperty(SERVER_PROP_APP_DIR_LETTERHEADS);
        if (dir == null) {
            dir = Paths
                    .get(getServerHome(), ServerPathEnum.LETTERHEADS.getPath())
                    .toString();
        }
        return dir;
    }

    /**
     * Loads the server properties from file {@link #FILENAME_SERVER_PROPERTIES}
     * .
     *
     * @return The {@link Properties}.
     * @throws IOException
     *             When error loading properties file.
     */
    public static Properties loadServerProperties() throws IOException {

        Properties serverProps = null;

        final String path = ConfigManager.getServerHome() + "/"
                + FILENAME_SERVER_PROPERTIES;

        try (FileInputStream fis = new FileInputStream(path);) {

            serverProps = new Properties();
            serverProps.load(fis);

            return serverProps;
        }
    }

    /**
     *
     * @return
     */
    public static String getDbBackupHome() {
        return ConfigManager.getServerHome() + "/data/backups";
    }

    /**
     * @return
     */
    public static File getDbScriptDir() {
        return Paths.get(ConfigManager.getServerHome(), "lib", "sql",
                instance().myDatabaseType.getScriptSubdir()).toFile();
    }

    /**
     *
     * @return
     */
    public static String getServerBinHome() {
        final StringBuilder home = new StringBuilder();
        home.append(getServerHome()).append("/bin/linux-");
        if (isOsArch64Bit()) {
            home.append("x64");
        } else {
            home.append("i686");
        }
        return home.toString();
    }

    /**
     *
     * @return The directory path with the print jobtickets.
     */
    public static Path getJobTicketsHome() {
        return Paths.get(getServerHome(),
                ServerPathEnum.PRINT_JOBTICKETS.getPath());
    }

    /**
     * @param store
     *            The store.
     * @return The directory path of the document store.
     */
    public static Path getDocStoreHome(final DocStoreTypeEnum store) {

        final ServerPathEnum serverPath;
        final String serverPathProp;

        switch (store) {
        case ARCHIVE:
            serverPath = ServerPathEnum.DOC_ARCHIVE;
            serverPathProp = theServerProps
                    .getProperty(SERVER_PROP_APP_DIR_DOC_STORE_ARCHIVE);
            break;
        case JOURNAL:
            serverPath = ServerPathEnum.DOC_JOURNAL;
            serverPathProp = theServerProps
                    .getProperty(SERVER_PROP_APP_DIR_DOC_STORE_JOURNAL);
            break;
        default:
            throw new UnknownError(store.toString());
        }

        if (serverPathProp != null) {
            return Paths.get(serverPathProp);
        }
        return Paths.get(getServerHome(), serverPath.getPath());
    }

    /**
     *
     * @return The directory path with the Admin Atom Feeds.
     */
    public static Path getAtomFeedsHome() {
        return Paths.get(getServerHome(), ServerPathEnum.FEEDS.getPath());
    }

    /**
     *
     * @throws IOException
     */
    private void lazyCreateJobTicketsHome() throws IOException {

        synchronized (this.createJobTicketsHomeMutex) {

            final Set<PosixFilePermission> permissions =
                    EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE);

            final FileAttribute<Set<PosixFilePermission>> fileAttributes =
                    PosixFilePermissions.asFileAttribute(permissions);

            Files.createDirectories(getJobTicketsHome(), fileAttributes);
        }
    }

    /**
     *
     * @return The directory with the server extension (plug-in) property files.
     */
    public static File getServerExtHome() {
        return new File(String.format("%s/ext", getServerHome()));
    }

    /**
     * @return The directory with the custom template files.
     */
    public static File getServerCustomTemplateHome() {
        return new File(String.format("%s%c%s", getServerHome(),
                File.separatorChar, ServerPathEnum.CUSTOM_TEMPLATE.getPath()));
    }

    /**
     * @return The directory with the custom Email template files.
     */
    public static File getServerCustomEmailTemplateHome() {

        final File customHome = getServerCustomTemplateHome();

        String subPath =
                instance().getConfigValue(Key.CUSTOM_TEMPLATE_HOME_MAIL);

        if (StringUtils.isBlank(subPath)) {
            subPath = instance().getConfigValue(Key.CUSTOM_TEMPLATE_HOME);
        }

        if (StringUtils.isBlank(subPath)) {
            return customHome;
        }

        return new File(String.format("%s%c%s", customHome.getAbsolutePath(),
                File.separatorChar, subPath));
    }

    /**
     * @return The directory with the custom CUPS files.
     */
    public static File getServerCustomCupsHome() {
        return new File(String.format("%s%c%s", getServerHome(),
                File.separatorChar, ServerPathEnum.CUSTOM_CUPS.getPath()));
    }

    /**
     * @return The directory with the custom CUPS i18n files.
     */
    public static File getServerCustomCupsI18nHome() {
        return new File(String.format("%s%c%s", getServerHome(),
                File.separatorChar, ServerPathEnum.CUSTOM_CUPS_I18N.getPath()));
    }

    /**
     * Gets the location of a custom resource for Wicket container class.
     *
     * @param clazz
     *            The class of the Wicket component.
     * @return The directory with the custom i18n files.
     */
    public static File getServerCustomI18nHome(final Class<?> clazz) {

        return Paths.get(getServerHome(), ServerPathEnum.CUSTOM_I18N.getPath(),
                StringUtils.replace(clazz.getPackage().getName(), ".",
                        File.separator))
                .toFile();
    }

    /**
     * @return The directory with the custom HTML injectable files.
     */
    public static File getServerCustomHtmlHome() {
        return new File(String.format("%s%c%s", getServerHome(),
                File.separatorChar, ServerPathEnum.CUSTOM_HTML.getPath()));
    }

    /**
     *
     * @return
     */
    public static boolean isOsArch64Bit() {
        return SystemPropertyEnum.OS_ARCH.getValue().indexOf("64") != -1;
    }

    /**
     *
     * @return
     */
    public static String getProcessUserName() {
        return SystemPropertyEnum.USER_NAME.getValue();
    }

    /**
     * TODO : how to get the real name
     *
     * @return
     */
    public static String getProcessGroupName() {
        return getProcessUserName();
    }

    /**
     * Returns "SavaPage Major.Minor.Revision"
     *
     * @return
     */
    public static String getAppNameVersion() {
        return String.format("%s %s", getAppName(), getAppVersion());
    }

    /**
     * Returns "SavaPage Major.Minor.Revision (Build)"
     *
     * @return
     */
    public static String getAppNameVersionBuild() {
        return String.format("%s %s", getAppName(), getAppVersionBuild());
    }

    /**
     * The single word (tm) application name ("SavaPage").
     *
     * @return
     */
    private static String getAppName() {
        return CommunityDictEnum.SAVAPAGE.getWord();
    }

    /**
     * The single word (c) application owner ("Datraverse B.V.").
     *
     * @return
     */
    public static String getAppOwner() {
        return APP_OWNER;
    }

    /**
     * @return "Major.Minor.RevisionStatus".
     */
    public static String getAppVersion() {
        return String.format("%s.%s.%s%s", VersionInfo.VERSION_A_MAJOR,
                VersionInfo.VERSION_B_MINOR, VersionInfo.VERSION_C_REVISION,
                VersionInfo.VERSION_D_STATUS);
    }

    /**
     * @return "Major.Minor.RevisionStatus (Build xxxx)".
     */
    public static String getAppVersionBuild() {
        return String.format("%s (Build %s)", getAppVersion(),
                VersionInfo.VERSION_E_BUILD);
    }

    /**
     * @return The {@link Currency} used in this application, or {@code null}
     *         when not defined.
     */
    public static Currency getAppCurrency() {

        final String currencyCode =
                instance().getConfigValue(Key.FINANCIAL_GLOBAL_CURRENCY_CODE);

        if (StringUtils.isBlank(currencyCode)) {
            return null;
        }

        return Currency.getInstance(currencyCode);
    }

    /**
     * @return The ISO currency code used in this application, or an empty
     *         string when not defined.
     */
    public static String getAppCurrencyCode() {

        final Currency currency = getAppCurrency();

        if (currency == null) {
            return "";
        }

        return currency.getCurrencyCode();
    }

    /**
     *
     * @param locale
     * @return
     */
    public static String getAppCurrencySymbol(final Locale locale) {
        final String symbol =
                CurrencyUtil.getCurrencySymbol(getAppCurrencyCode(), locale);
        return symbol;
    }

    /**
     * Calculates the runnable status of the configuration.
     *
     * @return {@code true} if runnable.
     */
    public boolean calcRunnable() {
        return myConfigProp.calcRunnable();
    }

    /**
     * @param props
     *            Server properties.
     */
    public static void setServerProps(final Properties props) {

        theServerProps = props;

        if (theServerProps != null
                && theServerProps.containsKey(SERVER_PROP_IPP_TRUST_IP_USER)) {

            final StringTokenizer st = new StringTokenizer(
                    theServerProps.getProperty(SERVER_PROP_IPP_TRUST_IP_USER));
            String ip = null;
            String user = null;
            while (st.hasMoreTokens()) {
                final String token = st.nextToken();
                if (ip == null) {
                    ip = token;
                } else if (user == null) {
                    user = token;
                }
                if (ip != null && user != null) {
                    theTrustedUsersByIP.put(ip, user);
                    ip = null;
                    user = null;
                }
            }
        }
    }

    /**
     *
     * @param remoteAddr
     *            Remote IP address.
     * @return Trusted user id, or {@code null} when not present.
     */
    public static String getTrustedUserByIP(final String remoteAddr) {
        return theTrustedUsersByIP.get(remoteAddr);
    }

    /**
     *
     * @param info
     *            JDBC information.
     */
    public static void putServerProps(final DbConfig.JdbcInfo info) {

        if (theServerProps == null) {
            theServerProps = new Properties();
        }
        if (info.getDriver() != null) {
            theServerProps.put(SERVER_PROP_DB_DRIVER, info.getDriver());
        }
        if (info.getUrl() != null) {
            theServerProps.put(SERVER_PROP_DB_URL, info.getUrl());
        }
    }

    /**
     *
     * @param info
     *            JDBC information.
     */
    public static void putServerProps(final DbConfig.HibernateInfo info) {

        if (theServerProps == null) {
            theServerProps = new Properties();
        }
        if (info.getDialect() != null) {
            theServerProps.put(SERVER_PROP_DB_HIBERNATE_DIALECT,
                    info.getDialect());
        }
    }

    /**
     * Sets the path of the User and Admin WebApp.
     * <p>
     * This method must be called after {@link #setServerProps(Properties)} .
     * </p>
     *
     * @param pathAdmin
     *            Path of the Admin WebApp.
     * @param pathUser
     *            Path of the User WebApp.
     * @param pathIppPrinterIcons
     *            The path of the IPP printer-icons.
     */
    public static void setWebAppPaths(final String pathAdmin,
            final String pathUser, final String pathIppPrinterIcons) {
        try {
            final String host = InetUtils.getServerHostAddress();
            final int port = Integer.valueOf(getServerSslPort()).intValue();
            theWebAppAdminSslUrl = new URL("https", host, port, pathAdmin);
            theWebAppUserSslUrl = new URL("https", host, port, pathUser);
            theIppPrinterIconsUrlPath = pathIppPrinterIcons;
        } catch (NumberFormatException | MalformedURLException
                | UnknownHostException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * @return The SSL {@link URL} of the Admin WebApp.
     */
    public static URL getWebAppAdminSslUrl() {
        return theWebAppAdminSslUrl;
    }

    /**
     * @return The SSL {@link URL} of the User WebApp.
     */
    public static URL getWebAppUserSslUrl() {
        return theWebAppUserSslUrl;
    }

    /**
     * @return URL Path of the IPP printer-icons.
     */
    public static String getIppPrinterIconsUrlPath() {
        return theIppPrinterIconsUrlPath;
    }

    /**
     *
     * @return
     */
    public static String getServerPort() {
        return getServerPort(theServerProps);
    }

    /**
     *
     * @return
     */
    public static String getServerSslPort() {
        return getServerSslPort(theServerProps);
    }

    /**
     *
     * @return
     */
    public static String getServerPort(final Properties props) {
        if (props == null) {
            return ConfigManager.theDefaultServerPort;
        }
        return props.getProperty("server.port",
                ConfigManager.theDefaultServerPort);
    }

    /**
     *
     * @return
     */
    public static String getServerSslPort(final Properties props) {
        return props.getProperty("server.ssl.port",
                ConfigManager.theDefaultServerSslPort);
    }

    /**
     *
     * @return
     */
    public static String getRawPrinterPort() {
        return theServerProps.getProperty(
                ConfigManager.SERVER_PROP_PRINTER_RAW_PORT,
                ConfigManager.PRINTER_RAW_PORT_DEFAULT);
    }

    /**
     * Gets the basename of the custom SavaPage CUPS notifier binary as present
     * in directory {@code /usr/lib/cups/notifier/} .
     *
     * @return the name of the binary.
     */
    public static String getCupsNotifier() {
        if (theServerProps == null) {
            return ConfigManager.DEFAULT_CUPS_NOTIFIER;
        }
        return theServerProps.getProperty(SERVER_PROP_CUPS_NOTIFIER,
                ConfigManager.DEFAULT_CUPS_NOTIFIER);
    }

    /**
     *
     * @return {@code true} if CUPS job status PUSH notification.
     */
    public static boolean isCupsPushNotification() {
        return instance().getConfigEnum(PullPushEnum.class,
                Key.CUPS_IPP_NOTIFICATION_METHOD) == PullPushEnum.PUSH;
    }

    /**
     * Initializes the core application depending on the {@link RunModeEnum}.
     * <p>
     * Additional initialization methods like {@link #initScheduler()} can be
     * called. The generic {@link #exit()} method takes care of closing down the
     * initialized components.
     * </p>
     * <p>
     * The caller is responsible for calling the {@link #exit()} method.
     * </p>
     *
     * @param mode
     *            The run-mode context of the application.
     * @param databaseTypeDefault
     *            The default database type.
     * @throws Exception
     */
    public synchronized void init(final RunModeEnum mode,
            final DatabaseTypeEnum databaseTypeDefault) throws Exception {

        if (runMode != null) {
            throw new SpException("application is already initialized");
        }

        createAppTmpDir();

        switch (mode) {
        case SERVER:
            initAsServer(new Properties());
            initOpenPGP();
            break;
        case LIB:
            initAsRunnableCoreLibrary(new Properties());
            break;
        case CORE:
            initAsCoreLibrary(databaseTypeDefault, new Properties());
            break;
        default:
            throw new SpException("mode [" + mode + "] is not supported");
        }

        runMode = mode;
    }

    /**
     * Initializes the PGP secret key.
     */
    private void initOpenPGP() {

        if (theServerProps == null) {
            return;
        }

        final String secretFileName =
                theServerProps.getProperty(SERVER_PROP_OPENPGP_SECRETKEY_FILE);

        if (secretFileName == null) {
            return;
        }

        final String publicFileName =
                theServerProps.getProperty(SERVER_PROP_OPENPGP_PUBLICKEY_FILE);

        if (publicFileName == null) {
            LOGGER.warn("OpenPGP public key file not configured.");
            return;
        }

        final File secretFile = Paths.get(getServerHome(),
                ServerPathEnum.DATA.getPath(), secretFileName).toFile();

        final File publicFile = Paths.get(getServerHome(),
                ServerPathEnum.DATA.getPath(), publicFileName).toFile();

        final PGPHelper helper = PGPHelper.instance();

        try {

            this.pgpPublicKeyInfo =
                    helper.readPublicKey(new FileInputStream(publicFile));

            this.pgpSecretKeyInfo = helper.readSecretKey(
                    new FileInputStream(secretFile), theServerProps.getProperty(
                            SERVER_PROP_OPENPGP_SECRETKEY_PASSPHRASE));

            SpInfo.instance().log(String.format("OpenPGP Key ID [%s]",
                    this.pgpSecretKeyInfo.formattedKeyID()));

            for (final InternetAddress addr : this.pgpSecretKeyInfo.getUids()) {
                SpInfo.instance().log(
                        String.format("OpenPGP UID [%s]", addr.toString()));
            }

            SpInfo.instance().log(String.format("OpenPGP Fingerprint [%s]",
                    this.pgpSecretKeyInfo.formattedFingerPrint()));

        } catch (FileNotFoundException e) {
            LOGGER.error("{}: {} not found.",
                    SERVER_PROP_OPENPGP_SECRETKEY_FILE,
                    secretFile.getAbsolutePath());
        } catch (PGPBaseException e) {
            LOGGER.error("{} is invalid.",
                    SERVER_PROP_OPENPGP_SECRETKEY_PASSPHRASE);
        }

        try {
            // Elicit an exception when URLs is wrong.
            this.getPGPPublicKeyServerUrl();
        } catch (MalformedURLException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Gets the URL of PGP Public Key Server.
     *
     * @return The URL, or {@code null} when unknown.
     * @throws MalformedURLException
     *             If URL template is ill-formed.
     */
    public URL getPGPPublicKeyServerUrl() throws MalformedURLException {

        final String value = this.getConfigValue(Key.PGP_PKS_URL);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return new URL(value);
    }

    /**
     * @return Configuration categories to setup for status "ready-to-use".
     */
    public EnumSet<SetupNeededEnum> getReadyToUseSetupNeeded() {
        final EnumSet<SetupNeededEnum> es =
                EnumSet.noneOf(SetupNeededEnum.class);

        if (!this.isValidValue(Key.MAIL_FROM_ADDRESS)) {
            es.add(SetupNeededEnum.MAIL);
        }
        if (!this.isValidValue(Key.FINANCIAL_GLOBAL_CURRENCY_CODE)) {
            es.add(SetupNeededEnum.CURRENCY);
        }
        return es;
    }

    /**
     * Checks whether the application is read-to-use.
     *
     * @return {@code true} when read-to-use.
     */
    private boolean isAppReadyToUse() {
        return myConfigProp.isRunnable();
    }

    /**
     * @return {@code true} when application is temporarily unavailable.
     */
    private static boolean isTempUnavailable() {

        boolean acquired = false;

        try {
            ReadWriteLockEnum.DATABASE_READONLY.tryReadLock();
            acquired = true;
        } catch (ReadLockObtainFailedException e) {
            acquired = false;
        } finally {
            if (acquired) {
                ReadWriteLockEnum.DATABASE_READONLY.setReadLock(false);
            }
        }
        return !acquired;
    }

    /**
     * Gets the system status.
     *
     * @return The {@link SystemStatusEnum}.
     */
    public SystemStatusEnum getSystemStatus() {

        final SystemStatusEnum stat;

        if (!this.isAppReadyToUse()) {
            stat = SystemStatusEnum.SETUP;
        } else if (isSysMaintenance()) {
            stat = SystemStatusEnum.MAINTENANCE;
        } else if (isTempUnavailable()) {
            stat = SystemStatusEnum.UNAVAILABLE;
        } else {
            stat = SystemStatusEnum.READY;
        }
        return stat;
    }

    /**
     * Checks whether the value for a {@link Prop} key is valid.
     *
     * @param key
     *            The property key.
     * @param value
     *            The value.
     * @return The {@link ValidationResult}.
     */
    public ValidationResult validate(final Key key, final String value) {
        return myConfigProp.validate(key, value);
    }

    /**
     * Checks whether the current value for a {@link Prop} key is valid.
     *
     * @param key
     *            The property key.
     * @return {@code true} if valid.
     */
    public boolean isValidValue(final Key key) {
        return this.validate(key, this.getConfigValue(key)).isValid();
    }

    /**
     *
     * @throws MalformedObjectNameException
     * @throws InstanceAlreadyExistsException
     * @throws MBeanRegistrationException
     * @throws NotCompliantMBeanException
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     * @throws UnrecoverableKeyException
     * @throws KeyManagementException
     */
    private void initJmx()
            throws MalformedObjectNameException, InstanceAlreadyExistsException,
            MBeanRegistrationException, NotCompliantMBeanException,
            KeyStoreException, NoSuchAlgorithmException, CertificateException,
            IOException, UnrecoverableKeyException, KeyManagementException {

        /*
         * Get the MBean server.
         */
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        /*
         * Register the MBean(s)
         */
        final CoreConfig mBean = new CoreConfig();

        final ObjectName name = new ObjectName("org.savapage:type=Core");

        mbs.registerMBean(mBean, name);
    }

    /**
     * Fully initializes the core application for a Web Server context.
     * <p>
     * The job scheduler is NOT initialized, this should be done by the (server)
     * application.
     * </p>
     *
     * @see #initScheduler()
     * @see #exitScheduler()
     * @see #exit()
     *
     * @param props
     *            The core properties.
     * @throws Exception
     */
    private void initAsServer(Properties props) throws Exception {

        if (runMode != null) {
            return;
        }

        myConfigProp.init(props);

        CryptoUser.init();

        this.initJmx();

        this.lazyCreateJobTicketsHome();

        /*
         * Bootstrap Hibernate.
         */
        initHibernate(null);

        SpInfo.instance().logSignature(this.getDbVersionInfo());

        /*
         * Now Hibernate is up we can open the context.
         */
        ServiceContext.open();

        ServiceContext.getDaoContext().beginTransaction();

        boolean committed = false;

        try {

            /*
             * initRunnable() DOES use Database access, but we need this method
             * to get the current schemaVersion from the database.
             */
            myConfigProp.initRunnable();

            setDefaultLocale(getConfigValue(Key.SYS_DEFAULT_LOCALE).trim());

            DbUpgManager.instance().check();

            /*
             * Database access can start from here...
             */
            if (myConfigProp.isRunnable()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("configuration is ready to run");
                }
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("configuration is NOT ready to run: "
                            + "administration needed");
                }
            }

            /*
             *
             */
            loadAdminProperties();

            final int nAliases = initUserNameAliasList();

            if (nAliases > 0) {
                SpInfo.instance().log(
                        String.format("Read [%d] User Aliases.", nAliases));
            }

            this.myPrintProxy =
                    ServiceContext.getServiceFactory().getProxyPrintService();

            this.myPrintProxy.init();

            /*
             *
             */
            ServiceContext.getServiceFactory().getUserGroupService()
                    .lazyCreateReservedGroups();

            ServiceContext.getServiceFactory().getQueueService()
                    .lazyCreateReservedQueues();

            /*
             *
             */
            Runtime.getRuntime().addShutdownHook(new CoreShutdownHook(this));

            /*
             * Last statement
             */
            ServiceContext.getDaoContext().commit();
            committed = true;

        } finally {

            if (!committed) {
                ServiceContext.getDaoContext().rollback();
            }
            /*
             * Do NOT close the context here, this has to be done by the caller.
             */
        }

        // Logging only.
        if (this.dbConnectionPoolProps != null) {
            for (final Entry<DbConnectionPoolEnum, String> entry : //
            this.dbConnectionPoolProps.entrySet()) {
                final DbConnectionPoolEnum key = entry.getKey();
                SpInfo.instance()
                        .log(String.format("%s > %s [%s]", key.getConfigKey(),
                                key.getC3p0Key(), entry.getValue()));
            }
        }

        // Fill cache.
        SpInfo.instance()
                .log(String.format(
                        "PDF Standard Fonts: [%s] substitutes retrieved.",
                        PdfDocumentFonts.Font.getStandardFontSubst().size()));

        //
        ServiceContext.getServiceFactory().start();

        ServiceContext.getServiceFactory().getSOfficeService()
                .start(new SOfficeConfigProps());

        ProxyPrintJobStatusMonitor.init();

        //
        if (GoogleLdapClient.init()) {
            SpInfo.instance().log(GoogleLdapClient.getCertCreateDateLogLine());
            SpInfo.instance().log(GoogleLdapClient.getCertExpireDateLogLine());
        }

        //
        SystemInfo.init();

        //
        DbTools.checkSequences();
    }

    /**
     * Initialize as a core library (without a fully functional database), so
     * basic operations can be performed. Followship operations are not allowed.
     *
     * @param databaseTypeDefault
     *            The default database type.
     * @param props
     *            The properties.
     * @throws IOException
     *             When IO errors.
     */
    private void initAsCoreLibrary(final DatabaseTypeEnum databaseTypeDefault,
            final Properties props) throws IOException {

        initHibernate(databaseTypeDefault);
        myConfigProp.init(props);
    }

    /**
     * Initialize as a runnable core library (with a fully functional database),
     * so basic operations can be performed. Followship operations are not
     * allowed.
     *
     * @param props
     * @throws IOException
     */
    private void initAsRunnableCoreLibrary(Properties props)
            throws IOException {

        initAsCoreLibrary(null, props);

        final EntityManager em = DaoContextImpl.peekEntityManager();

        em.getTransaction().begin();
        boolean committed = false;

        try {

            /*
             * initRunnable() DOES use Database access, but we need this method
             * to get the current schemaVersion from the database.
             */
            myConfigProp.initRunnable();

            /*
             * TODO: ServiceContext for the transaction MUST be used before lazy
             * create has any effect.
             */

            // ServiceContext.getServiceFactory().getUserGroupService()
            // .lazyCreateReservedGroups();

            /*
             * Last statement
             */
            em.getTransaction().commit();
            committed = true;

        } finally {

            if (!committed) {
                em.getTransaction().rollback();
            }
        }
    }

    /**
     * Reads the properties files for the administrator. When a "raw" admin
     * password is encountered, the properties file is updated with the HASH
     * checksum of the password.
     */
    private synchronized void loadAdminProperties() {

        final String pathProps = getAdminPropPath();

        myPropsAdmin = new Properties();
        try {
            myPropsAdmin.load(new java.io.FileInputStream(pathProps));
        } catch (IOException e) {
            throw new SpException(pathProps + " is missing.", e);
        }

        String pw = myPropsAdmin.getProperty(PROP_INTERNAL_ADMIN_PASSWORD);

        if (!pw.startsWith(CryptoUser.INTERNAL_USER_PW_CHECKSUM_PREFIX)) {
            setInternalAdminPassword(pw);
        }
    }

    /**
     * Gets the full file path of the admin.properties.
     *
     * @return The file path.
     */
    public static String getAdminPropPath() {
        return getServerHome() + "/admin.properties";
    }

    /**
     * Sets the default locale if specified, and different from the current
     * default.
     *
     * @param languageTag
     *            For example: nl-NL, en, ...
     */
    public static void setDefaultLocale(final String languageTag) {
        if (StringUtils.isNotBlank(languageTag)) {
            if (!languageTag.equals(Locale.getDefault().toLanguageTag())) {
                Locale.setDefault(new Locale.Builder()
                        .setLanguageTag(languageTag).build());
            }
        } else {
            Locale.setDefault(getServerHostLocale());
        }
    }

    /**
     * Sets the password of the internal administrator.
     *
     * @param plainPassword
     *            The plain password as entered by the user.
     */
    public void setInternalAdminPassword(final String plainPassword) {

        final String pw = CryptoUser.getHashedUserPassword(
                UserDao.INTERNAL_ADMIN_USERID, plainPassword);

        myPropsAdmin.setProperty(PROP_INTERNAL_ADMIN_PASSWORD, pw);

        try {
            myPropsAdmin.store(new FileOutputStream(getAdminPropPath()),
                    "The admin password can be changed here. "
                            + "SavaPage will convert it to a hash.");
        } catch (IOException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Initializes the job scheduler.
     * <p>
     * The {@link #exitScheduler()} is executed in the generic {@link #exit()}
     * method.
     * </p>
     */
    public void initScheduler() {
        SpJobScheduler.instance().init();
    }

    /**
     * Stops the job scheduler.
     */
    private void exitScheduler() {
        SpJobScheduler.instance().shutdown();
    }

    /**
     * Loads username aliases from file.
     *
     * @return Number of aliases.
     * @throws IOException
     *             When IO errors reading the list.
     */
    public static int initUserNameAliasList() throws IOException {
        return UserAliasList.instance()
                .load(new File(String.format("%s%c%s%c%s", getServerHome(),
                        File.separatorChar, ServerPathEnum.DATA_CONF.getPath(),
                        File.separatorChar,
                        SERVER_DATA_CONF_USERNAME_ALIASES_TXT)));
    }

    /**
     * Updates the string value of a configuration key in the database and the
     * internal cache. When key represents a user encrypted value, the value is
     * stored encrypted.
     * <p>
     * NOTE: the key MUST exist in the cache, if not, an exception is thrown.
     * </p>
     *
     * @param key
     *            The key as enum.
     * @param value
     *            The string value.
     * @param actor
     *            The actor.
     */
    public void updateConfigKey(final Key key, final String value,
            final String actor) {

        final String valUpdate;

        if (StringUtils.isNotBlank(value) && isUserEncrypted(key)) {
            valUpdate = CryptoUser.encrypt(value);
        } else {
            valUpdate = value;
        }

        myConfigProp.updateValue(key, valUpdate, actor);
    }

    /**
     * @see {@link #updateConfigKey(Key, String, String).
     */
    public void updateConfigKey(final Key key, final boolean value,
            final String actor) {
        updateConfigKey(key, value ? IConfigProp.V_YES : IConfigProp.V_NO,
                actor);
    }

    /**
     * See: {@link #updateConfigKey(Key, String, String)
     *
     */
    public void updateConfigKey(final Key key, final Long value,
            final String actor) {
        updateConfigKey(key, value.toString(), actor);
    }

    /**
     * Saves (updates or lazy inserts) the string value of a configuration key
     * in the <b>database</b> and updates the internal cache. When key
     * represents a user encrypted value, the value is encrypted.
     * <p>
     * NOTE: the key NEED NOT exist in the cache.
     * </p>
     *
     * @param key
     *            The key as enum.
     * @param value
     *            The string value.
     * @param actor
     *            The actor.
     */
    public void saveDbConfigKey(final Key key, final String value,
            final String actor) {

        String val = value;

        if (isUserEncrypted(key)) {
            val = CryptoUser.encrypt(value);
        }

        myConfigProp.saveDbValue(key, val, actor);

    }

    /**
     * Reads the string value of a configuration key from the <b>database</b>
     * bypassing the internal cache. When key value is encrypted it is returned
     * decrypted.
     *
     * @param key
     *            The key as enum.
     * @return {@code null} when the key does NOT exist in the database.
     */
    public String readDbConfigKey(final Key key) {

        String val = null;

        final ConfigProperty prop = ServiceContext.getDaoContext()
                .getConfigPropertyDao().findByName(getConfigKey(key));

        if (prop != null) {
            // Mantis #1105
            val = StringUtils.defaultString(prop.getValue());
            if (isUserEncrypted(key)) {
                val = CryptoUser.decrypt(val);
            }
        }
        return val;
    }

    /**
     * Checks if Printer is non-secure according to system configuration
     * settings {@link IConfigProp.Key#PROXY_PRINT_NON_SECURE} and
     * {@link IConfigProp.Key#PROXY_PRINT_NON_SECURE_PRINTER_GROUP}.
     * <p>
     * IMPORTANT: This method does NOT check printers and printer groups in
     * {@link Device} objects.
     * </p>
     *
     * @param printer
     *            The printer to check.
     * @return {@code true} if printer is non-secure, i.e. available for DIRECT
     *         print.
     */
    public boolean isNonSecureProxyPrinter(final Printer printer) {

        boolean isNonSecure = false;

        ConfigManager cm = ConfigManager.instance();

        if (cm.isConfigValue(Key.PROXY_PRINT_NON_SECURE)) {

            final String groupName =
                    cm.getConfigValue(Key.PROXY_PRINT_NON_SECURE_PRINTER_GROUP);

            if (StringUtils.isBlank(groupName)) {

                isNonSecure = true;

            } else {

                final PrinterGroup group = ServiceContext.getDaoContext()
                        .getPrinterGroupDao().findByName(groupName);

                if (group == null) {
                    isNonSecure = true;
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Printer Group [" + groupName
                                + "] is NOT found.");
                    }
                } else {

                    final PrinterService printerService = ServiceContext
                            .getServiceFactory().getPrinterService();

                    if (printerService.isPrinterGroupMember(group, printer)) {
                        isNonSecure = true;
                    }
                }
            }
        }
        return isNonSecure;
    }

    /**
     *
     * @return
     */
    public static int getUserBalanceDecimals() {
        return instance().getConfigInt(Key.FINANCIAL_USER_BALANCE_DECIMALS);
    }

    /**
     *
     * @return
     */
    public static int getPrinterCostDecimals() {
        return instance().getConfigInt(Key.FINANCIAL_PRINTER_COST_DECIMALS);
    }

    /**
     *
     * @return
     */
    public static int getFinancialDecimalsInDatabase() {
        return IConfigProp.MAX_FINANCIAL_DECIMALS_IN_DB;
    }

    /**
     * Is value of this key user encrypted?
     *
     * @param key
     * @return
     */
    public boolean isUserEncrypted(final IConfigProp.Key key) {
        return key == Key.AUTH_LDAP_ADMIN_PASSWORD
                || key == Key.API_JSONRPC_SECRET_KEY
                || key == Key.API_RESTFUL_AUTH_PASSWORD
                || key == Key.CLIAPP_AUTH_ADMIN_PASSKEY
                || key == Key.FEED_ATOM_ADMIN_PASSWORD
                || key == Key.EXT_PAPERCUT_USER_SYNC_PASSWORD
                || key == Key.MAIL_SMTP_PASSWORD
                || key == Key.PAPERCUT_DB_PASSWORD
                || key == Key.PAPERCUT_SERVER_AUTH_TOKEN
                || key == Key.PRINT_IMAP_PASSWORD
                || key == Key.WEB_LOGIN_TTP_API_KEY
                || key == Key.AUTH_MODE_YUBIKEY_API_SECRET_KEY
                || key == Key.EXT_TELEGRAM_BOT_TOKEN;
    }

    /**
     *
     */
    public String getConfigKey(final IConfigProp.Key key) {
        return myConfigProp.getKey(key);
    }

    /**
     * Gets the enum of the configuration key.
     *
     * @param key
     *            The string representation of the key.
     * @return The enum representation of the key, or {@code null} when the key
     *         is not found.
     */
    public IConfigProp.Key getConfigKey(final String key) {
        return myConfigProp.getKey(key);
    }

    /**
     *
     */
    public IConfigProp.LdapTypeEnum getConfigLdapType() {

        final String schema = myConfigProp.getString(Key.LDAP_SCHEMA_TYPE);

        if (schema.equals(IConfigProp.LDAP_TYPE_V_ACTIV)) {
            return IConfigProp.LdapTypeEnum.ACTD;
        } else if (schema.equals(IConfigProp.LDAP_TYPE_V_E_DIR)) {
            return IConfigProp.LdapTypeEnum.EDIR;
        } else if (schema.equals(IConfigProp.LDAP_TYPE_V_OPEN_LDAP)) {
            return IConfigProp.LdapTypeEnum.OPEN_LDAP;
        } else if (schema.equals(IConfigProp.LDAP_TYPE_V_FREE_IPA)) {
            return IConfigProp.LdapTypeEnum.FREE_IPA;
        } else if (schema.equals(IConfigProp.LDAP_TYPE_V_GOOGLE_CLOUD)) {
            return IConfigProp.LdapTypeEnum.GOOGLE_CLOUD;
        }
        return IConfigProp.LdapTypeEnum.OPEN_DIR;
    }

    /**
     * Gets the config value. When key value is encrypted the decrypted key
     * value is returned. See {@link #isUserEncrypted(Key)}.
     *
     * @param key
     *            The {@link Key}.
     * @return <code>null</code> when property is not found. In case of an
     *         encrypted value, an empty string is returned when decryption
     *         failed.
     */
    public String getConfigValue(final IConfigProp.Key key) {
        String val = myConfigProp.getString(key);
        if (isUserEncrypted(key)) {
            try {
                val = CryptoUser.decrypt(val);
            } catch (SpException e) {
                /*
                 * Be forgiving...
                 */
                val = "";
            }
        }
        return val;
    }

    /**
     * Gets the enum config value.
     *
     * @param enumClass
     *            The enum class.
     * @param key
     *            The {@link Key}.
     * @param <E>
     *            The enum type.
     * @return The enum, or {@code null} when not found.
     */
    public <E extends Enum<E>> E getConfigEnum(final Class<E> enumClass,
            final IConfigProp.Key key) {
        return EnumUtils.getEnum(enumClass, this.getConfigValue(key));
    }

    /**
     * Gets the enum set config value.
     *
     * @param enumClass
     *            The enum class.
     * @param key
     *            The {@link Key}.
     * @param <E>
     *            The enum type.
     * @return The enum set (can be empty).
     */
    public <E extends Enum<E>> EnumSet<E> getConfigEnumSet(
            final Class<E> enumClass, final IConfigProp.Key key) {
        return EnumSetValidator.getEnumSet(enumClass, this.getConfigValue(key));
    }

    /**
     * Gets the enum list config value (in order of appearance).
     *
     * @param enumClass
     *            The enum class.
     * @param key
     *            The {@link Key}.
     * @param <E>
     *            The enum type.
     * @return The enum list (can be empty).
     */
    public <E extends Enum<E>> List<E> getConfigEnumList(
            final Class<E> enumClass, final IConfigProp.Key key) {
        return EnumSetValidator.getEnumList(enumClass,
                this.getConfigValue(key));
    }

    /**
     * Gets the {@link InternalFontFamilyEnum} of a config key.
     *
     * @param key
     *            The {@link IConfigProp.Key}.
     * @return {@link IConfigProp#DEFAULT_INTERNAL_FONT_FAMILY} when or invalid
     *         or not found.
     */
    public static InternalFontFamilyEnum
            getConfigFontFamily(final IConfigProp.Key key) {

        InternalFontFamilyEnum font = IConfigProp.DEFAULT_INTERNAL_FONT_FAMILY;

        final String enumString = instance().getConfigValue(key);

        if (EnumUtils.isValidEnum(InternalFontFamilyEnum.class, enumString)) {
            font = InternalFontFamilyEnum.valueOf(enumString);
        }
        return font;
    }

    /**
     * Gets the value of an LDAP configuration key as string.
     *
     * @param ldapType
     *            The LDAP type.
     * @param key
     *            The key of the property.
     * @return <code>null</code> when property is not found.
     */
    public String getConfigValue(final IConfigProp.LdapTypeEnum ldapType,
            final IConfigProp.Key key) {
        return myConfigProp.getString(ldapType, key);
    }

    /**
     * Gets the value of an LDAP configuration key as Boolean.
     *
     * @param ldapType
     *            The LDAP type.
     * @param key
     *            The key of the property.
     * @return <code>null</code> when property is not found.
     */
    public Boolean isConfigValue(final IConfigProp.LdapTypeEnum ldapType,
            final IConfigProp.Key key) {
        return myConfigProp.getBoolean(ldapType, key);
    }

    /**
     * Does this configuration item represent multi-line text?
     *
     * @param key
     *            The config key.
     * @return {@code true} if multi-line text.
     */
    public boolean isConfigMultiline(Key key) {
        return myConfigProp.isMultiLine(key);
    }

    /**
     *
     * @param key
     * @return
     */
    public static String[] getConfigMultiline(Key key) {
        if (instance().isConfigMultiline(key)) {
            return StringUtils.splitPreserveAllTokens(
                    instance().getConfigValue(key).replace("\r\n", "\n"), '\n');
        } else {
            return new String[0];
        }
    }

    /**
     * Does this configuration item represent {@link BigDecimal} text?
     *
     * @param key
     *            The config key.
     * @return {@code true} if {@link BigDecimal} text.
     */
    public boolean isConfigBigDecimal(Key key) {
        return myConfigProp.isBigDecimal(key);
    }

    /**
     *
     * @param key
     *            The config key.
     * @return {@code null} when property is not found.
     */
    public long getConfigLong(final IConfigProp.Key key) {
        return myConfigProp.getLong(key);
    }

    /**
     *
     * @param key
     *            The key.
     * @param dfault
     *            The default value when property is not found.
     * @return The value.
     */
    public long getConfigLong(final IConfigProp.Key key, final long dfault) {
        final String value = getConfigValue(key);
        if (StringUtils.isBlank(value)) {
            return dfault;
        }
        return Long.parseLong(value);
    }

    /**
     * If the key is not present an empty {@link Set} is returned.
     *
     * @param key
     *            The config key.
     * @return
     */
    public Set<String> getConfigSet(final IConfigProp.Key key) {
        return myConfigProp.getSet(key);
    }

    /**
     *
     * @param key
     *            The config key.
     * @return
     */
    public Date getConfigDate(final IConfigProp.Key key) {
        return new Date(myConfigProp.getLong(key));
    }

    /**
     *
     * @param key
     * @return <code>null</code> when property is not found.
     */
    public double getConfigDouble(final IConfigProp.Key key) {
        return myConfigProp.getDouble(key);
    }

    /**
     *
     * @param key
     * @return <code>null</code> when property is not found.
     */
    public BigDecimal getConfigBigDecimal(final IConfigProp.Key key) {
        return myConfigProp.getBigDecimal(key);
    }

    /**
     *
     * @param key
     * @return <code>null</code> when property is not found.
     */
    public int getConfigInt(final IConfigProp.Key key) {
        return myConfigProp.getInt(key);
    }

    /**
     *
     * @param key
     * @return <code>null</code> when property is not found or specified.
     */
    public Integer getConfigInteger(final IConfigProp.Key key) {
        return myConfigProp.getInteger(key);
    }

    /**
     *
     * @param key
     * @param dfault
     * @return <code>null</code> when property is not found.
     */
    public int getConfigInt(final IConfigProp.Key key, final int dfault) {
        final String value = getConfigValue(key);
        if (StringUtils.isBlank(value)) {
            return dfault;
        }
        return Integer.parseInt(value);
    }

    /**
     *
     * @param key
     *            The key.
     * @return the boolean value..
     */
    public boolean isConfigValue(final IConfigProp.Key key) {
        return myConfigProp.getBoolean(key);
    }

    /**
     *
     * @param key
     *            The key.
     * @return {@code true} if Key can be updated by Public API, like JSON-RPC.
     */
    public boolean isConfigApiUpdatable(final IConfigProp.Key key) {
        return myConfigProp.isApiUpdatable(key);
    }

    /**
     * @return {@code true} if SavaPageDraw is enabled <i>and</i> SVG to PDF
     *         converter is installed.
     */
    public static boolean isSavaPageDrawEnabled() {
        return instance()
                .isConfigValue(Key.WEBAPP_USER_PAGE_BROWSER_DRAW_ENABLE)
                && SystemInfo.isRSvgConvertInstalled();
    }

    /**
     *
     * @return
     */
    public static boolean isInternalUsersEnabled() {
        return instance().isConfigValue(Key.INTERNAL_USERS_ENABLE);
    }

    /**
     * @return {@code true} if PaperCut Print integration is enabled.
     */
    public static boolean isPaperCutPrintEnabled() {
        return instance().isConfigValue(Key.PAPERCUT_ENABLE)
                && instance().isConfigValue(Key.PAPERCUT_DB_ENABLE);
    }

    /**
     * @return {@code true} if MailPrint is enabled.
     */
    public static boolean isMailPrintEnabled() {
        return instance().isConfigValue(Key.PRINT_IMAP_ENABLE);
    }

    /**
     * @param userId
     *            User ID.
     * @return {@code true} if user is Mail Print Ticket operator.
     */
    public static boolean isMailPrintTicketOperator(final String userId) {
        final String operator =
                instance().getConfigValue(Key.PRINT_IMAP_TICKET_OPERATOR);
        return operator != null && userId.equals(operator);
    }

    /**
     * @return {@code true} if MailPrint Ticketing is enabled.
     */
    public static boolean isMailPrintTicketingEnabled() {
        return getMailPrintTicketOperator() != null;
    }

    /**
     * Checks MailPrint Ticketing is enabled and user is the Ticket Operator.
     *
     * @param user
     *            MailPrint user (can be {@code null}).
     * @return {@code true} if MailPrint is enabled and redirected to user.
     */
    public static boolean isMailPrintTicketingEnabled(final User user) {
        return user != null && isMailPrintTicketOperator(user.getUserId());
    }

    /**
     * Checks MailPrint Ticketing is enabled and user is the Ticket Operator.
     *
     * @param userid
     *            MailPrint User ID (can be {@code null}).
     * @return {@code true} if MailPrint is enabled and redirected to user.
     */
    public static boolean isMailPrintTicketingEnabled(final String userid) {
        if (userid != null) {
            final String redirectUserId = getMailPrintTicketOperator();
            return redirectUserId != null
                    && redirectUserId.equalsIgnoreCase(userid);
        }
        return false;
    }

    /**
     * Gets the User ID of the MailPrint Ticket Operator.
     *
     * @return {@code null} if MailPrint or MailPrint Ticketing is disabled, or
     *         PrintIn journal is disabled or MailPrint Ticket Operator is not
     *         specified.
     */
    public static String getMailPrintTicketOperator() {
        final ConfigManager cm = instance();
        if (isMailPrintEnabled()
                && cm.isConfigValue(Key.PRINT_IMAP_TICKET_ENABLE)
                && ServiceContext.getServiceFactory().getDocStoreService()
                        .isEnabled(DocStoreTypeEnum.JOURNAL,
                                DocStoreBranchEnum.IN_PRINT)) {
            final String operator =
                    cm.getConfigValue(Key.PRINT_IMAP_TICKET_OPERATOR);
            if (StringUtils.isNotBlank(operator)) {
                return operator;
            }
        }
        return null;
    }

    /**
     * Checks if the FTP Print is activated.
     *
     * @return {@code true} when activated.
     */
    public static boolean isFtpPrintActivated() {
        // FTP Print is not implemented yet
        return false;
    }

    /**
     * @return {@code true} is Eco Print is enabled.
     */
    public static boolean isEcoPrintEnabled() {
        return instance().isConfigValue(Key.ECO_PRINT_ENABLE);
    }

    /**
     * @return {@code true} is PDF/PDF verification is enabled.
     */
    public static boolean isPdfPgpEnabled() {
        return instance().isConfigValue(Key.PDFPGP_VERIFICATION_ENABLE);
    }

    /**
     * @return {@code true} is PDF/PDF verification is available.
     */
    public static boolean isPdfPgpAvailable() {
        return instance().hasOpenPGP() && isPdfPgpEnabled();
    }

    /**
     *
     * @return
     */
    public static boolean isWebPrintEnabled() {
        return instance().isConfigValue(Key.WEB_PRINT_ENABLE);
    }

    /**
     * Tells whether the Application is part of an infrastructure that is
     * connected to the Internet.
     *
     * @return
     */
    public static boolean isConnectedToInternet() {
        return instance().isConfigValue(Key.INFRA_INTERNET_CONNECTED);
    }

    /**
     * @return {@code true} when setup for SavaPage is completed.
     */
    public boolean isSetupCompleted() {
        return isConfigValue(IConfigProp.Key.SYS_SETUP_COMPLETED);
    }

    /**
     * @return {@code true} when SavaPage is in maintenance mode.
     */
    private static boolean isSysMaintenance() {
        return instance().isConfigValue(IConfigProp.Key.SYS_MAINTENANCE);
    }

    /**
     * @return {@code true} if user row locking in database is to be applied to
     *         serialize access to database or user file system (safe-pages).
     */
    public static boolean isUserWebAppDatabaseUserRowLocking() {
        return instance().isConfigValue(
                IConfigProp.Key.WEBAPP_USER_DATABASE_USER_ROW_LOCKING_ENABLED);
    }

    /**
     * @return The {@link SystemModeEnum}.
     */
    public static SystemModeEnum getSystemMode() {
        if (ConfigManager.isSysMaintenance()) {
            return SystemModeEnum.MAINTENANCE;
        }
        return SystemModeEnum.PRODUCTION;
    }

    /**
     * .
     */
    private static void setShutdownInProgress() {
        shutdownInProgress = true;
    }

    /**
     *
     * @return {@code true} when shutdown is in progress.
     */
    public static boolean isShutdownInProgress() {
        return shutdownInProgress;
    }

    /**
     * Shuts down the initialized components. See {@link #init(RunModeEnum)}.
     * <p>
     * Method is idempotent: calling it a second time has no effect.
     * </p>
     *
     * @throws Exception
     *             When things went wrong.
     */
    public synchronized void exit() throws Exception {

        setShutdownInProgress();

        final boolean isServerRunMode =
                runMode != null && runMode == RunModeEnum.SERVER;

        if (isServerRunMode) {

            ServiceContext.getServiceFactory().shutdown();

            /*
             * Wait for current database access to finish.
             */
            ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(true);

            try {
                /*
                 * IppClient need to know about the shutdown, since CUPS might
                 * already be shut down. See Mantis #374.
                 */
                IppClient.instance().setShutdownRequested(true);

                myPrintProxy.exit();

                ProxyPrintJobStatusMonitor.exit();

            } finally {
                ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(false);
            }
        }

        exitScheduler();

        /*
         * Wait till last, since Hibernate is needed when shutting down
         * (scheduled) services.
         */
        if (myEmf != null) {
            /*
             * Mantis #381: do NOT close, since this will cause WARN logging
             */
            // myEmf.close();
        }

        /*
         * Mantis #496
         */
        if (isServerRunMode) {
            removeAppTmpDir();
        }

        runMode = null;
    }

    /**
     * Removes the user's home directory (if it exists).
     *
     * @param user
     * @throws IOException
     */
    public static void removeUserHomeDir(final String user) throws IOException {
        final Path path =
                FileSystems.getDefault().getPath(getUserHomeDir(user));
        if (path.toFile().exists()) {
            FileSystemHelper.removeDir(path);
        }
    }

    /**
     * Gets the size (bytes) of the user's home directory (SafePages).
     *
     * @param user
     *            The user.
     * @return The size in bytes.
     * @throws IOException
     *             When IO error occurs.
     */
    public static long getUserHomeDirSize(final String user)
            throws IOException {
        final MutableLong size = new MutableLong();
        File file = new File(getUserHomeDir(user));
        if (file.exists()) {
            FileSystemHelper.calcDirSize(
                    FileSystems.getDefault().getPath(file.getAbsolutePath()),
                    size);
        }
        return size.longValue();
    }

    /**
     * Creates the global temp directory.
     *
     * @throws IOException
     *             When directory cannot be created.
     */
    public static void createAppTmpDir() throws IOException {

        final String dirName = getAppTmpDir();
        final File dir = new File(dirName);

        if (dir.exists()) {
            removeAppTmpDir();
        }

        final FileSystem fs = FileSystems.getDefault();
        final Path p = fs.getPath(dirName);

        final Set<PosixFilePermission> permissions =
                EnumSet.of(OWNER_READ, OWNER_WRITE, OWNER_EXECUTE);

        final FileAttribute<Set<PosixFilePermission>> attr =
                PosixFilePermissions.asFileAttribute(permissions);

        java.nio.file.Files.createDirectories(p, attr);
    }

    /**
     * Removes the global temp directory.
     */
    public static void removeAppTmpDir() {

        final String dirName = getAppTmpDir();
        final File dir = new File(dirName);

        if (dir.exists()) {
            FileSystem fs = FileSystems.getDefault();
            Path p = fs.getPath(dirName);
            try {
                FileSystemHelper.removeDir(p);
            } catch (IOException e) {
                throw new SpException(e.getMessage(), e);
            }
        }
    }

    /**
     * Get the global temp directory for the SavaPage application.
     * <p>
     * Note: This does not affect temp directory settings for third party
     * components.
     * </p>
     *
     * @return The value of the server properties
     *         {@link #SERVER_PROP_APP_DIR_TMP} (when present) or the
     *         {@link SystemPropertyEnum#JAVA_IO_TMPDIR} property appended with
     *         {@code /savapage}.
     */
    public static String getAppTmpDir() {

        final String homeTmp;

        if (theServerProps == null) {
            homeTmp = null;
        } else {
            homeTmp = theServerProps.getProperty(SERVER_PROP_APP_DIR_TMP);
        }

        if (homeTmp != null) {
            return homeTmp;
        }

        return String.format("%s%c%s",
                SystemPropertyEnum.JAVA_IO_TMPDIR.getValue(),
                File.separatorChar, "savapage");
    }

    /**
     * Gets the user source.
     *
     * @return {@code null} when not present.
     */
    public IUserSource getUserSource() {

        final IUserSource source;

        final String mode = myConfigProp.getString(IConfigProp.Key.AUTH_METHOD);

        if (mode.equals(IConfigProp.AUTH_METHOD_V_NONE)) {

            source = new NoUserSource();

        } else if (mode.equals(IConfigProp.AUTH_METHOD_V_UNIX)) {

            source = new UnixUserSource();

        } else if (mode.equals(IConfigProp.AUTH_METHOD_V_LDAP)) {

            final LdapTypeEnum ldapType = getConfigLdapType();

            switch (ldapType) {
            case ACTD:
                source = new ActiveDirectoryUserSource();
                break;
            case GOOGLE_CLOUD:
                source = new GoogleLdapUserSource();
                break;
            default:
                source = new LdapUserSource(ldapType);
                break;
            }
        } else if (mode.equals(IConfigProp.AUTH_METHOD_V_CUSTOM)) {
            source = new CustomUserSource();
        } else {
            source = null;
        }
        return source;
    }

    /**
     *
     * @return {@code true} when users are synchronized with an LDAP user
     *         source.
     */
    public static boolean isLdapUserSync() {
        return instance().getConfigValue(Key.AUTH_METHOD)
                .equals(IConfigProp.AUTH_METHOD_V_LDAP);
    }

    /**
     * Gets the user authenticator.
     *
     * @return {@code null} when not present.
     */
    public IExternalUserAuthenticator getUserAuthenticator() {

        final IExternalUserAuthenticator auth;

        final String mode = myConfigProp.getString(IConfigProp.Key.AUTH_METHOD);

        if (mode.equals(IConfigProp.AUTH_METHOD_V_NONE)) {

            auth = null;

        } else if (mode.equals(IConfigProp.AUTH_METHOD_V_UNIX)) {

            auth = new UnixUserSource();

        } else if (mode.equals(IConfigProp.AUTH_METHOD_V_LDAP)) {

            final LdapTypeEnum ldapType = getConfigLdapType();

            switch (ldapType) {
            case ACTD:
                auth = new ActiveDirectoryUserSource();
                break;
            case GOOGLE_CLOUD:
                auth = new GoogleLdapUserSource();
                break;
            default:
                auth = new LdapUserSource(ldapType);
                break;
            }
        } else if (mode.equals(IConfigProp.AUTH_METHOD_V_CUSTOM)) {
            auth = new CustomUserSource();
        } else {
            auth = null;
        }
        return auth;
    }

    /**
     * Sets the Hibernate and system properties for Derby.
     *
     * @param config
     *            The configuration properties.
     */
    private void initHibernateDerby(final Map<String, Object> config) {

        if (theServerProps != null) {

            for (final SystemPropertyEnum prop : new SystemPropertyEnum[] {
                    SystemPropertyEnum.DERBY_DEADLOCK_TIMEOUT,
                    SystemPropertyEnum.DERBY_LOCKS_WAITTIMEOUT }) {

                final String value = theServerProps.getProperty(prop.getKey());

                if (StringUtils.isNotBlank(value)) {
                    prop.setValue(value);
                    SpInfo.instance().log(prop + "=" + value);
                }
            }
        }

        config.put(DbConfig.JPA_JDBC_DRIVER,
                org.apache.derby.jdbc.EmbeddedDriver.class.getName());

        config.put(DbConfig.HIBERNATE_DIALECT,
                org.hibernate.dialect.DerbyTenSevenDialect.class.getName());
    }

    /**
     * Sets the Hibernate configuration for PostgrSQL.
     *
     * @param config
     *            The configuration map.
     * @return {@code true} if JDBC user is configured.
     */
    private boolean initHibernatePostgreSQL(final Map<String, Object> config) {

        final String jdbcUser;

        if (theServerProps != null) {

            jdbcUser = theServerProps.getProperty(SERVER_PROP_DB_USER);

            DbConfig.configHibernatePostgreSQL(config, jdbcUser,
                    getDbUserPassword(),
                    theServerProps.getProperty(SERVER_PROP_DB_URL),
                    theServerProps.getProperty(SERVER_PROP_DB_DRIVER));

        } else {
            jdbcUser = null;
            DbConfig.configHibernatePostgreSQL(config);
        }
        return jdbcUser != null;
    }

    /**
     * Sets the Hibernate configuration for External database.
     *
     * @param config
     *            The configuration map.
     * @return {@code true} if JDBC user is configured.
     */
    private boolean initHibernateExternal(final Map<String, Object> config) {

        if (theServerProps == null) {
            throw new IllegalArgumentException(
                    "Server properties are missing.");
        }

        final String jdbcUser = theServerProps.getProperty(SERVER_PROP_DB_USER);

        DbConfig.configHibernateExternal(config, jdbcUser, getDbUserPassword(),
                theServerProps.getProperty(SERVER_PROP_DB_URL),
                theServerProps.getProperty(SERVER_PROP_DB_DRIVER),
                theServerProps.getProperty(SERVER_PROP_DB_HIBERNATE_DIALECT));

        return jdbcUser != null;
    }

    /**
     * @return The JDBC user of the external database.
     */
    public static String getExternalDbUser() {
        if (theServerProps == null) {
            return null;
        }
        return theServerProps.getProperty(SERVER_PROP_DB_USER);
    }

    /**
     * @return The JDBC user password of the external database.
     */
    public static String getExternalDbPassword() {
        if (theServerProps == null) {
            return null;
        }
        return theServerProps.getProperty(SERVER_PROP_DB_PASS);
    }

    /**
     * Bootstraps Hibernate.
     * <p>
     * We use the JPA 2 APIs to bootstrap Hibernate as much as we can.
     * </p>
     * <p>
     * See the <a href=
     * "http://docs.jboss.org/hibernate/stable/entitymanager/reference/en/html_single/"
     * >Hibernate EntityManager documentation</a>
     * </p>
     * <p>
     * NOTE: Since Mantis #348 (Hibernate HHH015016: deprecated
     * javax.persistence.spi.PersistenceProvider) we use
     * {@link HibernatePersistenceProvider#createEntityManagerFactory(String, Map)}
     * instead of {@link Persistence#createEntityManagerFactory(String, Map)}.
     * </p>
     *
     * @param databaseTypeDefault
     *            The default {@link DatabaseTypeEnum} to be used in case no
     *            server properties are found. This value can be {@code null}
     *            when the caller is sure that server properties are present.
     */
    public void initHibernate(final DatabaseTypeEnum databaseTypeDefault) {

        /*
         * This is the place to override with MySQL or PostgreSQL driver/dialect
         */
        final boolean useHibernateC3p0Parms;

        if (theServerProps == null) {

            this.myDatabaseType = databaseTypeDefault;
            useHibernateC3p0Parms = false;

        } else {
            this.myDatabaseType = DatabaseTypeEnum
                    .valueOf(theServerProps.getProperty(SERVER_PROP_DB_TYPE,
                            DatabaseTypeEnum.Internal.getPropertiesId()));

            useHibernateC3p0Parms = true;
        }

        final Map<String, Object> configOverrides =
                new HashMap<String, Object>();

        /*
         * The classname of a custom org.hibernate.connection.ConnectionProvider
         * which provides JDBC connections to Hibernate. See Mantis #349.
         */
        configOverrides.put(DbConfig.HIBERNATE_CONNECTION_PROVIDER,
                "org.hibernate.connection.C3P0ConnectionProvider");

        /*
         * Mantis #513: PostgreSQL: WebApp very slow.
         */
        if (useHibernateC3p0Parms) {
            this.dbConnectionPoolProps =
                    DbConnectionPoolEnum.createFromConfig(theServerProps);
            DbConfig.configHibernateC3p0(this.dbConnectionPoolProps,
                    configOverrides);
        } else {
            this.dbConnectionPoolProps = null;
        }

        //
        final boolean createEmf;
        switch (this.myDatabaseType) {
        case Internal:
            initHibernateDerby(configOverrides);
            createEmf = true;
            break;
        case PostgreSQL:
            createEmf = initHibernatePostgreSQL(configOverrides);
            break;
        case External:
            createEmf = initHibernateExternal(configOverrides);
            break;
        default:
            throw new SpException("Database type [" + this.myDatabaseType
                    + "] is NOT supported");
        }
        //
        this.jdbcInfo.setDriver(
                configOverrides.get(DbConfig.JPA_JDBC_DRIVER).toString());
        //
        this.hibernateInfo.setDialect(
                configOverrides.get(DbConfig.HIBERNATE_DIALECT).toString());
        //
        if (createEmf) {
            this.myEmf = DbConfig.createEntityManagerFactory(configOverrides);
            /*
             * Get JDBC URL from EntityManagerFactory properties. Derby URL is
             * implicit and created by EntityManagerFactory as property.
             */
            this.jdbcInfo.setUrl(this.myEmf.getProperties()
                    .get(DbConfig.JPA_JDBC_URL).toString());
        }
    }

    /**
     *
     * @return The {@link DatabaseTypeEnum}.
     */
    public static DatabaseTypeEnum getDatabaseType() {
        return instance().myDatabaseType;
    }

    /**
     * Creates an application managed entity manager.
     * <p>
     * NOTE: the created instance should be closed when not used anymore, i.e.
     * use close() method in finally catch block.
     * </p>
     *
     * @deprecated Use services from {@link ServiceFactory}.
     *
     * @return The {@link EntityManager}.
     *
     */
    @Deprecated
    public EntityManager createEntityManager() {
        /*
         * "Thanks to the EntityManagerFactory, you can retrieve an extended
         * entity manager. The extended entity manager keep the same persistence
         * context for the lifetime of the entity manager: in other words, the
         * entities are still managed between two transactions (unless you call
         * entityManager.clear() in between). You can see an entity manager as a
         * small wrapper on top of an Hibernate session."
         */
        return myEmf.createEntityManager();
    }

    /**
     * Returns a (lazy initialized) database version info object.
     *
     * @return {@link DbVersionInfo}.
     */
    public DbVersionInfo getDbVersionInfo() {

        synchronized (this.myDbVersionInfoMutex) {
            if (myDbVersionInfo == null) {
                myDbVersionInfo =
                        ServiceContext.getDaoContext().getDbVersionInfo();
            }
            return myDbVersionInfo;
        }
    }

}
