/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.VersionInfo;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerRegistry;
import org.savapage.core.circuitbreaker.CircuitDamagingException;
import org.savapage.core.circuitbreaker.CircuitNonTrippingException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.community.MemberCard;
import org.savapage.core.concurrent.ReadWriteLockEnum;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.config.IConfigProp.LdapType;
import org.savapage.core.config.IConfigProp.Prop;
import org.savapage.core.config.IConfigProp.ValidationResult;
import org.savapage.core.crypto.CryptoApp;
import org.savapage.core.crypto.CryptoUser;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.impl.DaoContextImpl;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.jmx.CoreConfig;
import org.savapage.core.job.SpJobScheduler;
import org.savapage.core.jpa.ConfigProperty;
import org.savapage.core.jpa.Device;
import org.savapage.core.jpa.Printer;
import org.savapage.core.jpa.PrinterGroup;
import org.savapage.core.jpa.User;
import org.savapage.core.jpa.tools.DatabaseTypeEnum;
import org.savapage.core.jpa.tools.DbUpgManager;
import org.savapage.core.jpa.tools.DbVersionInfo;
import org.savapage.core.print.proxy.IppClient;
import org.savapage.core.print.proxy.ProxyPrintJobStatusMonitor;
import org.savapage.core.services.PrinterService;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.ServiceFactory;
import org.savapage.core.users.ActiveDirectoryUserSource;
import org.savapage.core.users.IExternalUserAuthenticator;
import org.savapage.core.users.IUserSource;
import org.savapage.core.users.LdapUserSource;
import org.savapage.core.users.NoUserSource;
import org.savapage.core.users.UnixUserSource;
import org.savapage.core.users.UserAliasList;
import org.savapage.core.util.CurrencyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Datraverse B.V.
 */
public final class ConfigManager {

    /**
     * .
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(ConfigManager.class);

    /**
     * .
     */
    private static boolean shutdownInProgress = false;

    /**
     *
     */
    private RunMode runMode = null;

    /**
     * Prefix of local-loop IP addresses.
     * <p>
     * Note: Debian based distros have 127.0.0.1 (localhost) and 127.0.1.1
     * (linuxlnx) defined in {@code /etc/hosts}
     * </p>
     */
    private static final String IP_LOOP_BACK_ADDR_PREFIX = "127.0.";

    /**
     *
     */
    private static final String IP_LOOP_BACK_ADDR = "127.0.0.1";

    /**
     *
     */
    private static final String USER_TEMP_RELATIVE_PATH = ".temp";

    /**
     *
     */
    private static final String REL_PATH_USERALIAS_LIST =
            "data/conf/username-aliases.txt";

    /**
     * Path of SAVAPAGE.ppd (case sensitive!) relative to
     * {@link ConfigManager#getServerHome().
     */
    private static final String REL_PATH_SAVAPAGE_PPD_FILE =
            "../client/SAVAPAGE.ppd";

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

    /**
     *
     */
    private static final Class<? extends Exception>[] CIRCUIT_NON_TRIPPING_EXCEPTIONS =
            new Class[] { CircuitNonTrippingException.class };

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

    private static Properties theServerProps = null;

    public static final String SERVER_PROP_APP_DIR_SAFEPAGES =
            "app.dir.safepages";
    public static final String SERVER_PROP_APP_DIR_LETTERHEADS =
            "app.dir.letterheads";

    public static final String SERVER_PROP_SMARTSCHOOL_PRINT =
            "smartschool.print";

    private static final String SERVER_PROP_DB_TYPE = "database.type";
    private static final String SERVER_PROP_DB_URL = "database.url";
    private static final String SERVER_PROP_DB_USER = "database.user";
    private static final String SERVER_PROP_DB_PASS = "database.password";

    /*
     *
     */
    public static final String SERVER_PROP_CUPS_NOTIFIER = "cups.notifier";
    private static final String DEFAULT_CUPS_NOTIFIER = "savapage";

    /*
     *
     */
    private static final String SERVER_PROP_PRINT_PROXY_NOTIFICATION_METHOD =
            "print.proxy.notification.method";
    private static final String VAL_PRINT_PROXY_NOTIFICATION_METHOD_PUSH =
            "push";
    private static final String DEFAULT_PRINT_PROXY_NOTIFICATION_METHOD =
            VAL_PRINT_PROXY_NOTIFICATION_METHOD_PUSH;

    private static final String SERVER_PROP_CUPS_SERVER_PORT =
            "cups.server.port";

    private static final String DEFAULT_CUPS_HOST = "localhost";
    private static final String DEFAULT_CUPS_PORT = "631";

    public static final String SERVER_PROP_PRINTER_RAW_PORT =
            "server.print.port.raw";
    public static final String PRINTER_RAW_PORT_DEFAULT = "9100";

    // ========================================================================
    // Undocumented ad-hoc properties for testing purposes.
    // ========================================================================

    // ========================================================================

    /**
     *
     */
    public static final String SYS_PROP_SERVER_HOME = "server.home";

    private static final String APP_OWNER = "Datraverse B.V.";

    private final CryptoApp myCipher = new CryptoApp();

    private ProxyPrintService myPrintProxy;

    private DatabaseTypeEnum myDatabaseType;

    /**
     * For convenience we use ConfigPropImp instead of ConfigProp (because of
     * easy Eclipse hyperlinking.
     */
    private final ConfigPropImpl myConfigProp = new ConfigPropImpl();

    /**
     *
     */
    private Properties myPropsAdmin = null;

    /**
     *
     */
    private EntityManagerFactory myEmf = null;

    /**
     *
     */
    private final CircuitBreakerRegistry circuitBreakerRegistry =
            new CircuitBreakerRegistry();

    /**
     *
     */
    private DbVersionInfo myDbVersionInfo = null;

    /**
     *
     */
    private ConfigManager() {
        runMode = null;
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
     * @return {@code true) when properly initialized.

     */
    public boolean isInitialized() {
        return (runMode != null);
    }

    /**
     *
     * @return
     */
    public CryptoApp cipher() {
        return myCipher;
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
                ConfigManager.instance().getConfigValue(Key.SYS_DEFAULT_LOCALE)
                        .trim();

        if (StringUtils.isNotBlank(languageTag)) {
            Locale.Builder builder = new Locale.Builder();
            return builder.setLanguageTag(languageTag).build();
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
                    myPropsAdmin.getProperty(PROP_INTERNAL_ADMIN_PASSWORD),
                    uid, password)) {
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
    public static CircuitBreaker getCircuitBreaker(
            final CircuitBreakerEnum breakerEnum) {

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
        return !myConfigProp.getString(IConfigProp.Key.AUTH_METHOD).equals(
                IConfigProp.AUTH_METHOD_V_NONE)
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
        return new File(getServerHome() + File.separator
                + REL_PATH_SAVAPAGE_PPD_FILE);
    }

    /**
     * Gets the {@code hostname} of the host system this application is running
     * on.
     *
     * @return The hostname.
     */
    public static String getServerHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            throw new SpException(e.getMessage(), e);
        }
    }

    /**
     * Gets the assigned (static or dynamic) IPv4 address (no loop back address)
     * of the host system this application is running on, or the loop back
     * address when no assigned address is found.
     *
     * @return The local host IPv4 address.
     * @throws UnknownHostException
     *             When non-loop IPv4 address could not be found or I/O errors
     *             are encountered when. getting the network interfaces.
     */
    public static String getServerHostAddress() throws UnknownHostException {

        final String ipAddress = InetAddress.getLocalHost().getHostAddress();

        if (!ipAddress.startsWith(IP_LOOP_BACK_ADDR_PREFIX)) {
            return ipAddress;
        }

        /*
         * Traverse all network interfaces on this machine.
         */
        final Enumeration<NetworkInterface> networkEnum;

        try {
            networkEnum = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new UnknownHostException(e.getMessage());
        }

        while (networkEnum != null && networkEnum.hasMoreElements()) {

            final NetworkInterface inter = networkEnum.nextElement();

            /*
             * Traverse all addresses for this interface.
             */
            final Enumeration<InetAddress> enumAddr = inter.getInetAddresses();

            while (enumAddr.hasMoreElements()) {

                final InetAddress addr = enumAddr.nextElement();

                /*
                 * IPv4 addresses only.
                 */
                if (addr instanceof Inet4Address) {

                    if (!addr.getHostAddress().startsWith(
                            IP_LOOP_BACK_ADDR_PREFIX)) {
                        /*
                         * Bingo, this is a non-loop back address.
                         */
                        return addr.getHostAddress();
                    }

                }

            }
        }

        /*
         * No non-loop back IP v4 addresses found: return loop back address.
         */
        return IP_LOOP_BACK_ADDR;
    }

    /**
     * Dynamically gets the {@code server.home} system property as passed to the
     * JVM or set internally.
     * <p>
     * IMPORTANT: do NOT cache the value because we want to have the freedom to
     * change the system property on runtime.
     * </p>
     *
     * @return
     */
    public static String getServerHome() {
        return System.getProperty(SYS_PROP_SERVER_HOME);
    }

    /**
     * Returns the location where the user's SafePages are stored. As a default
     * this is a path relative to $(server.home)/data/internal/safepages
     *
     * @param user
     * @return
     */
    public static String getUserHomeDir(final String user) {
        return theServerProps.getProperty(SERVER_PROP_APP_DIR_SAFEPAGES,
                getServerHome() + "/data/internal/safepages") + "/" + user;
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
     * Returns the location where the public Letterheads are stored.
     *
     * @return
     */
    public static String getLetterheadDir() {
        return theServerProps.getProperty(SERVER_PROP_APP_DIR_LETTERHEADS,
                getServerHome() + "/data/internal/letterheads");
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

        FileInputStream fis = null;

        try {

            final String path =
                    ConfigManager.getServerHome() + "/"
                            + FILENAME_SERVER_PROPERTIES;

            fis = new FileInputStream(path);

            serverProps = new Properties();
            serverProps.load(fis);

            return serverProps;

        } finally {
            IOUtils.closeQuietly(fis);
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
     * TODO: now it is fixed Derby, should be dependent on db type.
     *
     * @return
     */
    public static String getDbScriptDir() {
        return ConfigManager.getServerHome() + "/lib/sql/Derby";
    }

    /**
     *
     * @return
     */
    public static String getServerBinHome() {

        String home = getServerHome() + "/bin/linux-";
        if (isOsArch64Bit()) {
            home += "x64";
        } else {
            home += "i686";
        }
        return home;
    }

    /**
     *
     * @return The directory with the server extension (plug-in) property files.
     */
    public static File getServerExtHome() {
        return new File(String.format("%s/ext", getServerHome()));
    }

    /**
     *
     * @return
     */
    public static boolean isOsArch64Bit() {
        return (System.getProperty("os.arch").indexOf("64") != -1);
    }

    /**
     *
     * @return
     */
    public static String getProcessUserName() {
        return System.getProperty("user.name");
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
     * Returns "Major.Minor.Revision"
     *
     * @return
     */
    public static String getAppVersion() {
        return String.format("%s.%s.%s", VersionInfo.VERSION_A_MAJOR,
                VersionInfo.VERSION_B_MINOR, VersionInfo.VERSION_C_REVISION);
    }

    /**
     * Returns "Major.Minor.Revision (Build xxxx)"
     *
     * @return
     */
    public static String getAppVersionBuild() {
        return String.format("%s.%s.%s (Build %s)",
                VersionInfo.VERSION_A_MAJOR, VersionInfo.VERSION_B_MINOR,
                VersionInfo.VERSION_C_REVISION, VersionInfo.VERSION_D_BUILD);
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
     * See {@link CryptoApp#createInitialVisitorStartDate()}.
     */
    public String createInitialVisitorStartDate() {
        return myCipher.createInitialVisitorStartDate();
    }

    /**
     * Calculates the runnable status of the configuration.
     *
     * @return <code>true</code if runnable.
     */
    public boolean calcRunnable() {
        return myConfigProp.calcRunnable();
    }

    /**
     *
     * @return
     */
    public static void setServerProps(final Properties props) {
        theServerProps = props;
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
     * @return
     */
    public static boolean isCupsPushNotification() {

        String method = DEFAULT_PRINT_PROXY_NOTIFICATION_METHOD;

        if (theServerProps != null) {
            method =
                    theServerProps
                            .getProperty(
                                    SERVER_PROP_PRINT_PROXY_NOTIFICATION_METHOD,
                                    ConfigManager.DEFAULT_PRINT_PROXY_NOTIFICATION_METHOD);
        }
        return method
                .equalsIgnoreCase(VAL_PRINT_PROXY_NOTIFICATION_METHOD_PUSH);
    }

    /**
     * Initializes the core application depending on the {@link RunMode}.
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
    public synchronized void init(final RunMode mode,
            final DatabaseTypeEnum databaseTypeDefault) throws Exception {

        if (runMode != null) {
            throw new SpException("application is already initialized");
        }

        createAppTmpDir();

        switch (mode) {
        case SERVER:
            initAsServer(new Properties());
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
     * Checks whether the application is read-to-use.
     *
     * @return {@code true} when read-to-use.
     */
    public boolean isAppReadyToUse() {
        return myConfigProp.isRunnable();
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
    private void initJmx() throws MalformedObjectNameException,
            InstanceAlreadyExistsException, MBeanRegistrationException,
            NotCompliantMBeanException, KeyStoreException,
            NoSuchAlgorithmException, CertificateException, IOException,
            UnrecoverableKeyException, KeyManagementException {

        /*
         * Get the MBean server.
         */
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

        /*
         * Register the MBean(s)
         */
        CoreConfig mBean = new CoreConfig();

        ObjectName name = new ObjectName("org.savapage:type=Core");

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
        myCipher.init();
        CryptoUser.init();

        this.initJmx();

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
                LOGGER.info("configuration is ready to run");
            } else {
                LOGGER.warn("configuration is NOT ready to run: "
                        + "administration needed");
            }

            MemberCard.instance().init();

            /*
             *
             */
            loadAdminProperties();
            initUserAliasList();

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

        //
        ProxyPrintJobStatusMonitor.init();
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
     */
    private void initAsCoreLibrary(final DatabaseTypeEnum databaseTypeDefault,
            final Properties props) throws IOException {

        initHibernate(databaseTypeDefault);
        myConfigProp.init(props);
        myCipher.initAsBasicLibrary();
    }

    /**
     * Initialize as a runnable core library (with a fully functional database),
     * so basic operations can be performed. Followship operations are not
     * allowed.
     *
     * @param props
     * @throws IOException
     */
    private void initAsRunnableCoreLibrary(Properties props) throws IOException {

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
                Locale.setDefault(new Locale.Builder().setLanguageTag(
                        languageTag).build());
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

        final String pw =
                CryptoUser.getHashedUserPassword(UserDao.INTERNAL_ADMIN_USERID,
                        plainPassword);

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
     *
     */
    private void initUserAliasList() {
        UserAliasList.instance().load(
                new File(getServerHome() + "/" + REL_PATH_USERALIAS_LIST));
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
     */
    public void updateConfigKey(final Key key, final String value,
            final String actor) {

        String val = value;

        if (isUserEncrypted(key)) {
            val = CryptoUser.encrypt(value);
        }

        myConfigProp.updateValue(key, val, actor);
    }

    /**
     * See: {@link #updateConfigKey(Key, String, String)

     */
    public void updateConfigKey(final Key key, final boolean value,
            final String actor) {
        updateConfigKey(key, value ? IConfigProp.V_YES : IConfigProp.V_NO,
                actor);
    }

    /**
     * See: {@link #updateConfigKey(Key, String, String)

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

        final ConfigProperty prop =
                ServiceContext.getDaoContext().getConfigPropertyDao()
                        .findByName(getConfigKey(key));

        if (prop != null) {
            val = prop.getValue();
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

                final PrinterGroup group =
                        ServiceContext.getDaoContext().getPrinterGroupDao()
                                .findByName(groupName);

                if (group == null) {
                    isNonSecure = true;
                    LOGGER.warn("Printer Group [" + groupName
                            + "] is NOT found.");
                } else {

                    final PrinterService printerService =
                            ServiceContext.getServiceFactory()
                                    .getPrinterService();

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
    private boolean isUserEncrypted(final IConfigProp.Key key) {
        return key == Key.AUTH_LDAP_ADMIN_PASSWORD
                || key == Key.CLIAPP_AUTH_ADMIN_PASSKEY
                || key == Key.MAIL_SMTP_PASSWORD
                || key == Key.PAPERCUT_DB_PASSWORD
                || key == Key.PAPERCUT_SERVER_AUTH_TOKEN
                || key == Key.PRINT_IMAP_PASSWORD
                || key == Key.SMARTSCHOOL_1_SOAP_PRINT_ENDPOINT_PASSWORD;
    }

    /**
     *
     */
    public String getConfigKey(final IConfigProp.Key key) {
        return myConfigProp.getKey(key);
    }

    /**
     *
     */
    public IConfigProp.Key getConfigKey(final String key) {
        return myConfigProp.getKey(key);
    }

    /**
     *
     */
    public IConfigProp.LdapType getConfigLdapType() {

        final String schema = myConfigProp.getString(Key.LDAP_SCHEMA_TYPE);

        if (schema.equals(IConfigProp.LDAP_TYPE_V_ACTIV)) {
            return IConfigProp.LdapType.ACTD;
        } else if (schema.equals(IConfigProp.LDAP_TYPE_V_E_DIR)) {
            return IConfigProp.LdapType.EDIR;
        } else if (schema.equals(IConfigProp.LDAP_TYPE_V_OPEN_LDAP)) {
            return IConfigProp.LdapType.OPEN_LDAP;
        }
        return IConfigProp.LdapType.OPEN_DIR;
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
     * Gets the {@link InternalFontFamilyEnum} of a config key.
     *
     * @param key
     *            The {@link IConfigProp.Key}.
     * @return {@link IConfigProp#DEFAULT_INTERNAL_FONT_FAMILY} when or invalid
     *         or not found.
     */
    public static InternalFontFamilyEnum getConfigFontFamily(
            final IConfigProp.Key key) {

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
    public String getConfigValue(final IConfigProp.LdapType ldapType,
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
    public Boolean isConfigValue(final IConfigProp.LdapType ldapType,
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
            return StringUtils.splitPreserveAllTokens(instance()
                    .getConfigValue(key).replace("\r\n", "\n"), '\n');
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
     * @return <code>null</code> when property is not found.
     */
    public boolean isConfigValue(final IConfigProp.Key key) {
        return myConfigProp.getBoolean(key);
    }

    /**
     *
     * @return
     */
    public static boolean isInternalUsersEnabled() {
        return instance().isConfigValue(Key.INTERNAL_USERS_ENABLE);
    }

    /**
     *
     * @return
     */
    public static boolean isPrintImapEnabled() {
        return instance().isConfigValue(Key.PRINT_IMAP_ENABLE);
    }

    /**
     * Checks if the SmartSchool Print module is activated.
     *
     * @return {@code true} when activated.
     */
    public static boolean isSmartSchoolPrintModuleActivated() {
        return theServerProps.getProperty(SERVER_PROP_SMARTSCHOOL_PRINT,
                "false").equalsIgnoreCase("true");
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
     *
     * @return
     */
    public static boolean isSmartSchoolPrintActiveAndEnabled() {
        return isSmartSchoolPrintModuleActivated()
                && isSmartSchoolPrintEnabled();
    }

    /**
     *
     * @return
     */
    public static boolean isSmartSchoolPrintEnabled() {
        return instance().isConfigValue(Key.SMARTSCHOOL_1_ENABLE)
                || instance().isConfigValue(Key.SMARTSCHOOL_2_ENABLE);
    }

    /**
     *
     * @return
     */
    public static boolean isWebPrintEnabled() {
        return instance().isConfigValue(Key.WEB_PRINT_ENABLE);
    }

    /**
     *
     * @return
     */
    public static boolean isGcpEnabled() {
        return instance().isConfigValue(Key.GCP_ENABLE);
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
     * Tells whether the setup for SavaPage is completed.
     *
     * @return
     */
    public boolean isSetupCompleted() {
        return isConfigValue(IConfigProp.Key.SYS_SETUP_COMPLETED);
    }

    /**
     * .
     */
    private static void setShutdownInProgress() {
        shutdownInProgress = true;
    }

    public static boolean isShutdownInProgress() {
        return shutdownInProgress;
    }

    /**
     * Shuts down the initialized components. See {@link #init(RunMode)}.
     *
     * @throws Exception
     *             When things wnet wrong.
     */
    public synchronized void exit() throws Exception {

        setShutdownInProgress();

        final boolean isServerRunMode =
                runMode != null && runMode == RunMode.SERVER;

        if (isServerRunMode) {

            /*
             * Wait for current database access to finish.
             */
            ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(true);

            /*
             * IppClient need to know about the shutdown, since CUPS might
             * already be shut down. See Mantis #374.
             */
            IppClient.instance().setShutdownRequested(true);

            myPrintProxy.exit();

            ProxyPrintJobStatusMonitor.exit();

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

        if (isServerRunMode) {
            ReadWriteLockEnum.DATABASE_READONLY.setWriteLock(false);
        }
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
            removeDir(path);
        }
    }

    /**
     * Removes a directory by recursively deleting files and sub directories.
     *
     * @param path
     * @throws IOException
     */
    private static void removeDir(final Path path) throws IOException {

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return CONTINUE;
            }

            @Override
            public FileVisitResult
                    postVisitDirectory(Path dir, IOException exc)
                            throws IOException {

                if (exc == null) {
                    Files.delete(dir);
                    return CONTINUE;
                } else {
                    throw exc;
                }
            }
        });
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
    public static long getUserHomeDirSize(final String user) throws IOException {
        final MutableLong size = new MutableLong();
        File file = new File(getUserHomeDir(user));
        if (file.exists()) {
            calcDirSize(FileSystems.getDefault()
                    .getPath(file.getAbsolutePath()), size);
        }
        return size.longValue();
    }

    /**
     *
     * @param path
     *            The directory path.
     * @param size
     *            The size in bytes.
     * @throws IOException
     *             When IO error occurs.
     */
    public static void calcDirSize(final Path path, final MutableLong size)
            throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                    BasicFileAttributes attrs) throws IOException {

                if (attrs.isDirectory()) {
                    calcDirSize(file, size); // recurse
                } else {
                    size.add(attrs.size());
                }
                return CONTINUE;
            }
        });
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
                removeDir(p);
            } catch (IOException e) {
                throw new SpException(e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the value of System property {@code java.io.tmpdir} appended with
     * {@code /savapage}.
     *
     * @return
     */
    public static String getAppTmpDir() {
        return System.getProperty("java.io.tmpdir") + "/savapage";
    }

    /**
     * @param userid
     *            The user id.
     * @return the path name of the personal user temp directory.
     */
    public static String getUserTempDir(final String userid) {
        return getUserHomeDir(userid) + "/" + USER_TEMP_RELATIVE_PATH;
    }

    /**
     * Gets the user source.
     *
     * @return {@code null} when not present.
     */
    public IUserSource getUserSource() {

        IUserSource source = null;

        final String mode = myConfigProp.getString(IConfigProp.Key.AUTH_METHOD);

        if (mode.equals(IConfigProp.AUTH_METHOD_V_NONE)) {

            source = new NoUserSource();

        } else if (mode.equals(IConfigProp.AUTH_METHOD_V_UNIX)) {

            source = new UnixUserSource();

        } else if (mode.equals(IConfigProp.AUTH_METHOD_V_LDAP)) {

            final LdapType ldapType = getConfigLdapType();

            if (ldapType == LdapType.ACTD) {
                source = new ActiveDirectoryUserSource();
            } else {
                source = new LdapUserSource(ldapType);
            }
        }
        return source;
    }

    /**
     *
     * @return {@code true} when users are ysnchronized with an LDAP user
     *         source.
     */
    public static boolean isLdapUserSync() {
        return instance().getConfigValue(Key.AUTH_METHOD).equals(
                IConfigProp.AUTH_METHOD_V_LDAP);
    }

    /**
     * Gets the user authenticator.
     *
     * @return {@code null} when not present.
     */
    public IExternalUserAuthenticator getUserAuthenticator() {

        IExternalUserAuthenticator auth = null;

        final String mode = myConfigProp.getString(IConfigProp.Key.AUTH_METHOD);

        if (mode.equals(IConfigProp.AUTH_METHOD_V_NONE)) {

            auth = null;

        } else if (mode.equals(IConfigProp.AUTH_METHOD_V_UNIX)) {

            auth = new UnixUserSource();

        } else if (mode.equals(IConfigProp.AUTH_METHOD_V_LDAP)) {

            final LdapType ldapType = getConfigLdapType();

            if (ldapType == LdapType.ACTD) {
                auth = new ActiveDirectoryUserSource();
            } else {
                auth = new LdapUserSource(ldapType);
            }
        }
        return auth;
    }

    /**
     * Sets the Hibernate adn system properties for Derby.
     *
     * @param properties
     *            The properties.
     */
    private void initHibernateDerby(final Map<String, Object> config) {

        /*
         * How to set Apache Derby properties
         * http://docs.oracle.com/javadb/10.8.1.2/ref/crefproper22250.html : See
         * sample code below:
         *
         * System.setProperty("derby.locks.deadlockTimeout", "25");
         * System.setProperty("derby.locks.waitTimeout", "30");
         */
        if (theServerProps != null) {

            final String KEY_DERBY_DEADLOCK_TIMEOUT =
                    "derby.locks.deadlockTimeout";
            final String KEY_DERBY_LOCKS_WAITTIMEOUT =
                    "derby.locks.waitTimeout";

            for (String key : new String[] { KEY_DERBY_DEADLOCK_TIMEOUT,
                    KEY_DERBY_LOCKS_WAITTIMEOUT }) {

                final String value = theServerProps.getProperty(key);

                if (StringUtils.isNotBlank(value)) {
                    System.setProperty(key, value);
                    SpInfo.instance().log(key + "=" + value);
                }
            }
        }

        config.put("javax.persistence.jdbc.driver",
                "org.apache.derby.jdbc.EmbeddedDriver");

        config.put("hibernate.dialect",
                "org.hibernate.dialect.DerbyTenSevenDialect");
    }

    /**
     * Sets the Hibernate properties for PostgrSQL.
     *
     * @param properties
     *            The properties.
     */
    private void initHibernatePostgreSQL(final Map<String, Object> properties) {

        properties
                .put("javax.persistence.jdbc.driver", "org.postgresql.Driver");
        properties.put("hibernate.dialect",
                "org.hibernate.dialect.PostgreSQLDialect");

        if (theServerProps != null) {
            properties.put("javax.persistence.jdbc.user",
                    theServerProps.getProperty(SERVER_PROP_DB_USER));
            properties.put("javax.persistence.jdbc.password",
                    getDbUserPassword());
            properties.put("javax.persistence.jdbc.url",
                    theServerProps.getProperty(SERVER_PROP_DB_URL));
        }
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

        final Map<String, Object> configOverrides =
                new HashMap<String, Object>();

        /*
         * The classname of a custom org.hibernate.connection.ConnectionProvider
         * which provides JDBC connections to Hibernate. See Mantis #349.
         */
        configOverrides.put("hibernate.connection.provider_class",
                "org.hibernate.connection.C3P0ConnectionProvider");

        /*
         * This is the place to override with MySQL or PostgreSQL driver/dialect
         */
        final boolean useHibernateC3p0Parms;

        if (theServerProps == null) {

            this.myDatabaseType = databaseTypeDefault;
            useHibernateC3p0Parms = false;

        } else {
            this.myDatabaseType =
                    DatabaseTypeEnum.valueOf(theServerProps.getProperty(
                            SERVER_PROP_DB_TYPE,
                            DatabaseTypeEnum.Internal.getPropertiesId()));

            useHibernateC3p0Parms = true;
        }

        /*
         * Mantis #513: PostgreSQL: WebApp very slow.
         */
        if (useHibernateC3p0Parms) {
            /*
             * Minimum number of JDBC connections in the pool.
             */
            configOverrides.put("hibernate.c3p0.min_size", "5");
            /*
             * Maximum number of JDBC connections in the pool.
             */
            configOverrides.put("hibernate.c3p0.max_size", "400");

            /*
             * When an idle connection is removed from the pool (in second).
             * Hibernate default: 0, never expire.
             */
            configOverrides.put("hibernate.c3p0.timeout", "600");

            /*
             * Number of prepared statements will be cached. Increase
             * performance. Hibernate default: 0 , caching is disable.
             */
            configOverrides.put("hibernate.c3p0.max_statements", "50");

            /*
             * idle time in seconds before a connection is automatically
             * validated. Hibernate default: 0.
             */
            configOverrides.put("hibernate.c3p0.idle_test_period", "120");
        }

        //
        switch (this.myDatabaseType) {
        case Internal:
            initHibernateDerby(configOverrides);
            break;
        case PostgreSQL:
            initHibernatePostgreSQL(configOverrides);
            break;
        default:
            throw new SpException("Database type [" + this.myDatabaseType
                    + "] is NOT supported");
        }

        /*
         * "The configuration for entity managers both inside an application
         * server and in a standalone application reside in a persistence
         * archive. A persistence archive is a JAR file which must define a
         * persistence.xml file that resides in the META-INF folder."
         *
         * Use persistence.xml configuration. The map is a set of overrides that
         * will take precedence over any properties defined in your
         * persistence.xml file.
         *
         * See <persistence-unit name="savapage" ...> in
         * resources/META-INF/persistence.xml
         */
        final String PERSISTENCE_UNIT_NAME = "savapage";

        /*
         * Since Mantis #348.
         *
         * Code below gives Hibernate warning HHH015016: deprecated
         * javax.persistence.spi.PersistenceProvider (bug in Hibernate?).
         */

        // myEmf = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME,
        // configOverrides);

        /*
         * Since Mantis #348
         */
        final HibernatePersistenceProvider persistenceProvider =
                new HibernatePersistenceProvider();

        /*
         * "An entity manager factory is typically create at application
         * initialization time and closed at application end. It's creation is
         * an expensive process. For those who are familiar with Hibernate, an
         * entity manager factory is very much like a session factory. Actually,
         * an entity manager factory is a wrapper on top of a session factory.
         * Calls to the entityManagerFactory are thread safe."
         */
        this.myEmf =
                persistenceProvider.createEntityManagerFactory(
                        PERSISTENCE_UNIT_NAME, configOverrides);

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
     * @return
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
     * @return
     */
    synchronized public DbVersionInfo getDbVersionInfo() {
        if (myDbVersionInfo == null) {
            myDbVersionInfo = ServiceContext.getDaoContext().getDbVersionInfo();
        }
        return myDbVersionInfo;
    }

}
