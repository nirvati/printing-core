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

import java.util.HashMap;
import java.util.Map;

/**
 * A registry of {@link CircuitBreaker} instances.
 *
 * @author Rijk Ravestein
 *
 */
public final class CircuitBreakerRegistry {

    /**
     *
     */
    private static final int DEFAULT_FAILURE_THRESHHOLD = 5;

    /**
     *
     */
    private static final int DEFAULT_MILLIS_UNTIL_RETRY = 60000;

    /** */
    private final Object mutexGetOrCreate = new Object();

    /**
     *
     */
    private Map<String, CircuitBreaker> circuitBreakers =
            new HashMap<String, CircuitBreaker>();

    /**
     *
     */
    private int defaultFailureThreshHold;

    /**
     *
     */
    private int defaultMillisUntilRetry;

    /**
     *
     */
    private Class<? extends Exception>[] defaultNonTrippingExceptions;

    /**
     *
     */
    private final Class<? extends Exception>[] defaultDamagingExceptions;

    /**
     * Default constructor.
     */
    @SuppressWarnings("unchecked")
    public CircuitBreakerRegistry() {

        defaultFailureThreshHold = DEFAULT_FAILURE_THRESHHOLD;
        defaultMillisUntilRetry = DEFAULT_MILLIS_UNTIL_RETRY;
        defaultNonTrippingExceptions = new Class[0];
        defaultDamagingExceptions = new Class[0];
    }

    /**
     *
     * @param defaultFailureThreshHold
     * @param defaultMillisUntilRetry
     * @param defaultNonTrippingExceptions
     */
    public CircuitBreakerRegistry(final int defaultFailureThreshHold,
            final int defaultMillisUntilRetry,
            final Class<? extends Exception>[] defaultNonTrippingExceptions,
            final Class<? extends Exception>[] defaultDamagingExceptions) {

        this.defaultFailureThreshHold = defaultFailureThreshHold;
        this.defaultMillisUntilRetry = defaultMillisUntilRetry;
        this.defaultNonTrippingExceptions = defaultNonTrippingExceptions;
        this.defaultDamagingExceptions = defaultDamagingExceptions;
    }

    /**
     *
     * @param id
     * @return
     */
    public final CircuitBreaker getOrCreateCircuitBreaker(final String id) {

        return this.getOrCreateCircuitBreaker(id, this.defaultFailureThreshHold,
                this.defaultMillisUntilRetry, this.defaultNonTrippingExceptions,
                this.defaultDamagingExceptions, null);
    }

    /**
     *
     * @param id
     * @param failureThreshHold
     * @param millisUntilRetry
     * @param nonTrippingExceptions
     * @param damagingExceptions
     * @param listener
     * @return
     */
    public final CircuitBreaker getOrCreateCircuitBreaker(final String id,
            final int failureThreshHold, final int millisUntilRetry,
            final Class<? extends Exception>[] nonTrippingExceptions,
            final Class<? extends Exception>[] damagingExceptions,
            final CircuitBreakerListener listener) {

        synchronized (this.mutexGetOrCreate) {
            if (!circuitBreakers.containsKey(id)) {
                circuitBreakers.put(id,
                        new CircuitBreaker(id, failureThreshHold,
                                millisUntilRetry, nonTrippingExceptions,
                                damagingExceptions, listener));
            }
            return circuitBreakers.get(id);
        }
    }

    /**
     *
     * @param id
     * @return
     */
    public final CircuitBreaker getCircuitBreaker(final String id) {

        if (!circuitBreakers.containsKey(id)) {
            throw new IllegalArgumentException(
                    "No circuit breaker defined with id [" + id + "]");
        }
        return circuitBreakers.get(id);
    }

    /**
     *
     */
    public final void openAllCircuits() {
        for (CircuitBreaker breaker : circuitBreakers.values()) {
            breaker.openCircuit();
        }
    }

    /**
     *
     */
    public final void closeAllCircuits() {
        for (CircuitBreaker breaker : circuitBreakers.values()) {
            breaker.closeCircuit();
        }
    }

    /**
     *
     * @param id
     */
    public final void openCircuit(final String id) {
        getCircuitBreaker(id).openCircuit();
    }

    /**
     *
     * @param id
     */
    public final void closeCircuit(final String id) {
        getCircuitBreaker(id).closeCircuit();
    }

    /**
     *
     * @return
     */
    public final int getDefaultFailureThreshHold() {
        return defaultFailureThreshHold;
    }

    /**
     *
     * @param defaultFailureThreshHold
     */
    public final void
            setDefaultFailureThreshHold(final int defaultFailureThreshHold) {
        this.defaultFailureThreshHold = defaultFailureThreshHold;
    }

    /**
     *
     * @return
     */
    public final int getDefaultMillisUntilRetry() {
        return defaultMillisUntilRetry;
    }

    /**
     *
     * @param defaultMillisUntilRetry
     */
    public final void
            setDefaultMillisUntilRetry(final int defaultMillisUntilRetry) {
        this.defaultMillisUntilRetry = defaultMillisUntilRetry;
    }

    /**
     *
     * @return
     */
    public final Class<? extends Exception>[]
            getDefaultNonTrippingExceptions() {
        return defaultNonTrippingExceptions;
    }

    /**
     *
     * @param defaultNonTrippingExceptions
     */
    public final void setDefaultNonTrippingExceptions(
            final Class<? extends Exception>[] defaultNonTrippingExceptions) {
        this.defaultNonTrippingExceptions = defaultNonTrippingExceptions;
    }

    /**
     * Gets the map of {@link CircuitBreaker) instances.
     *
     * @return The map of {@link CircuitBreaker) instances.
     */
    public final Map<String, CircuitBreaker> getCircuitBreakers() {
        return circuitBreakers;
    }

    /**
     * Sets the map of {@link CircuitBreaker) instances.
     *
     * @param circuitBreakers
     *            The map of {@link CircuitBreaker) instances.
     */
    public final void setCircuitBreakers(
            final Map<String, CircuitBreaker> circuitBreakers) {
        this.circuitBreakers = circuitBreakers;
    }
}
