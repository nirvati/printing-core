/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
package org.savapage.core.json;

import java.util.ArrayList;

import org.savapage.core.dto.IppMediaSourceMappingDto;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.print.proxy.JsonProxyPrinterOptGroup;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Datraverse B.V.
 */
@JsonInclude(Include.NON_NULL)
public final class JsonPrinterDetail extends JsonPrinter {

    /**
     *
     */
    private ArrayList<JsonProxyPrinterOptGroup> groups;

    /**
     * An list of media-sources with assigned media. The list is optional and
     * may be undetermined, i.e. {@code null}.
     * <p>
     * The list does NOT contain {@link IppKeyword#MEDIA_SOURCE_AUTO} and
     * {@link IppKeyword#MEDIA_SOURCE_MANUAL} instances.
     * </p>
     */
    private ArrayList<IppMediaSourceMappingDto> mediaSources;

    /**
     *
     * @return
     */
    public ArrayList<JsonProxyPrinterOptGroup> getGroups() {
        return groups;
    }

    /**
     *
     * @param groups
     */
    public void setGroups(final ArrayList<JsonProxyPrinterOptGroup> groups) {
        this.groups = groups;
    }

    /**
     * Gets the list of media-sources with assigned media. The list is optional
     * and may be undetermined, i.e. {@code null}.
     * <p>
     * The list does NOT contain {@link IppKeyword#MEDIA_SOURCE_AUTO} and
     * {@link IppKeyword#MEDIA_SOURCE_MANUAL} instances.
     * </p>
     *
     * @return {@code null} when undetermined.
     */
    public ArrayList<IppMediaSourceMappingDto> getMediaSources() {
        return mediaSources;
    }

    /**
     * Sets the list of media-sources with assigned media.
     * <p>
     * The list may NOT contain {@link IppKeyword#MEDIA_SOURCE_AUTO} and
     * {@link IppKeyword#MEDIA_SOURCE_MANUAL} instances.
     * </p>
     *
     * @param mediaSources
     *            The list.
     */
    public void setMediaSources(
            final ArrayList<IppMediaSourceMappingDto> mediaSources) {
        this.mediaSources = mediaSources;
    }

    /**
     * Creates a deep copy instance.
     *
     * @return The new copy.
     */
    public JsonPrinterDetail copy() {

        final JsonPrinterDetail copy = new JsonPrinterDetail();

        super.copy(copy);

        copy.groups = new ArrayList<>();

        for (final JsonProxyPrinterOptGroup group : groups) {
            copy.groups.add(group.copy());
        }

        return copy;
    }
}
