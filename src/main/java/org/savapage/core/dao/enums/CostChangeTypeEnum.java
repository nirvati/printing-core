/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.dao.enums;

import java.util.Locale;

import org.savapage.core.i18n.AdverbEnum;
import org.savapage.core.jpa.CostChange;

/**
 * Type of Cost Change.
 *
 * <p>
 * <b>Important</b>: the length of {@link CostChangeTypeEnum#toString()} MUST
 * NOT exceed the column width of {@link CostChange#getChgType()}.
 *
 * @author Rijk Ravestein
 *
 */
public enum CostChangeTypeEnum {

    /**
     *
     */
    AUTO,

    /**
     *
     */
    MANUAL;

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        if (this == AUTO) {
            return AdverbEnum.AUTOMATICALLY.uiText(locale);
        }
        return AdverbEnum.MANUALLY.uiText(locale);
    }

}
