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

import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Datraverse B.V.
 *
 */
@JsonInclude(Include.NON_NULL)
public class ProxyPrinterMediaSourcesDto extends AbstractDto {

    /**
     * primary key
     */
    @JsonProperty("id")
    private Long id;

    /**
     * The locale (languageTag) of the cost strings (e.g. {@code en-US}) See
     * {@link Locale#toLanguageTag()}.
     */
    @JsonProperty("locale")
    private String locale;

    @JsonProperty("sources")
    private List<IppMediaSourceCostDto> sources;

    /**
     *
     */
    @JsonProperty("auto")
    private IppMediaSourceCostDto sourceAuto;

    /**
     *
     */
    @JsonProperty("manual")
    private IppMediaSourceCostDto sourceManual;

    /**
    *
    */
    private Boolean defaultMonochrome;

    /**
     *
     * @return
     */
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets {@link #locale}.
     *
     * @return
     */
    public String getLocale() {
        return locale;
    }

    /**
     * Sets {@link #locale}.
     *
     * @param locale
     */
    public void setLocale(String locale) {
        this.locale = locale;
    }

    public List<IppMediaSourceCostDto> getSources() {
        return sources;
    }

    public void setSources(List<IppMediaSourceCostDto> sources) {
        this.sources = sources;
    }

    public IppMediaSourceCostDto getSourceAuto() {
        return sourceAuto;
    }

    public void setSourceAuto(IppMediaSourceCostDto sourceAuto) {
        this.sourceAuto = sourceAuto;
    }

    public IppMediaSourceCostDto getSourceManual() {
        return sourceManual;
    }

    public void setSourceManual(IppMediaSourceCostDto sourceManual) {
        this.sourceManual = sourceManual;
    }

    public Boolean getDefaultMonochrome() {
        return defaultMonochrome;
    }

    public void setDefaultMonochrome(Boolean defaultMonochrome) {
        this.defaultMonochrome = defaultMonochrome;
    }

}
