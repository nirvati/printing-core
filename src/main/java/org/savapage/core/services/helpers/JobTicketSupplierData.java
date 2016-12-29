/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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

import java.io.IOException;
import java.math.BigDecimal;

import org.savapage.core.SpException;
import org.savapage.core.json.JsonAbstractBase;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public final class JobTicketSupplierData extends JsonAbstractBase
        implements ExternalSupplierData {

    /**
     * Media cost total of all printed media.
     */
    private BigDecimal costMedia;

    /**
     * Copy cost total of all printed copies.
     */
    private BigDecimal costCopy;

    /**
     *
     * @return Cost total of all printed media.
     */
    public BigDecimal getCostMedia() {
        return costMedia;
    }

    /**
     *
     * @param cost
     *            Cost total of all printed media.
     */
    public void setCostMedia(final BigDecimal cost) {
        this.costMedia = cost;
    }

    /**
     *
     * @return Copy cost total of all printed copies.
     */
    public BigDecimal getCostCopy() {
        return costCopy;
    }

    /**
     * @return Gets the sums of all costs.
     */
    @JsonIgnore
    public BigDecimal getCostTotal() {
        BigDecimal total = BigDecimal.ZERO;
        if (costCopy != null) {
            total = total.add(costCopy);
        }
        if (costMedia != null) {
            total = total.add(costMedia);
        }
        return total;
    }

    /**
     *
     * @param cost
     *            Copy cost total of all printed copies.
     */
    public void setCostCopy(final BigDecimal cost) {
        this.costCopy = cost;
    }

    @Override
    public String dataAsString() {
        try {
            return this.stringify();
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
    }

}
