/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.lib.pgp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.PGPKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPHelper {

    /**
     * String format for public key lookup.
     */
    private static final String URL_KEY_SERVER_LOOKUP_FORMAT =
            "https://pgp.mit.edu//pks/lookup?op=get&search=%s";

    /**
     * Buffer for encryption streaming.
     */
    private static final class EncryptBuffer {

        /**
         * Buffer size of 64KB for encryption streaming.
         * <p>
         * NOTE: Size should be power of 2. If not, only the largest power of 2
         * bytes worth of the buffer will be used.
         * </p>
         */
        private static final int BUFFER_SIZE_64KB = 65536;

        /**
         * Creates Buffer for encryption streaming.
         *
         * @return the buffer;
         */
        private static byte[] create() {
            return new byte[BUFFER_SIZE_64KB];
        }
    }

    /** */
    private static final class SingletonHolder {
        /** */
        static final PGPHelper SINGLETON = new PGPHelper();
    }

    /** */
    private final BouncyCastleProvider bcProvider;

    /** */
    private final KeyFingerPrintCalculator keyFingerPrintCalculator;

    /**
     * Singleton instantiation.
     */
    private PGPHelper() {
        this.bcProvider = new BouncyCastleProvider();
        this.keyFingerPrintCalculator = new BcKeyFingerprintCalculator();
        Security.addProvider(this.bcProvider);
    }

    /**
     * @return The singleton instance.
     */
    public static PGPHelper instance() {
        return SingletonHolder.SINGLETON;
    }

    /**
     * Finds the first secret key in key ring or key file. A secret key contains
     * a private key that can be accessed with a password.
     *
     * @param istr
     *            Input Key file or key ring file.
     * @return The first private key found.
     * @throws PGPBaseException
     *             When errors or not found.
     */
    public PGPSecretKey readSecretKey(final InputStream istr)
            throws PGPBaseException {

        PGPSecretKey sKey = null;
        InputStream istrBinary = null;

        try {
            istrBinary = PGPUtil.getDecoderStream(istr);

            final PGPSecretKeyRingCollection pgpPriv =
                    new PGPSecretKeyRingCollection(istrBinary,
                            this.keyFingerPrintCalculator);

            final Iterator<PGPSecretKeyRing> it = pgpPriv.getKeyRings();

            PGPSecretKeyRing pbr = null;

            while (sKey == null && it.hasNext()) {

                final Object readData = it.next();

                if (readData instanceof PGPSecretKeyRing) {
                    pbr = (PGPSecretKeyRing) readData;
                    sKey = pbr.getSecretKey();
                }
            }

        } catch (IOException | PGPException e) {
            throw new PGPBaseException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(istrBinary);
        }

        if (sKey == null) {
            throw new PGPBaseException("No SecretKey found in SecretKeyRing.");
        }

        return sKey;
    }

    /**
     * Extracts the private key from secret key.
     *
     * @param secretKey
     *            The secret key.
     * @param secretKeyPassphrase
     *            The secret key passphrase.
     * @return The private key.
     * @throws PGPBaseException
     *             When errors.
     */
    public PGPPrivateKey extractPrivateKey(final PGPSecretKey secretKey,
            final String secretKeyPassphrase) throws PGPBaseException {

        try {
            return secretKey
                    .extractPrivateKey(new JcePBESecretKeyDecryptorBuilder()
                            .setProvider(this.bcProvider)
                            .build(secretKeyPassphrase.toCharArray()));
        } catch (PGPException e) {
            throw new PGPBaseException(e.getMessage(), e);
        }
    }

    /**
     * Finds the first public <i>encryption</i> key in the Key File or Key Ring
     * File, and fills a list with UIDs from the master key.
     *
     * @param istr
     *            {@link InputStream} of KeyRing or Key.
     * @return The {@link PGPPublicKeyInfo}.
     * @throws PGPBaseException
     *             When errors encountered, or no public key found.
     */
    public PGPPublicKeyInfo readPublicKey(final InputStream istr)
            throws PGPBaseException {

        InputStream istrBinary = null;
        PGPPublicKey encryptionKey = null;
        PGPPublicKey masterKey = null;

        try {
            istrBinary = PGPUtil.getDecoderStream(istr);

            final PGPPublicKeyRingCollection pgpPub =
                    new PGPPublicKeyRingCollection(istrBinary,
                            this.keyFingerPrintCalculator);

            final Iterator<PGPPublicKeyRing> iterKeyRing = pgpPub.getKeyRings();

            while ((encryptionKey == null || masterKey == null)
                    && iterKeyRing.hasNext()) {

                final PGPPublicKeyRing keyRing = iterKeyRing.next();

                final Iterator<PGPPublicKey> iterPublicKey =
                        keyRing.getPublicKeys();

                while ((encryptionKey == null || masterKey == null)
                        && iterPublicKey.hasNext()) {

                    final PGPPublicKey publicKeyCandidate =
                            iterPublicKey.next();

                    if (encryptionKey == null
                            && publicKeyCandidate.isEncryptionKey()) {
                        encryptionKey = publicKeyCandidate;
                    }

                    // The master key contains the uids.
                    if (masterKey == null && publicKeyCandidate.isMasterKey()) {
                        masterKey = publicKeyCandidate;
                    }
                }
            }

            if (encryptionKey == null) {
                throw new PGPBaseException(
                        "No Encryption Key found in PublicKeyRing.");
            }

            if (masterKey == null) {
                throw new PGPBaseException(
                        "No Master Key found in PublicKeyRing.");
            }

        } catch (IOException | PGPException e) {
            throw new PGPBaseException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(istrBinary);
        }

        return new PGPPublicKeyInfo(masterKey, encryptionKey);
    }

    /**
     * Downloads public ASCII armored public key from public key server and
     * writes to output stream.
     *
     * @param hexKeyID
     *            Hexadecimal KeyID, prefixed with "0x".
     * @param ostr
     *            The output stream.
     * @throws PGPBaseException
     *             When error retrieving or writing.
     */
    public static void downloadPublicKey(final String hexKeyID,
            final OutputStream ostr) throws PGPBaseException {

        InputStream istr = null;
        BufferedReader reader = null;

        try {
            final URL url = new URL(
                    String.format(URL_KEY_SERVER_LOOKUP_FORMAT, hexKeyID));

            istr = url.openStream();
            reader = new BufferedReader(new InputStreamReader(istr));

            String line;

            boolean processLine = false;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("-----BEGIN ")) {
                    processLine = true;
                }
                if (processLine) {
                    ostr.write(line.getBytes());
                    ostr.write('\n');
                    if (line.startsWith("-----END ")) {
                        break;
                    }
                }
            }

            reader.close();

        } catch (IOException e) {
            throw new PGPBaseException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(istr);
        }
    }

    /**
     *
     * @param publicKeyFiles
     *            List of public key files.
     * @return List of {@link PGPPublicKeyInfo} objects.
     * @throws PGPBaseException
     *             When error occurred.
     */
    public List<PGPPublicKeyInfo> getPublicKeyList(
            final List<File> publicKeyFiles) throws PGPBaseException {

        final List<PGPPublicKeyInfo> pgpPublicKeyList = new ArrayList<>();

        for (final File file : publicKeyFiles) {

            InputStream signPublicKeyInputStream = null;

            try {
                signPublicKeyInputStream = new FileInputStream(file);

                final PGPPublicKeyInfo encKey =
                        readPublicKey(signPublicKeyInputStream);
                pgpPublicKeyList.add(encKey);
            } catch (FileNotFoundException e) {
                throw new PGPBaseException(e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(signPublicKeyInputStream);
            }

        }
        return pgpPublicKeyList;
    }

    /**
     * Encrypts a file and SHA-256 ASCII armed signs it with a one pass
     * signature.
     *
     * @author John Opincar (C# example)
     * @author Bilal Soylu (C# to Java)
     * @author Rijk Ravestein (Refactoring)
     *
     * @param contentStream
     *            The input to encrypt.
     * @param contentStreamEncrypted
     *            The encrypted output.
     * @param secretKey
     *            The secret key container of the private key.
     * @param privateKey
     *            The private key to sign with.
     * @param publicKeyList
     *            The public keys to encrypt with.
     * @param embeddedFileName
     *            The "file" name embedded in the encrypted output.
     * @param embeddedFileDate
     *            The last modification time of the "file" embedded in the
     *            encrypted output.
     * @throws PGPBaseException
     *             When errors occur.
     */
    public void encryptOnePassSignature(final InputStream contentStream,
            final OutputStream contentStreamEncrypted,
            final PGPSecretKey secretKey, final PGPPrivateKey privateKey,
            final List<PGPPublicKeyInfo> publicKeyList,
            final String embeddedFileName, final Date embeddedFileDate)
                    throws PGPBaseException {

        // For now, always do integrity checks and use ASCII armor mode.
        final boolean asciiArmor = true;
        final boolean withIntegrityCheck = true;

        // Objects to be closed when finished.
        PGPLiteralDataGenerator literalDataGenerator = null;
        PGPCompressedDataGenerator compressedDataGenerator = null;
        PGPEncryptedDataGenerator encryptedDataGenerator = null;
        OutputStream literalOut = null;
        OutputStream compressedOut = null;
        OutputStream encryptedOut = null;
        OutputStream targetOut = null;

        try {

            if (asciiArmor) {
                targetOut = new ArmoredOutputStream(contentStreamEncrypted);
            } else {
                targetOut = contentStreamEncrypted;
            }

            // Init encrypted data generator.
            final JcePGPDataEncryptorBuilder encryptorBuilder =
                    new JcePGPDataEncryptorBuilder(
                            SymmetricKeyAlgorithmTags.CAST5);

            encryptorBuilder.setSecureRandom(new SecureRandom())
            .setProvider(this.bcProvider)
            .setWithIntegrityPacket(withIntegrityCheck);

            encryptedDataGenerator =
                    new PGPEncryptedDataGenerator(encryptorBuilder);

            for (final PGPPublicKeyInfo pgpPublicKeyInfo : publicKeyList) {
                final PGPKeyEncryptionMethodGenerator method =
                        new JcePublicKeyKeyEncryptionMethodGenerator(
                                pgpPublicKeyInfo.getEncryptionKey());
                encryptedDataGenerator.addMethod(method);
            }

            encryptedOut = encryptedDataGenerator.open(targetOut,
                    EncryptBuffer.create());

            // Start compression
            compressedDataGenerator = new PGPCompressedDataGenerator(
                    CompressionAlgorithmTags.ZIP);

            compressedOut = compressedDataGenerator.open(encryptedOut);

            // Start signature
            final PGPContentSignerBuilder csb = new BcPGPContentSignerBuilder(
                    secretKey.getPublicKey().getAlgorithm(),
                    PGPHashAlgorithmEnum.SHA256.getBcTag());

            final PGPSignatureGenerator signatureGenerator =
                    new PGPSignatureGenerator(csb);

            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);

            // Find first signature to use.
            for (final Iterator<String> i =
                    secretKey.getPublicKey().getUserIDs(); i.hasNext();) {

                final String userId = i.next();

                final PGPSignatureSubpacketGenerator spGen =
                        new PGPSignatureSubpacketGenerator();
                spGen.setSignerUserID(false, userId);

                signatureGenerator.setHashedSubpackets(spGen.generate());
                break;
            }

            signatureGenerator.generateOnePassVersion(false)
            .encode(compressedOut);

            // Create the Literal Data generator output stream.
            literalDataGenerator = new PGPLiteralDataGenerator();

            // Create output stream.
            literalOut = literalDataGenerator.open(compressedOut,
                    PGPLiteralData.BINARY, embeddedFileName, embeddedFileDate,
                    EncryptBuffer.create());

            // Read input file and write to target file using a buffer.
            final byte[] buf = EncryptBuffer.create();
            int len;
            while ((len = contentStream.read(buf, 0, buf.length)) > 0) {
                literalOut.write(buf, 0, len);
                signatureGenerator.update(buf, 0, len);
            }

            // (1) Close these down first!
            literalOut.close();
            literalDataGenerator.close();
            literalDataGenerator = null;

            // (2) Signature.
            signatureGenerator.generate().encode(compressedOut);

        } catch (IOException | PGPException e) {
            throw new PGPBaseException(e.getMessage(), e);
        } finally {

            // (3) In case we missed closes because of exception.
            IOUtils.closeQuietly(literalOut);
            closePGPDataGenerator(literalDataGenerator);

            // (4) Close the rest.
            IOUtils.closeQuietly(compressedOut);
            closePGPDataGenerator(compressedDataGenerator);

            IOUtils.closeQuietly(encryptedOut);
            closePGPDataGenerator(encryptedDataGenerator);

            if (asciiArmor) {
                IOUtils.closeQuietly(targetOut);
            }
        }
    }

    /**
     * Creates a SHA-256 ASCII armed signature of content input.
     *
     * @param contentStream
     *            The input to sign.
     * @param signatureStream
     *            The signed output.
     * @param secretKey
     *            The secret key container of the private key.
     * @param privateKey
     *            The private key to sign with.
     * @param hashAlgorithm
     *            The {@link PGPHashAlgorithmEnum}.
     * @throws PGPBaseException
     *             When errors occur.
     */
    public void createSignature(final InputStream contentStream,
            final OutputStream signatureStream, final PGPSecretKey secretKey,
            final PGPPrivateKey privateKey,
            final PGPHashAlgorithmEnum hashAlgorithm) throws PGPBaseException {

        final boolean asciiArmor = true;

        // Objects to be closed when finished.
        InputStream istr = null;
        OutputStream ostr = null;
        BCPGOutputStream pgostr = null;

        try {
            istr = new BufferedInputStream(contentStream);

            if (asciiArmor) {
                ostr = new ArmoredOutputStream(
                        new BufferedOutputStream(signatureStream));
            } else {
                ostr = new BufferedOutputStream(signatureStream);
            }

            pgostr = new BCPGOutputStream(ostr);

            final PGPSignatureGenerator signatureGenerator =
                    new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(
                            secretKey.getPublicKey().getAlgorithm(),
                            hashAlgorithm.getBcTag())
                            .setProvider(this.bcProvider));

            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT, privateKey);

            int ch;
            while ((ch = istr.read()) >= 0) {
                signatureGenerator.update((byte) ch);
            }

            istr.close();
            signatureGenerator.generate().encode(pgostr);

        } catch (PGPException | IOException e) {
            throw new PGPBaseException(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(istr);
            IOUtils.closeQuietly(pgostr);

            if (asciiArmor) {
                IOUtils.closeQuietly(ostr);
            }
        }

    }

    /**
     * Quietly closes a PGP*DataGenerator.
     * <p>
     * Note: this ugly solution is implemented because common interface type
     * "StreamGenerator" (containing the close() method) cannot be resolved to a
     * type (why?).
     * </p>
     *
     * @param obj
     *            The PGP*DataGenerator to close;
     */
    private static void closePGPDataGenerator(final Object obj) {
        if (obj == null) {
            return;
        }
        try {
            if (obj instanceof PGPLiteralDataGenerator) {
                ((PGPLiteralDataGenerator) obj).close();

            } else if (obj instanceof PGPCompressedDataGenerator) {
                ((PGPCompressedDataGenerator) obj).close();

            } else if (obj instanceof PGPEncryptedDataGenerator) {
                ((PGPEncryptedDataGenerator) obj).close();

            } else {
                throw new IllegalArgumentException(String.format(
                        "Unsupported class %s", obj.getClass().getName()));
            }
        } catch (IOException e) {
            // no code intended
        }
    }

}
