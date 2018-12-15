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
package org.savapage.core.ipp;

import java.util.Locale;

import org.savapage.core.util.LocaleHelper;

/**
 * Enumeration of IPP job states.
 * <p>
 * See <a href="https://tools.ietf.org/html/rfc8011#section-5.3.7">RFC 8011</a>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum IppPrinterStateEnum {

    /** Out-of-band 'unknown'. */
    UNKNOWN(0x00, "UNKNOWN"),

    /**
     * Indicates that new jobs can start processing without waiting.
     */
    IDLE(0x03, "IDLE"),

    /**
     * Indicates that jobs are processing: new jobs will wait before processing.
     */
    PROCESSING(0x04, "PROCESSING"),

    /**
     * Indicates that no jobs can be processed and intervention is required.
     */
    STOPPED(0x05, "STOPPED");

    /**
     *
     */
    private final int bitPattern;

    /**
     * Text to be used in user interface.
     */
    private final String logText;

    /**
     * Creates an enum value from an integer.
     *
     * @param value
     *            The integer.
     * @param text
     *            Text to be used in user interface.
     */
    IppPrinterStateEnum(final int value, final String text) {
        this.bitPattern = value;
        this.logText = text;
    }

    /**
     * Gets the int representing this enum value.
     *
     * @return The int value.
     */
    public int asInt() {
        return this.bitPattern;
    }

    /**
     * Gets the {@link Integer} representing this enum value.
     *
     * @return The Integer value.
     */
    public Integer asInteger() {
        return Integer.valueOf(this.asInt());
    }

    /**
     *
     * @return Text string to be used for logging.
     */
    public String asLogText() {
        return this.logText;
    }

    /**
     * @param locale
     *            The {@link Locale}.
     * @return The localized text.
     */
    public String uiText(final Locale locale) {
        return LocaleHelper.uiText(this, locale);
    }

    /**
     *
     * @param value
     *            The CUPS printers state.
     * @return The enum value.
     * @throws IllegalStateException
     *             If printer state is unidentified.
     */
    public static IppPrinterStateEnum asEnum(final int value) {
        if (value == IppPrinterStateEnum.UNKNOWN.asInt()) {
            return UNKNOWN;
        } else if (value == IppPrinterStateEnum.PROCESSING.asInt()) {
            return PROCESSING;
        } else if (value == IppPrinterStateEnum.IDLE.asInt()) {
            return IDLE;
        } else if (value == IppPrinterStateEnum.STOPPED.asInt()) {
            return STOPPED;
        }
        throw new IllegalStateException(
                String.format("unidentified CUPS printer state [%d]", value));
    }

}
