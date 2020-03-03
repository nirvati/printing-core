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
import org.savapage.core.dao.enums.AccessControlScopeEnum;
import org.savapage.core.dao.helpers.JsonUserGroupAccess;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodParms;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcMethodName;
import org.savapage.core.json.rpc.JsonRpcResult;
import org.savapage.core.json.rpc.impl.ParamsPrinterAccessControl;
import org.savapage.core.json.rpc.impl.ResultUserGroupAccess;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class CliPrinterAccessControl extends AbstractAppApi {

    /**
     *
     */
    private static final String API_VERSION = "0.10";

    /**
     *
     */
    private static final String METHOD_SHORT_DESCRIPT =
            "Controls user groups to either allow or "
                    + "deny access to a proxy printer.";

    /**
     *
     */
    private static final String CLI_OPT_PRINTERNAME = "printername";

    /**
    *
    */
    private static final String CLI_OPT_GROUPNAME = "groupname";

    /**
     *
     */
    private static final String CLI_SWITCH_ALLOW = "allow";

    /**
     *
     */
    private static final String CLI_SWITCH_DENY = "deny";

    /**
     *
     */
    private static final String CLI_SWITCH_REMOVE = "remove";

    /**
     *
     */
    private static final String CLI_SWITCH_REMOVE_ALL = "remove-all";

    /**
     *
     */
    private static final String CLI_SWITCH_LIST = "list";

    /**
     *
     */
    private static Object[][] theOptions = new Object[][] {
            { ARG_TEXT + "(255)", CLI_OPT_PRINTERNAME,
                    "CUPS name of the proxy printer.", Boolean.TRUE },
            { null, CLI_SWITCH_ALLOW,
                    "Allow access to --" + CLI_OPT_GROUPNAME
                            + " (existing denied user groups are removed)." },
            { null, CLI_SWITCH_DENY,
                    "Deny access to --" + CLI_OPT_GROUPNAME
                            + " (existing allowed user groups are removed)." },
            { null, CLI_SWITCH_REMOVE,
                    "Remove --" + CLI_OPT_GROUPNAME
                            + " from the access list." },
            { ARG_TEXT + "(255)", CLI_OPT_GROUPNAME,
                    "Name of the user group to --" + CLI_SWITCH_ALLOW + ", --"
                            + CLI_SWITCH_DENY + " or --" + CLI_SWITCH_REMOVE
                            + " access",
                    Boolean.FALSE },
            { null, CLI_SWITCH_REMOVE_ALL,
                    "Remove all user groups from the access list." },
            { null, CLI_SWITCH_LIST,
                    "Echoes the access list to stdout in CSV format." },

    };

    @Override
    protected final String getApiVersion() {
        return API_VERSION;
    }

    @Override
    protected final String getMethodName() {
        return JsonRpcMethodName.PRINTER_ACCESS_CONTROL.getMethodName();
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
        /*
         * Printer name is required.
         */
        if (!cmd.hasOption(CLI_OPT_PRINTERNAME)) {
            return false;
        }

        /*
         * Mutual exclusive switches.
         */
        int count = 0;

        for (final String opt : new String[] { CLI_SWITCH_ALLOW,
                CLI_SWITCH_DENY, CLI_SWITCH_REMOVE, CLI_SWITCH_REMOVE_ALL,
                CLI_SWITCH_LIST }) {
            if (cmd.hasOption(opt)) {
                count++;
            }
        }

        if (count > 1) {
            return false;
        }

        /*
         * Other.
         */
        if (cmd.hasOption(CLI_SWITCH_ALLOW) || cmd.hasOption(CLI_SWITCH_DENY)
                || cmd.hasOption(CLI_SWITCH_REMOVE)) {
            /*
             * User group name is required for allow, deny and remove.
             */
            if (!cmd.hasOption(CLI_OPT_GROUPNAME)) {
                return false;
            }

        } else {
            /*
             * Obsolete user group.
             */
            if (cmd.hasOption(CLI_OPT_GROUPNAME)) {
                return false;
            }

            /*
             * remove-all or list must be specified.
             */
            if (!cmd.hasOption(CLI_SWITCH_REMOVE_ALL)
                    && !cmd.hasOption(CLI_SWITCH_LIST)) {
                return false;
            }
        }

        return true;
    }

    @Override
    protected final AbstractJsonRpcMethodParms
            createMethodParms(final CommandLine cmd) {

        final ParamsPrinterAccessControl parms =
                new ParamsPrinterAccessControl();

        parms.setPrinterName(cmd.getOptionValue(CLI_OPT_PRINTERNAME));
        parms.setGroupName(cmd.getOptionValue(CLI_OPT_GROUPNAME));

        ParamsPrinterAccessControl.Action action = null;

        if (cmd.hasOption(CLI_SWITCH_ALLOW)) {
            parms.setScope(AccessControlScopeEnum.ALLOW);
            action = ParamsPrinterAccessControl.Action.ADD;
        } else if (cmd.hasOption(CLI_SWITCH_DENY)) {
            parms.setScope(AccessControlScopeEnum.DENY);
            action = ParamsPrinterAccessControl.Action.ADD;
        } else if (cmd.hasOption(CLI_SWITCH_LIST)) {
            action = ParamsPrinterAccessControl.Action.LIST;
        } else if (cmd.hasOption(CLI_SWITCH_REMOVE)) {
            action = ParamsPrinterAccessControl.Action.REMOVE;
        } else if (cmd.hasOption(CLI_SWITCH_REMOVE_ALL)) {
            action = ParamsPrinterAccessControl.Action.REMOVE_ALL;
        }

        parms.setAction(action);

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

        if (this.getCommandLine().hasOption(CLI_SWITCH_LIST)) {

            final String printerName =
                    this.getCommandLine().getOptionValue(CLI_OPT_PRINTERNAME);

            final JsonUserGroupAccess userGroupAccess = result
                    .data(ResultUserGroupAccess.class).getUserGroupAccess();

            for (final String group : userGroupAccess.getGroups()) {
                getDisplayStream().println("\"" + printerName + "\",\""
                        + userGroupAccess.getScope() + "\",\"" + group + "\"");
            }
        }

        return false;
    }

    @Override
    protected final boolean isSwitchOption(final String optionName) {
        return false;
    }

    @Override
    protected void onInit() throws Exception {
        // no code intended
    }

}
