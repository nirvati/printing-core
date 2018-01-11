/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.community;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang3.time.DateUtils;
import org.savapage.core.SpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class MemberCardManager {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(MemberCardManager.class);

    private static final String CARD_MODULE_PROP_HYPHEN = "-";
    public static final String CARD_PROP_PROCUCT_DELIMITER = ",";
    public static final String CARD_PROP_DATE_FORMAT = "yyyy-MM-dd";

    // private static final String PROP_VAL_TRUE = "true";
    private static final String PROP_VAL_FALSE = "false";

    // Generic: property key
    private static final String CARD_PROP_SIGNATURE = "signature";

    // Product: property keys
    public static final String CARD_PROP_COMMUNITY = "community";
    public static final String CARD_PROP_MEMBERSHIP_MODULES =
            "membership-modules";
    public static final String CARD_PROP_ISSUED_BY = "issued-by";
    public static final String CARD_PROP_ISSUED_DATE = "issued-date";
    public static final String CARD_PROP_EXPIRY_DATE = "expiry-date";
    public static final String CARD_PROP_MEMBER_NAME = "member-name";
    public static final String CARD_PROP_UNIQUE_ID = "unique-id";
    public static final String CARD_PROP_VISITOR = "visitor";

    // Product: property values
    public static final String CARD_VAL_ISSUED_BY = "Datraverse B.V.";

    // Module : property keys
    private static final String CARD_PROP_SAVAPAGE_VERSION = "version-";

    public static final String CARD_PROP_SAVAPAGE_VERSION_MAJOR =
            CARD_PROP_SAVAPAGE_VERSION + "major";

    public static final String CARD_PROP_SAVAPAGE_VERSION_MINOR =
            CARD_PROP_SAVAPAGE_VERSION + "minor";

    public static final String CARD_PROP_SAVAPAGE_VERSION_REVISION =
            CARD_PROP_SAVAPAGE_VERSION + "revision";

    // -------------------------------------------------------
    private static final String ZIP_ENTRY_FILENAME = "membercard.txt";

    /**
     * @deprecated
     */
    private enum eBytesToSign {
        /**
         * skip \r and \n.
         */
        SKIP_CRLF,
        /**
         *
         */
        ALL_BYTES
    }

    /**
     *
     * @param strModule
     * @param strProp
     * @return
     */
    public static String getModuleProperty(final String strModule,
            final String strProp) {
        return strModule + CARD_MODULE_PROP_HYPHEN + strProp;
    }

    /**
     * Get the formatted MAC address of the first encountered network interface
     * of this machine. If more than one network interface is found the first
     * one encountered is returned.
     *
     * Format is hex like: 08-00-27-DC-4A-9E
     *
     * @return The formatted hex string of the MAC address of the first network
     *         interface. Null is returned if no network interfaces can be
     *         found.
     * @throws SocketException
     *             If an I/O
     */
    public static final String getMacAddressFromLocalHost()
            throws SocketException {

        String strMacAddressFound = null;

        final Enumeration<NetworkInterface> niEnum =
                NetworkInterface.getNetworkInterfaces();

        while (niEnum.hasMoreElements()) {

            strMacAddressFound = null;

            final NetworkInterface ni = niEnum.nextElement();
            final byte[] mac = ni.getHardwareAddress();

            if (null != mac) {
                /*
                 * Extract each array of mac address and convert it to hex with
                 * the following format 08-00-27-DC-4A-9E.
                 */
                strMacAddressFound = "";

                for (int i = 0; i < mac.length; i++) {
                    if (i > 0) {
                        strMacAddressFound += "-";
                    }
                    strMacAddressFound += String.format("%02X", mac[i]);
                }

                break;
            }
        }
        return strMacAddressFound;
    }

    /**
     *
     * @param istr
     * @return MD5 string
     * @throws MemberCardException
     */
    public String getMD5(java.io.InputStream istr) throws MemberCardException {

        final java.io.ByteArrayOutputStream bos =
                new java.io.ByteArrayOutputStream();

        final String strKey = "@#$%^&*("; // some fixed random string
        String strRet = null;

        try {
            final byte[] aByte = new byte[2048];
            int nBytes = istr.read(aByte);
            while (-1 < nBytes) {
                bos.write(aByte, 0, nBytes);
                nBytes = istr.read(aByte);
            }

            final MessageDigest md5 = MessageDigest.getInstance("MD5");

            md5.update(bos.toByteArray());
            strRet = mySignatureBytesAsString(md5.digest(strKey.getBytes()));

        } catch (Exception e) {
            throw new MemberCardException(e.getMessage(), e);
        }
        return strRet;
    }

    /**
     * This is a one-time action to generate public and private key. The private
     * key is used to sign the Member Card. The public key is part of this jar.
     */
    public void generateKeyPair(final String cstrPublicKeyFile,
            final String cstrPrivateKeyFile) throws MemberCardException {

        try {

            final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");

            keyGen.initialize(1024);

            final KeyPair pair = keyGen.generateKeyPair();
            final PrivateKey privateMemberCardKey = pair.getPrivate();
            final PublicKey publicMemberCardKey = pair.getPublic();

            writeKeyToFile(publicMemberCardKey, cstrPublicKeyFile);
            writeKeyToFile(privateMemberCardKey, cstrPrivateKeyFile);

        } catch (Exception ex) {
            throw new MemberCardException(ex.getMessage(), ex);
        }
    }

    /**
     * see http://forum.java.sun.com/thread.jspa?threadID=5200692&messageID=
     * 9797698
     *
     * @param key
     * @param cstrKeyFile
     * @throws {@link
     *             MemberCardException}
     */
    private void writeKeyToFile(java.security.Key key, final String cstrKeyFile)
            throws MemberCardException {

        FileOutputStream fos = null;
        ObjectOutputStream oos = null;

        try {
            fos = new FileOutputStream(new File(cstrKeyFile));
            oos = new ObjectOutputStream(fos);
            oos.writeObject(key);

            oos.close();
            oos = null;

            fos.close();
            fos = null;

        } catch (IOException ex) {

            throw new MemberCardException(ex.getMessage(), ex);

        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }
    }

    /**
     *
     * @param cstrKeyFile
     * @param key
     */
    private java.security.Key readKeyFromFile(final String cstrKeyFile)
            throws MemberCardException {

        java.security.Key key = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {

            fis = new FileInputStream(new File(cstrKeyFile));
            ois = new ObjectInputStream(fis);
            key = (java.security.Key) ois.readObject();

            ois.close();
            ois = null;

            fis.close();
            fis = null;

        } catch (IOException ex) {
            throw new MemberCardException(ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            throw new MemberCardException(ex.getMessage(), ex);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }
        return key;
    }

    /**
     *
     * @param cstrKeyFile
     * @param key
     */
    private java.security.Key readKeyFromStream(final java.io.InputStream is)
            throws MemberCardException {

        java.security.Key key = null;
        ObjectInputStream ois = null;

        try {
            ois = new ObjectInputStream(is);
            key = (java.security.Key) ois.readObject();
        } catch (IOException ex) {
            throw new MemberCardException(ex.getMessage(), ex);
        } catch (ClassNotFoundException ex) {
            throw new MemberCardException(ex.getMessage(), ex);
        } finally {
            if (ois != null) {
                try {
                    ois.close();
                } catch (IOException ex) {
                    LOGGER.error(ex.getMessage(), ex);
                }
            }
        }
        return key;
    }

    /**
     *
     * @param str
     * @return
     */
    private byte[] mySignatureStringAsBytes(String strSignature)
            throws MemberCardException {

        final ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            for (int i = 0; i < strSignature.length(); i += 2) {
                String strHex = strSignature.substring(i, i + 2);
                bos.write((byte) Integer.parseInt(strHex, 16));
            }
        } catch (Exception ex) {
            throw new MemberCardException(
                    "Error interpreting signature [" + strSignature + "]", ex);
        }

        return bos.toByteArray();
    }

    /**
     *
     * @param bytes
     * @return
     */
    private String mySignatureBytesAsString(byte[] bytes) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02x", bytes[i]));
        }
        return sb.toString();
    }

    /**
     *
     * @return the key as string
     * @throws {@link
     *             MemberCardException}
     */
    public String publicMemberCardKeyAsString(java.io.InputStream istrPublicKey)
            throws MemberCardException {
        return readKeyFromStream(istrPublicKey).toString();
    }

    /**
     *
     * @return
     * @throws {@link
     *             MemberCardException}.
     */
    public java.security.PublicKey getPublicKeyMemberCard(
            java.io.InputStream istrPublicKey) throws MemberCardException {
        return (PublicKey) readKeyFromStream(istrPublicKey);
    }

    public String createSignatureString(final String cstrPrivateKeyFile,
            java.io.InputStream istr) throws MemberCardException {
        final PrivateKey privateMemberCardKey =
                (PrivateKey) readKeyFromFile(cstrPrivateKeyFile);
        return createSignatureString(privateMemberCardKey, istr);
    }

    /**
     * Create a signature string of InputStream.
     *
     * Note: Successive creation of signature on same content leads to different
     * signatures. This is because a source of randomness is used in initSign()
     * method of Signature.
     *
     * @param cstrPrivateKeyFile
     * @param istr
     * @return the signature string
     * @throws {@link
     *             MemberCardException}
     */
    public String createSignatureString(final PrivateKey privateMemberCardKey,
            final java.io.InputStream istr) throws MemberCardException {

        final eBytesToSign b2s = eBytesToSign.ALL_BYTES;

        String strRet = null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            byte[] aByte = new byte[2048];
            int nBytes = istr.read(aByte);
            while (-1 < nBytes) {
                if (eBytesToSign.SKIP_CRLF == b2s) {
                    for (int i = 0; i < nBytes; i++) {
                        if ((10 != aByte[i]) && (13 != aByte[i])) {
                            bos.write(aByte[i]);
                        }
                    }
                } else {
                    bos.write(aByte, 0, nBytes);
                }
                nBytes = istr.read(aByte);
            }
            bos.flush();

            // Sign with private key
            Signature dsa = Signature.getInstance("SHA1withDSA");
            dsa.initSign(privateMemberCardKey);
            dsa.update(bos.toByteArray());
            byte[] signature = dsa.sign();

            strRet = mySignatureBytesAsString(signature);

        } catch (Exception ex) {
            throw new MemberCardException(ex.getMessage(), ex);
        }
        return strRet;
    }

    /**
     *
     * @param cstrPrivateKeyFile
     *            the name of the file with private key
     * @param cstrMemberCardFileIn
     *            The name of the file to sign
     * @param ostrSignedMemberCard
     *            OutputStream with (zipped) signed MemberCard.
     * @param bZip
     *            {@code true} if output needs to zipped.
     * @throws MemberCardException
     *             When errors occur.
     */
    public void signMemberCard(final String cstrPrivateKeyFile,
            final String cstrMemberCardFileIn,
            final java.io.OutputStream ostrSignedMemberCard, final boolean bZip)
            throws MemberCardException {

        try {

            final java.io.InputStream istrPrivateKey =
                    new java.io.FileInputStream(cstrPrivateKeyFile);
            final java.io.InputStream istrToSign =
                    new java.io.FileInputStream(cstrMemberCardFileIn);

            signMemberCard(istrPrivateKey, istrToSign, ostrSignedMemberCard,
                    bZip);

        } catch (Exception ex) {
            throw new MemberCardException(ex.getLocalizedMessage(), ex);
        }

    }

    /**
     *
     * @param istrPrivateKey
     *            the name of the file with private key
     * @param istrToSign
     *            input stream to sign
     * @param ostrSigned
     *            OutputStream with (zipped) signed Member Card.
     * @param bZip
     *            {@code true} if output needs to zipped.
     * @throws MemberCardException
     *             When errors occur.
     */
    public void signMemberCard(final java.io.InputStream istrPrivateKey,
            final java.io.InputStream istrToSign,
            final java.io.OutputStream ostrSigned, final boolean bZip)
            throws MemberCardException {

        ZipOutputStream zipOut = null;

        try {
            final PrivateKey privateMemberCardKey =
                    (PrivateKey) readKeyFromStream(istrPrivateKey);

            final java.io.BufferedInputStream istrMemberCard =
                    new java.io.BufferedInputStream(istrToSign);

            istrMemberCard.mark(1024 * 1024 * 10); // reserve 10 MB // TODO:
                                                   // magic
                                                   // number
            String cstrSignature = CARD_PROP_SIGNATURE + "=";
            cstrSignature +=
                    createSignatureString(privateMemberCardKey, istrMemberCard)
                            + "\n";
            istrMemberCard.reset();

            java.io.OutputStream oos = null;

            if (bZip) {
                zipOut = new ZipOutputStream(ostrSigned);
                zipOut.putNextEntry(new ZipEntry(ZIP_ENTRY_FILENAME));
                oos = zipOut;
            } else {
                oos = ostrSigned;
            }

            oos.write(cstrSignature.getBytes());

            byte[] aByte = new byte[2048];
            int nBytes = istrMemberCard.read(aByte);
            while (-1 < nBytes) {
                oos.write(aByte, 0, nBytes);
                nBytes = istrMemberCard.read(aByte);
            }

            if (bZip) {
                ((ZipOutputStream) oos).closeEntry();
                ((ZipOutputStream) oos).close();
            }

        } catch (Exception ex) {

            throw new MemberCardException(ex.getLocalizedMessage(), ex);

        } finally {
            if (zipOut != null) {
                try {
                    zipOut.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     *
     * @return The properties.
     */
    public Map<String, String> getEditableMemberCardProperties() {

        final SimpleDateFormat formatter =
                new SimpleDateFormat(CARD_PROP_DATE_FORMAT);

        final Date today = new Date();
        final Date nextYearDay = DateUtils.addYears(today, 1);

        final Map<String, String> map = new HashMap<String, String>();

        map.put(CARD_PROP_ISSUED_BY, CARD_VAL_ISSUED_BY);
        map.put(CARD_PROP_MEMBER_NAME, null);
        map.put(CARD_PROP_ISSUED_DATE, formatter.format(today));
        map.put(CARD_PROP_EXPIRY_DATE, formatter.format(nextYearDay));
        map.put(CARD_PROP_VISITOR, PROP_VAL_FALSE);

        return map;
    }

    /**
     *
     * @param strDate
     *            The date as String.
     * @return {@code true} when date is valid.
     */
    public static boolean isMemberCardDateValid(final String strDate) {

        final SimpleDateFormat formatter =
                new SimpleDateFormat(CARD_PROP_DATE_FORMAT);
        try {
            formatter.parse(strDate);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    /**
     *
     * @param value
     *            The boolean as String.
     * @return {@code true} when boolean is valid.
     */
    public static boolean isMemberCardBoolValid(final String value) {
        return value.equals(Boolean.TRUE.toString())
                || value.equals(Boolean.FALSE.toString());
    }

    /**
     *
     * @param key
     *            The key.
     * @param value
     *            The value.
     * @return {@code true} when value is valid or key is unknown.
     */
    public static boolean checkEditableMemberCardProperty(final String key,
            final String value) {

        final boolean isValid;

        switch (key) {

        case CARD_PROP_ISSUED_DATE:
        case CARD_PROP_EXPIRY_DATE:
            isValid = isMemberCardDateValid(value);
            break;

        case CARD_PROP_VISITOR:
            isValid = isMemberCardBoolValid(value);
            break;

        default:
            isValid = true;
            break;
        }
        return isValid;
    }

    /**
     *
     * @param module
     *            The module.
     * @return The properties.
     */
    public Map<String, String>
            getEditableMemberCardProperties(final IMembershipModule module) {
        return module.getEditableMemberCardProperties();
    }

    /**
     *
     * @return The properties.
     */
    public Map<String, String> getMemberCardProperties() {
        final Map<String, String> map = new HashMap<String, String>();
        map.put(CARD_PROP_UNIQUE_ID,
                String.valueOf(System.currentTimeMillis()));
        return map;
    }

    /**
     *
     * @param module
     *            The module
     * @return The properties.
     */
    public Map<String, String>
            getMemberCardProperties(final IMembershipModule module) {

        final String pfxCardProp = module.getModule() + CARD_MODULE_PROP_HYPHEN;

        final Map<String, String> map = new HashMap<String, String>();

        map.put(pfxCardProp + CARD_PROP_SAVAPAGE_VERSION_MAJOR,
                module.getVersionMajor());
        map.put(pfxCardProp + CARD_PROP_SAVAPAGE_VERSION_MINOR,
                module.getVersionMinor());
        map.put(pfxCardProp + CARD_PROP_SAVAPAGE_VERSION_REVISION,
                module.getVersionRevision());
        map.putAll(module.getMemberCardProperties());

        return map;
    }

    /**
     *
     * @param props
     * @param module
     * @throws MemberCardException
     */
    private void checkMemberCardExpiryDate(final Properties props,
            final IMembershipModule module) throws MemberCardExpiredException {

        String strDateExpire = props.getProperty(CARD_PROP_EXPIRY_DATE, "");

        if (strDateExpire.length() == 0) {

            // this is an open ended MemberCard.

        } else {

            final Date dateNow = new Date(); // Get today's date
            final SimpleDateFormat formatter =
                    new SimpleDateFormat(CARD_PROP_DATE_FORMAT);

            try {
                final Date dateMemberCard = formatter.parse(strDateExpire);
                // .........................................................
                // dateformat.parse will accept any date as long as it's
                // in the format you defined, it simply rolls dates over,
                // for example, december 32 becomes jan 1 and december 0
                // becomes november 30
                // .........................................................
                if (!formatter.format(dateMemberCard).equals(strDateExpire)) {
                    throw new SpException(CommunityDictEnum.MEMBERSHIP.getWord()
                            + " expiry date [" + strDateExpire
                            + "] is an INVALID date");
                }
                if (dateNow.after(dateMemberCard)) {
                    throw new MemberCardExpiredException(
                            CommunityDictEnum.MEMBERSHIP.getWord()
                                    + " expired since [" + strDateExpire + "]");
                }
            } catch (ParseException ex) {
                throw new SpException(CommunityDictEnum.MEMBERSHIP.getWord()
                        + " expiry date [" + strDateExpire + "] : "
                        + ex.getMessage());
            }
        }
    }

    /**
     *
     * @param props
     * @param module
     * @throws MemberCardException
     */
    private void checkMemberCardIssuedDate(final Properties props,
            final IMembershipModule module) throws MemberCardExpiredException {

        final String strDateIssued =
                props.getProperty(MemberCardManager.CARD_PROP_ISSUED_DATE);

        if (null == strDateIssued) {

            throw new SpException(CommunityDictEnum.MEMBERSHIP.getWord()
                    + " property [" + MemberCardManager.CARD_PROP_ISSUED_DATE
                    + "] not found");

        } else {

            final Date dateNow = new Date(); // Get today's date

            final SimpleDateFormat formatter =
                    new SimpleDateFormat(CARD_PROP_DATE_FORMAT);

            try {
                final Date dateMemberCard = formatter.parse(strDateIssued);
                // .........................................................
                // dateformat.parse will accept any date as long as it's
                // in the format you defined, it simply rolls dates over,
                // for example, december 32 becomes jan 1 and december 0
                // becomes november 30
                // .........................................................
                if (!formatter.format(dateMemberCard).equals(strDateIssued)) {
                    throw new SpException(CommunityDictEnum.MEMBERSHIP.getWord()
                            + " issued date [" + strDateIssued
                            + "] is an INVALID date");
                }
                if (dateNow.before(dateMemberCard)) {
                    throw new MemberCardExpiredException(
                            CommunityDictEnum.MEMBERSHIP.getWord()
                                    + " cannot start before [" + strDateIssued
                                    + "]");
                }
            } catch (ParseException ex) {
                throw new SpException(CommunityDictEnum.MEMBERSHIP.getWord()
                        + " expiry date [" + strDateIssued + "] : "
                        + ex.getMessage());
            }
        }
    }

    /**
     *
     * @param memberCardProps
     * @param module
     * @throws MemberCardException
     */
    private void checkMembershipPeriod(final Properties memberCardProps,
            final IMembershipModule module) throws MemberCardExpiredException {
        checkMemberCardExpiryDate(memberCardProps, module);
        checkMemberCardIssuedDate(memberCardProps, module);
    }

    /**
     *
     * @param props
     * @param module
     * @throws MemberCardException
     * @throws MemberCardExpiredException
     */
    public void checkMemberCardProperties(final Properties props,
            final IMembershipModule module) throws MemberCardException,
            MemberCardExpiredException, MemberCardFatalException {

        String key = null;
        String value = null;

        // .............................................
        // Check product
        // .............................................
        {
            key = MemberCardManager.CARD_PROP_COMMUNITY;
            value = props.getProperty(key);
            if (null == value) {
                throw new MemberCardException(key + " not found");
            }
            if (!value.equals(module.getProduct())) {
                throw new MemberCardException(
                        CommunityDictEnum.MEMBERSHIP.getWord()
                                + " is for product " + "[" + value
                                + "], module belongs to product ["
                                + module.getProduct() + "]");
            }
        }
        // .............................................
        // Check module
        // .............................................
        {
            key = MemberCardManager.CARD_PROP_MEMBERSHIP_MODULES;
            value = props.getProperty(key);
            if (null == value) {
                throw new MemberCardException(key + " not found");
            }

            StringTokenizer st = new StringTokenizer(value,
                    MemberCardManager.CARD_PROP_PROCUCT_DELIMITER);
            boolean bFound = false;
            while (st.hasMoreTokens()) {
                if (st.nextToken().equals(module.getModule())) {
                    bFound = true;
                    break;
                }
            }
            if (!bFound) {
                throw new MemberCardException(
                        CommunityDictEnum.MEMBERSHIP.getWord()
                                + " is for module(s) " + "[" + value
                                + "], this is module [" + module.getModule()
                                + "]");
            }
        }

        // .............................................
        // Check version
        // .............................................
        {
            key = module.getModule() + CARD_MODULE_PROP_HYPHEN
                    + MemberCardManager.CARD_PROP_SAVAPAGE_VERSION_MAJOR;
            value = props.getProperty(key);
            if (null == value) {
                throw new MemberCardException(key + " not found");
            }
            if (!value.equals(module.getVersionMajor())) {
                throw new MemberCardException(
                        CommunityDictEnum.MEMBERSHIP.getWord()
                                + " is for version " + "[" + value
                                + "], module [" + key + "] has version ["
                                + module.getVersionMajor() + "]");
            }
        }

        // .............................................
        // Period
        // .............................................
        checkMembershipPeriod(props, module);

        // .................................................................
        // Last but not least , check properties of the module itself...
        // .................................................................
        module.checkMemberCardProperties(props);
    }

    /**
     *
     * @param pubKey
     * @param strContent
     * @param strSignature
     * @return
     * @throws Exception
     */
    public boolean isContentValid(final java.security.PublicKey pubKey,
            final String strContent, final String strSignature)
            throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bos.write(strContent.getBytes());

        Signature dsa = Signature.getInstance("SHA1withDSA");
        dsa.initVerify(pubKey);
        dsa.update(bos.toByteArray());
        return dsa.verify(mySignatureStringAsBytes(strSignature));
    }

    /**
     * Reads the properties from a MemberCard input stream.
     *
     * @param pubKey
     *            The public key.
     * @param istrSigned
     *            The input stream to with the MemberCard.
     * @param bZip
     *            Indicating if input stream is zipped.
     * @return Properties object with MemberCard properties.
     * @throws MemberCardException
     *             When the MemberCard has invalid syntax or could for some
     *             other reason not be read.
     */
    public final Properties readMemberCardProps(final PublicKey pubKey,
            final InputStream istrSigned, final boolean bZip)
            throws MemberCardException {

        Properties props = new Properties();
        if (!checkMemberCardFormat(pubKey, istrSigned, bZip, props)) {
            throw new MemberCardException("signature is not verified");
        }
        return props;
    }

    /**
     *
     * @param pubKey
     *            The public key.
     * @param istrSigned
     *            The input stream to with the MemberCard.
     * @param bZip
     *            Indicating if input stream is zipped.
     * @param memberCardProps
     *            Properties object where MemberCard properties are appended on.
     * @param module
     *            The module to check the MemberCard for.
     * @return true if valid, false if not.
     * @throws MemberCardException
     *             When the MemberCard has invalid syntax or could for some
     *             other reason not be read.
     * @throws MemberCardExpiredException
     * @throws MemberCardFatalException
     */
    public final boolean isMemberCardValid(final java.security.PublicKey pubKey,
            final java.io.InputStream istrSigned, final boolean bZip,
            final Properties memberCardProps, final IMembershipModule module)
            throws MemberCardException, MemberCardExpiredException,
            MemberCardFatalException {
        boolean bValid = checkMemberCardFormat(pubKey, istrSigned, bZip,
                memberCardProps);
        if (!bValid) {
            throw new MemberCardException("signature is not verified");
        }
        checkMemberCardProperties(memberCardProps, module);
        return bValid;
    }

    /**
     * Checks if the format of the MemberCard file is valid. The content is put
     * into the {@link Properties} argument.
     *
     * @param pubKey
     *            The public key is the signer.
     * @param istrSigned
     *            The inputstream of the MemberCard file.
     * @param bZip
     *            Indication if MemberCard is zipped.
     * @param memberCardProps
     * @return {@code true} if this is a valid MemberCard format.
     * @throws MemberCardException
     *             When NOT valid.
     */
    public boolean checkMemberCardFormat(java.security.PublicKey pubKey,
            java.io.InputStream istrSigned, boolean bZip,
            Properties memberCardProps) throws MemberCardException {

        eBytesToSign b2s = eBytesToSign.ALL_BYTES;

        ZipInputStream zipIn = null;

        try {
            ByteArrayOutputStream bosMemberCardContent =
                    new java.io.ByteArrayOutputStream();

            boolean bValid = false;

            String strSignature = "";

            java.io.InputStream istrSignedMemberCard = null;

            if (bZip) {
                zipIn = new ZipInputStream(istrSigned);
                zipIn.getNextEntry();
                istrSignedMemberCard = zipIn;
            } else {
                istrSignedMemberCard = istrSigned;
            }

            // get the signature from the first line
            int iByte = istrSignedMemberCard.read(); // initial read
            // advance to '='
            while ((-1 < iByte) && ('=' != iByte)) {
                bosMemberCardContent.write(iByte);
                iByte = istrSignedMemberCard.read();
            }

            if (-1 < iByte) {
                bosMemberCardContent.write(iByte);
                // skip '='
                iByte = istrSignedMemberCard.read();
            }

            while ((-1 < iByte) && ('\r' != iByte) && ('\n' != iByte)) {
                bosMemberCardContent.write(iByte);
                strSignature += (char) iByte;
                iByte = istrSignedMemberCard.read();
            }

            if (('\r' == iByte) || ('\n' == iByte)) {
                bosMemberCardContent.write(iByte);
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            byte[] aByte = new byte[2048];
            int nBytes = istrSignedMemberCard.read(aByte);
            while (-1 < nBytes) {
                bosMemberCardContent.write(aByte, 0, nBytes);
                if (eBytesToSign.SKIP_CRLF == b2s) {
                    for (int i = 0; i < nBytes; i++) {
                        if ((10 != aByte[i]) && (13 != aByte[i])) {
                            bos.write(aByte[i]);
                        }
                    }
                } else {
                    bos.write(aByte, 0, nBytes);
                }
                nBytes = istrSignedMemberCard.read(aByte);
            }
            bos.flush();

            Signature dsa = Signature.getInstance("SHA1withDSA");
            dsa.initVerify(pubKey);
            dsa.update(bos.toByteArray());
            bValid = dsa.verify(mySignatureStringAsBytes(strSignature));

            final ByteArrayInputStream bisMemberCardContent =
                    new ByteArrayInputStream(
                            bosMemberCardContent.toByteArray());

            memberCardProps.load(bisMemberCardContent);

            return bValid;

        } catch (SignatureException ex) {
            throw new MemberCardException(ex.getMessage(), ex);
        } catch (InvalidKeyException ex) {
            throw new MemberCardException(ex.getMessage(), ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new MemberCardException(ex.getMessage(), ex);
        } catch (IOException ex) {
            throw new MemberCardException(ex.getMessage(), ex);
        } finally {
            if (zipIn != null) {
                try {
                    zipIn.close();
                } catch (IOException e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        }
    }

}
