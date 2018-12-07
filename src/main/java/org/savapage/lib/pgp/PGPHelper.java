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
package org.savapage.lib.pgp;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.BCPGOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPOnePassSignature;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.KeyFingerPrintCalculator;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.PGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.PGPKeyEncryptionMethodGenerator;
import org.bouncycastle.openpgp.operator.PublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.bc.BcPublicKeyDataDecryptorFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.savapage.core.util.IOHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class PGPHelper {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PGPHelper.class);

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
    public static final String FILENAME_EXT_ASC = "asc";

    /**
     *
     */
    public static final PGPHashAlgorithmEnum CONTENT_SIGN_ALGORITHM =
            PGPHashAlgorithmEnum.SHA256;

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
     * Finds the first secret key in key ring or key file.
     *
     * @param istr
     *            Input Key file or key ring file.
     * @param passphrase
     *            The secret key passphrase.
     * @return The first secret key found.
     * @throws PGPBaseException
     *             When errors or not found.
     */
    public PGPSecretKeyInfo readSecretKey(final InputStream istr,
            final String passphrase) throws PGPBaseException {
        return readSecretKeyList(istr, passphrase).get(0);
    }

    /**
     * Reads Secret Keys from Key File or Key Ring File.
     *
     * @param istr
     *            Input Key file or key ring file.
     * @param passphrase
     *            The secret key passphrase.
     * @return The list of secrets keys.
     * @throws PGPBaseException
     *             When errors or no secret key found.
     */
    public List<PGPSecretKeyInfo> readSecretKeyList(final InputStream istr,
            final String passphrase) throws PGPBaseException {

        final List<PGPSecretKeyInfo> list = new ArrayList<>();

        try (InputStream istrBinary = PGPUtil.getDecoderStream(istr)) {

            final PGPSecretKeyRingCollection pgpPriv =
                    new PGPSecretKeyRingCollection(istrBinary,
                            this.keyFingerPrintCalculator);

            final Iterator<PGPSecretKeyRing> iterRings = pgpPriv.getKeyRings();

            while (iterRings.hasNext()) {

                final Object readData = iterRings.next();

                if (readData instanceof PGPSecretKeyRing) {
                    final PGPSecretKeyRing pbr = (PGPSecretKeyRing) readData;
                    final Iterator<PGPSecretKey> iterKeys = pbr.getSecretKeys();
                    while (iterKeys.hasNext()) {
                        final PGPSecretKey key = iterKeys.next();
                        if (key.isPrivateKeyEmpty()) {
                            continue;
                        }
                        list.add(new PGPSecretKeyInfo(key,
                                extractPrivateKey(key, passphrase)));
                    }
                }
            }

        } catch (IOException | PGPException e) {
            throw new PGPBaseException(e.getMessage(), e);
        }

        if (list.isEmpty()) {
            throw new PGPBaseException("No SecretKey found in SecretKeyRing.");
        }

        return list;
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
    private PGPPrivateKey extractPrivateKey(final PGPSecretKey secretKey,
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

        PGPPublicKey encryptionKey = null;
        PGPPublicKey masterKey = null;

        try (InputStream istrBinary = PGPUtil.getDecoderStream(istr);) {

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
        }

        return new PGPPublicKeyInfo(masterKey, encryptionKey);
    }

    /**
     * Reads Public Keys from Key File or Key Ring File.
     *
     * @param istr
     *            {@link InputStream} of KeyRing or Key file.
     * @return The list of public keys.
     * @throws PGPBaseException
     *             When errors encountered, or no public key found.
     */
    public List<PGPPublicKey> readPublicKeyList(final InputStream istr)
            throws PGPBaseException {

        final List<PGPPublicKey> list = new ArrayList<>();

        try (InputStream istrBinary = PGPUtil.getDecoderStream(istr);) {

            final PGPPublicKeyRingCollection pgpPub =
                    new PGPPublicKeyRingCollection(istrBinary,
                            this.keyFingerPrintCalculator);

            final Iterator<PGPPublicKeyRing> iterKeyRing = pgpPub.getKeyRings();

            while (iterKeyRing.hasNext()) {

                final PGPPublicKeyRing keyRing = iterKeyRing.next();

                final Iterator<PGPPublicKey> iterPublicKey =
                        keyRing.getPublicKeys();

                while (iterPublicKey.hasNext()) {
                    list.add(iterPublicKey.next());
                }
            }

        } catch (IOException | PGPException e) {
            throw new PGPBaseException(e.getMessage(), e);
        }

        if (list.isEmpty()) {
            throw new PGPBaseException("No PublicKey found in PublicKeyRing.");
        }
        return list;
    }

    /**
     * Downloads public ASCII armored public key from public key server and
     * writes to output stream.
     *
     * @param lookupUrl
     *            The lookup URL to download the a hexadecimal KeyID.
     * @param ostr
     *            The output stream.
     * @throws UnknownHostException
     *             When well-formed URL points to unknown host.
     * @throws IOException
     *             When connectivity error.
     */
    public static void downloadPublicKey(final URL lookupUrl,
            final OutputStream ostr) throws UnknownHostException, IOException {

        try (InputStream istr = lookupUrl.openStream();
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(istr));) {

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
            if (!processLine) {
                LOGGER.warn("{} : no public key.", lookupUrl);
            }
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

            try (InputStream signPublicKeyInputStream =
                    new FileInputStream(file);) {

                final PGPPublicKeyInfo encKey =
                        readPublicKey(signPublicKeyInputStream);

                pgpPublicKeyList.add(encKey);

            } catch (IOException e) {
                throw new PGPBaseException(e.getMessage(), e);
            }

        }
        return pgpPublicKeyList;
    }

    /**
     * Encode public key as ASCII armored byte array.
     *
     * @param pubKey
     *            Public key
     * @return ASCII armored byte array.
     * @throws IOException
     *             If IO error.
     */
    public static byte[] encodeArmored(final PGPPublicKey pubKey)
            throws IOException {

        try (ByteArrayOutputStream ostrPubKey = new ByteArrayOutputStream();
                ArmoredOutputStream ostrArmored =
                        new ArmoredOutputStream(ostrPubKey)) {
            pubKey.encode(ostrArmored);
            /*
             * Do not flush(), but close() !!
             */
            ostrArmored.close();
            return ostrPubKey.toByteArray();
        }
    }

    /**
     * Encrypts a file and SHA-256 signs it with a one pass signature.
     *
     * @author John Opincar (C# example)
     * @author Bilal Soylu (C# to Java)
     * @author Rijk Ravestein (Refactoring)
     *
     * @param contentStream
     *            The input to encrypt.
     * @param contentStreamEncrypted
     *            The encrypted output.
     * @param secretKeyInfo
     *            The secret key container to sign with.
     * @param publicKeyList
     *            The public keys to encrypt with.
     * @param embeddedFileName
     *            The "file" name embedded in the encrypted output.
     * @param embeddedFileDate
     *            The last modification time of the "file" embedded in the
     *            encrypted output.
     * @param asciiArmor
     *            If {@code true}, create ASCII armored output.
     * @throws PGPBaseException
     *             When errors occur.
     */
    public void encryptOnePassSignature(final InputStream contentStream,
            final OutputStream contentStreamEncrypted,
            final PGPSecretKeyInfo secretKeyInfo,
            final List<PGPPublicKeyInfo> publicKeyList,
            final String embeddedFileName, final Date embeddedFileDate,
            final boolean asciiArmor) throws PGPBaseException {

        // For now, always do integrity checks.
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
                    secretKeyInfo.getPublicKey().getAlgorithm(),
                    CONTENT_SIGN_ALGORITHM.getBcTag());

            final PGPSignatureGenerator signatureGenerator =
                    new PGPSignatureGenerator(csb);

            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT,
                    secretKeyInfo.getPrivateKey());

            // Find first signature to use.
            for (final Iterator<String> i =
                    secretKeyInfo.getPublicKey().getUserIDs(); i.hasNext();) {

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
            IOHelper.closeQuietly(literalOut);
            closePGPDataGenerator(literalDataGenerator);

            // (4) Close the rest.
            IOHelper.closeQuietly(compressedOut);
            closePGPDataGenerator(compressedDataGenerator);

            IOHelper.closeQuietly(encryptedOut);
            closePGPDataGenerator(encryptedDataGenerator);

            if (asciiArmor) {
                IOHelper.closeQuietly(targetOut);
            }
        }
    }

    /**
     * Creates a signature of content input.
     *
     * @param contentStream
     *            The input to sign.
     * @param signatureStream
     *            The signed output.
     * @param secretKeyInfo
     *            The secret key container.
     * @param hashAlgorithm
     *            The {@link PGPHashAlgorithmEnum}.
     * @param asciiArmor
     *            If {@code true}, create ASCII armored output.
     * @throws PGPBaseException
     *             When errors occur.
     */
    public void createSignature(final InputStream contentStream,
            final OutputStream signatureStream,
            final PGPSecretKeyInfo secretKeyInfo,
            final PGPHashAlgorithmEnum hashAlgorithm, final boolean asciiArmor)
            throws PGPBaseException {

        // Objects to be closed when finished.
        OutputStream ostr = null;
        BCPGOutputStream pgostr = null;

        try (InputStream istr = new BufferedInputStream(contentStream);) {

            if (asciiArmor) {
                ostr = new ArmoredOutputStream(
                        new BufferedOutputStream(signatureStream));
            } else {
                ostr = new BufferedOutputStream(signatureStream);
            }

            pgostr = new BCPGOutputStream(ostr);

            final PGPSignatureGenerator signatureGenerator =
                    new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(
                            secretKeyInfo.getPublicKey().getAlgorithm(),
                            hashAlgorithm.getBcTag())
                                    .setProvider(this.bcProvider));

            signatureGenerator.init(PGPSignature.BINARY_DOCUMENT,
                    secretKeyInfo.getPrivateKey());

            int ch;
            while ((ch = istr.read()) >= 0) {
                signatureGenerator.update((byte) ch);
            }

            istr.close();
            signatureGenerator.generate().encode(pgostr);

        } catch (PGPException | IOException e) {
            throw new PGPBaseException(e.getMessage(), e);
        } finally {
            IOHelper.closeQuietly(pgostr);

            if (asciiArmor) {
                IOHelper.closeQuietly(ostr);
            }
        }
    }

    /**
     * Gets signature object from signature input stream.
     *
     * @param signature
     *            The signature stream.
     * @return The {@link PGPSignature} object.
     * @throws PGPBaseException
     *             When error.
     */
    public PGPSignature getSignature(final InputStream signature)
            throws PGPBaseException {

        try (InputStream istr = PGPUtil.getDecoderStream(signature)) {

            JcaPGPObjectFactory pgpFact = new JcaPGPObjectFactory(istr);

            final PGPSignatureList p3;
            final Object o = pgpFact.nextObject();

            if (o instanceof PGPCompressedData) {
                PGPCompressedData c1 = (PGPCompressedData) o;

                pgpFact = new JcaPGPObjectFactory(c1.getDataStream());

                p3 = (PGPSignatureList) pgpFact.nextObject();
            } else {
                p3 = (PGPSignatureList) o;
            }

            return p3.get(0);

        } catch (IOException | PGPException e) {
            throw new PGPBaseException(e.getMessage(), e);
        }
    }

    /**
     * Verifies PGP signature against content and its public key.
     *
     * @see {@link org.bouncycastle.openpgp.examples.DetachedSignatureProcessor}.
     *
     * @param content
     *            The content.
     * @param sig
     *            The signature.
     * @param publicKey
     *            The public key of signature.
     * @return {@code true} when signature is valid.
     * @throws PGPBaseException
     *             When signature error.
     */
    public boolean verifySignature(final InputStream content,
            final PGPSignature sig, final PGPPublicKey publicKey)
            throws PGPBaseException {

        try {
            sig.init(new JcaPGPContentVerifierBuilderProvider()
                    .setProvider("BC"), publicKey);

            int ch;
            while ((ch = content.read()) >= 0) {
                sig.update((byte) ch);
            }

            content.close();

            return sig.verify();

        } catch (IOException | PGPException e) {
            throw new PGPBaseException(e.getMessage(), e);
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

    /**
     * Decrypts content encrypted with one or more public keys and signed with
     * private key as one pass signature.
     *
     * @param istrEncrypted
     *            The encrypted InputStream.
     * @param signPublicKeyList
     *            The {@link PGPPublicKey} list of the private key the content
     *            was signed with.
     * @param secretKeyInfoList
     *            The {@link PGPSecretKeyInfo} list of (one of) the public keys
     *            the content was encrypted with.
     * @param ostrClearContent
     *            The clear content OutputStream.
     * @return The signature.
     * @throws PGPBaseException
     *             When errors.
     */
    public PGPSignature decryptOnePassSignature(final InputStream istrEncrypted,
            final List<PGPPublicKey> signPublicKeyList,
            final List<PGPSecretKeyInfo> secretKeyInfoList,
            final OutputStream ostrClearContent) throws PGPBaseException {

        // Objects to be closed when finished.
        InputStream clearDataInputStream = null;

        try {
            // The private key we use to decrypt contents.
            PGPPrivateKey privateKey = null;

            // The PGP encrypted object representing the data to decrypt.
            PGPPublicKeyEncryptedData encryptedData = null;

            /*
             * Get list of encrypted objects in the message. If first object is
             * a PGP marker: skip it.
             */
            PGPObjectFactory objectFactory = new PGPObjectFactory(
                    PGPUtil.getDecoderStream(istrEncrypted),
                    this.keyFingerPrintCalculator);

            final Object firstObject = objectFactory.nextObject();
            final PGPEncryptedDataList dataList;
            if (firstObject instanceof PGPEncryptedDataList) {
                dataList = (PGPEncryptedDataList) firstObject;
            } else {
                dataList = (PGPEncryptedDataList) objectFactory.nextObject();
            }

            /*
             * Find the encrypted object associated with a private key in our
             * key ring.
             */
            @SuppressWarnings("rawtypes")
            final Iterator dataObjectsIterator =
                    dataList.getEncryptedDataObjects();

            while (privateKey == null && dataObjectsIterator.hasNext()) {

                encryptedData =
                        (PGPPublicKeyEncryptedData) dataObjectsIterator.next();

                for (final PGPSecretKeyInfo info : secretKeyInfoList) {
                    if (info.getSecretKey().getKeyID() == encryptedData
                            .getKeyID()) {
                        privateKey = info.getPrivateKey();
                        break;
                    }
                }
            }

            if (privateKey == null) {
                throw new PGPBaseException("Secret key of message not found.");
            }

            // Get a handle to the decrypted data as an input stream
            final PublicKeyDataDecryptorFactory dataDecryptorFactory =
                    new BcPublicKeyDataDecryptorFactory(privateKey);

            clearDataInputStream =
                    encryptedData.getDataStream(dataDecryptorFactory);

            final PGPObjectFactory clearObjectFactory = new PGPObjectFactory(
                    clearDataInputStream, this.keyFingerPrintCalculator);

            Object message = clearObjectFactory.nextObject();

            // Handle case where the data is compressed
            if (message instanceof PGPCompressedData) {
                PGPCompressedData compressedData = (PGPCompressedData) message;
                objectFactory =
                        new PGPObjectFactory(compressedData.getDataStream(),
                                this.keyFingerPrintCalculator);
                message = objectFactory.nextObject();
            }

            PGPOnePassSignature calculatedSignature = null;

            if (message instanceof PGPOnePassSignatureList) {

                calculatedSignature =
                        ((PGPOnePassSignatureList) message).get(0);

                PGPPublicKey signPublicKey = null;

                for (final PGPPublicKey key : signPublicKeyList) {
                    if (key.getKeyID() == calculatedSignature.getKeyID()) {
                        signPublicKey = key;
                        break;
                    }
                }
                if (signPublicKey == null) {
                    throw new PGPBaseException(
                            "Provided public key of signer does not match "
                                    + "Key ID if the signature.");
                }

                final PGPContentVerifierBuilderProvider verifierBuilderProv =
                        new BcPGPContentVerifierBuilderProvider();
                calculatedSignature.init(verifierBuilderProv, signPublicKey);

                message = objectFactory.nextObject();
            }

            /*
             * We should have literal data now, to read the decrypted message
             * from.
             */
            if (!(message instanceof PGPLiteralData)) {
                throw new PGPBaseException("Unexpected message type "
                        + message.getClass().getName());
            }

            final InputStream literalDataInputStream =
                    ((PGPLiteralData) message).getInputStream();
            int nextByte;

            while ((nextByte = literalDataInputStream.read()) >= 0) {
                /*
                 * InputStream.read() returns byte (range 0-255), so we can
                 * safely cast to char and byte.
                 */
                calculatedSignature.update((byte) nextByte); // also update
                /*
                 * calculated one pass signature result.append((char) nextByte);
                 * add to file instead of StringBuffer
                 */
                ostrClearContent.write((char) nextByte);
            }
            ostrClearContent.close();

            //
            PGPSignature messageSignature = null;

            if (calculatedSignature != null) {
                final PGPSignatureList signatureList =
                        (PGPSignatureList) objectFactory.nextObject();
                messageSignature = signatureList.get(0);
            }

            if (messageSignature == null) {
                throw new PGPBaseException("Signature not found.");
            }

            if (!calculatedSignature.verify(messageSignature)) {
                throw new PGPBaseException("Signature verification failed.");
            }

            if (encryptedData.isIntegrityProtected()
                    && !encryptedData.verify()) {
                throw new PGPBaseException("Message failed integrity check.");
            }

            return messageSignature;

        } catch (IOException | PGPException e) {
            throw new PGPBaseException(e.getMessage(), e);
        } finally {
            IOHelper.closeQuietly(clearDataInputStream);
        }
    }
}
