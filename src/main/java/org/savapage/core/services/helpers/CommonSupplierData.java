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

import java.io.IOException;

import org.savapage.core.SpException;
import org.savapage.core.json.JsonAbstractBase;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
public class CommonSupplierData extends JsonAbstractBase
        implements ExternalSupplierData {

    /**
     * The transaction weight total.
     */
    private Integer weightTotal;

    @Override
    public final String dataAsString() {
        try {
            return this.stringify();
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
    }

    /**
     * @return The transaction weight total.
     */
    public Integer getWeightTotal() {
        return weightTotal;
    }

    /**
     * @param weightTotal
     *            The transaction weight total.
     */
    public void setWeightTotal(final Integer weightTotal) {
        this.weightTotal = weightTotal;
    }

    /**
     * Creates an object from data string.
     *
     * @param data
     *            The serialized data.
     * @return The {@link CommonSupplierData} object.
     */
    public static CommonSupplierData createFromData(final String data) {
        return CommonSupplierData.create(CommonSupplierData.class, data);
    }
}
