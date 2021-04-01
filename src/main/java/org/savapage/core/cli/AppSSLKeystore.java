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
package org.savapage.core.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.RunModeEnum;
import org.savapage.core.config.ServerPathEnum;
import org.savapage.core.jpa.tools.DatabaseTypeEnum;
import org.savapage.core.util.IOHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class AppSSLKeystore extends AbstractApp {

    static {
        /*
         * IMPORTANT: Add Bouncy castle provider to java security. This is
         * needed so you can use the "BC" provider in security methods.
         */
        Security.addProvider(new BouncyCastleProvider());
    }

    /** */
    private static final String CLI_SWITCH_DEFAULT = "d";
    /** */
    private static final String CLI_SWITCH_DEFAULT_LONG = "default";

    /** */
    private static final String CLI_OPTION_CREATE = "create";

    /** */
    private static final String CLI_SWITCH_FORCE = "f";
    /** */
    private static final String CLI_SWITCH_FORCE_LONG = "force";

    /** */
    private static final String CLI_SWITCH_NO_CA_LONG = "no-ca";

    /** */
    private static final String CLI_OPTION_COMMON_NAME = "common-name";
    /** */
    private static final String CLI_OPTION_SUBJECT_ALT_DNS_NAME = "dns-name";

    /** */
    private final String keystorePathDefault;

    /** */
    private final String hostnameDefault;

    /**
     * Password of the keystore.
     */
    private String keystorePassword;

    /**
     * Password for the SSL certificate key entry in the keystore.
     */
    private String keyEntryPassword;

    /**
     *
     * @throws UnknownHostException
     *             If the default hostname could not be determined
     */
    private AppSSLKeystore() throws UnknownHostException {

        final String relPath =
                ServerPathEnum.DATA.getPath() + "/default-ssl-keystore";

        if (ConfigManager.getServerHome() == null) {
            this.keystorePathDefault = relPath;
        } else {
            this.keystorePathDefault =
                    ConfigManager.getServerHome() + "/" + relPath;
        }
        this.hostnameDefault = InetAddress.getLocalHost().getHostName();
    }

    /**
     *
     * @throws IOException
     *             When password file could not be read or written.
     */
    private synchronized void lazyCreateDefaultPasswords() throws IOException {

        final int pwLength = 48;
        final String propKeyPassword = "password";

        final Properties props = new Properties();
        final File filePw = new File(ConfigManager.getServerHome() + "/"
                + ServerPathEnum.DATA.getPath() + "/default-ssl-keystore.pw");

        InputStream istr = null;
        Writer writer = null;
        String pw = null;

        try {

            if (!filePw.exists()) {
                writer = new FileWriter(filePw);
                pw = RandomStringUtils.randomAlphanumeric(pwLength);
                props.setProperty(propKeyPassword, pw);
                props.store(writer, "Keep the contents of this "
                        + "file at a secure place.");
            } else {
                istr = new java.io.FileInputStream(filePw);
                props.load(istr);
                pw = props.getProperty(propKeyPassword);
            }

        } finally {
            IOHelper.closeQuietly(writer);
            IOHelper.closeQuietly(istr);
        }

        this.keystorePassword = pw;
        this.keyEntryPassword = pw;
    }

    /**
     * Creates a keystore with a self-signed SSL certificate.
     *
     * http://www.bouncycastle.org/wiki/display/JA1/BC+Version+2+APIs
     *
     * http://stackoverflow.com/questions/4828818/problem-obtaining-public-key-
     * from-subjectpublickeyinfo
     *
     * @param keystoreFile
     *            The keystore file.
     * @param holderCommonName
     *            CN name of the holder.
     * @param subjectAlternativeName
     *            Certificate Subject Alternative Name Extension If
     *            {@code null}. not applicable.
     * @param isCA
     *            If {@code true}, make certificate a Cert Authority (CA).
     * @throws Exception
     *             When things went wrong.
     */
    private void createKeystore(final File keystoreFile,
            final String holderCommonName, final String subjectAlternativeName,
            final boolean isCA) throws Exception {
        /*
         * GENERATE THE PUBLIC/PRIVATE RSA KEY PAIR
         */
        final int keysize = 2048; // 1024, 2048, 4096

        final KeyPairGenerator keyPairGenerator =
                KeyPairGenerator.getInstance("RSA", "BC");

        keyPairGenerator.initialize(keysize, new SecureRandom());
        final KeyPair keyPair = keyPairGenerator.generateKeyPair();

        /*
         * Yesterday, as notBefore date.
         */
        final Date dateNotBefore =
                new Date(System.currentTimeMillis() - 24L * 60L * 60L * 1000L);
        /*
         * 10 Years After, as notAfter date.
         */
        final Date dateNotAfter = new Date(System.currentTimeMillis()
                + (10L * 365 * 24L * 60L * 60L * 1000L));

        /*
         * GENERATE THE X509 CERTIFICATE.
         */
        final byte[] publickeyb = keyPair.getPublic().getEncoded();

        final SubjectPublicKeyInfo subPubKeyInfo = SubjectPublicKeyInfo
                .getInstance(ASN1Primitive.fromByteArray(publickeyb));

        // Mantis #560
        final X500Name holder = new X500Name("CN=" + holderCommonName);
        final X500Name issuer = holder;

        // Mantis #560
        final BigInteger serial =
                BigInteger.valueOf(System.currentTimeMillis());

        //
        // Examples:
        //
        // "CN=SavaPage Self-Signed Certificate, OU=SavaPage, O=Datraverse B.V.,
        // L=Almere, ST=Flevoland, C=NL";
        //
        // "CN=Apache Wicket Quickstart Certificate, OU=Apache Wicket, " +
        // "O=The Apache Software Foundation, L=Unknown, ST=Unknown, C=Unknown";
        //

        final X509v3CertificateBuilder certBuilder =
                new X509v3CertificateBuilder(issuer, serial, dateNotBefore,
                        dateNotAfter, holder, subPubKeyInfo);
        if (StringUtils.isNotBlank(subjectAlternativeName)) {
            // Mantis #1179.
            certBuilder.addExtension(Extension.subjectAlternativeName, false,
                    new GeneralNames(new GeneralName(GeneralName.dNSName,
                            subjectAlternativeName)));
        }
        if (isCA) {
            // This extension makes our cert a Cert Authority (CA) so it can be
            // imported into a browser as trusted CA. Mantis #1179.
            certBuilder.addExtension(Extension.basicConstraints, true,
                    new BasicConstraints(true));
        }

        // Mantis #561
        final ContentSigner contentSigner =
                new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC")
                        .build(keyPair.getPrivate());

        final X509CertificateHolder certHolder =
                certBuilder.build(contentSigner);

        final X509Certificate cert = new JcaX509CertificateConverter()
                .setProvider("BC").getCertificate(certHolder);

        // Create empty keystore.
        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(null);

        // Fill keystore.
        final String alias = CommunityDictEnum.SAVAPAGE.getWord();

        ks.setKeyEntry(alias, keyPair.getPrivate(),
                this.keyEntryPassword.toCharArray(),
                new X509Certificate[] { cert });

        // Write keystore.
        try (FileOutputStream ostr = new FileOutputStream(keystoreFile);) {
            ks.store(ostr, this.keystorePassword.toCharArray());
        }
    }

    /**
     *
     * @param ksFile
     *            The keystore file.
     * @param password
     *            The keystore password
     * @throws Exception
     *             When IO errors or certificate errors.
     */
    @SuppressWarnings("unused")
    private void displayKeystore(final File ksFile, final String password)
            throws Exception {

        final KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(ksFile), password.toCharArray());

        final String alias = ks.aliases().nextElement();
        final X509Certificate t = (X509Certificate) ks.getCertificate(alias);

        getDisplayStream().println("Version    : " + t.getVersion());
        getDisplayStream()
                .println("Serial#    : " + t.getSerialNumber().toString(16));
        getDisplayStream().println("SubjectDN  : " + t.getSubjectDN());
        getDisplayStream().println("IssuerDN   : " + t.getIssuerDN());
        getDisplayStream().println("NotBefore  : " + t.getNotBefore());
        getDisplayStream().println("NotAfter   : " + t.getNotAfter());
        getDisplayStream().println("SigAlgName : " + t.getSigAlgName());

        // byte[] sig = t.getSignature();
        // getDisplayStream().println(new BigInteger(sig).toString(16));
        // PublicKey pk = t.getPublicKey();
        // byte[] pkenc = pk.getEncoded();
        // for (int i = 0; i < pkenc.length; i++) {
        // getDisplayStream().print(pkenc[i] + ",");
        // }
    }

    @Override
    protected Options createCliOptions() throws Exception {

        final Options options = new Options();

        options.addOption(CLI_SWITCH_HELP, CLI_SWITCH_HELP_LONG, false,
                "Displays this help text.");

        options.addOption(CLI_SWITCH_DEFAULT, CLI_SWITCH_DEFAULT_LONG, false,
                "Creates the default keystore file '"
                        + new File(this.keystorePathDefault).getAbsolutePath()
                        + "'.");

        options.addOption(Option.builder().hasArg(true).argName("FILE")
                .longOpt(CLI_OPTION_CREATE)
                .desc("Creates a specific keystore file.").build());

        options.addOption(Option.builder().hasArg(true).argName("NAME")
                .longOpt(CLI_OPTION_COMMON_NAME)
                .desc("Subject and Issuer CN of the SSL Certificate "
                        + "(optional). If not set the computer hostname '"
                        + this.hostnameDefault + "' is used.")
                .build());

        options.addOption(Option.builder().hasArg(true).argName("NAME")
                .longOpt(CLI_OPTION_SUBJECT_ALT_DNS_NAME)
                .desc("Subject Alternative DNS Name "
                        + "of the SSL Certificate (optional). "
                        + "If not set the value of --" + CLI_OPTION_COMMON_NAME
                        + " is used.")
                .build());

        options.addOption(CLI_SWITCH_FORCE, CLI_SWITCH_FORCE_LONG, false,
                "Force. Overwrites any existing keystore file.");

        options.addOption(
                Option.builder().hasArg(false).longOpt(CLI_SWITCH_NO_CA_LONG)
                        .desc("SSL Certificate Issuer is not a "
                                + "Certificate Authority (CA).")
                        .build());

        return options;
    }

    @Override
    protected int run(final String[] args) throws Exception {

        final String cmdLineSyntax = "[OPTION]...";

        // ......................................................
        // Parse parameters from CLI
        // ......................................................
        final Options options = createCliOptions();
        final CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (org.apache.commons.cli.ParseException e) {
            getDisplayStream().println(e.getMessage());
            this.usage(cmdLineSyntax, options);
            return EXIT_CODE_PARMS_PARSE_ERROR;
        }

        // ......................................................
        // Help needed?
        // ......................................................
        if (args.length == 0 || cmd.hasOption(CLI_SWITCH_HELP)
                || cmd.hasOption(CLI_SWITCH_HELP_LONG)) {
            this.usage(cmdLineSyntax, options);
            return EXIT_CODE_OK;
        }

        // ......................................................
        //
        // ......................................................
        this.lazyCreateDefaultPasswords();

        // ......................................................
        //
        // ......................................................
        final String commonName = cmd.getOptionValue(CLI_OPTION_COMMON_NAME,
                this.hostnameDefault);

        final String subjectAlternativeName =
                cmd.getOptionValue(CLI_OPTION_SUBJECT_ALT_DNS_NAME, commonName);

        final boolean forceCreate = (cmd.hasOption(CLI_SWITCH_FORCE)
                || cmd.hasOption(CLI_SWITCH_FORCE_LONG));

        final boolean isCA = !cmd.hasOption(CLI_SWITCH_NO_CA_LONG);

        /*
         * Create the default key store
         */
        if (cmd.hasOption(CLI_SWITCH_DEFAULT)
                || cmd.hasOption(CLI_SWITCH_DEFAULT_LONG)) {

            final File file = new File(this.keystorePathDefault);
            final boolean exists = file.exists();

            if (forceCreate || !exists) {
                this.createKeystore(file, commonName, subjectAlternativeName,
                        isCA);
            }

            if (!forceCreate && exists) {
                getDisplayStream().println(
                        "SSL key store not created. File already exists.");
            }

            return EXIT_CODE_OK;
        }

        /*
         * Create the custom key store
         */
        final File keystore = new File(cmd.getOptionValue(CLI_OPTION_CREATE,
                this.keystorePathDefault));

        if (keystore.isDirectory()) {
            getErrorDisplayStream().println("Error: keystore "
                    + keystore.getAbsolutePath() + " refers to a directory");
            return EXIT_CODE_ERROR;
        }

        if (keystore.exists() && !forceCreate) {
            getErrorDisplayStream().println(
                    "Error: SSL key store " + keystore.getAbsolutePath()
                            + " already exists. " + "Use the --"
                            + CLI_SWITCH_FORCE_LONG + " option to overwrite");
            return EXIT_CODE_ERROR;
        }

        this.createKeystore(keystore, commonName, subjectAlternativeName, isCA);

        return EXIT_CODE_OK;
    }

    @Override
    protected void onInit() throws Exception {
        /*
         * Initialize as basic library. See {@link
         * ConfigManager#initAsBasicLibrary(Properties)}.
         */
        ConfigManager.instance().init(RunModeEnum.LIB,
                DatabaseTypeEnum.Internal);
    }

    /**
     * IMPORTANT: MUST return void, use System.exit() to get an exit code on JVM
     * execution.
     *
     * @param args
     *            The command-line arguments.
     */
    public static void main(final String[] args) {
        int status = EXIT_CODE_EXCEPTION;
        try {
            final AppSSLKeystore app = new AppSSLKeystore();
            status = app.run(args);
        } catch (Exception e) {
            getErrorDisplayStream().println(e.getMessage());
        }
        System.exit(status);
    }

}
