/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.services.helpers;

/**
 * An exception for logical errors during accounting.
 *
 * @author Rijk Ravestein
 *
 */
public final class AccountingException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new instance.
     *
     * @param cause
     *            The cause.
     */
    public AccountingException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new instance.
     *
     * @param message
     *            The detail message.
     */
    public AccountingException(final String message) {
        super(message);
    }

    /**
     * Constructs a new instance.
     *
     * @param message
     *            The detail message.
     * @param cause
     *            The cause.
     */
    public AccountingException(final String message, final Throwable cause) {
        super(message, cause);
    }

}
