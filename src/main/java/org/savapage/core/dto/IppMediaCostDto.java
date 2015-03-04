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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Wrapper class for costs of IPP media (name).
 *
 * @author Datraverse B.V.
 *
 */
@JsonInclude(Include.NON_NULL)
public final class IppMediaCostDto extends AbstractDto {

    public static final String DEFAULT_MEDIA = "default";

    /**
     * The IPP media name.
     * <p>
     * See the <a href=
     * "ftp://ftp.pwg.org/pub/pwg/candidates/cs-pwgmsn10-20020226-5101.1.pdf"
     * >PWG Standard for Media Standardized Names</a>.
     * <p>
     * Example: {@code iso_a4_210x297mm}
     * </p>
     */
    @JsonProperty("media")
    private String media;

    /**
     * Specifies whether costs are activated. If {@code false} the media is
     * present, but no costs are activated (for calculation costs are taken from
     * the default).
     */
    @JsonProperty("active")
    private Boolean active;

    @JsonProperty("cost")
    private MediaCostDto pageCost;

    @JsonIgnore
    public boolean isDefault() {
        return getMedia().equals(DEFAULT_MEDIA);
    }

    /**
     * Gets the IPP media name.
     *
     * @return E.g. "iso_a4_210x297mm"
     */
    public String getMedia() {
        return media;
    }

    /**
     * Sets the IPP media name.
     *
     * @param media
     *            E.g. "iso_a4_210x297mm"
     */
    public void setMedia(String media) {
        this.media = media;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public MediaCostDto getPageCost() {
        return pageCost;
    }

    public void setPageCost(MediaCostDto pageCost) {
        this.pageCost = pageCost;
    }

}
