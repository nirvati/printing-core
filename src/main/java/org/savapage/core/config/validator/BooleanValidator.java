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
package org.savapage.core.config.validator;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.IConfigProp;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class BooleanValidator implements ConfigPropValidator {

    /**
     * {@code true} if value is optional.
     */
    private final boolean isOptional;

    /**
     *
     * @param optional
     *            {@code true} if value is optional.
     */
    public BooleanValidator(final boolean optional) {
        this.isOptional = optional;
    }

    /** */
    private static final String getErrorMsg() {
        return "Value must be [" + IConfigProp.V_YES + "] or ["
                + IConfigProp.V_NO + "]";
    }

    @Override
    public ValidationResult validate(final String value) {

        final ValidationResult res = new ValidationResult(value);

        if (StringUtils.isBlank(value)) {

            if (!this.isOptional) {
                res.setStatus(ValidationStatusEnum.ERROR_EMPTY);
                res.setMessage(getErrorMsg());
            }

        } else {

            final boolean valid =
                    (value != null && (value.equals(IConfigProp.V_YES)
                            || value.equals(IConfigProp.V_NO)));

            if (!valid) {
                res.setStatus(ValidationStatusEnum.ERROR_YN);
            }

            if (!res.isValid()) {
                res.setMessage(getErrorMsg());
            }
        }
        return res;
    }

}