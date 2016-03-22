/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.config.validator;

import org.savapage.core.config.IConfigProp;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class BooleanValidator implements ConfigPropValidator {

    @Override
    public ValidationResult validate(final String value) {

        final ValidationResult res = new ValidationResult(value);

        final boolean valid = (value != null && (value.equals(IConfigProp.V_YES)
                || value.equals(IConfigProp.V_NO)));

        if (!valid) {
            res.setStatus(ValidationStatusEnum.ERROR_YN);
        }

        if (!res.isValid()) {
            res.setMessage("Value should be [" + IConfigProp.V_YES + "] or ["
                    + IConfigProp.V_NO + "]");
        }
        return res;
    }

}