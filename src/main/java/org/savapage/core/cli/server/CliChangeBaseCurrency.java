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

import java.io.IOException;
import java.util.Currency;

import org.apache.commons.cli.CommandLine;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodParms;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcMethodName;
import org.savapage.core.json.rpc.JsonRpcResult;
import org.savapage.core.json.rpc.ResultDataBasic;
import org.savapage.core.json.rpc.impl.ParamsChangeBaseCurrency;

/**
 *
 * @author Datraverse B.V.
 *
 */
public final class CliChangeBaseCurrency extends AbstractAppApi {

    /**
     *
     */
    private static final String API_VERSION = "0.10";

    /**
     * .
     */
    private static final String METHOD_SHORT_DESCRIPT =
            "Changes the base currency of the application.";

    /**
     * .
     */
    private static final String METHOD_LONG_DESCRIPT =
            "WARNING: This action creates financial transactions to align "
                    + "each account to the\nnew currency. Please create a "
                    + "database back-up before executing this command.";
    /**
     *
     */
    private static final String CLI_OPT_CURRENCY_FROM = "from";

    /**
    *
    */
    private static final String CLI_OPT_CURRENCY_TO = "to";

    /**
   *
   */
    private static final String CLI_OPT_EXCHANGE_RATE = "exchange-rate";

    /**
     *
     */
    private static Object[][] theOptions = new Object[][] {
            //
            { ARG_TEXT + "(3)", CLI_OPT_CURRENCY_FROM,
                    "The current currency code (ISO 4217).", Boolean.TRUE },
            //
            { ARG_TEXT + "(3)", CLI_OPT_CURRENCY_TO,
                    "The new currency code (ISO 4217).", Boolean.TRUE },
            //
            { ARG_DECIMAL, CLI_OPT_EXCHANGE_RATE, "The exchange rate.",
                    Boolean.TRUE }
    //
            };

    @Override
    protected String getApiVersion() {
        return API_VERSION;
    }

    @Override
    protected String getMethodName() {
        return JsonRpcMethodName.CHANGE_BASE_CURRENCY.getMethodName();
    }

    @Override
    protected Object[][] getOptionDictionary() {
        return theOptions;
    }

    @Override
    protected String getShortDecription() {
        return METHOD_SHORT_DESCRIPT;
    }

    @Override
    protected String getLongDecription() {
        return METHOD_LONG_DESCRIPT;
    }

    @Override
    protected boolean hasBatchOptions() {
        return false;
    }

    @Override
    protected boolean hasLocaleOption() {
        return false;
    }

    @Override
    protected boolean isValidCliInput(final CommandLine cmd) {
        if (!cmd.hasOption(CLI_OPT_CURRENCY_FROM)
                || !cmd.hasOption(CLI_OPT_CURRENCY_TO)
                || !cmd.hasOption(CLI_OPT_EXCHANGE_RATE)) {
            return false;
        }

        try {
            Currency.getInstance(cmd.getOptionValue(CLI_OPT_CURRENCY_FROM));
            Currency.getInstance(cmd.getOptionValue(CLI_OPT_CURRENCY_TO));
            Double.valueOf(cmd.getOptionValue(CLI_OPT_EXCHANGE_RATE));
        } catch (Exception e) {
            return false;
        }

        getDisplayStream().println(METHOD_LONG_DESCRIPT);
        getDisplayStream().println();
        getDisplayStream().print("Press 'Y' + <enter> to continue... ");

        final int answer;
        try {
            answer = System.in.read();
        } catch (IOException e) {
            return false;
        }
        return answer == 'Y';
    }

    @Override
    protected AbstractJsonRpcMethodParms
            createMethodParms(final CommandLine cmd) {

        final ParamsChangeBaseCurrency parms = new ParamsChangeBaseCurrency();

        parms.setCurrencyCodeFrom(cmd.getOptionValue(CLI_OPT_CURRENCY_FROM));
        parms.setCurrencyCodeTo(cmd.getOptionValue(CLI_OPT_CURRENCY_TO));
        parms.setExchangeRate(Double.valueOf(cmd
                .getOptionValue(CLI_OPT_EXCHANGE_RATE)));

        return parms;
    }

    @Override
    protected void onErrorResponse(final JsonRpcError error) {

        final ErrorDataBasic data = error.data(ErrorDataBasic.class);

        getErrorDisplayStream().println(
                "Error [" + error.getCode() + "]: " + error.getMessage());

        if (data.getReason() != null) {
            getErrorDisplayStream().println("Reason: " + data.getReason());
        }
    }

    @Override
    protected boolean onResultResponse(final JsonRpcResult result) {

        final ResultDataBasic data = result.data(ResultDataBasic.class);

        getDisplayStream().println(data.getMessage());

        return false;
    }

    @Override
    protected boolean isSwitchOption(final String optionName) {
        return false;
    }

    @Override
    protected void onInit() throws Exception {
        // no code intended
    }

}
