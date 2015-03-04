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

import java.io.IOException;

import org.savapage.core.SpException;
import org.savapage.core.json.JsonAbstractBase;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base class for all JSON-RPC Responses.
 *
 * @author Datraverse B.V.
 *
 */
public class JsonRpcResponseParser extends JsonAbstractBase {

    private JsonNode rootNode;
    private JsonNode resultNode;
    private JsonNode errorNode;

    public boolean isErrorResponse() {
        return errorNode != null;
    }

    public boolean isResultResponse() {
        return resultNode != null;
    }

    public JsonRpcResponseParser(final String jsonInput) {

        final JsonParser jp;
        try {
            jp = getMapper().getFactory().createJsonParser(jsonInput);
            rootNode = getMapper().readTree(jp);
            resultNode = rootNode.get("result");
            errorNode = rootNode.get("error");
        } catch (IOException e) {
            throw new SpException(e);
        }
    }

    /**
     * Deserializes to result.
     *
     * @return
     * @throws JsonProcessingException
     */
    public JsonRpcResult getResult() throws JsonProcessingException {
        return getMapper().treeToValue(resultNode, JsonRpcResult.class);
    }

    /**
     * Deserializes to error.
     *
     * @return
     * @throws JsonProcessingException
     */
    public JsonRpcError getError() throws JsonProcessingException {
        return getMapper().treeToValue(errorNode, JsonRpcError.class);
    }

}
