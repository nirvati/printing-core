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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.json.rpc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Rijk Ravestein
 *
 */
@JsonPropertyOrder({ "result" })
@JsonInclude(Include.NON_NULL)
public class JsonRpcMethodResult extends AbstractJsonRpcMethodResponse {

    @Override
    @JsonIgnore
    public boolean isError() {
        return false;
    }

    @JsonProperty("result")
    private JsonRpcResult result;

    public JsonRpcResult getResult() {
        return result;
    }

    public void setResult(JsonRpcResult result) {
        this.result = result;
    }

    /**
     * Creates a {@link JsonRpcMethodResult} with {@link ResultDataBasic} data.
     *
     * @return The JSON-RPC result message.
     */
    public static JsonRpcMethodResult createOkResult() {
        return createOkResult(null);
    }

    /**
     * Creates a {@link JsonRpcMethodResult} with {@link ResultDataBasic} data.
     *
     * @param message
     *            Can be {@code null}.
     * @return The JSON-RPC result message.
     */
    public static JsonRpcMethodResult createOkResult(String message) {
        ResultDataBasic data = new ResultDataBasic();
        data.setCode(0);
        data.setMessage(message);
        return createResult(data);
    }

    /**
     * Creates a {@link JsonRpcMethodResult} with {@link ResultEnum} data.
     *
     * @param enumObj
     *            An enum object.
     * @return The JSON-RPC result message.
     */
    public static JsonRpcMethodResult createEnumResult(final Enum<?> enumObj) {
        ResultEnum data = new ResultEnum();
        data.setId(enumObj.getClass().getSimpleName());
        data.setValue(enumObj.toString());
        return createResult(data);
    }

    /**
     * Creates a {@link JsonRpcMethodResult} with {@link JsonRpcResultDataMixin}
     * data.
     *
     * @param data
     * @return
     */
    public static JsonRpcMethodResult
            createResult(JsonRpcResultDataMixin data) {
        JsonRpcMethodResult methodResult = new JsonRpcMethodResult();
        JsonRpcResult result = new JsonRpcResult();
        result.setData(data);
        methodResult.setResult(result);
        return methodResult;
    }
}
