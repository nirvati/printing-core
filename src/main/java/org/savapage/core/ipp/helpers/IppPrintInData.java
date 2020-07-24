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
import org.savapage.core.ipp.attribute.IppDictJobDescAttr;
import org.savapage.core.ipp.attribute.IppDictOperationAttr;
import org.savapage.core.ipp.operation.IppOperationId;
import org.savapage.core.json.JsonAbstractBase;
import org.savapage.core.services.helpers.ExternalSupplierData;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@JsonPropertyOrder({ IppPrintInData.JSON_IPP_VERSION,
        IppPrintInData.JSON_PRINT_JOB, IppPrintInData.JSON_CREATE_JOB,
        IppPrintInData.JSON_SEND_DOCUMENT })
public final class IppPrintInData extends JsonAbstractBase
        implements ExternalSupplierData {

    /** */
    public static final String JSON_IPP_VERSION = "ipp-version";
    /** */
    public static final String JSON_PRINT_JOB = "print-job";
    /** */
    public static final String JSON_CREATE_JOB = "create-job";
    /** */
    public static final String JSON_SEND_DOCUMENT = "send-document";

    /** */
    @JsonProperty(JSON_IPP_VERSION)
    private String ippVersion;

    /** */
    @JsonProperty(JSON_PRINT_JOB)
    private Map<String, String> attrPrintJob;

    /** */
    @JsonProperty(IppPrintInData.JSON_CREATE_JOB)
    private Map<String, String> attrCreateJob;

    /** */
    @JsonProperty(IppPrintInData.JSON_SEND_DOCUMENT)
    private Map<String, String> attrSendDocument;

    /**
     * @return IPP version.
     */
    public String getIppVersion() {
        return ippVersion;
    }

    /**
     * @param version
     *            IPP version.
     */
    public void setIppVersion(final String version) {
        this.ippVersion = version;
    }

    /**
     * @return Selected {@link IppOperationId#PRINT_JOB} IPP attributes
     *         keyword/value.
     */
    public Map<String, String> getAttrPrintJob() {
        return attrPrintJob;
    }

    /**
     * @param attr
     *            Selected {@link IppOperationId#PRINT_JOB} IPP attributes
     *            keyword/value.
     */
    public void setAttrPrintJob(final Map<String, String> attr) {
        this.attrPrintJob = attr;
    }

    /**
     * @return Selected {@link IppOperationId#CREATE_JOB} IPP attributes
     *         keyword/value.
     */
    public Map<String, String> getAttrCreateJob() {
        return attrCreateJob;
    }

    /**
     * @param attr
     *            Selected {@link IppOperationId#CREATE_JOB} IPP attributes
     *            keyword/value.
     */
    public void setAttrCreateJob(final Map<String, String> attr) {
        this.attrCreateJob = attr;
    }

    /**
     * @return Selected {@link IppOperationId#SEND_DOC} IPP attributes
     *         keyword/value.
     */
    public Map<String, String> getAttrSendDocument() {
        return attrSendDocument;
    }

    /**
     * @param attr
     *            Selected {@link IppOperationId#SEND_DOC} IPP attributes
     *            keyword/value.
     */
    public void setAttrSendDocument(final Map<String, String> attr) {
        this.attrSendDocument = attr;
    }

    /**
     * @param map
     *            Key/value map
     * @param key
     *            Key
     * @return Value, or {@code null} if not found.
     */
    @JsonIgnore
    private static String findAttrValue(final Map<String, String> map,
            final String key) {
        if (map == null) {
            return null;
        }
        return map.get(key);
    }

    /**
     * @param ippKeyword
     *            IPP keyword.
     * @return {@code null} if not found.
     */
    @JsonIgnore
    private String getAttrValue(final String ippKeyword) {

        String value = findAttrValue(this.attrPrintJob, ippKeyword);
        if (value == null) {
            value = findAttrValue(this.attrCreateJob, ippKeyword);
            if (value == null) {
                value = findAttrValue(this.attrSendDocument, ippKeyword);
            }
        }
        return value;
    }

    /**
     * @return 'job-name' value, or {@code null} when not found.
     */
    @JsonIgnore
    public String getJobName() {
        return this.getAttrValue(IppDictJobDescAttr.ATTR_JOB_NAME);
    }

    /**
     * @return 'document-name' value, or {@code null} when not found.
     */
    @JsonIgnore
    public String getDocumentName() {
        return this.getAttrValue(IppDictOperationAttr.ATTR_DOCUMENT_NAME);
    }

    /**
     * @return 'document-format' value, or {@code null} when not found.
     */
    @JsonIgnore
    public String getDocumentFormat() {
        return this.getAttrValue(IppDictOperationAttr.ATTR_DOCUMENT_FORMAT);
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
