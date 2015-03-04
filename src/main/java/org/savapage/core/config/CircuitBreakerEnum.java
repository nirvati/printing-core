/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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

import java.io.IOException;
import java.net.URLConnection;

import javax.mail.MessagingException;

import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerListener;
import org.savapage.core.circuitbreaker.CircuitBreakerRegistry;
import org.savapage.core.circuitbreaker.CircuitNonTrippingException;
import org.savapage.core.circuitbreaker.CircuitTrippingException;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.jpa.AppLog;
import org.savapage.core.papercut.PaperCutConnectException;
import org.savapage.core.papercut.PaperCutException;

/**
 * Identification and configuration for {@link CircuitBreaker} instances as used
 * in {@link CircuitBreakerRegistry}.
 * <p>
 * For each breaker an {@link AbstractCircuitBreakerListener} is specified.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public enum CircuitBreakerEnum {

    /**
     * Breaker for Local CUPS/IPP connection.
     * <p>
     * See {@link CircuitBreakerListenerMixin} how {@link CircuitBreaker} events
     * are handled.
     * </p>
     *
     */
    CUPS_LOCAL_IPP_CONNECTION(1, 10000, new AbstractCircuitBreakerListener() {

        @Override
        public boolean isLogExceptionTracktrace(CircuitBreaker breaker,
                Exception exception) {
            return !(exception instanceof CircuitTrippingException);
        }

        @Override
        protected PubTopicEnum getPubTopic() {
            return PubTopicEnum.CUPS;
        }

        @Override
        protected String getMessageBaseKey() {
            return "circuit-cups-local-connection";
        }
    }),

    /**
     * Breaker for ALL remote CUPS/IPP connections.
     * <p>
     * NOTE: A zero (0) retry interval is used, cause this
     * {@link CircuitBreaker} is used for various connections (IP destinations).
     * </p>
     * <p>
     * See {@link CircuitBreakerListenerMixin} how {@link CircuitBreaker} events
     * are handled.
     * </p>
     *
     */
    CUPS_REMOTE_IPP_CONNECTIONS(1, 0, new AbstractCircuitBreakerListener() {

        @Override
        public boolean isLogExceptionTracktrace(CircuitBreaker breaker,
                Exception exception) {
            return !(exception instanceof CircuitTrippingException);
        }

        @Override
        protected PubTopicEnum getPubTopic() {
            return PubTopicEnum.CUPS;
        }

        @Override
        protected String getMessageBaseKey() {
            return "circuit-cups-remote-connection";
        }

        /**
         * No operation, cause this {@link CircuitBreaker} is used for various
         * connections (IP destinations).
         *
         * @param breaker
         */
        @Override
        public void onCircuitOpened(CircuitBreaker breaker) {
            // noop
        }

        /**
         * No operation, cause this {@link CircuitBreaker} is used for various
         * connections (IP destinations).
         *
         * @param breaker
         */
        @Override
        public void onCircuitClosed(CircuitBreaker breaker) {
            // noop
        }

        /**
         * Just a message, no {@link AppLog}, cause this {@link CircuitBreaker}
         * is used for various connections (IP destinations).
         *
         * @param breaker
         * @param cause
         */
        @Override
        public void
                onTrippingException(CircuitBreaker breaker, Exception cause) {
            this.publishWarning(cause);
        }

    }),

    /**
     * Breaker for Google Cloud Print connectivity.
     * <p>
     * See {@link CircuitBreakerListenerMixin} how {@link CircuitBreaker} events
     * are handled.
     * </p>
     */
    GCP_CONNECTION(1, 60000, new AbstractCircuitBreakerListener() {

        @Override
        public boolean isLogExceptionTracktrace(CircuitBreaker breaker,
                Exception exception) {
            return true;
        }

        @Override
        protected PubTopicEnum getPubTopic() {
            return PubTopicEnum.GCP_PRINT;
        }

        @Override
        protected String getMessageBaseKey() {
            return "circuit-gcp-connection";
        }
    }),

    /**
     * Breaker for {@link URLConnection} to the internet (as opposed to
     * <i>intranet</i>).
     * <p>
     * See {@link CircuitBreakerListenerMixin} how {@link CircuitBreaker} events
     * are handled.
     * </p>
     */
    INTERNET_URL_CONNECTION(1, 60000, new AbstractCircuitBreakerListener() {

        @Override
        public boolean isLogExceptionTracktrace(CircuitBreaker breaker,
                Exception exception) {
            return !(exception instanceof CircuitTrippingException
                    || exception instanceof IOException || exception instanceof MessagingException);
        }

        @Override
        protected PubTopicEnum getPubTopic() {
            return PubTopicEnum.INTERNET_URL_CONNECTION;
        }

        @Override
        protected String getMessageBaseKey() {
            return "circuit-url-connection";
        }

    }),

    /**
     * Breaker for MailPrint connectivity.
     * <p>
     * See {@link CircuitBreakerListenerMixin} how {@link CircuitBreaker} events
     * are handled.
     * </p>
     */
    MAILPRINT_CONNECTION(1, 60000, new AbstractCircuitBreakerListener() {

        @Override
        public boolean isLogExceptionTracktrace(CircuitBreaker breaker,
                Exception exception) {
            return !(exception instanceof IOException
                    || exception instanceof MessagingException || exception instanceof CircuitTrippingException);
        }

        @Override
        protected PubTopicEnum getPubTopic() {
            return PubTopicEnum.MAILPRINT;
        }

        @Override
        protected String getMessageBaseKey() {
            return "circuit-mailprint-connection";
        }

    }),

    /**
     * Breaker for PaperCut connectivity.
     * <p>
     * See {@link CircuitBreakerListenerMixin} how {@link CircuitBreaker} events
     * are handled.
     * </p>
     */
    PAPERCUT_CONNECTION(1, 60000, new AbstractCircuitBreakerListener() {

        @Override
        public boolean isLogExceptionTracktrace(CircuitBreaker breaker,
                Exception exception) {
            return !(exception instanceof PaperCutConnectException
                    || exception instanceof PaperCutException || exception instanceof CircuitNonTrippingException);
        }

        @Override
        protected PubTopicEnum getPubTopic() {
            return PubTopicEnum.PAPERCUT;
        }

        @Override
        protected String getMessageBaseKey() {
            return "circuit-papercut-connection";
        }

    }),

    /**
     * Breaker for SmartSchool connectivity.
     * <p>
     * See {@link CircuitBreakerListenerMixin} how {@link CircuitBreaker} events
     * are handled.
     * </p>
     */
    SMARTSCHOOL_CONNECTION(1, 60000, new AbstractCircuitBreakerListener() {

        @Override
        public boolean isLogExceptionTracktrace(CircuitBreaker breaker,
                Exception exception) {
            return !(exception instanceof IOException
                    || exception instanceof MessagingException || exception instanceof CircuitTrippingException);
        }

        @Override
        protected PubTopicEnum getPubTopic() {
            return PubTopicEnum.SMARTSCHOOL;
        }

        @Override
        protected String getMessageBaseKey() {
            return "circuit-smartschool-connection";
        }

    }),

    /**
     * Breaker for SMTP connectivity.
     * <p>
     * See {@link CircuitBreakerListenerMixin} how {@link CircuitBreaker} events
     * are handled.
     * </p>
     */
    SMTP_CONNECTION(1, 30000, new AbstractCircuitBreakerListener() {

        @Override
        public boolean isLogExceptionTracktrace(CircuitBreaker breaker,
                Exception exception) {
            return !(exception instanceof CircuitTrippingException);
        }

        @Override
        protected PubTopicEnum getPubTopic() {
            return PubTopicEnum.SMTP;
        }

        @Override
        protected String getMessageBaseKey() {
            return "circuit-smtp-connection";
        }

    })

    //
    ;

    /**
     * The number of failures before the circuit is opened.
     */
    private final int failureThreshHold;

    /**
     *
     */
    private final int millisUntilRetry;

    /**
     *
     */
    private final CircuitBreakerListener breakerListener;

    /**
     *
     * @param failureThreshHold
     * @param millisUntilRetry
     *            The number of milliseconds until to retry the connection.
     * @param breakerListener
     *            The {@link CircuitBreakerListener}.
     */
    private CircuitBreakerEnum(final int failureThreshHold,
            final int millisUntilRetry,
            final CircuitBreakerListener breakerListener) {
        this.failureThreshHold = failureThreshHold;
        this.millisUntilRetry = millisUntilRetry;
        this.breakerListener = breakerListener;
    }

    /**
     *
     * @return
     */
    public int getFailureThreshHold() {
        return failureThreshHold;
    }

    /**
     *
     * @return
     */
    public int getMillisUntilRetry() {
        return millisUntilRetry;
    }

    public CircuitBreakerListener getBreakerListener() {
        return breakerListener;
    }

}
