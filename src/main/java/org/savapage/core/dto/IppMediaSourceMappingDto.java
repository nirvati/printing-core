/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Media mapping information about a IPP media-source (tray).
 *
 * @author Datraverse B.V.
 *
 */
@JsonInclude(Include.NON_NULL)
public final class IppMediaSourceMappingDto extends AbstractDto {

    /**
     * The IPP media-source.
     *
     */
    @JsonProperty("source")
    private String source;

    /**
     * The IPP media assigned to the media-source. E.g. "iso_a4_210x297mm".
     */
    @JsonProperty("media")
    private String media;

    /**
     *
     * @return The IPP media-source.
     */
    public String getSource() {
        return source;
    }

    /**
     *
     * @param source
     *            The IPP media-source.
     */
    public void setSource(final String source) {
        this.source = source;
    }

    /**
     * Gets the IPP media assigned to the media-source.
     *
     * @return The IPP media string: e.g. "iso_a4_210x297mm".
     */
    public String getMedia() {
        return media;
    }

    /**
     * Sets the IPP media assigned to the media-source.
     *
     * @param media
     *            The IPP media string: e.g. "iso_a4_210x297mm".
     */
    public void setMedia(final String media) {
        this.media = media;
    }

}
