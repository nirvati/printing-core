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
package org.savapage.ext.google;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Properties;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.ServerPathEnum;
import org.savapage.core.util.DateUtil;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class GoogleLdapClient {

    /** */
    private static final String CLIENT_CERT_FILE_NAME =
            "google-ldap-client-cert.p12";
    /** */
    private static final String CLIENT_CERT_PROPS_FILE_NAME =
            "google-ldap-client-cert.pw";

    /** */
    private static final String PROP_KEY_PASSWORD = "password";

    /** */
    private static final String KEYSTORE_TYPE_PKCS12 = "PKCS12";
    /** */
    private static final String SSLCONTEXT_PROTOCOL_TLS = "TLS";
    /** */
    private static final String KEYMANAGERFACTORY_ALGORITHM_SUNX509 = "SunX509";

    /** */
    private static final String LOG_LINE_PREFIX = "Google Cloud LDAP Cert";

    /** */
    private static final int CERT_EXPIRE_NEARING_DAYS = 30;

    /** */
    private static class SingletonHolder {
        /** */
        public static final GoogleLdapClient INSTANCE = new GoogleLdapClient();
    }

    /** */
    private char[] clientKeyStorePassword;

    /** */
    private KeyStore clientKeyStore;

    /** */
    private SSLContext sslContext;

    /**  */
    private SSLSocketFactory sslSocketFactory;

    /**  */
    private Date certExpireDate;
    /**  */
    private Date certCreateDate;

    /** */
    private GoogleLdapClient() {
    }

    /**
     * @return The singleton instance.
     */
    private static GoogleLdapClient instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Initializes singleton instance.
     *
     * @return {@code true} if valid configuration.
     */
    public static boolean init() {
        return instance().initialize();
    }

    /**
     * @return {@code true} if valid configuration.
     */
    public static boolean isConfigured() {
        return getCertExpireDate() != null;
    }

    /**
     * @return Create date.
     */
    public static Date getCertCreateDate() {
        return instance().certCreateDate;
    }

    /**
     * @return Expire date.
     */
    public static Date getCertExpireDate() {
        return instance().certExpireDate;
    }

    /**
     * @param when
     *            Reference date.
     * @return {@code true} if certificate is expired.
     */
    public static boolean isCertExpired(final Date when) {
        return getCertExpireDate().before(when);
    }

    /**
     * @param when
     *            Reference date.
     * @return {@code true} if certificate expiration is nearing.
     */
    public static boolean isCertExpireNearing(final Date when) {
        return isCertExpired(DateUtils.addDays(when, CERT_EXPIRE_NEARING_DAYS));
    }

    /** */
    private void setCertDates() {
        try {
            final Enumeration<String> aliases = this.clientKeyStore.aliases();
            if (!aliases.hasMoreElements()) {
                throw new IllegalStateException("no expiry date found");
            }

            final String alias = aliases.nextElement();
            final X509Certificate c =
                    (X509Certificate) this.clientKeyStore.getCertificate(alias);
            this.certExpireDate = c.getNotAfter();
            this.certCreateDate = c.getNotBefore();

        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @return {@code true} if valid configuration.
     */
    private boolean initialize() {

        this.clientKeyStorePassword = readCertFilePassword();

        if (this.clientKeyStorePassword != null) {

            this.clientKeyStore = this.loadClientKeyStore();

            if (this.clientKeyStore != null) {
                this.sslContext = this.createSSLContext();
                this.sslSocketFactory = this.sslContext.getSocketFactory();
                this.setCertDates();
            }
        }
        return isConfigured();
    }

    /**
     * @return SSLSocketFactory
     */
    public static SSLSocketFactory getSSLSocketFactory() {
        return instance().sslSocketFactory;
    }

    /**
     * @return Log line with certificate create date.
     */
    public static String getCertCreateDateLogLine() {
        return String.format("%s Created [%s]", LOG_LINE_PREFIX,
                DateUtil.localizedLongMediumDateTime(
                        GoogleLdapClient.getCertCreateDate(), Locale.ENGLISH));
    }

    /**
     * @return Log line with certificate expire date.
     */
    public static String getCertExpireDateLogLine() {
        return String.format("%s Expires [%s]", LOG_LINE_PREFIX,
                DateUtil.localizedLongMediumDateTime(
                        GoogleLdapClient.getCertExpireDate(), Locale.ENGLISH));
    }

    /**
     * @return The client certificate properties file.
     */
    private static File getCertFileProps() {
        return Paths.get(ConfigManager.getServerHome(),
                ServerPathEnum.DATA.getPath(), CLIENT_CERT_PROPS_FILE_NAME)
                .toFile();
    }

    /**
     *
     * @return The client certificate file.
     */
    private static File getCertFile() {
        return Paths
                .get(ConfigManager.getServerHome(),
                        ServerPathEnum.DATA.getPath(), CLIENT_CERT_FILE_NAME)
                .toFile();
    }

    /**
     * Reads certificate password from properties file.
     *
     * @return The password or {@code null} if properties file does not exist or
     *         password key is not present.
     */
    private static char[] readCertFilePassword() {

        final File propsFile = getCertFileProps();

        if (!propsFile.exists()) {
            return null;
        }

        try (FileInputStream fis = new FileInputStream(propsFile);) {

            Properties props = new Properties();
            props.load(fis);

            if (props.containsKey(PROP_KEY_PASSWORD)) {
                return props.getProperty(PROP_KEY_PASSWORD).toCharArray();
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return null;
    }

    /**
     * Loads keystore from certificate file with client certificate and private
     * key.
     *
     * @return The {@link Keystore} or {@code null} if certificate file does not
     *         exist.
     */
    private KeyStore loadClientKeyStore() {

        final File certFile = getCertFile();

        if (!certFile.exists()) {
            return null;
        }
        try (FileInputStream fis = new FileInputStream(certFile);) {
            final KeyStore ks = KeyStore.getInstance(KEYSTORE_TYPE_PKCS12);
            ks.load(fis, this.clientKeyStorePassword);
            return ks;
        } catch (KeyStoreException | NoSuchAlgorithmException
                | CertificateException | IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     *
     * @return SSL Context.
     */
    private SSLContext createSSLContext() {

        try {
            final KeyManagerFactory kmf = KeyManagerFactory
                    .getInstance(KEYMANAGERFACTORY_ALGORITHM_SUNX509);
            kmf.init(this.clientKeyStore, this.clientKeyStorePassword);

            final SSLContext sc =
                    SSLContext.getInstance(SSLCONTEXT_PROTOCOL_TLS);
            sc.init(kmf.getKeyManagers(), null, null);

            return sc;

        } catch (KeyStoreException | NoSuchAlgorithmException
                | UnrecoverableKeyException | KeyManagementException e) {
            throw new IllegalStateException(e);
        }
    }

}
