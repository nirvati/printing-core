/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.ipp.attribute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.savapage.core.ipp.attribute.syntax.AbstractIppAttrSyntax;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper list of {@link IppAttrValue} objects.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class IppAttrList {

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(IppAttrList.class);

    /**
     *
     */
    private final Map<String, IppAttrValue> attrLookup = new HashMap<>();

    /**
     *
     */
    private List<IppAttrValue> attributes = new ArrayList<>();

    /**
     *
     * @return The list.
     */
    public final List<IppAttrValue> getAttributes() {
        return attributes;
    }

    /**
     *
     * @param attributeList
     *            The attribute list.
     */
    public final void setAttributes(final List<IppAttrValue> attributeList) {
        this.attributes = attributeList;
    }

    /**
     *
     */
    public final void resetAttributes() {
        this.attributes = new ArrayList<>();
        this.attrLookup.clear();
    }

    /**
     * Adds an {@link IppAttrValue} to the group attributes.
     * <p>
     * </p>
     *
     * @param attr
     *            The {@link IppAttrValue} to add.
     */
    public final void addAttribute(final IppAttrValue attr) {

        // Add to the list.
        attributes.add(attr);

        final String attrKey = attr.getAttribute().getKeyword();

        final boolean doLookupUpdate;

        /*
         * Mantis #373 WORKAROUND FIX: preserve color mode default 'color'.
         *
         * However, when lpoptions -p shows "print-color-mode=color" we still
         * get "monochrome" as print-color-mode-default. Why?
         */
        if (attrKey.equals(IppDictJobTemplateAttr.ATTR_PRINT_COLOR_MODE_DFLT)) {

            final IppAttrValue presentDefault = attrLookup.get(attrKey);

            doLookupUpdate =
                    presentDefault == null || presentDefault.getSingleValue()
                            .equals(IppKeyword.PRINT_COLOR_MODE_MONOCHROME);

            if (!doLookupUpdate && LOGGER.isDebugEnabled()) {

                final StringBuilder msg = new StringBuilder();

                msg.append("[")
                        .append(attrLookup
                                .get(IppDictPrinterDescAttr.ATTR_PRINTER_NAME)
                                .getSingleValue())
                        .append("] [").append(attrKey).append("] [")
                        .append(attr.getAttribute().getSyntax().getValueTag())
                        .append("] is NOT overwritten.");

                LOGGER.debug(msg.toString());
            }

        } else {
            doLookupUpdate = true;
        }

        if (doLookupUpdate) {

            final IppAttrValue alreadyPresent = attrLookup.put(attrKey, attr);

            if (alreadyPresent != null) {
                /*
                 * Several attributes are returned twice (CUPS bug?). For all
                 * these attributes the value is valid and consistent, except
                 * for "print-color-mode-default". Therefore the color default
                 * value is not reliable.
                 *
                 * CUPS should return a consistent "print-color-mode-default"
                 * value.
                 *
                 * See Mantis #373.
                 */
                if (LOGGER.isDebugEnabled()) {

                    final StringBuilder msg = new StringBuilder();

                    msg.append("[").append(attrKey).append("] [").append(
                            attr.getAttribute().getSyntax().getValueTag())
                            .append("]");

                    for (final String value : alreadyPresent.getValues()) {
                        msg.append(" [").append(value).append("]");
                    }

                    /*
                     * NOTE: the values of the new attribute are unknown at this
                     * point.
                     */
                    msg.append(" overwritten.");

                    LOGGER.debug(msg.toString());
                }
            }
        }
    }

    /**
     * Convenience method to add an {@link IppAttrValue}.
     *
     * @param keyword
     *            The keyword (name) of the attribute.
     * @param syntax
     *            The {@link AbstractIppAttrSyntax}.
     * @param value
     *            The value.
     */
    public final void add(final String keyword,
            final AbstractIppAttrSyntax syntax, final String value) {

        final IppAttrValue attr =
                new IppAttrValue(new IppAttr(keyword, syntax));
        attr.addValue(value);
        addAttribute(attr);
    }

    /**
     * Convenience method to add an {@link IppAttrValue}.
     *
     * @param attr
     *            The {@link IppAttr}.
     * @param value
     *            The value.
     */
    public final void add(final IppAttr attr, final String value) {
        final IppAttrValue attrValue = new IppAttrValue(attr);
        attrValue.addValue(value);
        addAttribute(attrValue);
    }

    /**
     * Convenience method to add an {@link IppAttrValue} with blank value.
     *
     * @param attr
     *            The {@link IppAttr}.
     */
    public final void add(final IppAttr attr) {
        this.add(attr, "");
    }

    /**
     * Gets the attribute value of an attribute keyword.
     *
     * @param keyword
     *            The keyword (name) of the attribute.
     * @return {@code null} if NOT found.
     */
    public final IppAttrValue getAttrValue(final String keyword) {
        return attrLookup.get(keyword);
    }

    /**
     * Gets the attribute values of an attribute keyword.
     *
     * @param keyword
     *            The keyword (name) of the attribute.
     * @return {@code null} if NOT found.
     */
    public final List<String> getAttrValues(final String keyword) {

        final IppAttrValue attrValue = attrLookup.get(keyword);

        if (attrValue != null) {
            return attrValue.getValues();
        } else {
            return null;
        }
    }

    /**
     * Gets the first (and only) value on the list.
     *
     * @param keyword
     *            The keyword (name) of the attribute.
     * @return {@code null} when key is unknown or when the list of values does
     *         NOT have exactly ONE element.
     */
    public final String getAttrSingleValue(final String keyword) {

        final IppAttrValue attrValue = attrLookup.get(keyword);

        if (attrValue != null) {
            return attrValue.getSingleValue();
        } else {
            return null;
        }
    }

    /**
     * Gets the first (and only) value on the list or the default when not
     * found.
     *
     * @param key
     *            The attribute name (keyword).
     * @param dfault
     *            The default value when key is unknown or when the list of
     *            values does NOT have exactly ONE element.
     * @return Value found or the default.
     */
    public final String getAttrSingleValue(final String key,
            final String dfault) {

        final String value = getAttrSingleValue(key);

        if (value == null) {
            return dfault;
        } else {
            return value;
        }
    }

}
