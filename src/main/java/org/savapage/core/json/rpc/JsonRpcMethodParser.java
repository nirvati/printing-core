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
public class JsonRpcMethodParser extends JsonAbstractBase {

    private final JsonNode rootNode;
    private final JsonNode paramsNode;

    private String jsonrpc;
    private String id;
    private String apiKey;
    private String apiVersion;

    public String getMethod() {
        return rootNode.get("method").textValue();
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
        this.id = rootNode.get("id").textValue();
        this.jsonrpc = rootNode.get("jsonrpc").textValue();

        this.paramsNode = rootNode.get("params");
        if (this.paramsNode != null) {
            JsonNode node = this.paramsNode.get("apiKey");
            if (node != null) {
                this.apiKey = node.textValue();
            }
            this.paramsNode.get("apiVersion");
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
     */
    public <T extends AbstractJsonRpcMethodParms> T getParams(Class<T> clazz)
            throws JsonProcessingException {
        return getMapper().treeToValue(paramsNode, clazz);
    }

}
