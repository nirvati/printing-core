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
package org.savapage.core.print.proxy;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JsonProxyPrinterOptGroup {

    @JsonProperty("id")
    private ProxyPrinterOptGroupEnum groupId;

    @JsonProperty("uiText")
    private String uiText;

    @JsonProperty("options")
    private ArrayList<JsonProxyPrinterOpt> options;

    @JsonProperty("subgroups")
    private ArrayList<JsonProxyPrinterOptGroup> subgroups = new ArrayList<>();

    /**
     *
     * @return
     */
    public JsonProxyPrinterOptGroup copy() {

        final JsonProxyPrinterOptGroup copy = new JsonProxyPrinterOptGroup();

        copy.groupId = this.groupId;
        copy.uiText = this.uiText;
        copy.options = new ArrayList<>();

        for (final JsonProxyPrinterOpt opt : options) {
            copy.options.add(opt.copy());
        }

        for (final JsonProxyPrinterOptGroup subgroup : subgroups) {
            copy.subgroups.add(subgroup.copy());
        }

        return copy;
    }

    public ProxyPrinterOptGroupEnum getGroupId() {
        return groupId;
    }

    public void setGroupId(final ProxyPrinterOptGroupEnum id) {
        this.groupId = id;
    }

    public String getUiText() {
        return uiText;
    }

    public void setUiText(String text) {
        this.uiText = text;
    }

    public ArrayList<JsonProxyPrinterOpt> getOptions() {
        return options;
    }

    public void setOptions(ArrayList<JsonProxyPrinterOpt> options) {
        this.options = options;
    }

    public ArrayList<JsonProxyPrinterOptGroup> getSubgroups() {
        return subgroups;
    }

    public void setSubgroups(ArrayList<JsonProxyPrinterOptGroup> subgroups) {
        this.subgroups = subgroups;
    }

}
