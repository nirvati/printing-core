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
package org.savapage.core.json.rpc;

import org.savapage.core.json.rpc.impl.ResultListQuickSearchItem;
import org.savapage.core.json.rpc.impl.ResultListStrings;
import org.savapage.core.json.rpc.impl.ResultListUsers;
import org.savapage.core.json.rpc.impl.ResultPosDeposit;
import org.savapage.core.json.rpc.impl.ResultPrinterSnmp;
import org.savapage.core.json.rpc.impl.ResultUserGroupAccess;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Base class for all JSON-RPC Error Data.
 *
 * <p>
 * See <a href="http://wiki.fasterxml.com/JacksonPolymorphicDeserialization">
 * Polymorphic Type Handling</a> at fastterxml.com, and <a href=
 * "http://programmerbruce.blogspot.nl/2011/05/deserialize-json-with-jackson-into.html"
 * >Deserialize JSON with Jackson into Polymorphic Types - A Complete
 * Example</a>
 * </p>
 *
 * @author Rijk Ravestein
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
        property = JsonRpcConfig.TYPE_INFO_PROPERTY)
@JsonSubTypes({
        @Type(value = ResultDataBasic.class,
                name = JsonRpcResultDataMixin.JsonSubType.BASIC),
        @Type(value = ResultEnum.class,
                name = JsonRpcResultDataMixin.JsonSubType.ENUM),
        @Type(value = ResultBoolean.class,
                name = JsonRpcResultDataMixin.JsonSubType.BOOL),
        @Type(value = ResultString.class,
                name = JsonRpcResultDataMixin.JsonSubType.STRING),
        @Type(value = ResultListUsers.class,
                name = JsonRpcResultDataMixin.JsonSubType.USER_LIST),
        @Type(value = ResultListStrings.class,
                name = JsonRpcResultDataMixin.JsonSubType.USER_GROUP_LIST),
        @Type(value = ResultListQuickSearchItem.class,
                name = JsonRpcResultDataMixin.JsonSubType.QUICK_SEARCH_ITEM_LIST),
        @Type(value = ResultPosDeposit.class,
                name = JsonRpcResultDataMixin.JsonSubType.POS_DEPOSIT),
        @Type(value = ResultUserGroupAccess.class,
                name = JsonRpcResultDataMixin.JsonSubType.USER_GROUP_ACCESS),
        @Type(value = ResultPrinterSnmp.class,
                name = JsonRpcResultDataMixin.JsonSubType.PRINTER_SNMP)
        //
})
public class JsonRpcResultDataMixin {

    public static class JsonSubType {

        public static final String BASIC = "BASIC";
        public static final String ENUM = "enum";
        public static final String USER_LIST = "USER_LIST";
        public static final String USER_GROUP_LIST = "USER_GROUP_LIST";
        public static final String QUICK_SEARCH_ITEM_LIST =
                "QUICK_SEARCH_ITEM_LIST";
        public static final String POS_DEPOSIT = "POS_DEPOSIT";
        public static final String USER_GROUP_ACCESS = "USER_GROUP_ACCESS";

        public static final String PRINTER_SNMP = "PRINTER_SNMP";

        public static final String BOOL = "boolean";
        public static final String STRING = "string";
    }

    protected JsonRpcResultDataMixin() {

    }
}
