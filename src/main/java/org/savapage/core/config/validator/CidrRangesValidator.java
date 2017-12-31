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

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.util.InetUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class CidrRangesValidator implements ConfigPropValidator {

    /**
     * {@code true} if value is optional.
     */
    private final boolean isOptional;

    /**
     *
     * @param optional
     *            {@code true} if value is optional.
     */
    public CidrRangesValidator(final boolean optional) {
        this.isOptional = optional;
    }

    @Override
    public ValidationResult validate(final String value) {

        final ValidationResult res = new ValidationResult(value);

        if (StringUtils.isBlank(value)) {

            if (!this.isOptional) {
                res.setStatus(ValidationStatusEnum.ERROR_EMPTY);
                res.setMessage("Please enter a value");
            }

        } else {

            try {
                /*
                 * Probe with a non-localhost address.
                 */
                InetUtils.isIp4AddrInCidrRanges(value, "10.0.0.1");

            } catch (final Throwable thr) {
                res.setStatus(ValidationStatusEnum.ERROR_SYNTAX);
                res.setMessage("Value is not CIDR syntax.");
            }
        }
        return res;
    }
}
