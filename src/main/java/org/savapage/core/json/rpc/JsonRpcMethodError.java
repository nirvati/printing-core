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
@JsonPropertyOrder({ AbstractJsonRpcMessage.ATTR_JSONRPC,
        AbstractJsonRpcMessage.ATTR_ID, JsonRpcMethodResult.ATTR_RESULT,
        "error" })
@JsonInclude(Include.NON_NULL)
public final class JsonRpcMethodError extends AbstractJsonRpcMethodResponse {

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
    public <T extends JsonRpcError> T error(final Class<T> jsonClass) {
        return (T) this.error;
    }

    /**
     * Creates a {@link JsonRpcMethodError} with {@link ErrorDataBasic} data.
     *
     * @param code
     *            The code.
     * @param message
     *            The error message.
     * @return The JSON-RPC error message.
     */
    public static JsonRpcMethodError createBasicError(
            final JsonRpcError.Code code, final String message) {
        return createBasicError(code, message, "unknown");
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
    public static JsonRpcMethodError createBasicError(
            final JsonRpcError.Code code, final String message,
            final String reason) {

        final JsonRpcMethodError methodError = new JsonRpcMethodError();

        final JsonRpcError error = new JsonRpcError();
        methodError.setError(error);

        final ErrorDataBasic data = new ErrorDataBasic();
        error.setData(data);

        error.setCode(code.asInt());
        if (message == null) {
            error.setMessage(code.toString());
        } else {
            error.setMessage(message);
        }

        data.setReason(reason);

        return methodError;
    }

}
