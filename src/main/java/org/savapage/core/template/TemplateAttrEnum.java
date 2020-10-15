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
package org.savapage.core.template;

import org.savapage.core.template.dto.TemplateAdminFeedDto;
import org.savapage.core.template.dto.TemplateAppDto;
import org.savapage.core.template.dto.TemplateDto;
import org.savapage.core.template.dto.TemplateJobTicketDto;
import org.savapage.core.template.dto.TemplateUserDto;
import org.savapage.core.template.email.EmailStationary;

/**
 * Template attributes as object identifiers.
 *
 * @author Rijk Ravestein
 *
 */
public enum TemplateAttrEnum {

    /**
     * Application.
     */
    APP(TemplateAppDto.class),

    /**
     * Email Content-ID.
     */
    CID(null),

    /**
     * Admin Feed.
     */
    FEED_ADMIN(TemplateAdminFeedDto.class),

    /**
     * Email stationary.
     */
    STATIONARY(EmailStationary.class),

    /**
     * Job Ticket.
     */
    TICKET(TemplateJobTicketDto.class),

    /**
     * User.
     */
    USER(TemplateUserDto.class);

    /**
     * Reserved for future use.
     */
    private final Class<?> dtoClass;

    /**
     * @param <T>
     *            Class of type {@link TemplateDto}.
     * @param clazz
     *            The class.
     */
    <T extends TemplateDto> TemplateAttrEnum(final Class<T> clazz) {
        this.dtoClass = clazz;
    }

    /**
     * @return The attribute/object name as lower-case enum value.
     */
    public String asAttr() {
        return this.toString().toLowerCase();
    }

}
