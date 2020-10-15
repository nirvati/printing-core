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
package org.savapage.core.template.dto;

import org.savapage.core.jpa.User;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobBaseDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class TemplateDtoCreator {

    /**
     * Just static methods.
     */
    private TemplateDtoCreator() {
    }

    /**
     * Creates instance.
     *
     * @param user
     *            The User.
     * @return The created instance.
     */
    public static TemplateUserDto templateUserDto(final User user) {

        final TemplateUserDto templateUser = new TemplateUserDto();

        templateUser.setFullName(user.getFullName());

        return templateUser;
    }

    /**
     * Creates instance.
     *
     * @param dto
     *            The {@link OutboxJobBaseDto}.
     * @param operator
     *            The name of the ticket operator.
     * @return The created instance.
     */
    public static TemplateJobTicketDto templateJobTicketDto(
            final OutboxJobBaseDto dto, final String operator) {

        final TemplateJobTicketDto templateDto = new TemplateJobTicketDto();

        templateDto.setNumber(dto.getTicketNumber());
        templateDto.setOperator(operator);
        templateDto.setName(dto.getJobName());

        return templateDto;
    }

}
