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
import java.util.List;

import org.savapage.core.ipp.encoding.IppDelimiterTag;

/**
 * A group identified with {@link IppDelimiterTag} containing
 * {@link IppAttrValue} and {@link IppAttrCollection} objects.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppAttrGroup extends IppAttrList {

    /**
    *
    */
    private final List<IppAttrCollection> collections = new ArrayList<>();

    /**
     *
     */
    private IppDelimiterTag delimiterTag;

    /**
     *
     */
    @SuppressWarnings("unused")
    private IppAttrGroup() {

    }

    /**
     * The constructor.
     *
     * @param delimiter
     *            The {@linkIppDelimiterTag}.
     */
    public IppAttrGroup(final IppDelimiterTag delimiter) {
        super();
        this.setDelimiterTag(delimiter);
    }

    /**
     *
     * @return The {@link IppDelimiterTag}.
     */
    public IppDelimiterTag getDelimiterTag() {
        return delimiterTag;
    }

    /**
     *
     * @param delimiter
     *            The {@linkIppDelimiterTag}.
     */
    public void setDelimiterTag(final IppDelimiterTag delimiter) {
        this.delimiterTag = delimiter;
    }

    /**
     *
     * @return The collection list.
     */
    public List<IppAttrCollection> getCollections() {
        return collections;
    }

    /**
     * Adds an {@link IppAttrCollection}.
     *
     * @param collection
     *            The {@link IppAttrCollection} to add.
     */
    public void addCollection(final IppAttrCollection collection) {
        collections.add(collection);
    }

}
