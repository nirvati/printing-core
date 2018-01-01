/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.services.helpers;

import java.math.BigDecimal;

import org.savapage.core.dto.AbstractDto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class ProxyPrintCostDto extends AbstractDto {

    /**
     * The total cost for media of the proxy print job.
     */
    private BigDecimal costMedia;

    /**
     * The total cost for external operations per sheet.
     */
    private BigDecimal costSheet;

    /**
     * The total cost for external operations per copy.
     */
    private BigDecimal costCopy;

    /**
     * The cost for the set of copies.
     */
    private BigDecimal costSet;

    /**
     * Constructor. Sets all cost to {@link BigDecimal#ZERO}.
     */
    public ProxyPrintCostDto() {
        this.costMedia = BigDecimal.ZERO;
        this.costSheet = BigDecimal.ZERO;
        this.costCopy = BigDecimal.ZERO;
        this.costSet = BigDecimal.ZERO;
    }

    /**
     *
     * @return The total cost (media, external operations per copy and set
     *         costs) of the proxy print job.
     */
    @JsonIgnore
    public BigDecimal getCostTotal() {
        return costMedia.add(costSheet).add(costCopy).add(costSet);
    }

    /**
     *
     * @return The total cost for media of the proxy print job.
     */
    public BigDecimal getCostMedia() {
        return costMedia;
    }

    /**
     *
     * @param cost
     *            The total cost for media of the proxy print job.
     */
    public void setCostMedia(final BigDecimal cost) {
        this.costMedia = cost;
    }

    /**
     * @return The total cost for external operations per copy.
     */
    public BigDecimal getCostCopy() {
        return costCopy;
    }

    /**
     * @param cost
     *            The total cost for external operations per copy.
     */
    public void setCostCopy(final BigDecimal cost) {
        this.costCopy = cost;
    }

    /**
     * @return The total cost for external operations per sheet.
     */
    public BigDecimal getCostSheet() {
        return costSheet;
    }

    /**
     * @param cost
     *            The total cost for external operations per sheet.
     */
    public void setCostSheet(final BigDecimal cost) {
        this.costSheet = cost;
    }

    /**
     * @return The cost for the set of copies.
     */
    public BigDecimal getCostSet() {
        return costSet;
    }

    /**
     * @param cost
     *            The cost for the set of copies.
     */
    public void setCostSet(final BigDecimal cost) {
        this.costSet = cost;
    }

}
