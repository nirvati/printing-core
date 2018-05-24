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

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractSetValidator implements ConfigPropValidator {

    /**
     * {@code true} if value is optional.
     */
    private final boolean isOptional;

    /**
     * @param optional
     *            {@code true} if value is optional.
     */
    public AbstractSetValidator(final boolean optional) {
        super();
        this.isOptional = optional;
    }

    @Override
    public final ValidationResult validate(final String value) {

        final ValidationResult res = new ValidationResult(value);

        final Set<String> set = new HashSet<>();

        boolean isValid = true;

        if (StringUtils.isNotBlank(value)) {

            for (final String item : StringUtils.split(value, " ,;:")) {
                if (set.contains(item) || !this.onItem(item)) {
                    isValid = false;
                    break;
                }
                set.add(item);
            }
        } else if (!this.isOptional) {
            isValid = false;
        }

        if (!isValid) {
            res.setStatus(ValidationStatusEnum.ERROR_ENUM);
            res.setMessage(String.format("Invalid value [%s].", value));
        }

        return res;
    }

    /**
     * Notifies item in the Set.
     *
     * @param item
     *            The item to validate.
     * @return {@code true} when item is valid.
     */
    protected abstract boolean onItem(String item);

}
