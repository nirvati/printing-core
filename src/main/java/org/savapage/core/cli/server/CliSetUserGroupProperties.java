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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.core.dto.UserAccountingDto.CreditLimitEnum;
import org.savapage.core.dto.UserGroupPropertiesDto;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodParms;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcMethodName;
import org.savapage.core.json.rpc.JsonRpcResult;
import org.savapage.core.json.rpc.impl.ParamsSetUserGroupProperties;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class CliSetUserGroupProperties extends AbstractAppApi {

    /**
     *
     */
    private static final String API_VERSION = "0.11";

    /**
     *
     */
    private static final String METHOD_SHORT_DESCRIPT =
            "Sets properties of an Internal or External User Group.";

    /**
     * .
     */
    private static final String CLI_OPT_GROUPNAME = "groupname";

    /**
     * Accounting.
     */
    private static final String CLI_OPT_BALANCE = "balance";

    /**
     * Accounting.
     */
    private static final String CLI_OPT_CREDIT_LIMIT_AMOUNT =
            "credit-limit-amount";

    /**
     * Accounting.
     */
    private static final String CLI_SWITCH_CREDIT_LIMIT = "credit-limit";

    /**
     * Accounting.
     */
    private static final String CLI_SWITCH_CREDIT_LIMIT_NONE =
            "credit-limit-none";

    /**
     * Role.
     */
    private static final String CLI_OPT_ROLE_JOB_TICKET_CREATOR =
            "role-job-ticket-creator";

    private static final String CLI_OPT_ROLE_JOB_TICKET_OPERATOR =
            "role-job-ticket-operator";

    private static final String CLI_OPT_ROLE_WEB_CASHIER = "role-web-cashier";

    private static final String CLI_OPT_ROLE_PRINT_CREATOR =
            "role-print-creator";

    private static final String CLI_OPT_ROLE_PRINT_DELEGATOR =
            "role-print-delegator";

    private static final String CLI_OPT_ROLE_PRINT_DELEGATE =
            "role-print-delegate";

    /**
     * Mapping CLI role option to {@link ACLRoleEnum}.
     */
    private static final String[][] OPT_ROLE_MAP = new String[][] {
            { CLI_OPT_ROLE_JOB_TICKET_CREATOR,
                    ACLRoleEnum.JOB_TICKET_CREATOR.toString() },
            { CLI_OPT_ROLE_JOB_TICKET_OPERATOR,
                    ACLRoleEnum.JOB_TICKET_OPERATOR.toString() },
            { CLI_OPT_ROLE_PRINT_CREATOR,
                    ACLRoleEnum.PRINT_CREATOR.toString() },
            { CLI_OPT_ROLE_PRINT_DELEGATE,
                    ACLRoleEnum.PRINT_DELEGATE.toString() },
            { CLI_OPT_ROLE_PRINT_DELEGATOR,
                    ACLRoleEnum.PRINT_DELEGATOR.toString() },
            { CLI_OPT_ROLE_WEB_CASHIER, ACLRoleEnum.WEB_CASHIER.toString() } };

    /**
     *
     */
    private static Object[][] theOptions = new Object[][] {

            /*
             * Regular options.
             */
            { ARG_TEXT + "(255)", CLI_OPT_GROUPNAME, "Unique group name.",
                    Boolean.TRUE },

            /*
             * Accounting.
             */
            { ARG_DECIMAL, CLI_OPT_BALANCE,
                    "The user's initial account balance." },

            { null, CLI_SWITCH_CREDIT_LIMIT,
                    "Assign default credit limit amount to new users." },

            { ARG_DECIMAL, CLI_OPT_CREDIT_LIMIT_AMOUNT,
                    "Assign custom credit limit amount to new users." },

            { null, CLI_SWITCH_CREDIT_LIMIT_NONE,
                    "Assign no credit limit restriction to new users "
                            + "(opposed to --" + CLI_SWITCH_CREDIT_LIMIT
                            + " and --" + CLI_OPT_CREDIT_LIMIT_AMOUNT + ")." },

            { ARG_ROLE, CLI_OPT_ROLE_JOB_TICKET_CREATOR,
                    "Assign Job Ticket Creator role." },
            { ARG_ROLE, CLI_OPT_ROLE_JOB_TICKET_OPERATOR,
                    "Assign Job Ticket Operator role." },
            { ARG_ROLE, CLI_OPT_ROLE_PRINT_CREATOR,
                    "Assign Print Creator role." },
            { ARG_ROLE, CLI_OPT_ROLE_PRINT_DELEGATE,
                    "Assign Print Delegate role." },
            { ARG_ROLE, CLI_OPT_ROLE_PRINT_DELEGATOR,
                    "Assign Print Delegator role." },
            { ARG_ROLE, CLI_OPT_ROLE_WEB_CASHIER, "Assign Web Cashier role." },

            //
    };

    @Override
    protected final String getApiVersion() {
        return API_VERSION;
    }

    @Override
    protected final String getMethodName() {
        return JsonRpcMethodName.SET_USER_GROUP_PROPERTIES.getMethodName();
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
        return true;
    }

    @Override
    protected final boolean hasLocaleOption() {
        return true;
    }

    @Override
    protected final boolean isValidCliInput(final CommandLine cmd) {
        /*
         * INVARIANT: User name is required.
         */
        if (!cmd.hasOption(CLI_OPT_GROUPNAME)) {
            return false;
        }

        /*
         * INVARIANT (Accounting): Credit limit option or switch can NOT be
         * combined with remove.
         */
        if ((cmd.hasOption(CLI_SWITCH_CREDIT_LIMIT)
                || cmd.hasOption(CLI_OPT_CREDIT_LIMIT_AMOUNT))
                && cmd.hasOption(CLI_SWITCH_CREDIT_LIMIT_NONE)) {
            return false;
        }

        /*
         * INVARIANT (Accounting): Credit limit option can NOT be combined with
         * switch.
         */
        if (cmd.hasOption(CLI_SWITCH_CREDIT_LIMIT)
                && cmd.hasOption(CLI_OPT_CREDIT_LIMIT_AMOUNT)) {
            return false;
        }

        /**
         *
         */
        for (final String[] pair : OPT_ROLE_MAP) {
            final String opt = pair[0];

            if (cmd.hasOption(opt)
                    && !isArgRoleValid(cmd.getOptionValue(opt))) {
                return false;
            }
        }

        //
        return true;
    }

    @Override
    protected final AbstractJsonRpcMethodParms
            createMethodParms(final CommandLine cmd) {

        ParamsSetUserGroupProperties parms = new ParamsSetUserGroupProperties();

        UserGroupPropertiesDto dto = new UserGroupPropertiesDto();
        parms.setUserGroupProperties(dto);

        dto.setGroupName(cmd.getOptionValue(CLI_OPT_GROUPNAME));

        /*
         * Accounting.
         */
        UserAccountingDto dtoAccounting = new UserAccountingDto();

        String value;

        value = cmd.getOptionValue(CLI_OPT_BALANCE);
        if (value != null) {
            dtoAccounting.setBalance(value);
            dtoAccounting.setKeepBalance(Boolean.FALSE);
            dto.setAccounting(dtoAccounting);
        }

        value = cmd.getOptionValue(CLI_OPT_CREDIT_LIMIT_AMOUNT);
        if (value != null) {
            dtoAccounting.setCreditLimitAmount(value);
            dtoAccounting.setCreditLimit(CreditLimitEnum.INDIVIDUAL);
            dto.setAccounting(dtoAccounting);
        }

        if (this.getSwitchValue(cmd, CLI_SWITCH_CREDIT_LIMIT)) {
            dtoAccounting.setCreditLimit(CreditLimitEnum.DEFAULT);
            dto.setAccounting(dtoAccounting);
        }

        if (this.getSwitchValue(cmd, CLI_SWITCH_CREDIT_LIMIT_NONE)) {
            dtoAccounting.setCreditLimit(CreditLimitEnum.NONE);
            dto.setAccounting(dtoAccounting);
        }

        if (dto.getAccounting() != null) {
            dto.getAccounting().setLocale(getLocaleOption(cmd));
        }

        /*
         * Roles.
         */
        final Map<ACLRoleEnum, Boolean> roleUpdate = new HashMap<>();

        for (final String[] pair : OPT_ROLE_MAP) {
            value = cmd.getOptionValue(pair[0]);
            if (value != null) {
                roleUpdate.put(ACLRoleEnum.valueOf(pair[1]),
                        getArgRoleValue(value));
                dto.setRoleUpdate(roleUpdate);
            }
        }

        //
        return parms;
    }

    @Override
    protected final boolean isSwitchOption(final String optionName) {

        final boolean isSwitch;

        switch (optionName) {
        case CLI_SWITCH_CREDIT_LIMIT:
        case CLI_SWITCH_CREDIT_LIMIT_NONE:
        case CLI_SWITCH_HELP:
        case CLI_SWITCH_HELP_LONG:
            isSwitch = true;
            break;
        default:
            isSwitch = false;
        }
        return isSwitch;
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
    protected void onInit() throws Exception {
        // no code intended
    }

}
