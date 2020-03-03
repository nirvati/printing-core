/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: 2011-2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.cli.server;

import org.apache.commons.cli.CommandLine;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodParms;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcMethodName;
import org.savapage.core.json.rpc.JsonRpcResult;
import org.savapage.core.json.rpc.impl.ParamsNameValue;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class CliSetConfigProperty extends AbstractAppApi {

    /**
     *
     */
    private static final String API_VERSION = "0.10";

    /** */
    private static final String METHOD_SHORT_DESCRIPT =
            "Sets configuration property value.";

    /** */
    private static final String METHOD_LONG_DESCRIPT =
            "The property must be present in the database. "
                    + "Not all properties are " + "available for update.\n"
                    + SENTENCE_ADV_COMMAND;

    /** */
    private static final String CLI_OPT_NAME = "name";

    /** */
    private static final String CLI_OPT_VALUE = "value";

    /**
     *
     */
    private static Object[][] theOptions = new Object[][] {
            //
            { ARG_TEXT + "(100)", CLI_OPT_NAME, "Property name.",
                    Boolean.TRUE },
            //
            { ARG_TEXT + "(1000)", CLI_OPT_VALUE, "Property value.",
                    Boolean.TRUE }
            //
    };

    @Override
    protected final String getApiVersion() {
        return API_VERSION;
    }

    @Override
    protected final void onInit() throws Exception {
    }

    @Override
    protected final String getMethodName() {
        return JsonRpcMethodName.SET_CONFIG_PROPERTY.getMethodName();
    }

    @Override
    protected final Object[][] getOptionDictionary() {
        return theOptions;
    }

    @Override
    protected final String getShortDecription() {
        return METHOD_SHORT_DESCRIPT;
    }

    @Override
    protected String getLongDecription() {
        return METHOD_LONG_DESCRIPT;
    }

    @Override
    protected final boolean hasBatchOptions() {
        return true;
    }

    @Override
    protected final boolean hasLocaleOption() {
        return false;
    }

    @Override
    protected final boolean isValidCliInput(final CommandLine cmd) {
        return cmd.hasOption(CLI_OPT_NAME) && cmd.hasOption(CLI_OPT_VALUE);
    }

    @Override
    protected final AbstractJsonRpcMethodParms
            createMethodParms(final CommandLine cmd) {

        final ParamsNameValue parms = new ParamsNameValue();

        parms.setName(cmd.getOptionValue(CLI_OPT_NAME));
        parms.setValue(cmd.getOptionValue(CLI_OPT_VALUE));

        return parms;
    }

    @Override
    protected final void onErrorResponse(final JsonRpcError error) {

        final ErrorDataBasic data = error.data(ErrorDataBasic.class);

        getErrorDisplayStream().println(
                "Error [" + error.getCode() + "]: " + error.getMessage());

        if (data.getReason() != null) {
            getErrorDisplayStream().println("Reason: " + data.getReason());
        }
    }

    @Override
    protected final boolean onResultResponse(final JsonRpcResult result) {
        return false;
    }

    @Override
    protected final boolean isSwitchOption(final String optionName) {
        return false;
    }

}
