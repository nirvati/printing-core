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
package org.savapage.core.dto;

import java.io.IOException;
import java.text.ParseException;
import java.util.Locale;

import org.savapage.core.SpException;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper class for IPP media-source (tray).
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class IppMediaSourceCostDto extends AbstractDto {

    /**
     * Specifies whether the media source is activated.
     */
    @JsonProperty("active")
    private Boolean active;

    /**
     * Specifies whether the media source is preferred.
     */
    @JsonProperty("preferred")
    private Boolean preferred;

    /**
     * The IPP media-source (tray).
     *
     */
    @JsonProperty("source")
    private String source;

    /**
     *
     */
    @JsonProperty("display")
    private String display;

    /**
    *
    */
    @JsonProperty("media")
    private IppMediaCostDto media;

    /**
     *
     * @return
     */
    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getPreferred() {
        return preferred;
    }

    public void setPreferred(Boolean preferred) {
        this.preferred = preferred;
    }

    public IppMediaCostDto getMedia() {
        return media;
    }

    public void setMedia(IppMediaCostDto media) {
        this.media = media;
    }

    /**
     * Converts a this object to a database ready object with plain string
     * representation of BigDecimal media cost (if present).
     *
     * @param locale
     *            The {@link Locale} of the input {@link MediaCostDto}.
     */
    public final void toDatabaseObject(final Locale locale) {

        if (this.media != null) {
            try {
                this.media.setPageCost(MediaCostDto
                        .toDatabaseObject(this.media.getPageCost(), locale));
            } catch (ParseException e) {
                throw new SpException(e.getMessage());
            }
        }
    }

    /**
     * Creates an object from JSON string.
     *
     * @param json
     *            The JSON string.
     * @return The object.
     * @throws IOException
     *             When JSON parsing error.
     */
    public static IppMediaSourceCostDto create(final String json)
            throws IOException {
        return getMapper().readValue(json, IppMediaSourceCostDto.class);
    }

    /**
     *
     * @return {@code true} If media source is
     *         {@link IppKeyword#MEDIA_SOURCE_MANUAL}.
     */
    @JsonIgnore
    public boolean isManualSource() {
        return this.source.equals(IppKeyword.MEDIA_SOURCE_MANUAL);
    }

    /**
     *
     * @return {@code true} If media source is
     *         {@link IppKeyword#MEDIA_SOURCE_AUTO}.
     */
    @JsonIgnore
    public boolean isAutoSource() {
        return this.source.equals(IppKeyword.MEDIA_SOURCE_AUTO);
    }
}
