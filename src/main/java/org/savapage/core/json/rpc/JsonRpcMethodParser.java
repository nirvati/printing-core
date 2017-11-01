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

import java.io.IOException;

import org.savapage.core.json.JsonAbstractBase;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Base class for all JSON-RPC Responses.
 *
 * @author Rijk Ravestein
 *
 */
public class JsonRpcMethodParser extends JsonAbstractBase {

    private final JsonNode rootNode;
    private final JsonNode paramsNode;

    private String jsonrpc;
    private String id;
    private String apiKey;
    private String apiVersion;

    public String getMethod() {
        return rootNode.get(JsonRpcMethod.ATTR_METHOD).textValue();
    }

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public void setApiVersion(String apiVersion) {
        this.apiVersion = apiVersion;
    }

    public boolean hasParams() {
        return this.paramsNode != null;
    }

    /**
     *
     * @param jsonInput
     * @throws IOException
     */
    public JsonRpcMethodParser(final String jsonInput) throws IOException {

        final JsonParser jp =
                getMapper().getFactory().createJsonParser(jsonInput);

        this.rootNode = getMapper().readTree(jp);
        this.id = rootNode.get(AbstractJsonRpcMessage.ATTR_ID).textValue();
        this.jsonrpc =
                rootNode.get(AbstractJsonRpcMessage.ATTR_JSONRPC).textValue();

        this.paramsNode = rootNode.get(JsonRpcMethod.ATTR_PARAMS);

        if (this.paramsNode != null) {
            JsonNode node = this.paramsNode
                    .get(AbstractJsonRpcMethodParms.ATTR_API_KEY);
            if (node != null) {
                this.apiKey = node.textValue();
            }
            this.paramsNode.get(AbstractJsonRpcMethodParms.ATTR_API_VERSION);
            if (node != null) {
                this.apiVersion = node.textValue();
            }
        }
    }

    /**
     * Deserializes to params.
     *
     * @param clazz
     * @return
     * @throws JsonProcessingException
     *             When error processing JSON.
     * @throws JsonRpcParserException
     *             When error parsing JSON.
     */
    public <T extends AbstractJsonRpcMethodParms> T getParams(Class<T> clazz)
            throws JsonProcessingException, JsonRpcParserException {
        try {
            return getMapper().treeToValue(paramsNode, clazz);
        } catch (JsonProcessingException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonRpcParserException(e.getMessage(), e);
        }
    }

}
