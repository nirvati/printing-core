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

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Locale;

import org.savapage.core.util.BigDecimalUtil;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A container for one-sided and two-sided page {@link MediaPageCostDto}.
 *
 * @author Datraverse B.V.
 *
 */
@JsonInclude(Include.NON_NULL)
public final class MediaCostDto extends AbstractDto {
    /**
     * Cost per page for a single-sided print.
     */
    @JsonProperty("oneSided")
    private MediaPageCostDto costOneSided;

    /**
     * Cost per page for a double-sided print.
     */
    @JsonProperty("twoSided")
    private MediaPageCostDto costTwoSided;

    public MediaPageCostDto getCostOneSided() {
        return costOneSided;
    }

    public void setCostOneSided(MediaPageCostDto costOneSided) {
        this.costOneSided = costOneSided;
    }

    public MediaPageCostDto getCostTwoSided() {
        return costTwoSided;
    }

    public void setCostTwoSided(MediaPageCostDto costTwoSided) {
        this.costTwoSided = costTwoSided;
    }

    /**
     * Converts a {@link MediaCostDto} object to a new object with plain string
     * representation of BigDecimal cost.
     *
     * @param dto
     *            The {@link MediaCostDto} to convert.
     * @param locale
     *            The {@link Locale} of the input {@link MediaCostDto}.
     * @return A new {@link MediaCostDto}.
     * @throws ParseException
     *             When error parsing to {@link BigDecimal}.
     */
    public static MediaCostDto toDatabaseObject(final MediaCostDto dto,
            final Locale locale) throws ParseException {

        final MediaCostDto obj = new MediaCostDto();

        //
        MediaPageCostDto newCost;
        MediaPageCostDto oldCost;

        //
        newCost = new MediaPageCostDto();
        obj.setCostOneSided(newCost);

        oldCost = dto.getCostOneSided();

        newCost.setCostColor(BigDecimalUtil.toPlainString(
                oldCost.getCostColor(), locale, false, false));
        newCost.setCostGrayscale(BigDecimalUtil.toPlainString(
                oldCost.getCostGrayscale(), locale, false, false));

        //
        newCost = new MediaPageCostDto();
        obj.setCostTwoSided(newCost);

        oldCost = dto.getCostTwoSided();

        newCost.setCostColor(BigDecimalUtil.toPlainString(
                oldCost.getCostColor(), locale, false, false));
        newCost.setCostGrayscale(BigDecimalUtil.toPlainString(
                oldCost.getCostGrayscale(), locale, false, false));

        //
        return obj;
    }

    /**
     * Creates a JSON string with plain string representation of BigDecimal
     * cost.
     *
     * @param locale
     *            The {@code Locale} of the cost strings.
     * @return The JSON string.
     * @throws IOException
     *             When something goes wrong with JSON processing.
     * @throws ParseException
     *             When something goes wrong with BigDecimal parsing.
     */
    public String stringify(final Locale locale) throws IOException,
            ParseException {
        return toDatabaseObject(this, locale).stringify();
    }

}
