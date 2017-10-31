/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.json.rpc;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonPropertyOrder({ JsonRpcMethod.ATTR_METHOD, JsonRpcMethod.ATTR_PARAMS })
@JsonInclude(Include.NON_NULL)
public class JsonRpcMethod extends AbstractJsonRpcMessage {

    public static final String ATTR_METHOD = "method";
    public static final String ATTR_PARAMS = "params";


    @JsonProperty(JsonRpcMethod.ATTR_METHOD)
    private String method;

    @JsonProperty(JsonRpcMethod.ATTR_PARAMS)
    private AbstractJsonRpcMethodParms params;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public AbstractJsonRpcMethodParms getParams() {
        return params;
    }

    public void setParams(AbstractJsonRpcMethodParms params) {
        this.params = params;
    }

}
