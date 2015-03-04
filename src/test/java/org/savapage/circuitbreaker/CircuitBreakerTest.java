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
package org.savapage.circuitbreaker;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;
import org.savapage.core.circuitbreaker.CircuitBreaker;
import org.savapage.core.circuitbreaker.CircuitBreakerException;
import org.savapage.core.circuitbreaker.CircuitBreakerOperation;
import org.savapage.core.circuitbreaker.CircuitBreakerRegistry;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class CircuitBreakerTest {

    private static final String CIRCUIT_TEST_OK = "TEST_OK";
    private static final String CIRCUIT_TEST_TRIP = "TEST_TRIP";

    private final CircuitBreakerRegistry registry = new CircuitBreakerRegistry();

    /**
     *
     */
    public static class ConnectivityException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    /**
     *
     */
    public static class LegalException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    //
    final CircuitBreakerOperation trippingOperation =
            new CircuitBreakerOperation() {
                @Override
                public Object execute(final CircuitBreaker circuitBreaker) {
                    throw new ConnectivityException();
                }
            };

    //
    final CircuitBreakerOperation legalExceptionOperation =
            new CircuitBreakerOperation() {
                @Override
                public Object execute(final CircuitBreaker circuitBreaker) {
                    throw new LegalException();
                }
            };

    //
    private static final String OK = "OK";

    final CircuitBreakerOperation successOperation =
            new CircuitBreakerOperation() {
                @Override
                public Object execute(final CircuitBreaker circuitBreaker) {
                    return OK;
                }
            };

    @Before
    public void initTest() {
        /*
         * Set up a simple log4j configuration that logs on the console.
         */
        BasicConfigurator.configure();
    }

    @Test
    public void testOK() throws InterruptedException {

        final CircuitBreaker breaker =
                registry.getOrCreateCircuitBreaker(CIRCUIT_TEST_OK);

        breaker.setFailureThreshold(2);
        breaker.setMillisUntilRetry(3000);
        breaker.setLogExceptionTracktrace(false);

        try {
            assertTrue(registry.getCircuitBreaker(CIRCUIT_TEST_OK)
                    .execute(successOperation).toString().equals(OK));
        } catch (CircuitBreakerException e) {
            fail(e.getMessage());
        }

    }

    @Test
    public void testTripping() throws InterruptedException {

        final int millisUntilRetry = 1000;
        final int failureThreshold = 2;
        final int trialsAfterThreshold = 2;

        final CircuitBreaker breaker =
                registry.getOrCreateCircuitBreaker(CIRCUIT_TEST_TRIP);

        breaker.setFailureThreshold(failureThreshold);
        breaker.setMillisUntilRetry(millisUntilRetry);
        breaker.setLogExceptionTracktrace(false);

        breaker.setNonTrippingExceptions(new Class[] { LegalException.class });

        /*
         * Execute till failureThreshold and circuit opens + execute
         * trialsAfterThreshold.
         */
        for (int i = 0; i < failureThreshold + trialsAfterThreshold; i++) {

            final boolean isFailureThreshold = (i + 1 >= failureThreshold);

            try {

                registry.getCircuitBreaker(CIRCUIT_TEST_TRIP).execute(
                        trippingOperation);

                if (isFailureThreshold) {
                    fail(CircuitBreakerException.class.getSimpleName()
                            + " expected");
                } else {
                    fail(ConnectivityException.class.getSimpleName()
                            + " expected");
                }

            } catch (ConnectivityException e) {

                if (isFailureThreshold) {
                    fail(e.getMessage());
                }

            } catch (CircuitBreakerException e) {

                if (!isFailureThreshold) {
                    fail(e.getMessage());
                }
            }

        }

        /*
         * At this point the circuit is opened.
         */
        assertTrue(breaker.isCircuitOpen());

        /*
         * A operation throwing a non-tripping exception still produces a
         * CircuitException on an open circuit.
         */
        try {
            registry.getCircuitBreaker(CIRCUIT_TEST_TRIP).execute(
                    legalExceptionOperation);
            fail(CircuitBreakerException.class.getSimpleName() + " expected");
        } catch (CircuitBreakerException e) {
            // noop
        } catch (LegalException e) {
            fail(e.getMessage());
        }

        /*
         * Even a potential successful operation throws an CircuitException.
         */
        try {
            registry.getCircuitBreaker(CIRCUIT_TEST_TRIP).execute(
                    successOperation);
            fail(CircuitBreakerException.class.getSimpleName() + " expected");
        } catch (CircuitBreakerException e) {
            // noop
        }

        /*
         * Wait millisUntilRetry: a successful operation will close the circuit.
         */
        Thread.sleep(millisUntilRetry + 10);

        try {
            registry.getCircuitBreaker(CIRCUIT_TEST_TRIP).execute(
                    successOperation);
        } catch (CircuitBreakerException e) {
            fail(e.getMessage());
        }

        assertTrue(breaker.isCircuitClosed());

        /*
         * Execute till failureThreshold with NON-tripping exception + execute
         * trialsAfterThreshold.
         */
        for (int i = 0; i < failureThreshold + trialsAfterThreshold; i++) {
            try {
                registry.getCircuitBreaker(CIRCUIT_TEST_TRIP).execute(
                        legalExceptionOperation);
                fail(LegalException.class.getSimpleName() + " expected");
            } catch (CircuitBreakerException e) {
                fail(e.getMessage());
            } catch (LegalException e) {
                // noop
            }
        }

        /*
         * The NON-tripping exceptions did NOT open the circuit.
         */
        assertTrue(breaker.isCircuitClosed());

    }
}
