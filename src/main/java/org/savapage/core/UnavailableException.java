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
package org.savapage.core;

/**
 * Defines an exception that a service throws to indicate that it is permanently
 * or temporarily unavailable.
 *
 * @author Rijk Ravestein
 *
 */
public final class UnavailableException extends Exception {

    /**
     *
     */
    public static enum State {
        TEMPORARY, PERMANENT
    }

    /**
     *
     */
    private final State state;

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@link UnavailableException} with the specified cause.
     *
     * @param state
     *            The state.
     * @param cause
     *            The cause.
     */
    public UnavailableException(final State state, final Throwable cause) {
        super(cause);
        this.state = state;
    }

    /**
     * Constructs a new {@link UnavailableException} with the specified detail
     * message.
     *
     * @param state
     *            The state.
     * @param message
     *            The detail message.
     */
    public UnavailableException(final State state, final String message) {
        super(message);
        this.state = state;
    }

    /**
     * Constructs a new {@link UnavailableException} with the specified detail
     * message and cause.
     *
     * @param state
     *            The state.
     * @param message
     *            The detail message.
     * @param cause
     *            The cause.
     */
    public UnavailableException(final State state, final String message,
            final Throwable cause) {
        super(message, cause);
        this.state = state;
    }

    /**
     *
     * @return The reason.
     */
    public State getState() {
        return state;
    }

}
