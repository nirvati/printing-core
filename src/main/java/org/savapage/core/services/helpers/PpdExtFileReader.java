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
package org.savapage.core.services.helpers;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dto.IppCostRule;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.JsonProxyPrinterOptGroup;
import org.savapage.core.print.proxy.ProxyPrinterOptGroupEnum;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AbstractConfigFileReader;
import org.savapage.core.util.BigDecimalUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reader of file with SavaPage PPD extensions from
 * {@link ConfigManager#SERVER_REL_PATH_CUSTOM_CUPS}.
 *
 * @author Rijk Ravestein
 *
 */
public final class PpdExtFileReader extends AbstractConfigFileReader {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(PpdExtFileReader.class);

    /**
     * The Line Continuation character.
     */
    private static final Character LINE_CONTINUE_CHAR = Character.valueOf('\\');

    /**
     *
     */
    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    /**
     * The prefix for a default PPD keyword choice.
     */
    private static final String PPD_CHOICE_DEFAULT_PFX = "*";

    /**
     * The prefix for all options.
     */
    private static final String PPD_OPTION_PFX_CHAR = "*";

    /**
     * The UIConstraints as selected/imported from the corresponding PPD file.
     */
    private static final String PPD_UI_CONSTRAINT =
            PPD_OPTION_PFX_CHAR + "UIConstraints:";

    /**
     *
     */
    private static final String SP_JOBTICKET_PFX =
            PPD_OPTION_PFX_CHAR + "SPJobTicket";

    private static final String SP_JOBTICKET_MEDIA_SFX = "/Media";

    private static final String SP_JOBTICKET_COPY_SFX = "/Copy";

    private static final String SP_JOBTICKET_COST_SFX = "/Cost";

    private static final String SP_JOBTICKET_MEDIA =
            SP_JOBTICKET_PFX + SP_JOBTICKET_MEDIA_SFX + ":";

    private static final String SP_JOBTICKET_MEDIA_COST = SP_JOBTICKET_PFX
            + SP_JOBTICKET_MEDIA_SFX + SP_JOBTICKET_COST_SFX + ":";

    private static final String SP_JOBTICKET_COPY =
            SP_JOBTICKET_PFX + SP_JOBTICKET_COPY_SFX + ":";

    private static final String SP_JOBTICKET_COPY_COST = SP_JOBTICKET_PFX
            + SP_JOBTICKET_COPY_SFX + SP_JOBTICKET_COST_SFX + ":";

    /**
     * .
     */
    private static final char SP_JOBTICKET_ATTR_CHOICE_SEPARATOR = '/';

    /**
     * .
     */
    private static final String SP_JOBTICKET_ATTR_CHOICE_NEGATE = "!";

    /**
     * Minimal number of arguments for option with
     * {@link #SP_JOBTICKET_COST_SFX}.
     */
    private static final int SP_JOBTICKET_COST_MIN_ARGS = 3;

    /**
     * The number of words after a PPD option constant.
     */
    private static final int PPD_OPTION_CONSTANT_WORDS = 1;

    /**
     * The number of words after a PPD option mapping.
     */
    private static final int PPD_OPTION_MAPPING_WORDS = 2;

    /**
     * The number of words after a PPD option value mapping.
     */
    private static final int PPD_OPTION_VALUE_MAPPING_WORDS = 3;

    /**
     * The number of words after a UIConstraint.
     */
    private static final int PPD_UI_CONSTRAINT_WORDS = 5;

    /**
     * PPD Option as key to JsonProxyPrinterOpt with IPP mapping.
     */
    private Map<String, JsonProxyPrinterOpt> ppdOptionMap;

    /**
     * IPP Option as key to JsonProxyPrinterOpt with PPD/IPP mapping.
     */
    private Map<String, JsonProxyPrinterOpt> ppdOptionMapOnIpp;

    /**
     * SpJobTicket Media Option as key to JsonProxyPrinterOpt.
     */
    private Map<String, JsonProxyPrinterOpt> jobTicketOptMapMedia;

    /**
     * SpJobTicket Copy Option as key to JsonProxyPrinterOpt.
     */
    private Map<String, JsonProxyPrinterOpt> jobTicketOptMapCopy;

    /**
     * SpJobTicket Media <i>and</i> Copy Options as key to JsonProxyPrinterOpt.
     */
    private Map<String, JsonProxyPrinterOpt> jobTicketOptMap;

    /**
     *
     */
    private List<IppCostRule> jobTicketCostRulesMedia;

    /**
     *
     */
    private List<IppCostRule> jobTicketCostRulesCopy;

    /**
     * IPP printer options as retrieved from CUPS.
     */
    private final Map<String, JsonProxyPrinterOpt> optionsFromCUPS;

    /**
     *
     * @param cupsOptionsLookup
     *            IPP printer options as retrieved from CUPS.
     */
    private PpdExtFileReader(
            final Map<String, JsonProxyPrinterOpt> cupsOptionsLookup) {
        this.optionsFromCUPS = cupsOptionsLookup;
    }

    /**
     * Lazy creates a PPD mapped {@link JsonProxyPrinterOpt}.
     *
     * @param ppdOption
     *            The PPD option name.
     * @return The {@link JsonProxyPrinterOpt}.
     */
    private JsonProxyPrinterOpt
            lazyCreatePpdOptionMapping(final String ppdOption) {
        return lazyCreateMapping(this.ppdOptionMap, ppdOption);
    }

    /**
     * Lazy creates a mapped IPP attribute {@link JsonProxyPrinterOpt} for Job
     * Ticket.
     *
     * @param map
     *            The map to lazy add on.
     * @param ippAttr
     *            The IPP attribute name.
     * @return The {@link JsonProxyPrinterOpt}.
     */
    private static JsonProxyPrinterOpt lazyCreateJobTicketMapping(
            final Map<String, JsonProxyPrinterOpt> map, final String ippAttr) {
        final JsonProxyPrinterOpt opt = lazyCreateMapping(map, ippAttr);
        opt.setJobTicket(true);
        return opt;
    }

    /**
     * Lazy creates a mapped IPP attribute {@link JsonProxyPrinterOpt} .
     *
     * @param map
     *            The map to lazy add on.
     * @param ippMediaAttr
     *            The IPP attribute name.
     * @return The {@link JsonProxyPrinterOpt}.
     */
    private static JsonProxyPrinterOpt lazyCreateMapping(
            final Map<String, JsonProxyPrinterOpt> map,
            final String ippMediaAttr) {

        JsonProxyPrinterOpt opt = map.get(ippMediaAttr);

        if (opt == null) {
            opt = new JsonProxyPrinterOpt();
            opt.setKeyword(ippMediaAttr);
            map.put(ippMediaAttr, opt);
        }
        return opt;
    }

    @Override
    protected Character getLineContinuationChar() {
        return LINE_CONTINUE_CHAR;
    }

    /**
     * Notifies start-of-file.
     */
    @Override
    protected void onInit() {

        this.ppdOptionMap = new HashMap<>();
        this.ppdOptionMapOnIpp = new HashMap<>();

        this.jobTicketOptMap = new HashMap<>();

        this.jobTicketOptMapMedia = new HashMap<>();
        this.jobTicketCostRulesMedia = new ArrayList<>();

        this.jobTicketOptMapCopy = new HashMap<>();
        this.jobTicketCostRulesCopy = new ArrayList<>();
    }

    /**
     * Notifies end-of-file.
     */
    @Override
    protected void onEof() {
    }

    /**
     * Notifies a UIContraint.
     *
     * @param words
     *            The words.
     */
    private void onUiConstraint(final String[] words) {
    }

    /**
     * Notifies a {@link #SP_JOBTICKET_MEDIA} or {@link #SP_JOBTICKET_COPY}
     * line.
     *
     * @param map
     *            The map to lazy add on.
     * @param ppdeFile
     *            The .ppde file (for logging).
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     * @return The {@link JsonProxyPrinterOpt}.
     */
    private static JsonProxyPrinterOpt onSpJobTicket(
            final Map<String, JsonProxyPrinterOpt> map, final File ppdeFile,
            final int lineNr, final String[] words) {

        final String ippAttr = words[0].trim();

        if (words.length < 2) {
            LOGGER.warn(String.format(
                    "%s line %d: no values for IPP attribute [%s]",
                    ppdeFile.getName(), lineNr, ippAttr));
            return null;
        }

        final JsonProxyPrinterOpt opt =
                lazyCreateJobTicketMapping(map, ippAttr);

        for (final String choice : ArrayUtils.remove(words, 0)) {

            final String ippChoice =
                    StringUtils.stripStart(choice, PPD_CHOICE_DEFAULT_PFX);

            if (!choice.equals(ippChoice)) {
                opt.setDefchoice(ippChoice);
                opt.setDefchoiceIpp(ippChoice);
            }

            final JsonProxyPrinterOptChoice optChoice =
                    opt.addChoice(ippChoice, ippChoice);

            optChoice.setChoicePpd(ippChoice);
        }

        return opt;
    }

    /**
     * Notifies a {@link #SP_JOBTICKET_COPY} line.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     */
    private void onSpJobTicketCopy(final int lineNr, final String[] words) {
        /*
         * Example: org.savapage-finishing-external *none laminate bind glue
         * folder
         */
        final JsonProxyPrinterOpt opt = onSpJobTicket(this.jobTicketOptMapCopy,
                this.getConfigFile(), lineNr, words);
        this.jobTicketOptMap.put(opt.getKeyword(), opt);
    }

    /**
     * Notifies a {@link #SP_JOBTICKET_MEDIA} line.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     */
    private void onSpJobTicketMedia(final int lineNr, final String[] words) {
        /*
         * Example: media-color *white blue red green orange
         */
        final JsonProxyPrinterOpt opt = onSpJobTicket(this.jobTicketOptMapMedia,
                this.getConfigFile(), lineNr, words);
        this.jobTicketOptMap.put(opt.getKeyword(), opt);
    }

    /**
     * Notifies a {@link #SP_JOBTICKET_MEDIA_COST} or
     * {@link #SP_JOBTICKET_COPY_COST} line.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     * @param optMap
     *            Valid options for rule option parts.
     * @param costRules
     *            The list to append on.
     */
    private void onSpJobTicketCost(final int lineNr, final String[] words,
            final Map<String, JsonProxyPrinterOpt> optMap,
            final List<IppCostRule> costRules) {

        if (words.length < SP_JOBTICKET_COST_MIN_ARGS) {
            LOGGER.warn(
                    String.format("%s line %d: incomplete cost specification",
                            getConfigFile().getName(), lineNr));
            return;
        }

        final BigDecimal cost;

        try {
            cost = BigDecimalUtil.parse(words[0].trim(), Locale.ENGLISH, false,
                    false);
        } catch (ParseException e) {
            LOGGER.warn(String.format("%s line %d: cost syntax error [%s]",
                    getConfigFile().getName(), lineNr, e.getMessage()));
            return;
        }

        final String alias = words[1].trim();

        //
        final IppCostRule costRule = new IppCostRule(alias, cost);

        boolean isLineValid = true;

        for (final String attrChoice : ArrayUtils.removeAll(words, 0, 1)) {

            final String[] splitWords = StringUtils.split(attrChoice,
                    SP_JOBTICKET_ATTR_CHOICE_SEPARATOR);

            if (splitWords.length != 2) {
                LOGGER.warn(String.format("%s line %d: \"%s\" syntax invalid",
                        getConfigFile().getName(), lineNr, attrChoice));
                isLineValid = false;
                continue;
            }

            final String ippAttr = splitWords[0];

            /*
             * INVARIANT: IPP attribute must be known.
             */
            final JsonProxyPrinterOpt opt;

            /*
             * NOTE: The order of checking containsKey() is important. Check
             * optionsFromCUPS as last, since ppdOptionMapOnIpp may contain
             * overrides.
             */
            if (optMap.containsKey(ippAttr)) {
                opt = optMap.get(ippAttr);
            } else if (this.ppdOptionMapOnIpp.containsKey(ippAttr)) {
                opt = this.ppdOptionMapOnIpp.get(ippAttr);
            } else if (this.optionsFromCUPS.containsKey(ippAttr)) {
                opt = this.optionsFromCUPS.get(ippAttr);
            } else {
                LOGGER.warn(String.format(
                        "%s line %d: IPP attribute \"%s\" is unknown",
                        getConfigFile().getName(), lineNr, ippAttr));
                isLineValid = false;
                continue;
            }

            /*
             * INVARIANT: IPP attribute choice must be known.
             */
            final String ippChoiceRaw = splitWords[1];
            final boolean isChoiceNegate =
                    ippChoiceRaw.startsWith(SP_JOBTICKET_ATTR_CHOICE_NEGATE);
            final String ippChoice;

            if (isChoiceNegate) {
                ippChoice = StringUtils.substring(ippChoiceRaw, 1);
            } else {
                ippChoice = ippChoiceRaw;
            }
            if (!opt.hasChoice(ippChoice)) {
                LOGGER.warn(String.format(
                        "%s line %d: IPP attribute/choice \"%s/%s\" is unknown",
                        getConfigFile().getName(), lineNr, ippAttr, ippChoice));
                isLineValid = false;
                continue;
            }

            costRule.addRuleChoice(ippAttr, ippChoice, !isChoiceNegate);
        }

        if (!isLineValid) {
            return;
        }

        costRules.add(costRule);
    }

    /**
     * Notifies a {@link #SP_JOBTICKET_MEDIA_COST} line.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     */
    private void onSpJobTicketMediaCost(final int lineNr,
            final String[] words) {
        this.onSpJobTicketCost(lineNr, words, this.jobTicketOptMapMedia,
                this.jobTicketCostRulesMedia);
    }

    /**
     * Notifies a {@link #SP_JOBTICKET_COPY_COST} line.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     */
    private void onSpJobTicketCopyCost(final int lineNr, final String[] words) {
        this.onSpJobTicketCost(lineNr, words, this.jobTicketOptMap,
                this.jobTicketCostRulesCopy);
    }

    /**
     * Notifies an SpJobTicket option.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param firstword
     *            The first word.
     * @param nextwords
     *            The next words.
     */
    private void onSpJobTicket(final int lineNr, final String firstword,
            final String[] nextwords) {

        switch (firstword) {
        case SP_JOBTICKET_COPY:
            this.onSpJobTicketCopy(lineNr, nextwords);
            break;
        case SP_JOBTICKET_COPY_COST:
            this.onSpJobTicketCopyCost(lineNr, nextwords);
            break;
        case SP_JOBTICKET_MEDIA:
            this.onSpJobTicketMedia(lineNr, nextwords);
            break;
        case SP_JOBTICKET_MEDIA_COST:
            this.onSpJobTicketMediaCost(lineNr, nextwords);
            break;
        default:
            LOGGER.warn(String.format("%s line %d [%s] is NOT handled.",
                    this.getConfigFile().getName(), lineNr, firstword));
            break;
        }
    }

    /**
     * Notifies a PPD constant.
     *
     * @param ppdOption
     *            The PPD option name.
     */
    private void onConstant(final String ppdOption) {
    }

    /**
     * Notifies a PPD option to IPP attribute mapping.
     *
     * @param ppdKeyword
     *            The PPD option keyword.
     * @param ippKeyword
     *            The IPP attribute keyword.
     */
    private void onOptionMapping(final String ppdKeyword,
            final String ippKeyword) {

        final JsonProxyPrinterOpt opt =
                this.lazyCreatePpdOptionMapping(ppdKeyword);

        opt.setKeyword(ippKeyword);
        opt.setKeywordPpd(
                StringUtils.stripStart(ppdKeyword, PPD_OPTION_PFX_CHAR));

        this.ppdOptionMapOnIpp.put(ippKeyword, opt);
    }

    /**
     * Notifies a PPD option to IPP attribute choice mapping.
     *
     * @param ppdOption
     *            The PPD option keyword.
     * @param ppdChoice
     *            The PPD option choice.
     * @param ippChoice
     *            The IPP attribute choice.
     */
    private void onOptionChoiceMapping(final String ppdOption,
            final String ppdChoice, final String ippChoice) {

        final JsonProxyPrinterOpt opt =
                this.lazyCreatePpdOptionMapping(ppdOption);

        final JsonProxyPrinterOptChoice choice =
                opt.addChoice(ippChoice, ippChoice);

        if (ppdChoice.startsWith(PPD_CHOICE_DEFAULT_PFX)) {
            opt.setDefchoice(ippChoice);
            opt.setDefchoiceIpp(ippChoice);
        }
        choice.setChoicePpd(
                StringUtils.stripStart(ppdChoice, PPD_CHOICE_DEFAULT_PFX));
    }

    @Override
    protected void onConfigLine(final int lineNr, final String strLine) {

        final String[] words = StringUtils.split(strLine);

        if (words.length == 0) {
            return;
        }

        final String firstWord = words[0].trim();

        if (firstWord.startsWith(SP_JOBTICKET_PFX)) {
            if (words.length > 1) {
                this.onSpJobTicket(lineNr, firstWord,
                        ArrayUtils.remove(words, 0));
            } else {
                LOGGER.warn(String.format("%s line %d: [%s] syntax error.",
                        this.getConfigFile().getName(), lineNr, firstWord));
            }
            return;
        }

        if (firstWord.equals(PPD_UI_CONSTRAINT)) {
            if (words.length == PPD_UI_CONSTRAINT_WORDS) {
                this.onUiConstraint(words);
            }
            return;
        }

        if (words.length == PPD_OPTION_CONSTANT_WORDS) {
            this.onConstant(firstWord);
            return;
        }

        if (words.length == PPD_OPTION_MAPPING_WORDS) {
            this.onOptionMapping(firstWord, words[1].trim());
            return;
        }

        if (words.length == PPD_OPTION_VALUE_MAPPING_WORDS) {
            this.onOptionChoiceMapping(firstWord, words[1].trim(),
                    words[2].trim());
            return;
        }
    }

    /**
     * Injects the SavaPage PPD extensions defined in {@link File} into the IPP
     * options of a proxy printer.
     *
     * @param proxyPrinter
     *            The {@link JsonProxyPrinter} containing the IPP options.
     * @param filePpdExt
     *            The {@link File} with the SavaPage PPD extensions.
     * @throws IOException
     *             The file IO errors.
     */
    public static void injectPpdExt(final JsonProxyPrinter proxyPrinter,
            final File filePpdExt) throws IOException {

        final Map<String, JsonProxyPrinterOpt> optionsLookup =
                proxyPrinter.getOptionsLookup();

        final PpdExtFileReader reader = new PpdExtFileReader(optionsLookup);
        reader.read(filePpdExt);

        final IppDictJobTemplateAttr ippDict =
                IppDictJobTemplateAttr.instance();

        // The mapped PPD options
        for (final JsonProxyPrinterOpt opt : reader.ppdOptionMap.values()) {

            final String keywordIpp = opt.getKeyword();

            final ProxyPrinterOptGroupEnum optGroupEnum;

            if (keywordIpp.startsWith(
                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_PFX_FINISHINGS)) {
                optGroupEnum = PROXYPRINT_SERVICE.getUiOptGroup(keywordIpp);
            } else if (ippDict.isCustomAttr(keywordIpp)
                    || ippDict.getAttr(keywordIpp) == null) {
                optGroupEnum = null;
            } else {
                optGroupEnum = PROXYPRINT_SERVICE.getUiOptGroup(keywordIpp);
            }

            if (optGroupEnum == null) {
                LOGGER.warn(String.format(
                        "%s: IPP attribute [%s] is not supported.", filePpdExt,
                        keywordIpp));
                continue;
            }

            if (keywordIpp.equals(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE)) {
                // Both choices must be present.
                proxyPrinter.setSheetCollate(
                        Boolean.valueOf(opt.getChoices().size() == 2));
            }

            final JsonProxyPrinterOpt optPresent =
                    optionsLookup.get(opt.getKeyword());

            if (optPresent != null) {
                optPresent.copyFrom(opt);
                continue;
            }

            final JsonProxyPrinterOptGroup optGroupInject =
                    lazyCreateOptGroup(optGroupEnum, proxyPrinter.getGroups());

            optGroupInject.getOptions().add(opt);
        }

        // The SpJobTicket Copy/Media options
        final JsonProxyPrinterOptGroup optGroupJobTicket = lazyCreateOptGroup(
                ProxyPrinterOptGroupEnum.JOB_TICKET, proxyPrinter.getGroups());

        optGroupJobTicket.getOptions().clear();

        for (final JsonProxyPrinterOpt opt : reader.jobTicketOptMapMedia
                .values()) {
            optGroupJobTicket.getOptions().add(opt);
        }
        for (final JsonProxyPrinterOpt opt : reader.jobTicketOptMapCopy
                .values()) {
            optGroupJobTicket.getOptions().add(opt);
        }

        // Custom cost rules.
        proxyPrinter.setCustomCostRulesCopy(reader.jobTicketCostRulesCopy);
        proxyPrinter.setCustomCostRulesMedia(reader.jobTicketCostRulesMedia);

        //
        proxyPrinter.setInjectPpdExt(true);
    }

    /**
     * Adds an {@link JsonProxyPrinterOptGroup} to the end of the groups list
     * when it does not exist.
     *
     * @param groupId
     *            The group id.
     * @param groups
     *            The groups list.
     * @return The existing or added group.
     */
    private static JsonProxyPrinterOptGroup lazyCreateOptGroup(
            final ProxyPrinterOptGroupEnum groupId,
            final ArrayList<JsonProxyPrinterOptGroup> groups) {

        for (final JsonProxyPrinterOptGroup group : groups) {
            if (group.getGroupId() == groupId) {
                return group;
            }
        }

        final JsonProxyPrinterOptGroup group = new JsonProxyPrinterOptGroup();

        group.setGroupId(groupId);
        group.setUiText(groupId.toString());
        group.setOptions(new ArrayList<JsonProxyPrinterOpt>());
        groups.add(group);

        return group;
    }

}
