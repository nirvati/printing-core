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
import org.savapage.core.template.dto.TemplateMailTicketDto;
import org.savapage.core.template.dto.TemplateUserDto;
import org.savapage.core.template.email.EmailStationary;

/**
 * Template attributes as object identifiers.
 * <p>
 * <b>Warning</b>: Changing any enum value will invalidate all dependent XML
 * templates.
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
public enum TemplateAttrEnum {

    /**
     * Application.
     */
    APP("app", TemplateAppDto.class),

    /**
     * Email Content-ID.
     */
    CID("cid", null),

    /**
     * Admin Feed.
     */
    FEED_ADMIN("feed_admin", TemplateAdminFeedDto.class),

    /**
     * Email stationary.
     */
    STATIONARY("stationary", EmailStationary.class),

    /**
     * Job Ticket.
     */
    TICKET("ticket", TemplateJobTicketDto.class),

    /**
     * Mail Ticket.
     */
    MAIL_TICKET("mailTicket", TemplateMailTicketDto.class),

    /**
     * User.
     */
    USER("user", TemplateUserDto.class);

    /**
     * The attribute/object name used as object identifier in any dependent XML
     * template.
     */
    private final String attr;

    /**
     * Reserved for future use.
     */
    @SuppressWarnings("unused")
    private final Class<?> dtoClass;

    /**
     * @param <T>
     *            Class of type {@link TemplateDto}.
     * @param attrName
     *            Attribute name as used as object identifier in any dependent
     *            XML template.
     * @param clazz
     *            The class.
     */
    <T extends TemplateDto> TemplateAttrEnum(final String attrName,
            final Class<T> clazz) {
        this.attr = attrName;
        this.dtoClass = clazz;
    }

    /**
     * @return The attribute/object name as lower-case enum value. This name is
     *         used as object identifier in any dependent XML template.
     */
    public String asAttr() {
        return this.attr;
    }

}
