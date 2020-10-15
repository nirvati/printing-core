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

import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ProxyPrinterMediaSourcesDto extends AbstractDto {

    /**
     * Primary key.
     */
    @JsonProperty("id")
    private Long id;

    /**
     * The locale (languageTag) of the cost strings (e.g. {@code en}) See
     * {@link Locale#toLanguageTag()}.
     */
    @JsonProperty("language")
    private String language;

    /**
     * The locale (country) of the cost strings (e.g. {@code US}) See
     * {@link Locale#getCountry()}.
     */
    @JsonProperty("country")
    private String country;

    /**
     * .
     */
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
     * .
     */
    private Boolean defaultMonochrome;

    /**
     * {@code true} if client-side monochrome conversion.
     */
    private Boolean clientSideMonochrome;

    /**
     * .
     */
    private Set<String> jobSheetsMediaSources;

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

    /**
     *
     * @return {@code true} if monochrome conversion is performed client-side
     *         (locally).
     */
    public Boolean getClientSideMonochrome() {
        return clientSideMonochrome;
    }

    /**
     *
     * @param clientSideMonochrome
     *            {@code true} if monochrome conversion is performed client-side
     *            (locally).
     */
    public void setClientSideMonochrome(Boolean clientSideMonochrome) {
        this.clientSideMonochrome = clientSideMonochrome;
    }

    public Set<String> getJobSheetsMediaSources() {
        return jobSheetsMediaSources;
    }

    public void setJobSheetsMediaSources(Set<String> jobSheetsMediaSources) {
        this.jobSheetsMediaSources = jobSheetsMediaSources;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

}
