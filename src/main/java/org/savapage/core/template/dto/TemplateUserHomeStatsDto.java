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

    /**  */
    private Long letterheadDocs;

    /**  */
    private String letterheadBytes;

    /**  */
    private Long pgpPubRingFiles;

    /**  */
    private String pgpPubRingBytes;

    /**  */
    private Long unknownFiles;

    /**  */
    private String unknownBytes;

    /** */
    private Boolean cleaned;

    /** */
    private Long cleanupFiles;

    /** */
    private String cleanupBytes;

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

    public Long getLetterheadDocs() {
        return letterheadDocs;
    }

    public void setLetterheadDocs(Long letterheadDocs) {
        this.letterheadDocs = letterheadDocs;
    }

    public String getLetterheadBytes() {
        return letterheadBytes;
    }

    public void setLetterheadBytes(String letterheadBytes) {
        this.letterheadBytes = letterheadBytes;
    }

    public Long getPgpPubRingFiles() {
        return pgpPubRingFiles;
    }

    public void setPgpPubRingFiles(Long pgpPubRingFiles) {
        this.pgpPubRingFiles = pgpPubRingFiles;
    }

    public String getPgpPubRingBytes() {
        return pgpPubRingBytes;
    }

    public void setPgpPubRingBytes(String pgpPubRingBytes) {
        this.pgpPubRingBytes = pgpPubRingBytes;
    }

    public Long getUnknownFiles() {
        return unknownFiles;
    }

    public void setUnknownFiles(Long unknownFiles) {
        this.unknownFiles = unknownFiles;
    }

    public String getUnknownBytes() {
        return unknownBytes;
    }

    public void setUnknownBytes(String unknownBytes) {
        this.unknownBytes = unknownBytes;
    }

    public Boolean getCleaned() {
        return cleaned;
    }

    public void setCleaned(Boolean cleaned) {
        this.cleaned = cleaned;
    }

    public Long getCleanupFiles() {
        return cleanupFiles;
    }

    public void setCleanupFiles(Long cleanupFiles) {
        this.cleanupFiles = cleanupFiles;
    }

    public String getCleanupBytes() {
        return cleanupBytes;
    }

    public void setCleanupBytes(String cleanupBytes) {
        this.cleanupBytes = cleanupBytes;
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
        if (scope != null && scope.getCount() > 0) {
            dto.inboxDocs = scope.getCount();
            dto.inboxBytes = FileUtils.byteCountToDisplaySize(scope.getSize());
        }
        scope = info.getCurrent().getOutbox();
        if (scope != null && scope.getCount() > 0) {
            dto.outboxDocs = scope.getCount();
            dto.outboxBytes = FileUtils.byteCountToDisplaySize(scope.getSize());
        }
        scope = info.getCurrent().getLetterheads();
        if (scope != null && scope.getCount() > 0) {
            dto.letterheadDocs = scope.getCount();
            dto.letterheadBytes =
                    FileUtils.byteCountToDisplaySize(scope.getSize());
        }
        scope = info.getCurrent().getPgpPubRing();
        if (scope != null && scope.getCount() > 0) {
            dto.pgpPubRingFiles = scope.getCount();
            dto.pgpPubRingBytes =
                    FileUtils.byteCountToDisplaySize(scope.getSize());
        }
        scope = info.getCurrent().getUnkown();
        if (scope != null && scope.getCount() > 0) {
            dto.unknownFiles = scope.getCount();
            dto.unknownBytes =
                    FileUtils.byteCountToDisplaySize(scope.getSize());
        }

        final UserHomeStatsDto.Stats cleanup = info.getCleanup();

        if (cleanup != null) {
            final long count = info.calcCleanupFiles();
            if (count > 0) {
                dto.cleanupFiles = count;
                dto.cleanupBytes = FileUtils
                        .byteCountToDisplaySize(info.calcCleanupBytes());
                if (info.isCleaned()) {
                    dto.cleaned = Boolean.TRUE;
                }
            }
        }

        return dto;
    }
}
