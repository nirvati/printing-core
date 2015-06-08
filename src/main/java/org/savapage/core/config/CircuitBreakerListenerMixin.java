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

import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerListener;
import org.savapage.core.circuitbreaker.CircuitStateEnum;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.jpa.AppLog;
import org.savapage.core.util.AppLogHelper;
import org.savapage.core.util.Messages;

/**
 * Abstract {@link CircuitBreakerListener} implementation.
 *
 * @author Datraverse B.V.
 *
 */
public abstract class CircuitBreakerListenerMixin implements
        CircuitBreakerListener {

    private static final String MSG_SUFFIX_CLOSED = "-closed";
    private static final String MSG_SUFFIX_OPENED = "-opened";
    private static final String MSG_SUFFIX_DAMAGED = "-damaged";
    private static final String MSG_SUFFIX_EXCEPTION = "-exception";

    /**
     *
     * @return
     */
    protected abstract PubTopicEnum getPubTopic();

    /**
     * Gets the base key for messages. The base key is suffixed to retrieve
     * specific messages for notifying status changes.
     *
     * @return The base key for messages.
     */
    protected abstract String getMessageBaseKey();

    @Override
    public void onCircuitClosed(CircuitBreaker breaker) {
        final String msg =
                AppLogHelper.logInfo(this.getClass(), getMessageBaseKey()
                        + MSG_SUFFIX_CLOSED);
        publishMsg(PubLevelEnum.CLEAR, msg);
    }

    @Override
    public void onCircuitOpened(CircuitBreaker breaker) {
        final String msg =
                AppLogHelper.logWarning(this.getClass(), getMessageBaseKey()
                        + MSG_SUFFIX_OPENED);
        publishMsg(PubLevelEnum.WARN, msg);
    }

    @Override
    public void onCircuitDamaged(CircuitBreaker breaker) {
        final String msg =
                AppLogHelper.logError(this.getClass(), getMessageBaseKey()
                        + MSG_SUFFIX_DAMAGED);
        publishMsg(PubLevelEnum.ERROR, msg);
    }

    @Override
    public void onCircuitAcquired(CircuitBreaker breaker) {
        if (breaker.isCircuitDamaged()) {
            /*
             * Give a message each time a damaged circuit is acquired.
             */
            final String msg =
                    localizedMessage(getMessageBaseKey() + MSG_SUFFIX_DAMAGED);
            publishMsg(PubLevelEnum.ERROR, msg);
        }
    }

    /**
     * Acts on a tripping exception:
     * <ul>
     * <li>A warning {@link AppLog} entry is written <i>only when</i> current
     * state of breaker is {@link CircuitStateEnum#CLOSED}.</li>
     * <li>A warning message is <i>always</i> published with
     * {@link AdminPublisher}</li>
     * </ul>
     */
    @Override
    public void onTrippingException(CircuitBreaker breaker, Exception cause) {

        final String messageKey = getMessageBaseKey() + MSG_SUFFIX_EXCEPTION;
        final String messageArg = cause.getMessage();

        final String msg;

        if (breaker.isCircuitClosed()) {
            msg =
                    AppLogHelper.logWarning(this.getClass(), messageKey,
                            messageArg);
        } else {
            msg = localizedMessage(messageKey, messageArg);
        }

        publishMsg(PubLevelEnum.WARN, msg);
    }

    @Override
    public void onNonTrippingException(CircuitBreaker breaker, Exception cause) {
        final String msg =
                localizedMessage(getMessageBaseKey() + MSG_SUFFIX_EXCEPTION,
                        cause.getMessage());
        publishMsg(PubLevelEnum.WARN, msg);
    }

    @Override
    public void onDamagingException(CircuitBreaker breaker, Exception cause) {
        final String msg =
                AppLogHelper.logError(this.getClass(), getMessageBaseKey()
                        + MSG_SUFFIX_EXCEPTION, cause.getMessage());
        publishMsg(PubLevelEnum.ERROR, msg);
    }

    /**
     *
     * @param messageKey
     * @param args
     * @return
     */
    private String localizedMessage(String messageKey, String... args) {
        return Messages.getMessage(this.getClass(),
                ConfigManager.getDefaultLocale(), messageKey, args);
    }

    /**
     * Publishes a message using {@link AdminPublisher}.
     *
     * @param level
     * @param msg
     */
    private void publishMsg(PubLevelEnum level, String msg) {
        AdminPublisher.instance().publish(this.getPubTopic(), level, msg);
    }

    /**
     * Publishes a {@link PubLevelEnum#WARN} message for an exception using
     * {@link AdminPublisher}.
     *
     * @param throwable
     *            The {@link Throwable} to publish.
     */
    protected void publishWarning(final Throwable throwable) {
        publishMsg(
                PubLevelEnum.WARN,
                localizedMessage(getMessageBaseKey() + MSG_SUFFIX_EXCEPTION,
                        throwable.getMessage()));
    }

}
