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
package org.savapage.core.circuitbreaker;

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Circuit Breaker based on the circuit breaker concept as described in the
 * book "Release It!" by Michael T. Nygard.
 * <p>
 * Circuit breakers are applied to services that implement integration points to
 * external systems.
 * </p>
 * <p>
 * If such a service returns too many errors or responds too slowly, the breaker
 * trips to {@link CircuitStateEnum#OPEN}. In this state, any client which
 * attempts to reach the service will fail fast.
 * </p>
 * <p>
 * When the breaker has been open for some specified period of time, it will
 * move to {@link CircuitStateEnum#HALF_OPEN}, allowing the client’s next
 * request to reach the service. Depending on the result of this call, the
 * circuit breaker will either move to {@link CircuitStateEnum#CLOSED} or back
 * to {@link CircuitStateEnum#OPEN}.
 * </p>
 * <p>
 * NOTE: This class uses {@link Logger} for logging errors, warning, debug and
 * trace messages.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public final class CircuitBreaker {

    /**
     * The logger.
     */
    private static final Logger LOG = LoggerFactory
            .getLogger(CircuitBreaker.class);

    /**
     * The state of this circuit. Initially it is
     * {@link CircuitStateEnum#CLOSED}.
     */
    private AtomicReference<CircuitStateEnum> state =
            new AtomicReference<CircuitStateEnum>(CircuitStateEnum.CLOSED);

    /**
     * The number of consecutive failures after which circuit goes to
     * {@link CircuitStateEnum#OPEN}.
     */
    private int failureThreshold;

    /**
     * The number of consecutive failures.
     */
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /**
     * Number of milliseconds after which a retry is permitted.
     */
    private int millisUntilRetry;

    /**
     * Last time the circuit was opened.
     */
    private long lastOpenedTime = 0L;

    /**
     * The unique circuit ID.
     */
    private String circuitId;

    /**
     * The number of times the circuit was opened.
     */
    private int openCount;

    /**
     * .
     */
    private Class<? extends Exception>[] nonTrippingExceptions;

    /**
     * .
     */
    private Class<? extends Exception>[] damagingExceptions;

    /**
     * Used for serializing access to this instance.
     */
    private final Semaphore semaphore = new Semaphore(1);

    /**
     *
     */
    private boolean logExceptionTracktrace = true;

    /**
     *
     */
    private CircuitBreakerListener circuitListener;

    /**
     * Default constructor.
     */
    public CircuitBreaker() {
    }

    /**
     *
     * @param circuitId
     *            The circuit id.
     * @param failureThreshold
     * @param millisUntilRetry
     * @param nonTrippingExceptions
     * @param damagingExceptions
     * @param circuitListener
     */
    public CircuitBreaker(final String circuitId, final int failureThreshold,
            final int millisUntilRetry,
            final Class<? extends Exception>[] nonTrippingExceptions,
            final Class<? extends Exception>[] damagingExceptions,
            final CircuitBreakerListener circuitListener) {

        this();

        this.circuitId = circuitId;
        this.failureThreshold = failureThreshold;
        this.millisUntilRetry = millisUntilRetry;
        this.nonTrippingExceptions = nonTrippingExceptions;
        this.damagingExceptions = damagingExceptions;
        this.circuitListener = circuitListener;
    }

    /**
     *
     * @param circuitId
     *            The circuit id.
     * @param failureThreshold
     * @param millisUntilRetry
     * @param nonTrippingExceptions
     */
    public CircuitBreaker(final String circuitId, final int failureThreshold,
            final int millisUntilRetry,
            final Class<? extends Exception>[] nonTrippingExceptions,
            final Class<? extends Exception>[] damagingExceptions) {

        this(circuitId, failureThreshold, millisUntilRetry,
                nonTrippingExceptions, damagingExceptions, null);
    }

    /**
     *
     * @return
     */
    public final boolean isLogExceptionTracktrace() {
        return logExceptionTracktrace;
    }

    /**
     *
     * @param logExceptionTracktrace
     */
    public final void setLogExceptionTracktrace(
            final boolean logExceptionTracktrace) {
        this.logExceptionTracktrace = logExceptionTracktrace;
    }

    /**
     *
     * @param t
     */
    private void onNonTrippingException(final Exception t) {
        if (this.circuitListener != null) {
            this.circuitListener.onNonTrippingException(this, t);
        }
    }

    /**
     *
     * @param t
     */
    private void onDamagingException(final Exception t) {
        if (this.circuitListener != null) {
            this.circuitListener.onDamagingException(this, t);
        }
    }

    /**
     * When a tripping exception occurs.
     * <p>
     * IMPORTANT: this event should be triggered <i>before</i>
     * {@link CircuitStateEnum} value of this instance is changed, so the
     * {@link CircuitBreakerListener} can check its current state before the
     * change dus to the exception.
     * </p>
     *
     * @param t
     */
    private void onTrippingException(final Exception t) {
        if (this.circuitListener != null) {
            this.circuitListener.onTrippingException(this, t);
        }
    }

    /**
     *
     * @param t
     */
    private void onCircuitAcquired() {
        if (this.circuitListener != null) {
            this.circuitListener.onCircuitAcquired(this);
        }
    }

    /**
     *
     * @param msg
     * @param t
     */
    private void logError(final String msg, final Exception t) {

        final boolean stacktrace;

        if (this.circuitListener == null) {
            stacktrace = this.isLogExceptionTracktrace();
        } else {
            stacktrace = this.circuitListener.isLogExceptionTracktrace(this, t);
        }

        if (stacktrace) {
            LOG.error(msg, t);
        } else {
            LOG.error(msg);
        }
    }

    /**
     *
     * @param operation
     *            The {@link CircuitBreakerOperation}.
     * @return The object returned from
     *         {@link CircuitBreakerOperation#execute(CircuitBreaker)}.
     * @throws CircuitBreakerException
     *             When circuit is not closed.
     * @throws InterruptedException
     *             When the thread is interrupted.
     */
    public Object execute(final CircuitBreakerOperation operation)
            throws CircuitBreakerException, InterruptedException {

        final String operClassName;

        if (StringUtils.isNotBlank(operation.getClass().getSimpleName())) {
            operClassName = operation.getClass().getSimpleName();
        } else {
            operClassName = "anonymous";
        }

        final String logPrefix =
                "Circuit [" + this.getCircuitId() + "] Operation ["
                        + operClassName + "]: ";

        LOG.debug(logPrefix + "executing...");

        try {

            this.getSemaphore().acquire();

            LOG.debug(logPrefix + "semaphore acquired.");
            this.onCircuitAcquired();

            //
            if (this.isCircuitDamaged()) {

                final String msg = logPrefix + "Circuit is damaged.";
                LOG.trace(msg);

                throw new CircuitBreakerException(msg);
            }

            //
            if (this.isCircuitHalfOpen()) {

                final String msg =
                        logPrefix
                                + "Busy retrying operation in opened circuit.";
                LOG.trace(msg);

                throw new CircuitBreakerException(msg);
            }

            Object returnValue = null;

            if (this.isCircuitOpen()) {

                /*
                 * Circuit is open and wait time not exceeded: throw exception
                 * immediately.
                 */
                if (!this.isWaitTimeExceeded()) {

                    final String msg =
                            logPrefix + "cannot be performed due to open"
                                    + " circuit (too many failures).";
                    LOG.trace(msg);

                    throw new CircuitBreakerException(msg);
                }

                /*
                 * So, we waited long enough to retry.
                 */
                try {
                    /*
                     * Open the circuit for this one call only.
                     */
                    this.openCircuitHalf();

                    LOG.debug(logPrefix + "Retrying because waitTime "
                            + "exceeded.");

                    /*
                     * Execute the operation.
                     */
                    returnValue = operation.execute(this);

                    LOG.debug(logPrefix + "Retry succeeded, closing circuit.");

                    /*
                     * Operation succeeded: close circuit (and reset the failure
                     * count).
                     */
                    this.closeCircuit();

                } catch (Exception e) {

                    if (this.isDamagingException(e)) {

                        final String msg =
                                logPrefix + "Retry threw damaging exception ["
                                        + e.getClass().getSimpleName() + "] ("
                                        + e.getMessage()
                                        + ") : damaging circuit.";

                        logError(msg, e);

                        this.onDamagingException(e);

                        this.damageCircuit();

                        throw e;
                    }

                    if (this.isNonTrippingException(e)) {

                        final String msg =
                                logPrefix
                                        + "Retry threw non-tripping exception ["
                                        + e.getClass().getSimpleName() + "] ("
                                        + e.getMessage()
                                        + ") : closing circuit anyway.";

                        logError(msg, e);

                        /*
                         * Operation failed because of a non-tripping exception:
                         * close circuit (and reset the failure count).
                         */
                        this.onNonTrippingException(e);

                        this.closeCircuit();

                        throw e;
                    }

                    final String msg =
                            logPrefix + "Retry failed. Reason ["
                                    + e.getClass().getSimpleName() + "] ("
                                    + e.getMessage()
                                    + ") : keep circuit closed.";

                    logError(msg, e);

                    /*
                     * Operation failed: open circuit (and reset the wait
                     * period).
                     */
                    this.onTrippingException(e);

                    this.openCircuit();

                    throw new CircuitBreakerException(logPrefix
                            + "Too many failures: opening circuit.", e);
                }

            } else if (this.isCircuitClosed()) {

                LOG.debug(logPrefix + "is closed");

                /*
                 * Circuit is closed, execute operation.
                 */
                try {

                    returnValue = operation.execute(this);

                    this.closeCircuit();

                } catch (Exception e) {

                    String msg;

                    msg =
                            logPrefix + "Failure. Reason ["
                                    + e.getClass().getSimpleName() + "] ("
                                    + e.getMessage() + ")";

                    logError(msg, e);

                    if (this.isDamagingException(e)) {
                        this.onDamagingException(e);
                        this.damageCircuit();
                        throw e;
                    }

                    if (this.isNonTrippingException(e)) {
                        this.onNonTrippingException(e);
                        throw e;
                    }

                    if (this.addFailure() >= this.getFailureThreshold()) {

                        msg =
                                logPrefix + "Tripped on failure. Reason ["
                                        + e.getClass().getSimpleName() + "] ("
                                        + e.getMessage() + ")";

                        LOG.warn(msg);

                        this.onTrippingException(e);

                        this.openCircuit();

                        throw new CircuitBreakerException(logPrefix
                                + "Too many failures: opening circuit", e);
                    } else {

                        this.onTrippingException(e);

                        throw e;
                    }
                }

            }

            return returnValue;

        } finally {

            this.getSemaphore().release();
            LOG.debug(logPrefix + "semaphore released.");
            LOG.debug(logPrefix + "execution finished.");
        }

    }

    /**
     * Gets the Circuit ID.
     *
     * @return The ID.
     */
    public final String getCircuitId() {
        return circuitId;
    }

    /**
     * Sets the state.
     *
     * @param state
     *            The state.
     */
    public final void setCircuitState(final CircuitStateEnum state) {
        if (this.state == null) {
            this.state = new AtomicReference<CircuitStateEnum>();
        } else {
            this.state.getAndSet(state);
        }
    }

    public CircuitStateEnum getCircuitState() {
        return this.state.get();
    }

    public void setLastOpenedTime(final long lastOpenedTime) {
        this.lastOpenedTime = lastOpenedTime;
    }

    public long getLastOpenedTime() {
        return this.lastOpenedTime;
    }

    /**
     * Increments the failure counter.
     *
     * @return The incremented failure counter.
     */
    public int addFailure() {
        return this.failureCount.incrementAndGet();
    }

    public int getFailureCount() {
        return this.failureCount.get();
    }

    public boolean isWaitTimeExceeded() {
        final long diff = System.currentTimeMillis() - this.millisUntilRetry;
        return diff > this.lastOpenedTime;
    }

    public boolean isThresholdReached() {
        return getFailureCount() >= getFailureThreshold();
    }

    public int getFailureThreshold() {
        return this.failureThreshold;
    }

    public Class<? extends Exception>[] getNonTrippingExceptions() {
        return this.nonTrippingExceptions;
    }

    public int getMillisUntilRetry() {
        return this.millisUntilRetry;
    }

    public void setFailureThreshold(int failureThreshold) {
        this.failureThreshold = failureThreshold;
    }

    public void setNonTrippingExceptions(
            Class<? extends Exception>[] nonTrippingExceptions) {
        this.nonTrippingExceptions = nonTrippingExceptions;
    }

    public void setMillisUntilRetry(int millisUntilRetry) {
        this.millisUntilRetry = millisUntilRetry;
    }

    public void setCircuitId(String id) {
        this.circuitId = id;
    }

    /**
     * Gets the semaphore.
     *
     * @return The semaphore.
     */
    public final Semaphore getSemaphore() {
        return this.semaphore;
    }

    /**
     * Opens the circuit and notifies the {@link CircuitBreakerListener} when
     * previous state was {@link CircuitStateEnum#CLOSED}.
     */
    public void openCircuit() {

        final boolean notify =
                this.circuitListener != null && isCircuitClosed();

        setLastOpenedTime(System.currentTimeMillis());
        setCircuitState(CircuitStateEnum.OPEN);
        this.openCount++;

        if (notify) {
            this.circuitListener.onCircuitOpened(this);
        }
    }

    /**
     * Half opens the circuit.
     */
    public final void openCircuitHalf() {
        setCircuitState(CircuitStateEnum.HALF_OPEN);
    }

    /**
     * Closes the circuit and notifies the {@link CircuitBreakerListener} when
     * previous state was <i>not</i> {@link CircuitStateEnum#CLOSED}.
     */
    public final void closeCircuit() {

        final boolean notify =
                this.circuitListener != null && !isCircuitClosed();

        this.failureCount.set(0);
        setCircuitState(CircuitStateEnum.CLOSED);

        if (notify) {
            this.circuitListener.onCircuitClosed(this);
        }
    }

    /**
     * Damages the circuit and notifies the {@link CircuitBreakerListener}.
     */
    public void damageCircuit() {
        setCircuitState(CircuitStateEnum.DAMAGED);
        this.circuitListener.onCircuitDamaged(this);
    }

    /**
     * Tells whether the circuit is open.
     *
     * @return {@code true} when {@link CircuitStateEnum#OPEN} .
     */
    public boolean isCircuitOpen() {
        return getCircuitState() == CircuitStateEnum.OPEN;
    }

    public boolean isCircuitHalfOpen() {
        return getCircuitState() == CircuitStateEnum.HALF_OPEN;
    }

    public boolean isCircuitClosed() {
        return getCircuitState() == CircuitStateEnum.CLOSED;
    }

    public boolean isCircuitDamaged() {
        return getCircuitState() == CircuitStateEnum.DAMAGED;
    }

    public int getOpenCount() {
        return this.openCount;
    }

    private boolean isNonTrippingException(Exception t) {
        for (Class<?> exception : this.getNonTrippingExceptions()) {
            if (exception.isAssignableFrom(t.getClass())) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param ex
     *            The exception to check.
     * @return {@true} when exception is damaging.
     */
    private boolean isDamagingException(final Exception ex) {

        for (Class<?> exception : this.getDamagingExceptions()) {
            if (exception.isAssignableFrom(ex.getClass())) {
                return true;
            }
        }
        return false;
    }

    public final Class<? extends Exception>[] getDamagingExceptions() {
        return damagingExceptions;
    }

    public final void setDamagingExceptions(
            final Class<? extends Exception>[] damagingExceptions) {
        this.damagingExceptions = damagingExceptions;
    }

}
// end-of-file
