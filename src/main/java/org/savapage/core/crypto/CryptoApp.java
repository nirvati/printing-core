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
package org.savapage.core.crypto;

import java.io.IOException;
import java.io.InputStream;
//import java.net.InetAddress;
//import java.net.NetworkInterface;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.Date;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;

import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.jpa.tools.DbTools;

import net.iharder.Base64;

/**
 * This class takes care for encryption of SavaPage Internals.
 *
 * @author Rijk Ravestein
 *
 */
public class CryptoApp {

    /**
     * Copied from {@link MemberCard#MD5_PUBLIC_KEY_MEMBERCARD}.
     *
     * IMPORTANT: Do NOT change, since it will invalidate all persistent
     * encrypted content.
     */
    private static final String PASSPHRASE = "5958cd46c39f140056190cd4ac2323e4";

    /**
     * IMPORTANT: Do NOT change, since it will invalidate all persistent
     * encrypted content.
     */
    private final byte[] mySalt = { (byte) 0xB1, (byte) 0x5C, (byte) 0x3D,
            (byte) 0x42, (byte) 0x67, (byte) 0x73, (byte) 0x2E, (byte) 0x40 };

    /**
     *
     */
    private String myDefaultSslPw = null;

    /**
     *
     */
    private Cipher myEcipher = null;

    /**
     *
     */
    private Cipher myDcipher = null;

    /**
     * Reads the unique password of the default SSL keystore which we generated
     * on install time.
     *
     * @throws IOException
     */
    private void readPw() throws IOException {
        myDefaultSslPw = null;
        InputStream istr = null;
        try {
            final Properties propsPw = new Properties();
            istr =
                    new java.io.FileInputStream(ConfigManager.getServerHome()
                            + "/data/default-ssl-keystore.pw");
            propsPw.load(istr);
            myDefaultSslPw = propsPw.getProperty("password");
        } finally {
            if (istr != null) {
                istr.close();
            }
        }
    }

    /**
     *
     * @throws IOException
     */
    public void init() throws IOException {
        initAsBasicLibrary();
        readPw();
    }

    /**
     * Init as basic library. SSL default password is not read.
     *
     * @throws IOException
     */
    public void initAsBasicLibrary() throws IOException {

        if (myEcipher != null) {
            /*
             * Already initialized
             */
            return;
        }

        final String passPhrase = PASSPHRASE;

        final int iterationCount = 19;

        try {
            // Create the key
            KeySpec keySpec =
                    new PBEKeySpec(passPhrase.toCharArray(), mySalt,
                            iterationCount);
            SecretKey key =
                    SecretKeyFactory.getInstance("PBEWithMD5AndDES")
                            .generateSecret(keySpec);
            myEcipher = Cipher.getInstance(key.getAlgorithm());
            myDcipher = Cipher.getInstance(key.getAlgorithm());

            // Prepare the parameter to the ciphers
            AlgorithmParameterSpec paramSpec =
                    new PBEParameterSpec(mySalt, iterationCount);

            // Create the ciphers
            myEcipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            myDcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
        } catch (Exception ex) {
            throw new SpException(ex);
        }

    }

    /**
     * @param str
     * @return
     */
    public String encrypt(String str) {
        try {
            // Encode the string into bytes using utf-8
            byte[] utf8 = str.getBytes("UTF8");
            // Encrypt
            byte[] enc = myEcipher.doFinal(utf8);
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
    public String decrypt(String str) {
        try {
            // Decode base64 to get bytes
            byte[] dec = Base64.decode(str);
            // Decrypt
            byte[] utf8 = myDcipher.doFinal(dec);
            // Decode using utf-8
            return new String(utf8, "UTF8");
        } catch (Exception ex) {
            throw new SpException(ex);
        }
    }
}
