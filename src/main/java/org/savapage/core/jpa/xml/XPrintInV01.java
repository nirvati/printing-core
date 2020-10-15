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
package org.savapage.core.jpa.xml;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.savapage.core.jpa.schema.PrintInV01;

/**
 * IPP Print Input Document.
 *
 * @author Rijk Ravestein
 *
 */
@Entity
@Table(name = PrintInV01.TABLE_NAME)
public class XPrintInV01 extends XEntityVersion {

    @Id
    @Column(name = "print_in_id")
    private Long id;

    @Column(name = "queue_id", nullable = false)
    private Long queue;

    @Column(name = "paper_size", length = 20, nullable = true,
            insertable = true, updatable = true)
    private String paperSize;

    @Column(name = "paper_height_mm", nullable = true, insertable = true,
            updatable = true)
    private Integer paperHeight;

    @Column(name = "paper_width_mm", nullable = true, insertable = true,
            updatable = true)
    private Integer paperWidth;

    @Column(name = "printed", nullable = false, insertable = true,
            updatable = true)
    private Boolean printed;

    @Column(name = "denied_reason", length = 30, nullable = true,
            insertable = true, updatable = true)
    private String deniedReason;

    @Override
    public final String xmlName() {
        return "PrintIn";
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPaperSize() {
        return paperSize;
    }

    public void setPaperSize(String paperSize) {
        this.paperSize = paperSize;
    }

    public Integer getPaperHeight() {
        return paperHeight;
    }

    public void setPaperHeight(Integer paperHeight) {
        this.paperHeight = paperHeight;
    }

    public Integer getPaperWidth() {
        return paperWidth;
    }

    public void setPaperWidth(Integer paperWidth) {
        this.paperWidth = paperWidth;
    }

    public Boolean getPrinted() {
        return printed;
    }

    public void setPrinted(Boolean printed) {
        this.printed = printed;
    }

    public String getDeniedReason() {
        return deniedReason;
    }

    public void setDeniedReason(String deniedReason) {
        this.deniedReason = deniedReason;
    }

    public Long getQueue() {
        return queue;
    }

    public void setQueue(Long queue) {
        this.queue = queue;
    }

}
