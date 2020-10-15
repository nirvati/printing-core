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

import org.savapage.core.jpa.Printer;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public class ProxyPrinterCostDto extends AbstractDto {

    /**
     * Primary key of {@link Printer}.
     */
    @JsonProperty("id")
    private Long id;

    /**
     *
     */
    @JsonProperty("chargeType")
    private Printer.ChargeType chargeType;

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
     * (FinAmount) formatted according to Locale.
     */
    @JsonProperty("defaultCost")
    private String defaultCost;

    @JsonProperty("mediaCost")
    private List<IppMediaCostDto> mediaCost;

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

    public Printer.ChargeType getChargeType() {
        return chargeType;
    }

    public void setChargeType(Printer.ChargeType chargeType) {
        this.chargeType = chargeType;
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

    public String getDefaultCost() {
        return defaultCost;
    }

    public void setDefaultCost(String defaultCost) {
        this.defaultCost = defaultCost;
    }

    public List<IppMediaCostDto> getMediaCost() {
        return mediaCost;
    }

    public void setMediaCost(List<IppMediaCostDto> mediaCost) {
        this.mediaCost = mediaCost;
    }

}
