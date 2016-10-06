/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.ipp.helpers;

import java.util.Map;

import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppOptionMap {

    /**
     * PP option map.
     */
    private final Map<String, String> optionValues;

    /**
     * Constructor.
     */
    /**
     *
     * @param options
     *            The IPP option map.
     */
    public IppOptionMap(final Map<String, String> options) {
        this.optionValues = options;
    }

    /**
     *
     * @return {@code true} if this is a color job.
     */
    public boolean isColorJob() {
        return isOptionPresent(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE,
                IppKeyword.PRINT_COLOR_MODE_COLOR);
    }

    /**
     *
     * @return {@code true} if this is a duplex job.
     */
    public boolean isDuplexJob() {
        return isOptionPresent(IppDictJobTemplateAttr.ATTR_SIDES,
                IppKeyword.SIDES_TWO_SIDED_LONG_EDGE)
                || isOptionPresent(IppDictJobTemplateAttr.ATTR_SIDES,
                        IppKeyword.SIDES_TWO_SIDED_SHORT_EDGE);
    }

    /**
     *
     * @return The Number-Up attribute value;
     */
    public Integer getNumberUp() {
        final String value =
                this.optionValues.get(IppDictJobTemplateAttr.ATTR_NUMBER_UP);
        if (value != null) {
            return Integer.valueOf(value);
        }
        return Integer.valueOf(1);
    }

    /**
     *
     * @return {@code true} if job requests punch finishing.
     */
    public boolean hasFinishingPunch() {
        return isOptionPresentUnequal(
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH,
                IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH_NONE);
    }

    /**
     *
     * @return {@code true} if job requests fold finishing.
     */
    public boolean hasFinishingFold() {
        return isOptionPresentUnequal(
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_FOLD,
                IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_FOLD_NONE);
    }

    /**
     *
     * @return {@code true} if job requests booklet finishing.
     */
    public boolean hasFinishingBooklet() {
        return isOptionPresentUnequal(
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET,
                IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET_NONE);
    }

    /**
     *
     * @return {@code true} if requests staple finishing.
     */
    public boolean hasFinishingStaple() {
        return isOptionPresentUnequal(
                IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE,
                IppKeyword.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE_NONE);
    }

    /**
     * Checks if an option is present and has value unequal to compareValue.
     *
     * @param key
     *            The option key.
     * @param compareValue
     *            The option value to compare with.
     * @return {@code true} if option is present and has value unequal to
     *         compareValue.
     */
    public boolean isOptionPresentUnequal(final String key,
            final String compareValue) {
        final String found = this.optionValues.get(key);
        return found != null && !found.equals(compareValue);
    }

    /**
     * Checks if option is present.
     *
     * @param key
     *            The option key.
     * @return {@code true} if option is present.
     */
    public boolean isOptionPresent(final String key) {
        return this.getOptionValue(key) != null;
    }

    /**
     * Gets the option value.
     *
     * @param key
     *            The option key.
     * @return {@code null} when option is not present.
     */
    public String getOptionValue(final String key) {
        return this.optionValues.get(key);
    }

    /**
     * Checks if option value is present.
     *
     * @param key
     *            The option key.
     * @param value
     *            The option value;
     * @return {@code true} if option value is present.
     */
    public boolean isOptionPresent(final String key, final String value) {
        final String found = this.optionValues.get(key);
        return found != null && found.equals(value);
    }

}
