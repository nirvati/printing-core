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
package org.savapage.core.cli.server;

import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.savapage.core.dto.QuickSearchItemDto;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodParms;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcMethodName;
import org.savapage.core.json.rpc.JsonRpcResult;
import org.savapage.core.json.rpc.impl.ParamsSingleFilterList;
import org.savapage.core.json.rpc.impl.ResultListQuickSearchItem;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class CliListUserGroupMembers extends AbstractAppApi {

    /**
     *
     */
    private static final String API_VERSION = "0.10";

    /**
     *
     */
    private static final String METHOD_SHORT_DESCRIPT =
            "Lists the names of the user group members in the system, "
                    + "sorted by user name, one per line.";

    /**
    *
    */
    private static final String CLI_OPT_GROUPNAME = "groupname";

    /**
     *
     */
    private static Object[][] theOptions =
            new Object[][] {
            //
            { ARG_TEXT + "(255)", CLI_OPT_GROUPNAME, "Unique group name.",
                    Boolean.TRUE }
            //
            };

    /**
    *
    */
    private static final int ITEMS_PER_PAGE = 1000;

    /**
    *
    */
    private int startIndex = 0;

    @Override
    protected final String getApiVersion() {
        return API_VERSION;
    }

    @Override
    protected final void onInit() throws Exception {
        this.startIndex = 0;
    }

    @Override
    protected final String getMethodName() {
        return JsonRpcMethodName.LIST_USER_GROUP_MEMBERS.getMethodName();
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
    protected final boolean hasBatchOptions() {
        return false;
    }

    @Override
    protected final boolean hasLocaleOption() {
        return false;
    }

    @Override
    protected final boolean isValidCliInput(final CommandLine cmd) {
        if (!cmd.hasOption(CLI_OPT_GROUPNAME)) {
            return false;
        }
        return true;
    }

    @Override
    protected final AbstractJsonRpcMethodParms createMethodParms(
            final CommandLine cmd) {

        final ParamsSingleFilterList parms =
                new ParamsSingleFilterList();

        parms.setFilter(cmd.getOptionValue(CLI_OPT_GROUPNAME));
        parms.setStartIndex(this.startIndex);
        parms.setItemsPerPage(ITEMS_PER_PAGE);

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

        final ResultListQuickSearchItem data =
                result.data(ResultListQuickSearchItem.class);

        final PrintStream displayStream = getDisplayStream();

        for (QuickSearchItemDto dto : data.getItems()) {
            displayStream.println(dto.getText());
        }

        this.startIndex += data.getItems().size();
        return data.getItems().size() == ITEMS_PER_PAGE;
    }

    @Override
    protected final boolean isSwitchOption(final String optionName) {
        return false;
    }

}
