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
package org.savapage.core.dto;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class QuickSearchMailTicketItemDto extends QuickSearchItemDto {

    private String email;
    private boolean docStore;
    private Integer printOutJobs;

    private String paperSize;
    private String pages;
    private String title;
    private String byteCount;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isDocStore() {
        return docStore;
    }

    public void setDocStore(boolean docStore) {
        this.docStore = docStore;
    }

    public Integer getPrintOutJobs() {
        return printOutJobs;
    }

    public void setPrintOutJobs(Integer printOutJobs) {
        this.printOutJobs = printOutJobs;
    }

    public String getPaperSize() {
        return paperSize;
    }

    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    public String getPages() {
        return pages;
    }

    public void setPages(String pages) {
        this.pages = pages;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getByteCount() {
        return byteCount;
    }

    public void setByteCount(String byteCount) {
        this.byteCount = byteCount;
    }

}
