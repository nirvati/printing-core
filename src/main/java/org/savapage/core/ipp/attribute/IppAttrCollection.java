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

/**
 *
 * @author Rijk Ravestein
 *
 */
public class IppAttrCollection extends IppAttrList {

    /**
     * The keyword (name) of the collection.
     */
    private final String keyword;

    /**
     * The nested collections.
     */
    private List<IppAttrCollection> collections = new ArrayList<>();

    /**
     *
     */
    @SuppressWarnings("unused")
    private IppAttrCollection() {
        this.keyword = null;
    }

    /**
     * @param keyword
     *            The keyword (name) of the collection attribute.
     */
    public IppAttrCollection(final String keyword) {
        this.keyword = keyword;
    }

    /**
     *
     * @return The keyword (name) of the collection attribute.
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     *
     * @return The nested collection list.
     */
    public List<IppAttrCollection> getCollections() {
        return collections;
    }

    public void setCollections(final List<IppAttrCollection> collections) {
        this.collections = collections;
    }

    /**
     * Resets the nested collection list.
     */
    public void resetCollections() {
        this.collections = new ArrayList<>();
    }

    /**
     * Adds an {@link IppAttrCollection} to the nested collection list.
     *
     * @param collection
     *            The {@link IppAttrCollection} to add.
     */
    public void addCollection(final IppAttrCollection collection) {
        collections.add(collection);
    }

    /**
     *
     * @return
     */
    public boolean isSetOfCollections() {
        return false;
    }

}
