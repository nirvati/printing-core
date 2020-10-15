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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Base class for all JSON-RPC Errors.
 *
 * @author Rijk Ravestein
 *
 */
@JsonPropertyOrder({ "code", "message", "data" })
public class JsonRpcError {

    public static class JsonSubType {
        public static final String USER_ADD = "AddUser";
    }

    /**
     * See specification
     * <a href="http://www.jsonrpc.org/specification#error_object">here</a>.
     * <p>
     * Note: Range -32000 to -32099 is reserved for implementation-defined
     * server-errors.
     * </p>
     */
    public static enum Code {

        /**
         * Parse error: Invalid JSON was received by the server. An error
         * occurred on the server while parsing the JSON text.
         */
        PARSE_ERROR(-32700),

        /**
         * Invalid Request: The JSON sent is not a valid Request object.
         */
        INVALID_REQUEST(-32600),

        /**
         * Method not found: The method does not exist / is not available.
         */
        METHOD_NOT_FOUND(-32601),

        /**
         * Invalid params: Invalid method parameter(s).
         */
        INVALID_PARAMS(-32602),

        /**
         * Internal error: Internal JSON-RPC error.
         */
        INTERNAL_ERROR(-32603);

        /**
         *
         */
        private Integer code = 0;

        /**
         * Creates an enum value from an integer.
         *
         * @param value
         *            The integer.
         */
        Code(final int value) {
            this.code = value;
        }

        /**
         * Gets the integer representing this enum value.
         *
         * @return The integer.
         */
        public Integer asInt() {
            return this.code;
        }
    }

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    private String message;

    @JsonProperty("data")
    private JsonRpcErrorDataMixin data;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public JsonRpcErrorDataMixin getData() {
        return data;
    }

    public void setData(JsonRpcErrorDataMixin data) {
        this.data = data;
    }

    @SuppressWarnings("unchecked")
    @JsonIgnore
    public <T extends JsonRpcErrorDataMixin> T data(Class<T> jsonClass) {
        return (T) this.data;
    }

}
