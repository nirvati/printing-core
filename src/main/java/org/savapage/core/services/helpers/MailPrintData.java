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
package org.savapage.core.services.helpers;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.json.JsonAbstractBase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * MailPrint data.
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ MailPrintData.JSON_FROM, MailPrintData.JSON_SUBJECT })
public final class MailPrintData extends JsonAbstractBase
        implements ExternalSupplierData {

    /** */
    public static final String JSON_FROM = "from";
    /** */
    public static final String JSON_SUBJECT = "subject";

    /**
     * Email address of sender.
     */
    @JsonProperty(JSON_FROM)
    private String fromAddress;

    /**
     * Message subject.
     */
    @JsonProperty(JSON_SUBJECT)
    private String subject;

    /**
     * @return Email address of sender.
     */
    public String getFromAddress() {
        return fromAddress;
    }

    /**
     * @param from
     *            Email address of sender.
     */
    public void setFromAddress(String from) {
        this.fromAddress = from;
    }

    /**
     * @return Message subject.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * @param subject
     *            Message subject.
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String dataAsString() {
        try {
            return this.stringify();
        } catch (IOException e) {
            throw new SpException(e.getMessage());
        }
    }

    /**
     * Creates an object from data string.
     *
     * @param json
     *            The serialized data.
     * @return The {@link MailPrintData} object or {@code null} if JSON is blank
     *         or de-serialization failed.
     */
    public static MailPrintData createFromData(final String json) {
        if (!StringUtils.isBlank(json)) {
            try {
                return MailPrintData.create(MailPrintData.class, json);
            } catch (Exception e) {
                // noop
            }
        }
        return null;
    }
}
