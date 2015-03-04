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

/**
 * An unchecked fatal exception that damages the {@link CircuitBreaker}.
 * <p>
 * This class should be used in {@link CircuitBreakerOperation#execute()} to
 * wrap {@link Throwable} instances that will damage the {@link CircuitBreaker}.
 * </p>
 *
 * @author Datraverse B.V.
 *
 */
public class CircuitDamagingException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@link CircuitDamagingException}.
     *
     * @param message
     *            The detail message.
     * @param cause
     *            The cause.
     */
    public CircuitDamagingException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a {@link CircuitDamagingException}.
     *
     * @param message
     *            The detail message.
     */
    public CircuitDamagingException(final String message) {
        super(message);
    }

    /**
     * Creates a {@link CircuitDamagingException}.
     *
     * @param cause
     *            The cause.
     */
    public CircuitDamagingException(final Throwable cause) {
        super(cause);
    }

}