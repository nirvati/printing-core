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

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class EnumSetValidator<E extends Enum<E>>
        implements ConfigPropValidator {

    /** */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(EnumSetValidator.class);

    /** */
    private final Class<E> type;

    /**
     *
     * @param enumType
     *            The Enum type.
     */
    public EnumSetValidator(final Class<E> enumType) {
        this.type = enumType;
    }

    @Override
    public ValidationResult validate(final String value) {

        final ValidationResult res = new ValidationResult(value);

        final List<E> enumList = new ArrayList<>();

        if (!fillEnumList(enumList, this.type, value)) {
            res.setStatus(ValidationStatusEnum.ERROR_ENUM);
            res.setMessage(String.format("Invalid %s value [%s].",
                    this.type.getSimpleName(), value));
        }
        return res;
    }

    /**
     * Gets the enum set config value. Invalid enum values are not added to the
     * list.
     *
     * @param enumList
     *            The {@link List} to add on.
     * @param enumClass
     *            The enum class.
     * @param list
     *            The raw " ,;:" separated list of enum values.
     * @param <E>
     *            The enum type.
     * @return {@code true} when all values are valid.
     */
    private static <E extends Enum<E>> boolean fillEnumList(
            final List<E> enumList, final Class<E> enumClass,
            final String list) {

        boolean isValid = true;

        if (StringUtils.isNotBlank(list)) {

            for (final String value : StringUtils.split(list, " ,;:")) {

                final E enumValue = EnumUtils.getEnum(enumClass, value);

                if (enumValue == null) {
                    isValid = false;
                } else {
                    enumList.add(enumValue);
                }
            }
        }
        return isValid;
    }

    /**
     * Gets the enum set config value. Invalid enum values are not added to the
     * set.
     *
     * @param enumClass
     *            The enum class.
     * @param list
     *            The raw " ,;:" separated list of enum values.
     * @param <E>
     *            The enum type.
     * @return The enum set (can be empty)
     */
    public static <E extends Enum<E>> EnumSet<E>
            getEnumSet(final Class<E> enumClass, final String list) {

        return EnumSet.copyOf(getEnumList(enumClass, list));
    }

    /**
     * Gets the enum list config value. Invalid enum values are not added to the
     * list.
     *
     * @param enumClass
     *            The enum class.
     * @param list
     *            The raw " ,;:" separated list of enum values.
     * @param <E>
     *            The enum type.
     * @return The enum set (can be empty)
     */
    public static <E extends Enum<E>> List<E>
            getEnumList(final Class<E> enumClass, final String list) {

        final List<E> enumList = new ArrayList<>();
        if (!fillEnumList(enumList, enumClass, list)) {
            LOGGER.warn("Some values in \"{}\" are not of type {} and ignored.",
                    list, enumClass.getName());
        }
        return enumList;
    }

}
