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
package org.savapage.core.json.rpc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 *
 * @author Datraverse B.V.
 *
 */
@JsonPropertyOrder({ "error" })
@JsonInclude(Include.NON_NULL)
public class JsonRpcMethodError extends AbstractJsonRpcMethodResponse {

    @Override
    @JsonIgnore
    public boolean isError() {
        return true;
    }

    @JsonProperty("error")
    private JsonRpcError error;

    public JsonRpcError getError() {
        return error;
    }

    public void setError(JsonRpcError error) {
        this.error = error;
    }

    @SuppressWarnings("unchecked")
    @JsonIgnore
    public <T extends JsonRpcError> T error(Class<T> jsonClass) {
        return (T) this.error;
    }

    /**
     * Creates a {@link JsonRpcMethodError} with {@link ErrorDataBasic} data.
     *
     * @param code
     *            The code.
     * @param message
     *            A single sentence message.
     * @param reason
     *            A more elaborate explanation. Can be {@code null}.
     * @return The JSON-RPC error message.
     */
    public static JsonRpcMethodError createBasicError(JsonRpcError.Code code,
            String message, String reason) {

        JsonRpcMethodError methodError = new JsonRpcMethodError();

        JsonRpcError error = new JsonRpcError();
        methodError.setError(error);

        ErrorDataBasic data = new ErrorDataBasic();
        error.setData(data);

        error.setCode(code.asInt());
        error.setMessage(message);

        data.setReason(reason);

        return methodError;
    }

}
