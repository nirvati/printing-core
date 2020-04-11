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
package org.savapage.lib.totp;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.BaseNCodec;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.ietf.tools.TOTP;

/**
 * Uses Time-Based One-Time Password Algorithm from {@link org.ietf.tools.TOTP}
 * as published in <a href="http://tools.ietf.org/html/rfc6238">RFC 6238</a>.
 *
 * @author Rijk Ravestein
 *
 */
public final class TOTPAuthenticator {

    /**
     * The default period a TOTP code will be valid for, in seconds. Same as
     * used by Google Authenticator.
     */
    public static final int TIME_STEP_SECONDS_DEFAULT = 30;

    /**
     * Default number of periods (steps) used to check codes generated in the
     * near past and future.
     */
    public static final int SYNC_TIME_STEPS_DEFAULT = 3;

    /**
     *
     * @author Rijk Ravestein
     *
     */
    public static class Builder {

        private String key;
        private BaseNCodecEnum keyCodec;
        private KeySizeEnum keySize;
        private CodeDigitsEnum returnDigits;
        private HmacAlgorithmEnum hmacAlgorithm;
        private int stepSeconds;

        /**
         * Number of periods (steps) used to check codes generated in the near
         * past or future.
         *
         * "Because of possible clock drifts between a client and a validation
         * server, we RECOMMEND that the validator be set with a specific limit
         * to the number of time steps a prover can be "out of synch" before
         * being rejected.
         */
        private int syncSteps;

        /**
         * Constructs defaults.
         */
        private Builder() {
            this.keyCodec = BaseNCodecEnum.getDefault();
            this.keySize = KeySizeEnum.SIZE_32;
            this.returnDigits = CodeDigitsEnum.getDefault();
            this.hmacAlgorithm = HmacAlgorithmEnum.getDefault();
            this.stepSeconds = TIME_STEP_SECONDS_DEFAULT;
            this.syncSteps = SYNC_TIME_STEPS_DEFAULT;
        }

        /**
         * Constructs defaults with secret key.
         *
         * @param secretKey
         *            Secret key.
         */
        public Builder(final String secretKey) {
            this();
            this.key = secretKey;
        }

        /**
         * Generates secret key using {@link #keyCodec} and {@link #keySize}.
         *
         * @return Builder.
         */
        public Builder generateKey() {
            this.key = generateSecretKey(this.keyCodec, this.keySize);
            return this;
        }

        /**
         * Sets the secret key.
         *
         * @param secretKey
         *            Secret key.
         * @return Builder.
         */
        public Builder setKey(final String secretKey) {
            this.key = secretKey;
            return this;
        }

        /**
         * Gets the secret key.
         *
         * @return Secret key.
         */
        public String getKey() {
            return this.key;
        }

        /**
         * Sets the BaseN codec.
         *
         * @param codec
         *            BaseNCodecEnum
         * @return Builder.
         */
        public Builder setKeyCodec(final BaseNCodecEnum codec) {
            this.keyCodec = codec;
            return this;
        }

        /**
         * Sets the secret key size.
         *
         * @param size
         *            Key size.
         * @return Builder.
         */
        public Builder setKeySize(final KeySizeEnum size) {
            this.keySize = size;
            return this;
        }

        /**
         * Sets the HMAC Algorithm.
         *
         * @param hmac
         *            HmacAlgorithmEnum
         * @return Builder.
         */
        public Builder setHmacAlgorithm(final HmacAlgorithmEnum hmac) {
            this.hmacAlgorithm = hmac;
            return this;
        }

        /**
         * Creates object copy.
         *
         * @param obj
         *            Object.
         * @return Copy.
         */
        private static Builder copy(final Builder obj) {

            final Builder copy = new Builder(obj.key);

            copy.keyCodec = obj.keyCodec;
            copy.keySize = obj.keySize;
            copy.returnDigits = obj.returnDigits;
            copy.hmacAlgorithm = obj.hmacAlgorithm;
            copy.stepSeconds = obj.stepSeconds;
            copy.syncSteps = obj.syncSteps;

            return copy;
        }

        /**
         * @return Instance of {@link TOTPAuthenticator}.
         */
        public TOTPAuthenticator build() {
            return new TOTPAuthenticator(copy(this));
        }
    }

    /** */
    public enum CodeDigitsEnum {

        /** */
        DIGITS_6(6),

        /**
         * Max digits supported by {@link org.ietf.tools.TOTP}.
         */
        DIGITS_8(8);

        /** Number of digits. */
        private final int digits;

        /**
         *
         * @param value
         *            Number of digits.
         */
        CodeDigitsEnum(final int value) {
            this.digits = value;
        }

        /**
         * @return Number of digits.
         */
        public int getDigits() {
            return this.digits;
        }

        /**
         * @return Number of digits.
         */
        public String getDigitsAsString() {
            return String.valueOf(this.digits);
        }

        /**
         * @return Default digits. Same as used by Google Authenticator.
         */
        public static CodeDigitsEnum getDefault() {
            return CodeDigitsEnum.DIGITS_6;
        }
    }

    /** */
    public enum BaseNCodecEnum {

        /** */
        BASE32(new Base32()),

        /**
         * Create URL Safe (true) encoder, without line separator.
         */
        BASE64(new Base64(1024, new byte[] {}, true));

        /** */
        private final BaseNCodec codec;

        /**
         * @param value
         *            codec value;
         */
        BaseNCodecEnum(final BaseNCodec value) {
            this.codec = value;
        }

        /**
         * Decodes a String containing characters in the Base-N alphabet.
         *
         * @param pArray
         *            A String containing Base-N character data.
         * @return A byte array containing binary data.
         */
        byte[] decode(final String pArray) {
            return this.codec.decode(pArray);
        }

        /**
         * Encodes a byte[] containing binary data, into an String containing
         * characters in the alphabet.
         * <ul>
         * <li>Base32 encoded string is all one case.</li>
         * <li>Base64 encoded string is case sensitive.</li>
         * </ul>
         *
         * @param pArray
         *            A byte array containing binary data.
         * @return A string containing only the base N alphabetic character
         *         data.
         */
        String encodeToString(final byte[] pArray) {
            return this.codec.encodeToString(pArray);
        }

        /**
         * @return Default codec. Same as used by Google Authenticator.
         */
        public static BaseNCodecEnum getDefault() {
            return BaseNCodecEnum.BASE32;
        }

    }

    /** */
    public enum HmacAlgorithmEnum {
        /** */
        SHA1("HmacSHA1", "SHA1"),
        /** */
        SHA256("HmacSHA256", "SHA256"),
        /** */
        SHA512("HmacSHA512", "SHA512");

        /**
         * Full name.
         */
        private final String name;

        /**
         * Short name for URI value.
         */
        private final String uriValue;

        /**
         *
         * @param value
         *            Full name.
         * @param uri
         *            Short name for URI value.
         */
        HmacAlgorithmEnum(final String value, final String uri) {
            this.name = value;
            this.uriValue = uri;
        }

        /**
         * @return Algorithm name.
         */
        public String getName() {
            return this.name;
        }

        /**
         * @return Value for "otpauth://totp/" URI parameter.
         */
        public String getUriValue() {
            return this.uriValue;
        }

        /**
         * @return Default algorithm. Same as used by Google Authenticator.
         */
        public static HmacAlgorithmEnum getDefault() {
            return HmacAlgorithmEnum.SHA1;
        }

    }

    /** */
    public enum KeySizeEnum {

        /** 16 characters. */
        SIZE_16(10, 12),

        /** 32 characters. */
        SIZE_32(20, 24);

        /**
         * Base32 encodes 5 bytes to 8 characters.
         */
        private final int nBytesBase32;

        /**
         * Base64 encodes 3 bytes to 4 characters.
         */
        private final int nBytesBase64;

        /**
         * @param size32
         *            Number of buffer bytes needed for Base32 encoding.
         * @param size64
         *            Number of buffer bytes needed for Base64 encoding.
         */
        KeySizeEnum(final int size32, final int size64) {
            this.nBytesBase32 = size32;
            this.nBytesBase64 = size64;
        }

        /**
         * @param ncode
         *            BaseN encoding.
         * @return Number of buffer bytes needed to generate secret key with
         *         required number of BaseN encoded characters.
         */
        public int getNumberOfBytes(final BaseNCodecEnum ncode) {

            switch (ncode) {
            case BASE32:
                return this.nBytesBase32;

            case BASE64:
                return this.nBytesBase64;

            default:
                throw new IllegalArgumentException(ncode.toString());
            }
        }
    }

    /**
     * Generator of pseudo-random number stream.
     */
    private static final Random RANDOM = new Random();

    /** */
    private final Builder builder;

    /**
     * @param parms
     *            Builder.
     */
    private TOTPAuthenticator(final Builder parms) {
        this.builder = parms;
    }

    /**
     * Creates secret key with default settings.
     *
     * @return Builder.
     */
    public static Builder buildKey() {
        return new Builder().generateKey();
    }

    /**
     * Creates secret key with default settings.
     *
     * @param codec
     *            Key codec.
     * @param size
     *            Key size.
     * @return Builder.
     */
    public static Builder buildKey(final BaseNCodecEnum codec,
            final KeySizeEnum size) {
        return new Builder().setKeyCodec(codec).setKeySize(size).generateKey();
    }

    /**
     * Generates a TOTP value on reference date/time.
     *
     * @param date
     *            Reference data/time.
     * @return TOTP code.
     */
    private String generateTOTP(final Date date) {

        final long steps =
                getTimeStepsFromZeroTime(date, this.builder.stepSeconds);
        final String stepsHex = StringUtils
                .leftPad(Long.toHexString(steps).toUpperCase(), 16, '0');

        return TOTP.generateTOTP(
                Hex.encodeHexString(
                        this.builder.keyCodec.decode(this.builder.key)),
                stepsHex, this.builder.returnDigits.getDigitsAsString(),
                this.builder.hmacAlgorithm.getName());
    }

    /**
     * Generates the current (time = now) TOTP value.
     *
     * @return TOTP code.
     */
    public String generateTOTP() {
        return this.generateTOTP(new Date());
    }

    /**
     * Generates the current (time = now) TOTP value.
     *
     * @return TOTP code as long value.
     */
    public long generateTOTPAsLong() {
        return Long.parseLong(this.generateTOTP());
    }

    /**
     * Verifies if TOTP code is valid at <i>this</i> point in time (now).
     *
     * @param code
     *            code to check.
     *
     * @return {@code true} if code is valid.
     *
     * @throws TOTPException
     *             When key or algorithm is invalid.
     */
    public boolean verifyTOTP(final long code) throws TOTPException {

        final Date now = new Date();

        for (int i =
                -this.builder.syncSteps; i <= this.builder.syncSteps; ++i) {

            final Date stepDate =
                    DateUtils.addSeconds(now, i * this.builder.stepSeconds);
            final long codeProbe = Long.parseLong(this.generateTOTP(stepDate));
            if (code == codeProbe) {
                return true;
            }
        }
        return false;
    }

    /**
     * See <a href=
     * "https://github.com/google/google-authenticator/wiki/Key-Uri-Format"> Key
     * URI Format</a>.
     *
     * @param issuer
     *            The organization this account belongs to.
     * @param account
     *            User's account name. e.g. email address or username.
     * @return URI.
     */
    public String getURI(final String issuer, final String account) {

        final StringBuilder uri = new StringBuilder();
        try {
            uri.append("otpauth://totp/");
            uri.append(URLEncoder
                    .encode(String.format("%s:%s", issuer, account), "UTF-8")
                    .replace("+", "%20"));
            uri.append("?secret=").append(this.builder.key).append("&issuer=");
            uri.append(URLEncoder.encode(issuer, "UTF-8").replace("+", "%20"));
            uri.append("&algorithm=")
                    .append(this.builder.hmacAlgorithm.getUriValue())
                    .append("&digits=")
                    .append(this.builder.returnDigits.getDigits())
                    .append("&period=").append(this.builder.stepSeconds);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        return uri.toString();
    }

    /**
     * Generates a BaseN encoded secret key.
     *
     * @param baseNCodec
     *            BaseN codec.
     * @param keySize
     *            Key size.
     * @return BaseN encoded secret key.
     */
    public static String generateSecretKey(final BaseNCodecEnum baseNCodec,
            final KeySizeEnum keySize) {

        final byte[] buffer = new byte[keySize.getNumberOfBytes(baseNCodec)];

        // Fill buffer with random bytes.
        RANDOM.nextBytes(buffer);

        // Convert to BaseN and return as string.
        return baseNCodec.encodeToString(buffer);
    }

    /**
     * @param date
     *            Reference date/time.
     * @param stepSeconds
     *            Time step in seconds.
     * @return Number of time steps between the initial counter time t0 and the
     *         current Unix time. (RFC 6238, p3).
     */
    private static long getTimeStepsFromZeroTime(final Date date,
            final long stepSeconds) {
        return date.getTime() / TimeUnit.SECONDS.toMillis(stepSeconds);
    }

}
