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
package org.savapage.core.ipp.helpers;

import java.io.IOException;
import java.util.Map;

import org.savapage.core.SpException;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.services.helpers.ExternalSupplierData;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * IPP PrintIn data.
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "operation", "job" })
public final class IppPrintInData extends JsonAbstractBase
        implements ExternalSupplierData {

    /** */
    @JsonProperty("operation")
    private Map<String, String> attrOperation;

    /** */
    @JsonProperty("job")
    private Map<String, String> attrJob;

    /**
     * @return IPP Operation attribute keyword/value.
     */
    public Map<String, String> getAttrOperation() {
        return attrOperation;
    }

    /**
     * @param attr
     *            IPP Operation attribute keyword/value.
     */
    public void setAttrOperation(final Map<String, String> attr) {
        this.attrOperation = attr;
    }

    /**
     * @return IPP Job attribute keyword/value.
     */
    public Map<String, String> getAttrJob() {
        return attrJob;
    }

    /**
     * @param attr
     *            IPP Job attribute keyword/value.
     */
    public void setAttrJob(final Map<String, String> attr) {
        this.attrJob = attr;
    }

    /**
     * Creates an object from data string.
     *
     * @param data
     *            The serialized data.
     * @return The {@link IppPrintInData} object.
     */
    public static IppPrintInData createFromData(final String data) {
        return IppPrintInData.create(IppPrintInData.class, data);
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
