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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Test;
import org.savapage.core.dto.UserDto;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcConfig;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcErrorDataMixin;
import org.savapage.core.json.rpc.JsonRpcMethod;
import org.savapage.core.json.rpc.JsonRpcMethodName;
import org.savapage.core.json.rpc.JsonRpcMethodParser;
import org.savapage.core.json.rpc.JsonRpcResponseParser;
import org.savapage.core.json.rpc.JsonRpcResult;
import org.savapage.core.json.rpc.JsonRpcResultDataMixin;
import org.savapage.core.json.rpc.ResultDataBasic;
import org.savapage.core.json.rpc.impl.ParamsAddInternalUser;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class JsonRpcTest {

    @Test
    public void testMethodToJson() throws Exception {

        final String userName = "jscott";
        final String password = "etrs776";

        ParamsAddInternalUser user = new ParamsAddInternalUser();
        user.setApiKey("some-api-key");
        UserDto userDto = new UserDto();
        user.setUser(userDto);

        userDto.setUserName(userName);
        userDto.setPassword(password);

        JsonRpcMethod request = new JsonRpcMethod();
        request.setMethod(JsonRpcMethodName.ADD_INTERNAL_USER.getMethodName());
        request.setId(String.valueOf(new Date().getTime()));
        request.setParams(user);

        // System.out.println(request.stringifyPrettyPrinted());

    }

    @Test
    public void testJsonToMethod02() throws Exception {

        final String userName = "jscott";
        final String password = "etrs776";

        final String jsonInput =
                "{" + "\"jsonrpc\":\"" + JsonRpcConfig.RPC_VERSION + "\","
                        + "\"id\":\"1\"," + "\"method\":\""
                        + JsonRpcMethodName.ADD_INTERNAL_USER.getMethodName()
                        + "\"," + "\"params\":{"
                        + "\"apiKey\":\"my-secret-api-key\","
                        + "\"apiVersion\":\"1.0\", "
                        //
                        + "\"user\":{"
                        //
                        + "\"userName\":\"" + userName + "\",\"password\":\""
                        + password + "\"}}}";

        JsonRpcMethodParser method = new JsonRpcMethodParser(jsonInput);
        ParamsAddInternalUser params =
                method.getParams(ParamsAddInternalUser.class);

        assertEquals("Check userName", userName, params.getUser().getUserName());
        assertEquals("Check password", password, params.getUser().getPassword());
    }

    @Test
    public void testRpcResult() throws Exception {

        final long code = 0;

        final String jsonInput =
                "{\"jsonrpc\":\"" + JsonRpcConfig.RPC_VERSION
                        + "\",\"id\":\"1\",\"result\":{\"data\":{"
                        +
                        //
                        "\"" + JsonRpcConfig.TYPE_INFO_PROPERTY + "\": \""
                        + JsonRpcResultDataMixin.JsonSubType.BASIC + "\""
                        //
                        + ",\"code\":" + code + "}}}";

        JsonRpcResponseParser parser = new JsonRpcResponseParser(jsonInput);

        if (parser.isResultResponse()) {
            JsonRpcResult result = parser.getResult();
            ResultDataBasic data = result.data(ResultDataBasic.class);
            assertTrue(data.getCode().longValue() == code);
        } else {
            fail("no 'result' node found.");
        }
    }

    @Test
    public void testRpcError() throws Exception {

        final String reason = "some reason";
        final long code = 39000;

        final String jsonInput =
                "{\"jsonrpc\":\"" + JsonRpcConfig.RPC_VERSION
                        + "\",\"id\":\"1\",\"error\":{\"code\": " + code + ", "
                        + "\"message\": \"oops\"," + "\"data\":{"
                        +
                        //
                        "\"" + JsonRpcConfig.TYPE_INFO_PROPERTY + "\": \""
                        + JsonRpcErrorDataMixin.JsonSubType.BASIC + "\""
                        //
                        + ",\"reason\":\"" + reason + "\"" + "}}}";

        JsonRpcResponseParser parser = new JsonRpcResponseParser(jsonInput);

        if (parser.isErrorResponse()) {
            JsonRpcError error = parser.getError();
            ErrorDataBasic data = error.data(ErrorDataBasic.class);
            assertTrue(error.getCode().longValue() == code);
            assertTrue(data.getReason().equals(reason));
        } else {
            fail("no 'error' node found.");
        }
    }

    @Test
    public void testRpcMethodNameEnum() {

        assertTrue(JsonRpcMethodName.asEnum(JsonRpcMethodName.LIST_USERS
                .getMethodName()) == JsonRpcMethodName.LIST_USERS);

        assertFalse(JsonRpcMethodName.asEnum(JsonRpcMethodName.LIST_USERS
                .getMethodName()) == JsonRpcMethodName.ADD_INTERNAL_USER);

        assertNull(JsonRpcMethodName.asEnum(""));
    }

}
