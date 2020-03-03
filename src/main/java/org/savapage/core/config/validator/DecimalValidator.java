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
import org.savapage.core.util.BigDecimalUtil;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class DecimalValidator implements ConfigPropValidator {

    @Override
    public ValidationResult validate(final String plainString) {

        final ValidationResult res = new ValidationResult(plainString);

        if (StringUtils.isBlank(plainString)) {
            res.setStatus(ValidationStatusEnum.ERROR_EMPTY);
            res.setMessage("Value must be a decimal.");
        } else {
            if (!BigDecimalUtil.isValid(plainString)) {
                res.setStatus(ValidationStatusEnum.ERROR_NOT_DECIMAL);
                res.setMessage("Decimal value is invalid.");
            }
        }
        return res;
    }
}
