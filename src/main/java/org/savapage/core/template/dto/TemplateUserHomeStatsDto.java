/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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

import org.apache.commons.io.FileUtils;
import org.savapage.core.dto.UserHomeStatsDto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class TemplateUserHomeStatsDto implements TemplateDto {

    /** */
    private Long users;

    /**  */
    private Long inboxDocs;

    /**  */
    private String inboxBytes;

    /**  */
    private Long outboxDocs;

    /**  */
    private String outboxBytes;

    public Long getUsers() {
        return users;
    }

    public void setUsers(Long users) {
        this.users = users;
    }

    public Long getInboxDocs() {
        return inboxDocs;
    }

    public void setInboxDocs(Long inboxDocs) {
        this.inboxDocs = inboxDocs;
    }

    public String getInboxBytes() {
        return inboxBytes;
    }

    public void setInboxBytes(String inboxBytes) {
        this.inboxBytes = inboxBytes;
    }

    public Long getOutboxDocs() {
        return outboxDocs;
    }

    public void setOutboxDocs(Long outboxDocs) {
        this.outboxDocs = outboxDocs;
    }

    public String getOutboxBytes() {
        return outboxBytes;
    }

    public void setOutboxBytes(String outboxBytes) {
        this.outboxBytes = outboxBytes;
    }

    /**
     * Creates template from info.
     *
     * @param info
     *            {@link UserHomeStatsDto}.
     * @return
     */
    public static TemplateUserHomeStatsDto create(final UserHomeStatsDto info) {

        final TemplateUserHomeStatsDto dto = new TemplateUserHomeStatsDto();

        dto.users = info.getCurrent().getUsers().getCount();

        UserHomeStatsDto.Scope scope;

        scope = info.getCurrent().getInbox();
        if (scope.getCount() > 0) {
            dto.inboxDocs = scope.getCount();
            dto.inboxBytes = FileUtils.byteCountToDisplaySize(scope.getSize());
        }
        scope = info.getCurrent().getOutbox();
        if (scope.getCount() > 0) {
            dto.outboxDocs = scope.getCount();
            dto.outboxBytes = FileUtils.byteCountToDisplaySize(scope.getSize());
        }
        return dto;
    }
}
