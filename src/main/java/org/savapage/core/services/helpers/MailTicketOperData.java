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
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.jpa.User;
import org.savapage.core.json.JsonAbstractBase;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Mail Ticket Operator data.
 *
 * @author Rijk Ravestein
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ MailTicketOperData.OPERATOR })
public final class MailTicketOperData extends JsonAbstractBase
        implements ExternalSupplierData {

    /** */
    public static final String OPERATOR = "operator";

    /**
     * {@link User#getUserId()} of {@link ACLRoleEnum#MAIL_TICKET_OPERATOR}.
     */
    @JsonProperty(OPERATOR)
    private String operator;

    /**
     * @return {@link User#getUserId()} of
     *         {@link ACLRoleEnum#MAIL_TICKET_OPERATOR}.
     */
    public String getOperator() {
        return operator;
    }

    /**
     * @param operator
     *            {@link User#getUserId()} of
     *            {@link ACLRoleEnum#MAIL_TICKET_OPERATOR}.
     */
    public void setOperator(String operator) {
        this.operator = operator;
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
     * @return The {@link MailTicketOperData} object or {@code null} if JSON is
     *         blank or de-serialization failed.
     */
    public static MailTicketOperData createFromData(final String json) {
        if (!StringUtils.isBlank(json)) {
            try {
                return MailTicketOperData.create(MailTicketOperData.class,
                        json);
            } catch (Exception e) {
                // noop
            }
        }
        return null;
    }
}
