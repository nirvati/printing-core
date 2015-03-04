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
package org.savapage.core;

/**
 *
 * @author Datraverse B.V.
 */
public class LetterheadNotFoundException extends RuntimeException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a {@link LetterheadNotFoundException} instance.
     *
     * @param cause
     *            The cause.
     */
    public LetterheadNotFoundException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a {@link LetterheadNotFoundException} instance.
     *
     * @param message
     *            The detail message.
     */
    public LetterheadNotFoundException(final String message) {
        super(message);
    }

    /**
     * Creates a {@link LetterheadNotFoundException} instance.
     *
     * @param message
     *            The detail message.
     * @param cause
     *            The cause.
     */
    public LetterheadNotFoundException(final String message,
            final Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a {@link LetterheadNotFoundException} instance.
     *
     * @param publicLetterhead
     *            {@code true} if a public letterhead.
     * @param letterheadId
     *            The letterhead ID.
     * @return The created instance.
     */
    public static LetterheadNotFoundException create(
            final boolean publicLetterhead, final String letterheadId) {

        final StringBuilder msg = new StringBuilder();

        if (publicLetterhead) {
            msg.append("Public");
        } else {
            msg.append("Private");
        }
        msg.append(" letterhead [").append(letterheadId).append("] not found");

        return new LetterheadNotFoundException(msg.toString());
    }
}
