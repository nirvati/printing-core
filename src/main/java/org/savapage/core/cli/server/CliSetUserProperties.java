/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
import org.savapage.core.dto.CreditLimitDtoEnum;
import org.savapage.core.dto.UserAccountingDto;
import org.savapage.core.dto.UserPropertiesDto;
import org.savapage.core.json.rpc.AbstractJsonRpcMethodParms;
import org.savapage.core.json.rpc.ErrorDataBasic;
import org.savapage.core.json.rpc.JsonRpcError;
import org.savapage.core.json.rpc.JsonRpcMethodName;
import org.savapage.core.json.rpc.JsonRpcResult;
import org.savapage.core.json.rpc.impl.ParamsSetUserProperties;

/**
 *
 * @author Rijk Ravestein
 *
 */
public class CliSetUserProperties extends AbstractAppApi {

    /**
     *
     */
    private static final String API_VERSION = "0.30";

    /**
     *
     */
    private static final String METHOD_SHORT_DESCRIPT =
            "Sets properties for " + "an existing Internal or External User.";

    /**
     *
     */
    private static final String CLI_OPT_USERNAME = "username";

    /**
     *
     */
    private static final String CLI_OPT_PASSWORD = "password";

    /**
     *
     */
    private static final String CLI_OPT_FULL_NAME = "full-name";

    /**
     *
     */
    private static final String CLI_OPT_EMAIL = "email";

    /**
     *
     */
    private static final String CLI_OPT_EMAIL_OTHER = "email-other";

    /**
     *
     */
    private static final String CLI_OPT_CARD = "card";

    /**
     *
     */
    private static final String CLI_OPT_CARD_FORMAT = "card-format";

    /**
     *
     */
    private static final String CLI_OPT_CARD_FIRST_BYTE = "card-first-byte";

    /**
     *
     */
    private static final String CLI_OPT_ID = "id";

    /**
    *
    */
    private static final String CLI_OPT_YUBIKEY = "yubikey";

    /**
     *
     */
    private static final String CLI_OPT_PIN = "pin";

    /**
    *
    */
    private static final String CLI_OPT_UUID = "uuid";

    /**
     * Accounting.
     */
    private static final String CLI_OPT_BALANCE = "balance";

    /**
     * Accounting.
     */
    private static final String CLI_OPT_BALANCE_COMMENT = "balance-comment";

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
     *
     */
    private static final String CLI_SWITCH_KEEP_CARD =
            CLI_SWITCH_PFX_KEEP + CLI_OPT_CARD;

    /**
    *
    */
    private static final String CLI_SWITCH_KEEP_ID =
            CLI_SWITCH_PFX_KEEP + CLI_OPT_ID;

    /**
     *
     */
    private static final String CLI_SWITCH_KEEP_EMAIL_OTHER =
            CLI_SWITCH_PFX_KEEP + CLI_OPT_EMAIL_OTHER;

    /**
     *
     */
    private static final String CLI_SWITCH_KEEP_PASSWORD =
            CLI_SWITCH_PFX_KEEP + CLI_OPT_PASSWORD;

    /**
     *
     */
    private static final String CLI_SWITCH_KEEP_PIN =
            CLI_SWITCH_PFX_KEEP + CLI_OPT_PIN;

    /**
     * .
     */
    private static final String CLI_SWITCH_KEEP_UUID =
            CLI_SWITCH_PFX_KEEP + CLI_OPT_UUID;

    /**
     *
     */
    private static final String CLI_SWITCH_REMOVE_EMAIL =
            CLI_SWITCH_PFX_REMOVE + CLI_OPT_EMAIL;

    /**
     *
     */
    private static final String CLI_SWITCH_REMOVE_EMAIL_OTHER =
            CLI_SWITCH_PFX_REMOVE + CLI_OPT_EMAIL_OTHER;

    /**
     *
     */
    private static final String CLI_SWITCH_REMOVE_CARD =
            CLI_SWITCH_PFX_REMOVE + CLI_OPT_CARD;

    /**
     *
     */
    private static final String CLI_SWITCH_REMOVE_ID =
            CLI_SWITCH_PFX_REMOVE + CLI_OPT_ID;

    /**
    *
    */
    private static final String CLI_SWITCH_REMOVE_YUBIKEY =
            CLI_SWITCH_PFX_REMOVE + CLI_OPT_YUBIKEY;

    /**
     *
     */
    private static final String CLI_SWITCH_REMOVE_PASSWORD =
            CLI_SWITCH_PFX_REMOVE + "password";

    /**
     *
     */
    private static final String CLI_SWITCH_REMOVE_PIN =
            CLI_SWITCH_PFX_REMOVE + CLI_OPT_PIN;

    /**
     * .
     */
    private static final String CLI_SWITCH_REMOVE_UUID =
            CLI_SWITCH_PFX_REMOVE + CLI_OPT_UUID;

    /**
     *
     */
    private static Object[][] theOptions = new Object[][] {

            /*
             * Regular options.
             */
            { ARG_TEXT + "(50)", CLI_OPT_USERNAME, "Unique user name.",
                    Boolean.TRUE },
            { ARG_TEXT + "(64)", CLI_OPT_PASSWORD,
                    "Password (Internal User only)." },
            { ARG_TEXT + "(255)", CLI_OPT_FULL_NAME, "Full user name." },
            { ARG_TEXT + "(255)", CLI_OPT_EMAIL, "Primary Email address." },
            { ARG_LIST, CLI_OPT_EMAIL_OTHER,
                    "List of space separated other (secondary) Email addresses." },
            { ARG_TEXT + "(16)", CLI_OPT_CARD, "NFC Card Number." },
            { ARG_CARD_FORMAT, CLI_OPT_CARD_FORMAT,
                    "NFC Card Number Format [default: HEX]." },
            { ARG_CARD_FIRST_BYTE, CLI_OPT_CARD_FIRST_BYTE,
                    "NFC Card Number First Byte [default: LSB]." },
            { ARG_TEXT + "(16)", CLI_OPT_ID, "ID Number." },
            { ARG_TEXT + "(16)", CLI_OPT_PIN, "PIN for ID and Card." },
            { ARG_TEXT + "(12)", CLI_OPT_YUBIKEY, "YubiKey Public ID." },
            { ARG_TEXT + "(36)", CLI_OPT_UUID, "The user's secret UUID." },

            /*
             * Accounting.
             */
            { ARG_DECIMAL, CLI_OPT_BALANCE,
                    "The user's current account balance." },

            { ARG_TEXT + "(255)", CLI_OPT_BALANCE_COMMENT,
                    "A comment to be associated with the --" + CLI_OPT_BALANCE
                            + " transaction." },

            { null, CLI_SWITCH_CREDIT_LIMIT,
                    "Assign default credit limit amount." },

            { ARG_DECIMAL, CLI_OPT_CREDIT_LIMIT_AMOUNT,
                    "Assign custom credit limit amount." },

            { null, CLI_SWITCH_CREDIT_LIMIT_NONE,
                    "No credit limit restriction (opposed to --"
                            + CLI_SWITCH_CREDIT_LIMIT + " and --"
                            + CLI_OPT_CREDIT_LIMIT_AMOUNT + ")." },

            /*
             * Keep switches.
             */
            { null, CLI_SWITCH_KEEP_CARD,
                    "Keep existing Card Number, or use --" + CLI_OPT_CARD
                            + " value when not present." },
            { null, CLI_SWITCH_KEEP_ID,
                    "Keep existing ID Number, or use --" + CLI_OPT_ID
                            + " value when not present." },
            { null, CLI_SWITCH_KEEP_EMAIL_OTHER,
                    "Keep existing other (secondary) Email addresses, or use --"
                            + CLI_OPT_EMAIL_OTHER
                            + " value when not present." },
            { null, CLI_SWITCH_KEEP_PASSWORD,
                    "Keep existing Password, or use --" + CLI_OPT_PASSWORD
                            + " value when not present (Internal User only)." },
            { null, CLI_SWITCH_KEEP_PIN,
                    "Keep existing PIN, or use --" + CLI_OPT_PIN
                            + " value when not present." },

            { null, CLI_SWITCH_KEEP_UUID,
                    "Keep existing UUID, or use --" + CLI_OPT_UUID
                            + " value when not present." },
            /*
             * Remove switches.
             */
            { null, CLI_SWITCH_REMOVE_EMAIL,
                    "Remove Primary Email address (opposed to --"
                            + CLI_OPT_EMAIL + ")." },
            { null, CLI_SWITCH_REMOVE_EMAIL_OTHER,
                    "Remove other (secondary) Email addresses (opposed to --"
                            + CLI_OPT_EMAIL_OTHER + ")." },
            { null, CLI_SWITCH_REMOVE_CARD,
                    "Remove NFC Card Number (opposed to --" + CLI_OPT_CARD
                            + ")." },
            { null, CLI_SWITCH_REMOVE_ID,
                    "Remove ID Number (opposed to --" + CLI_OPT_ID + ")." },
            { null, CLI_SWITCH_REMOVE_PASSWORD,
                    "Remove Password (Internal User only)." },
            { null, CLI_SWITCH_REMOVE_PIN,
                    "Remove PIN (opposed to --" + CLI_OPT_PIN + ")." },
            { null, CLI_SWITCH_REMOVE_YUBIKEY,
                    "Remove YubiKey Public ID (opposed to --" + CLI_OPT_YUBIKEY
                            + ")." },
            { null, CLI_SWITCH_REMOVE_UUID,
                    "Remove UUID (opposed to --" + CLI_OPT_UUID + ")." }
            //
    };

    @Override
    protected final String getApiVersion() {
        return API_VERSION;
    }

    @Override
    protected final String getMethodName() {
        return JsonRpcMethodName.SET_USER_PROPERTIES.getMethodName();
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
        if (!cmd.hasOption(CLI_OPT_USERNAME)) {
            return false;
        }

        /*
         * INVARIANT: Card Format/First Byte options MUST be combined with Card
         * option.
         */
        if ((cmd.hasOption(CLI_OPT_CARD_FIRST_BYTE)
                || cmd.hasOption(CLI_OPT_CARD_FORMAT))
                && !cmd.hasOption(CLI_OPT_CARD)) {
            return false;
        }

        /*
         * INVARIANT: "keep" switches MUST be combined with their corresponding
         * options.
         */
        if ((cmd.hasOption(CLI_SWITCH_KEEP_CARD)
                && !cmd.hasOption(CLI_OPT_CARD))
                || (cmd.hasOption(CLI_SWITCH_KEEP_ID)
                        && !cmd.hasOption(CLI_OPT_ID))
                || (cmd.hasOption(CLI_SWITCH_KEEP_EMAIL_OTHER)
                        && !cmd.hasOption(CLI_OPT_EMAIL_OTHER))
                || (cmd.hasOption(CLI_SWITCH_KEEP_PASSWORD)
                        && !cmd.hasOption(CLI_OPT_PASSWORD))
                || (cmd.hasOption(CLI_SWITCH_KEEP_PIN)
                        && !cmd.hasOption(CLI_OPT_PIN))
                || (cmd.hasOption(CLI_SWITCH_KEEP_UUID)
                        && !cmd.hasOption(CLI_OPT_UUID))) {
            return false;
        }

        /*
         * INVARIANT: "remove" switches can NOT be combined with their
         * corresponding options.
         */
        if ((cmd.hasOption(CLI_SWITCH_REMOVE_CARD)
                && cmd.hasOption(CLI_OPT_CARD))
                || (cmd.hasOption(CLI_SWITCH_REMOVE_EMAIL)
                        && cmd.hasOption(CLI_OPT_EMAIL))
                || (cmd.hasOption(CLI_SWITCH_REMOVE_EMAIL_OTHER)
                        && cmd.hasOption(CLI_OPT_EMAIL_OTHER))
                || (cmd.hasOption(CLI_SWITCH_REMOVE_ID)
                        && cmd.hasOption(CLI_OPT_ID))
                || (cmd.hasOption(CLI_SWITCH_REMOVE_YUBIKEY)
                        && cmd.hasOption(CLI_OPT_YUBIKEY))
                || (cmd.hasOption(CLI_SWITCH_REMOVE_PASSWORD)
                        && cmd.hasOption(CLI_OPT_PASSWORD))
                || (cmd.hasOption(CLI_SWITCH_REMOVE_PIN)
                        && cmd.hasOption(CLI_OPT_PIN))
                || (cmd.hasOption(CLI_SWITCH_REMOVE_UUID)
                        && cmd.hasOption(CLI_OPT_UUID))) {
            return false;
        }

        /*
         * INVARIANT (Accounting): A balance comment MUST be combined with
         * balance amount.
         */
        if (cmd.hasOption(CLI_OPT_BALANCE_COMMENT)
                && !cmd.hasOption(CLI_OPT_BALANCE)) {
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

        //
        return true;
    }

    @Override
    protected final AbstractJsonRpcMethodParms
            createMethodParms(final CommandLine cmd) {

        ParamsSetUserProperties parms = new ParamsSetUserProperties();

        UserPropertiesDto dto = new UserPropertiesDto();
        parms.setUserProperties(dto);

        dto.setCard(cmd.getOptionValue(CLI_OPT_CARD));
        dto.setFullName(cmd.getOptionValue(CLI_OPT_FULL_NAME));
        dto.setCardFormat(cmd.getOptionValue(CLI_OPT_CARD_FORMAT));
        dto.setCardFirstByte(cmd.getOptionValue(CLI_OPT_CARD_FIRST_BYTE));
        dto.setEmail(cmd.getOptionValue(CLI_OPT_EMAIL));

        if (cmd.hasOption(CLI_OPT_EMAIL_OTHER)) {
            dto.importEmailOther(cmd.getOptionValue(CLI_OPT_EMAIL_OTHER));
        }

        dto.setId(cmd.getOptionValue(CLI_OPT_ID));
        dto.setYubiKeyPubId(cmd.getOptionValue(CLI_OPT_YUBIKEY));
        dto.setPassword(cmd.getOptionValue(CLI_OPT_PASSWORD));
        dto.setPin(cmd.getOptionValue(CLI_OPT_PIN));
        dto.setUuid(cmd.getOptionValue(CLI_OPT_UUID));
        dto.setUserName(cmd.getOptionValue(CLI_OPT_USERNAME));
        //
        dto.setKeepCard(this.getSwitchValue(cmd, CLI_SWITCH_KEEP_CARD));
        dto.setKeepId(this.getSwitchValue(cmd, CLI_SWITCH_KEEP_ID));
        dto.setKeepEmailOther(
                this.getSwitchValue(cmd, CLI_SWITCH_KEEP_EMAIL_OTHER));
        dto.setKeepPassword(this.getSwitchValue(cmd, CLI_SWITCH_KEEP_PASSWORD));
        dto.setKeepPin(this.getSwitchValue(cmd, CLI_SWITCH_KEEP_PIN));
        //
        dto.setRemoveCard(this.getSwitchValue(cmd, CLI_SWITCH_REMOVE_CARD));
        dto.setRemoveEmail(this.getSwitchValue(cmd, CLI_SWITCH_REMOVE_EMAIL));
        dto.setRemoveEmailOther(
                this.getSwitchValue(cmd, CLI_SWITCH_REMOVE_EMAIL_OTHER));
        dto.setRemoveId(this.getSwitchValue(cmd, CLI_SWITCH_REMOVE_ID));
        dto.setRemoveYubiKey(
                this.getSwitchValue(cmd, CLI_SWITCH_REMOVE_YUBIKEY));
        dto.setRemovePassword(
                this.getSwitchValue(cmd, CLI_SWITCH_REMOVE_PASSWORD));
        dto.setRemovePin(this.getSwitchValue(cmd, CLI_SWITCH_REMOVE_PIN));

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

        value = cmd.getOptionValue(CLI_OPT_BALANCE_COMMENT);
        if (value != null) {
            dtoAccounting.setComment(value);
            dto.setAccounting(dtoAccounting);
        }

        value = cmd.getOptionValue(CLI_OPT_CREDIT_LIMIT_AMOUNT);
        if (value != null) {
            dtoAccounting.setCreditLimitAmount(value);
            dtoAccounting.setCreditLimit(CreditLimitDtoEnum.INDIVIDUAL);
            dto.setAccounting(dtoAccounting);
        }

        if (this.getSwitchValue(cmd, CLI_SWITCH_CREDIT_LIMIT)) {
            dtoAccounting.setCreditLimit(CreditLimitDtoEnum.DEFAULT);
            dto.setAccounting(dtoAccounting);
        }

        if (this.getSwitchValue(cmd, CLI_SWITCH_CREDIT_LIMIT_NONE)) {
            dtoAccounting.setCreditLimit(CreditLimitDtoEnum.NONE);
            dto.setAccounting(dtoAccounting);
        }

        if (dto.getAccounting() != null) {
            dto.getAccounting().setLocale(getLocaleOption(cmd));
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
        case CLI_SWITCH_KEEP_CARD:
        case CLI_SWITCH_KEEP_ID:
        case CLI_SWITCH_KEEP_EMAIL_OTHER:
        case CLI_SWITCH_KEEP_PASSWORD:
        case CLI_SWITCH_KEEP_PIN:
        case CLI_SWITCH_REMOVE_CARD:
        case CLI_SWITCH_REMOVE_EMAIL:
        case CLI_SWITCH_REMOVE_EMAIL_OTHER:
        case CLI_SWITCH_REMOVE_ID:
        case CLI_SWITCH_REMOVE_PASSWORD:
        case CLI_SWITCH_REMOVE_PIN:
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
