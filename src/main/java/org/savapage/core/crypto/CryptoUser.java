/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.crypto;

import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;
import static java.nio.file.attribute.PosixFilePermission.OWNER_WRITE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.EnumSet;
import java.util.Properties;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.SpInfo;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.util.IOHelper;

import net.iharder.Base64;

/**
 * This class takes care for encryption of User Data.
 *
 * @author Rijk Ravestein
 *
 */
public final class CryptoUser {

    public static final String INTERNAL_USER_PW_CHECKSUM_PREFIX = "HASH:";

    private static final String PROP_CIPHER_PASSWORD = "cipher.password";
    private static final String PROP_CIPHER_SALT = "cipher.salt";
    private static final String PROP_CIPHER_INTERATION_COUNT =
            "cipher.iteration-count";
    private static final String PROP_HMAC_KEY = "hmac.key";

    private static String cipherPassword = null;
    private static int cipherIterationCount = 19;
    private byte[] cipherSalt = null;

    private static String hmacKey = null;

    private Cipher cipherEncrypt = null;
    private Cipher cipherDecrypt = null;

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link CryptoUser#getInstance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     * <p>
     * <a href=
     * "http://en.wikipedia.org/wiki/Singleton_pattern#The_solution_of_Bill_Pugh"
     * >The Singleton solution of Bill Pugh</a>
     * </p>
     */
    private static class SingletonHolder {
        public static final CryptoUser INSTANCE = new CryptoUser();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    private static CryptoUser getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Initialization of the private singleton so the
     * {@link ConfigManager#SERVER_REL_PATH_ENCRYPTION_PROPERTIES} gets lazy
     * created.
     *
     * @throws IOException
     */
    synchronized static public void init() throws IOException {
        getInstance();
    }

    /**
     *
     */
    private CryptoUser() {
        try {

            readProperties();

            /*
             * Create the key
             */
            KeySpec keySpec = new PBEKeySpec(cipherPassword.toCharArray(),
                    cipherSalt, cipherIterationCount);
            SecretKey key = SecretKeyFactory.getInstance("PBEWithMD5AndDES")
                    .generateSecret(keySpec);
            cipherEncrypt = Cipher.getInstance(key.getAlgorithm());
            cipherDecrypt = Cipher.getInstance(key.getAlgorithm());

            /*
             * Prepare the parameter to the ciphers
             */
            AlgorithmParameterSpec paramSpec =
                    new PBEParameterSpec(cipherSalt, cipherIterationCount);

            /*
             * Create the ciphers
             */
            cipherEncrypt.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            cipherDecrypt.init(Cipher.DECRYPT_MODE, key, paramSpec);

        } catch (Exception ex) {
            throw new SpException(ex);
        }
    }

    /**
     * Reads the properties
     * <p>
     * The {@link ConfigManager#SERVER_REL_PATH_ENCRYPTION_PROPERTIES} gets lazy
     * created.
     * </p>
     *
     * @throws IOException
     *             When file IO errors.
     *
     */
    private synchronized void readProperties() throws IOException {

        final int pwLength = 48;

        final Properties props = new Properties();
        final File fileProp = Paths
                .get(ConfigManager.getServerHome(),
                        ConfigManager.SERVER_REL_PATH_ENCRYPTION_PROPERTIES)
                .toFile();

        InputStream istr = null;
        Writer writer = null;

        try {

            boolean saveProps = !fileProp.exists();

            cipherPassword = null;
            cipherIterationCount = 0;
            cipherSalt = null;
            hmacKey = null;

            String cipherSaltTxt = null;

            if (!saveProps) {
                istr = new java.io.FileInputStream(fileProp);
                props.load(istr);
                istr.close();
                istr = null;
                cipherPassword = props.getProperty(PROP_CIPHER_PASSWORD);
                cipherIterationCount = Integer.valueOf(
                        props.getProperty(PROP_CIPHER_INTERATION_COUNT));
                cipherSaltTxt = props.getProperty(PROP_CIPHER_SALT);
                hmacKey = props.getProperty(PROP_HMAC_KEY);
            }

            if (cipherPassword == null) {
                cipherPassword = RandomStringUtils.randomAlphanumeric(pwLength);
                props.setProperty(PROP_CIPHER_PASSWORD, cipherPassword);
                saveProps = true;
            }

            if (cipherIterationCount == 0) {
                cipherIterationCount = 19;
                props.setProperty(PROP_CIPHER_INTERATION_COUNT,
                        String.valueOf(cipherIterationCount));
                saveProps = true;
            }

            if (cipherSaltTxt == null) {
                cipherSaltTxt = RandomStringUtils.randomAlphanumeric(8);
                props.setProperty(PROP_CIPHER_SALT, cipherSaltTxt);
                saveProps = true;
            }
            cipherSalt = cipherSaltTxt.getBytes();

            if (hmacKey == null) {
                hmacKey = RandomStringUtils.randomAlphanumeric(pwLength);
                props.setProperty(PROP_HMAC_KEY, hmacKey);
                saveProps = true;
            }

            if (saveProps) {

                writer = new FileWriter(fileProp);
                props.store(writer, "Keep the content of this "
                        + "file at a secure place.");

                writer.close();
                writer = null;

                final FileSystem fs = FileSystems.getDefault();
                final Set<PosixFilePermission> permissions =
                        EnumSet.of(OWNER_READ, OWNER_WRITE);

                Files.setPosixFilePermissions(
                        fs.getPath(fileProp.getAbsolutePath()), permissions);

                SpInfo.instance().log(String.format("Created %s",
                        ConfigManager.SERVER_REL_PATH_ENCRYPTION_PROPERTIES));
            }

        } finally {
            IOHelper.closeQuietly(writer);
            IOHelper.closeQuietly(istr);
        }
    }

    /**
     * @param str
     * @return
     */
    private String doEncrypt(String str) {
        try {
            // Encode the string into bytes using utf-8
            byte[] utf8 = str.getBytes("UTF8");
            // Encrypt
            byte[] enc = cipherEncrypt.doFinal(utf8);
            // Encode bytes to base64 to get a string
            return Base64.encodeBytes(enc);
        } catch (Exception ex) {
            throw new SpException(ex);
        }
    }

    /**
     * @param str
     * @return
     */
    private String doDecrypt(String str) {
        try {
            // Decode base64 to get bytes
            byte[] dec = Base64.decode(str);
            // Decrypt
            byte[] utf8 = cipherDecrypt.doFinal(dec);
            // Decode using utf-8
            return new String(utf8, "UTF8");
        } catch (Exception ex) {
            throw new SpException(ex);
        }
    }

    /**
     * @param str
     * @return
     */
    public static String encrypt(String str) {
        return getInstance().doEncrypt(str);
    }

    /**
     * @param str
     * @return
     */
    public static String decrypt(String str) {
        return getInstance().doDecrypt(str);
    }

    /**
     * Encrypts a User attribute value.
     * <p>
     * NOTE: When the attribute value to encrypt is null or empty an empty
     * String is returned.
     * <p>
     *
     * @see {@link #decryptUserAttr(Long, String)}
     * @param userKey
     *            The primary key of the User.
     * @param value
     *            The value to encrypt.
     * @return The encrypted value.
     */
    public static String encryptUserAttr(final Long userKey,
            final String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        return encrypt(String.format("%s|%s", userKey.toString(), value));
    }

    /**
     * Decrypts a User attribute value.
     * <p>
     * NOTE: When the encrypted attribute value is null or empty an empty String
     * is returned.
     * <p>
     *
     * @see {@link #encryptUserAttr(Long, String)}
     * @param userKey
     *            The primary key of the User.
     * @param encrypted
     *            The encrypted attribute value .
     * @return The decrypted attribute value .
     */
    public static String decryptUserAttr(final Long userKey,
            final String encrypted) {

        if (StringUtils.isBlank(encrypted)) {
            return "";
        }
        return StringUtils.removeStart(decrypt(encrypted),
                String.format("%s|", userKey.toString()));
    }

    /**
     * Creates a Hash-based message authentication code.
     * <p>
     * The algorithm used is:
     * <ul>
     * <li>Digest = Hash(message)</li>
     * <li>Signature = Hash(Digest || Key)</li>
     * </ul>
     * </p>
     * <p>
     * See
     * <a href="http://en.wikipedia.org/wiki/HMAC">http://en.wikipedia.org/wiki
     * /HMAC</a>.
     * <p>
     *
     * @param message
     *            The message.
     * @param sha1
     *            If {@code true} the SHA-1 digest is used. If {@code false} the
     *            MD5 digest is used.
     * @return The HMAC.
     * @throws UnsupportedEncodingException
     */
    public static String createHmac(final String message, final boolean sha1)
            throws UnsupportedEncodingException {

        byte[] bytes = message.getBytes("UTF-8");
        String digest;

        if (sha1) {
            digest = DigestUtils.sha1Hex(bytes);
        } else {
            digest = DigestUtils.md5Hex(bytes);
        }

        bytes = String.format("%s%s", digest, hmacKey).getBytes("UTF-8");
        if (sha1) {
            return DigestUtils.sha1Hex(bytes);
        } else {
            return DigestUtils.md5Hex(bytes);
        }
    }

    /**
     * Creates a hex formatted MD5 checksum from input string.
     *
     * @param input
     *            The input string.
     * @return The checksum.
     */
    public static String createMd5(final String input) {
        byte[] bytes;
        try {
            bytes = input.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            bytes = input.getBytes();
        }
        return DigestUtils.md5Hex(bytes);
    }

    /**
     * Creates a hex formatted SHA1 checksum from input string.
     *
     * @param input
     *            The input string.
     * @return The checksum.
     */
    public static String createSha1(final String input) {
        byte[] bytes;
        try {
            bytes = input.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            bytes = input.getBytes();
        }
        return DigestUtils.sha1Hex(bytes);
    }

    /**
     * Gets the hashed user password as string. The string is prefixed with
     * <code>HASH:</code>
     *
     * @param username
     * @param password
     * @return
     */
    public static String getHashedUserPassword(final String username,
            final String password) {
        final String input = String.format("%s|%s",
                username.trim().toLowerCase(), password.trim());
        return INTERNAL_USER_PW_CHECKSUM_PREFIX + createSha1(input);
    }

    /**
     * Checks if the password is valid for a user according to the checksum
     * applied.
     *
     * @param checkSum
     *            The checksum.
     * @param username
     * @param password
     * @return <code>true</code> if this is a valid password for the user.
     */
    public static boolean isUserPasswordValid(final String checkSum,
            final String username, final String password) {
        return getHashedUserPassword(username, password).equals(checkSum);
    }

}
