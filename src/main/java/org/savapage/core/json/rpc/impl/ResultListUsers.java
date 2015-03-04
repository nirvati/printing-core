/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
 * Author: Rijk Ravestein.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.json.rpc.impl;

import java.util.List;

import org.savapage.core.dto.UserDto;
import org.savapage.core.json.rpc.AbstractResultDataPaging;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Datraverse B.V.
 *
 */
@JsonPropertyOrder({ "items" })
public class ResultListUsers extends AbstractResultDataPaging {

    @JsonProperty("items")
    private List<UserDto> items;

    public List<UserDto> getItems() {
        return items;
    }

    public void setItems(List<UserDto> items) {
        this.items = items;
    }

}
