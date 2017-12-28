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
package org.savapage.core.cli.server;

import org.apache.commons.cli.CommandLine;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodParms;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcMethodName;
import org.savapage.core.json.rpc.JsonRpcResult;
import org.savapage.core.json.rpc.ResultDataBasic;
import org.savapage.core.json.rpc.impl.ParamsSyncUsers;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class CliSyncUsersAndGroups extends AbstractAppApi {

    /** */
    private static final String API_VERSION = "0.10";

    /** */
    private static final String METHOD_SHORT_DESCRIPT =
            "Starts user and group synchronization "
                    + "with external user source.";

    /** */
    private static final String METHOD_LONG_DESCRIPT =
            "This is equivalent to clicking \"Synchronize now\" "
                    + "in the Admin Web App.\n"
                    + "Synchronization completes in the background.";

    /** */
    private static final String CLI_SWITCH_DELETE_USERS = "delete-users";

    /**
     *
     */
    private static Object[][] theOptions = new Object[][] {
            //
            { null, CLI_SWITCH_DELETE_USERS, "Remove users that do not exist "
                    + "in external user source." },
            //
    };

    @Override
    protected final String getApiVersion() {
        return API_VERSION;
    }

    @Override
    protected final String getMethodName() {
        return JsonRpcMethodName.SYNC_USERS_AND_GROUPS.getMethodName();
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
    protected final String getLongDecription() {
        return METHOD_LONG_DESCRIPT;
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
        return true;
    }

    @Override
    protected final AbstractJsonRpcMethodParms
            createMethodParms(final CommandLine cmd) {

        final ParamsSyncUsers parms = new ParamsSyncUsers();
        parms.setDeleteUsers(Boolean
                .valueOf(this.getSwitchValue(cmd, CLI_SWITCH_DELETE_USERS)));
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

        final ResultDataBasic data = result.data(ResultDataBasic.class);

        getDisplayStream().println(data.getMessage());

        return false;
    }

    @Override
    protected final boolean isSwitchOption(final String optionName) {

        switch (optionName) {
        case CLI_SWITCH_DELETE_USERS:
        case CLI_SWITCH_HELP:
        case CLI_SWITCH_HELP_LONG:
            return true;
        default:
            return false;
        }
    }

    @Override
    protected void onInit() throws Exception {
        // no code intended
    }

}
