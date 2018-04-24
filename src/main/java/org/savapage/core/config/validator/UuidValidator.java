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
package org.savapage.core.config.validator;

import java.util.UUID;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class UuidValidator implements ConfigPropValidator {

    /**
     * {@code true} if value is optional.
     */
    private final boolean isOptional;

    /**
     * @param optional
     *            {@code true} if value is optional.
     */
    public UuidValidator(final boolean optional) {
        super();
        this.isOptional = optional;
    }

    /**
     *
     * @param value
     *            The value.
     * @throws Exception
     *             When check fails.
     */
    protected void customCheck(final String value) throws Exception {
        UUID.fromString(value);
    }

    @Override
    public ValidationResult validate(final String value) {

        final ValidationResult res = new ValidationResult(value);

        if (!this.isOptional || !value.isEmpty()) {

            try {
                this.customCheck(value);
            } catch (Exception e) {
                res.setStatus(ValidationStatusEnum.ERROR_SYNTAX);
            }
        }

        if (!res.isValid()) {
            res.setMessage("Invalid UUID.");
        }
        return res;
    }
}
