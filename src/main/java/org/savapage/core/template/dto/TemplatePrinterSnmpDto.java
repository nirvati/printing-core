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

import java.util.Date;
import java.util.List;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class TemplatePrinterSnmpDto implements TemplateDto {

    /** */
    private List<String> names;

    /** */
    private String model;

    /** */
    private String serial;

    /** */
    private Date date;

    /** */
    private List<String> alerts;

    /** */
    private List<String> markerNames;

    /** */
    private List<Integer> markerPercs;

    public List<String> getNames() {
        return names;
    }

    public void setNames(List<String> names) {
        this.names = names;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<String> getAlerts() {
        return alerts;
    }

    public void setAlerts(List<String> alerts) {
        this.alerts = alerts;
    }

    public List<String> getMarkerNames() {
        return markerNames;
    }

    public void setMarkerNames(List<String> markerNames) {
        this.markerNames = markerNames;
    }

    public List<Integer> getMarkerPercs() {
        return markerPercs;
    }

    public void setMarkerPercs(List<Integer> markerPercs) {
        this.markerPercs = markerPercs;
    }

}
