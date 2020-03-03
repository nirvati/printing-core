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
package org.savapage.core.circuitbreaker;

/**
 *
 * @author Rijk Ravestein
 *
 */
public interface CircuitBreakerListener {

    /**
     * Circuit closed.
     *
     * @param breaker
     *            The {@link CircuitBreaker} sender.
     */
    void onCircuitClosed(final CircuitBreaker breaker);

    /**
     * Circuit opened.
     *
     * @param breaker
     *            The {@link CircuitBreaker} sender.
     */
    void onCircuitOpened(final CircuitBreaker breaker);

    /**
     * Circuit damaged.
     *
     * @param breaker
     *            The {@link CircuitBreaker} sender.
     */
    void onCircuitDamaged(final CircuitBreaker breaker);

    /**
     * An exception that did trip (opened) the {@link CircuitBreaker}.
     *
     * @param breaker
     *            The {@link CircuitBreaker} sender.
     * @param cause
     *            The cause.
     */
    void onTrippingException(final CircuitBreaker breaker,
            final Exception cause);

    /**
     * An exception that did NOT trip (opened) the {@link CircuitBreaker}.
     *
     * @param breaker
     *            The {@link CircuitBreaker} sender.
     * @param cause
     *            The cause.
     */
    void onNonTrippingException(final CircuitBreaker breaker,
            final Exception cause);

    /**
     * An exception that damaged (permanently opened) the {@link CircuitBreaker}
     * .
     *
     * @param breaker
     *            The {@link CircuitBreaker} sender.
     * @param cause
     *            The cause.
     */
    void onDamagingException(final CircuitBreaker breaker,
            final Exception cause);

    /**
     *
     * @param breaker
     *            The {@link CircuitBreaker} sender.
     */
    void onCircuitAcquired(final CircuitBreaker breaker);

    /**
     * Is full stack trace logging needed for exception?
     *
     * @param breaker
     *            The {@link CircuitBreaker} sender.
     * @param exception
     *            The {@link Exception}.
     * @return {@code true} when a full stacktrace is needed, {@code false} when
     *         a single line message is sufficient.
     */
    boolean isLogExceptionTracktrace(final CircuitBreaker breaker,
            final Exception exception);

}
