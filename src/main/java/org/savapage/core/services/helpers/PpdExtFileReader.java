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
package org.savapage.core.services.helpers;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.rules.IppRuleConstraint;
import org.savapage.core.ipp.rules.IppRuleCost;
import org.savapage.core.ipp.rules.IppRuleExtra;
import org.savapage.core.ipp.rules.IppRuleNumberUp;
import org.savapage.core.ipp.rules.IppRuleSubst;
import org.savapage.core.pdf.PdfPageRotateHelper;
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
     * PPD attribute.
     */
    private static final String PPD_ATTR_LANDSCAPE_ORIENTATION =
            PPD_OPTION_PFX_CHAR + "LandscapeOrientation:";

    /**
     * PPD attribute value.
     */
    private static final String PPD_ATTR_LANDSCAPE_ORIENTATION_PLUS90 =
            "Plus90";

    /**
     * PPD attribute value.
     */
    private static final String PPD_ATTR_LANDSCAPE_ORIENTATION_MINUS90 =
            "Minus90";

    /** */
    private static final String SP_CONSTRAINT =
            PPD_OPTION_PFX_CHAR + "SPConstraint:";

    /** */
    private static final String SP_JOBTICKET_PFX =
            PPD_OPTION_PFX_CHAR + "SPJobTicket";

    /** */
    private static final String SP_JOBTICKET_MEDIA_SFX = "/Media";

    /** */
    private static final String SP_JOBTICKET_SHEET_SFX = "/Sheet";

    /** */
    private static final String SP_JOBTICKET_COPY_SFX = "/Copy";

    /** */
    private static final String SP_JOBTICKET_SET_SFX = "/Set";

    /** */
    private static final String SP_JOBTICKET_COST_SFX = "/Cost";

    /** */
    private static final String SP_JOBTICKET_MEDIA =
            SP_JOBTICKET_PFX + SP_JOBTICKET_MEDIA_SFX + ":";

    /** */
    private static final String SP_JOBTICKET_MEDIA_COST = SP_JOBTICKET_PFX
            + SP_JOBTICKET_MEDIA_SFX + SP_JOBTICKET_COST_SFX + ":";

    /** */
    private static final String SP_JOBTICKET_SHEET =
            SP_JOBTICKET_PFX + SP_JOBTICKET_SHEET_SFX + ":";

    /** */
    private static final String SP_JOBTICKET_SHEET_COST = SP_JOBTICKET_PFX
            + SP_JOBTICKET_SHEET_SFX + SP_JOBTICKET_COST_SFX + ":";

    /** */
    private static final String SP_JOBTICKET_COPY =
            SP_JOBTICKET_PFX + SP_JOBTICKET_COPY_SFX + ":";

    /** */
    private static final String SP_JOBTICKET_COPY_COST = SP_JOBTICKET_PFX
            + SP_JOBTICKET_COPY_SFX + SP_JOBTICKET_COST_SFX + ":";

    /** */
    private static final String SP_JOBTICKET_SET =
            SP_JOBTICKET_PFX + SP_JOBTICKET_SET_SFX + ":";

    /** */
    private static final String SP_JOBTICKET_SET_COST = SP_JOBTICKET_PFX
            + SP_JOBTICKET_SET_SFX + SP_JOBTICKET_COST_SFX + ":";

    /** */
    private static final char SP_JOBTICKET_ATTR_CHOICE_SEPARATOR = '/';

    /** */
    private static final String SP_JOBTICKET_ATTR_CHOICE_NEGATE = "!";

    /**
     * An SPJobTicket prefix indicating an <i>extended</i> option choice.
     */
    private static final String SP_JOBTICKET_OPTION_CHOICE_EXTENDED = "+";

    /**
     * Minimal number of arguments for option with
     * {@link #SP_JOBTICKET_COST_SFX}.
     */
    private static final int SP_JOBTICKET_COST_MIN_ARGS = 3;

    /* ==================================================================== */
    private static final String SP_RULE_PFX = PPD_OPTION_PFX_CHAR + "SPRule";
    private static final char SP_RULE_ATTR_CHOICE_SEPARATOR = '/';

    private static final String SP_RULE_NUMBER_UP_SFX =
            "/" + IppDictJobTemplateAttr.ATTR_NUMBER_UP;

    private static final String SP_RULE_NUMBER_UP =
            SP_RULE_PFX + SP_RULE_NUMBER_UP_SFX + ":";

    private static final String SP_RULE_OPTION_DEPENDENT_PFX = "*";
    private static final String SP_RULE_OPTION_CHOICE_NONE = "-";

    /**
     * Minimal number of arguments for option with {@link #SP_RULE_NUMBER_UP}.
     */
    private static final int SP_RULE_NUMBER_UP_MIN_ARGS = 7;

    /* ==================================================================== */
    private static final String SP_EXTRA_PFX = PPD_OPTION_PFX_CHAR + "SPExtra";
    private static final String SP_EXTRA_PPD_OPTION_PFX = "*";
    private static final String SP_EXTRA_ATTR_CHOICE_NEGATE = "!";

    /**
     * Minimal number of arguments for option with {@link #SP_EXTRA_PFX}.
     */
    private static final int SP_EXTRA_MIN_ARGS = 2;

    /* ==================================================================== */
    private static final String SP_SUBST_PFX = PPD_OPTION_PFX_CHAR + "SPSubst";
    private static final String SP_SUBST_PPD_VALUE_PFX = "*";
    private static final String SP_SUBST_ATTR_CHOICE_NEGATE = "!";

    /**
     * Minimal number of arguments for option with {@link #SP_EXTRA_PFX}.
     */
    private static final int SP_SUBST_MIN_ARGS = 2;

    /* ==================================================================== */

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
     * The number of words after an SPConstraint.
     */
    private static final int SP_CONSTRAINT_WORDS = 4;

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
     * SpJobTicket Sheet Option as key to JsonProxyPrinterOpt.
     */
    private Map<String, JsonProxyPrinterOpt> jobTicketOptMapSheet;

    /**
     * SpJobTicket Copy Option as key to JsonProxyPrinterOpt.
     */
    private Map<String, JsonProxyPrinterOpt> jobTicketOptMapCopy;

    /**
     * SpJobTicket Set Option as key to JsonProxyPrinterOpt.
     */
    private Map<String, JsonProxyPrinterOpt> jobTicketOptMapSet;

    /**
     * SpJobTicket Media <i>and</i> Copy Options as key to JsonProxyPrinterOpt.
     */
    private Map<String, JsonProxyPrinterOpt> jobTicketOptMap;

    /** */
    private List<IppRuleCost> jobTicketCostRulesMedia;

    /** */
    private List<IppRuleCost> jobTicketCostRulesSheet;

    /** */
    private List<IppRuleCost> jobTicketCostRulesCopy;

    /** */
    private List<IppRuleCost> jobTicketCostRulesSet;

    /** */
    private List<IppRuleNumberUp> numberUpRules;

    /** */
    private List<IppRuleConstraint> rulesConstraint;

    /** */
    private List<IppRuleExtra> rulesExtra;

    /** */
    private List<IppRuleSubst> rulesSubst;

    /**
     * IPP printer options as retrieved from CUPS.
     */
    private final Map<String, JsonProxyPrinterOpt> optionsFromCUPS;

    /**
     *
     */
    private Boolean ppdLandscapeMinus90;

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

        this.jobTicketOptMapSheet = new HashMap<>();
        this.jobTicketCostRulesSheet = new ArrayList<>();

        this.jobTicketOptMapCopy = new HashMap<>();
        this.jobTicketCostRulesCopy = new ArrayList<>();

        this.jobTicketOptMapSet = new HashMap<>();
        this.jobTicketCostRulesSet = new ArrayList<>();

        this.numberUpRules = new ArrayList<>();
        this.rulesConstraint = new ArrayList<>();
        this.rulesExtra = new ArrayList<>();
        this.rulesSubst = new ArrayList<>();

        this.ppdLandscapeMinus90 = null;
    }

    /**
     * Notifies end-of-file.
     */
    @Override
    protected void onEof() {
    }

    /**
     * Notifies an {@link #SP_CONSTRAINT}.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     */
    private void onSpConstraint(final int lineNr, final String[] words) {

        final String alias = words[0].trim();

        final IppRuleConstraint rule = new IppRuleConstraint(alias);

        final List<Pair<String, String>> ippContraints = new ArrayList<>();
        final Set<String> ippNegateSet = new HashSet<>();

        boolean isLineValid = true;

        for (final String attrChoice : ArrayUtils.removeAll(words, 0)) {

            final String[] splitWords = StringUtils.split(attrChoice,
                    SP_RULE_ATTR_CHOICE_SEPARATOR);

            final String attr = splitWords[0];

            if (splitWords.length != 2) {
                LOGGER.warn(String.format("%s line %d: \"%s\" syntax invalid",
                        getConfigFile().getName(), lineNr, attrChoice));
                isLineValid = false;
                continue;
            }

            final String ippChoiceRaw = splitWords[1];

            final boolean isChoiceNegate =
                    ippChoiceRaw.startsWith(SP_EXTRA_ATTR_CHOICE_NEGATE);

            final String ippChoice;

            if (isChoiceNegate) {
                ippChoice = StringUtils.substring(ippChoiceRaw, 1);
                ippNegateSet.add(attr);
            } else {
                ippChoice = ippChoiceRaw;
            }

            ippContraints
                    .add(new ImmutablePair<String, String>(attr, ippChoice));
        }

        if (!isLineValid) {
            return;
        }

        if (ippContraints.size() != 2) {
            LOGGER.warn(String.format("%s line %d: \"%s\" syntax invalid",
                    getConfigFile().getName(), lineNr, alias));
            return;
        }

        rule.setIppContraints(ippContraints);
        rule.setIppNegateSet(ippNegateSet);

        this.rulesConstraint.add(rule);
    }

    /**
     * Notifies a {@link #SP_JOBTICKET_MEDIA}, {@link #SP_JOBTICKET_SHEET} ,
     * {@link #SP_JOBTICKET_COPY} or {@link #SP_JOBTICKET_SET} line.
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

            String ippChoice =
                    StringUtils.stripStart(choice, PPD_CHOICE_DEFAULT_PFX);

            final boolean extended;

            if (choice.equals(ippChoice)) {
                ippChoice = StringUtils.stripStart(ippChoice,
                        SP_JOBTICKET_OPTION_CHOICE_EXTENDED);
                extended = !choice.equals(ippChoice);
            } else {
                opt.setDefchoice(ippChoice);
                opt.setDefchoiceIpp(ippChoice);
                extended = false;
            }

            final JsonProxyPrinterOptChoice optChoice =
                    opt.addChoice(ippChoice, ippChoice);

            optChoice.setExtended(extended);
            optChoice.setChoicePpd(ippChoice);
        }

        return opt;
    }

    /**
     * Notifies a {@link #SP_JOBTICKET_SET} line.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     */
    private void onSpJobTicketSet(final int lineNr, final String[] words) {
        /*
         * Example: org.savapage-job-sheets none *job-sheet-start
         */
        final JsonProxyPrinterOpt opt = onSpJobTicket(this.jobTicketOptMapSet,
                this.getConfigFile(), lineNr, words);
        this.jobTicketOptMap.put(opt.getKeyword(), opt);
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
     * Notifies a {@link #SP_JOBTICKET_SHEET} line.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     */
    private void onSpJobTicketSheet(final int lineNr, final String[] words) {
        /*
         * Example: media-color *white blue red green orange
         */
        final JsonProxyPrinterOpt opt = onSpJobTicket(this.jobTicketOptMapSheet,
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
            final List<IppRuleCost> costRules) {

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
        final IppRuleCost costRule = new IppRuleCost(alias, cost);

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
     * Notifies a {@link #SP_JOBTICKET_SHEET_COST} line.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     */
    private void onSpJobTicketSheetCost(final int lineNr,
            final String[] words) {
        this.onSpJobTicketCost(lineNr, words, this.jobTicketOptMapSheet,
                this.jobTicketCostRulesSheet);
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
     * Notifies a {@link #SP_JOBTICKET_SET_COST} line.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     */
    private void onSpJobTicketSetCost(final int lineNr, final String[] words) {
        this.onSpJobTicketCost(lineNr, words, this.jobTicketOptMap,
                this.jobTicketCostRulesSet);
    }

    /**
     * Notifies an SPJobTicket option.
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
        case SP_JOBTICKET_SET:
            this.onSpJobTicketSet(lineNr, nextwords);
            break;
        case SP_JOBTICKET_SET_COST:
            this.onSpJobTicketSetCost(lineNr, nextwords);
            break;
        case SP_JOBTICKET_SHEET:
            this.onSpJobTicketSheet(lineNr, nextwords);
            break;
        case SP_JOBTICKET_SHEET_COST:
            this.onSpJobTicketSheetCost(lineNr, nextwords);
            break;
        default:
            LOGGER.warn(String.format("%s line %d [%s] is NOT handled.",
                    this.getConfigFile().getName(), lineNr, firstword));
            break;
        }
    }

    /**
     * Notifies a {@link #SP_RULE_NUMBER_UP} line.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param words
     *            The words.
     */
    private void onSpRuleNumberUp(final int lineNr, final String[] words) {

        if (words.length < SP_RULE_NUMBER_UP_MIN_ARGS) {
            LOGGER.warn(String.format("%s line %d: incomplete %s",
                    getConfigFile().getName(), lineNr, SP_RULE_NUMBER_UP));
            return;
        }

        final String alias = words[0].trim();
        final IppRuleNumberUp rule = new IppRuleNumberUp(alias);

        boolean isLineValid = true;
        boolean isLandscapeOutput = false;

        for (final String attrChoice : ArrayUtils.removeAll(words, 0)) {

            final String[] splitWords = StringUtils.split(attrChoice,
                    SP_RULE_ATTR_CHOICE_SEPARATOR);

            final String attr = StringUtils
                    .stripStart(splitWords[0], SP_RULE_OPTION_DEPENDENT_PFX)
                    .toLowerCase();

            if (attr.equals(
                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_LANDSCAPE)) {
                isLandscapeOutput = true;
                continue;
            }

            if (splitWords.length != 2) {
                LOGGER.warn(String.format("%s line %d: \"%s\" syntax invalid",
                        getConfigFile().getName(), lineNr, attrChoice));
                isLineValid = false;
                continue;
            }

            final String choice = splitWords[1];

            boolean isChoiceValid = true;

            switch (attr) {

            case "pdf-orientation":
                rule.setLandscape(choice.equals("landscape"));
                break;

            case "pdf-rotation":
                isChoiceValid = NumberUtils.isDigits(choice);
                if (isChoiceValid) {
                    final Integer rotation = Integer.valueOf(choice);
                    if (PdfPageRotateHelper.isPdfRotationValid(rotation)) {
                        rule.setPdfRotation(rotation.intValue());
                    } else {
                        isChoiceValid = false;
                    }
                }
                break;

            case "pdf-content-rotation":
                isChoiceValid = NumberUtils.isDigits(choice);
                if (isChoiceValid) {
                    final Integer rotation = Integer.valueOf(choice);
                    if (PdfPageRotateHelper.isPdfRotationValid(rotation)) {
                        rule.setPdfContentRotation(rotation.intValue());
                    } else {
                        isChoiceValid = false;
                    }
                }
                break;

            case "user-rotate":
                isChoiceValid = NumberUtils.isDigits(choice);
                if (isChoiceValid) {
                    final Integer pdfRotation = Integer.valueOf(choice);
                    if (PdfPageRotateHelper.isPdfRotationValid(pdfRotation)) {
                        rule.setUserRotate(pdfRotation.intValue());
                    } else {
                        isChoiceValid = false;
                    }
                }
                break;

            case IppDictJobTemplateAttr.ATTR_NUMBER_UP:
                isChoiceValid = IppKeyword.checkNumberUp(choice);
                if (isChoiceValid) {
                    rule.setNumberUp(choice);
                }
                break;

            case IppDictJobTemplateAttr.CUPS_ATTR_ORIENTATION_REQUESTED:
                if (!choice.equals(SP_RULE_OPTION_CHOICE_NONE)) {
                    if (IppKeyword.checkOrientationRequested(choice)) {
                        rule.setOrientationRequested(choice);
                    } else {
                        isChoiceValid = false;
                    }
                }
                break;

            case IppDictJobTemplateAttr.CUPS_ATTR_NUMBER_UP_LAYOUT:
                if (!choice.equals(SP_RULE_OPTION_CHOICE_NONE)) {
                    if (IppKeyword.checkNumberUpLayout(choice)) {
                        rule.setNumberUpLayout(choice);
                    } else {
                        isChoiceValid = false;
                    }
                }
                break;

            default:
                LOGGER.warn(
                        String.format("%s line %d: attribute \"%s\" is unknown",
                                getConfigFile().getName(), lineNr, attr));
                isLineValid = false;
                continue;
            }

            if (!isChoiceValid) {
                LOGGER.warn(String.format(
                        "%s line %d: attribute/choice \"%s/%s\" is invalid",
                        getConfigFile().getName(), lineNr, attr, choice));
                isLineValid = false;
                continue;
            }
        }
        if (!isLineValid) {
            return;
        }

        rule.setLandscapePrint(isLandscapeOutput);
        this.numberUpRules.add(rule);
    }

    /**
     * Notifies an SPRule option.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param firstword
     *            The first word.
     * @param nextwords
     *            The next words.
     */
    private void onSpRule(final int lineNr, final String firstword,
            final String[] nextwords) {

        switch (firstword) {
        case SP_RULE_NUMBER_UP:
            this.onSpRuleNumberUp(lineNr, nextwords);
            break;
        default:
            LOGGER.warn(String.format("%s line %d [%s] is NOT handled.",
                    this.getConfigFile().getName(), lineNr, firstword));
            break;
        }
    }

    /**
     * Notifies an SPExtra option.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param firstword
     *            The first word.
     * @param nextwords
     *            The next words.
     */
    private void onSpExtra(final int lineNr, final String firstword,
            final String[] nextwords) {

        final String[] splitFirstWords =
                StringUtils.split(StringUtils.removeEnd(firstword, ":"),
                        SP_RULE_ATTR_CHOICE_SEPARATOR);

        if (splitFirstWords.length < 3
                || nextwords.length < SP_EXTRA_MIN_ARGS) {
            LOGGER.warn(String.format("%s line %d: incomplete %s",
                    getConfigFile().getName(), lineNr, SP_EXTRA_PFX));
            return;
        }

        final String alias = nextwords[0].trim();

        final IppRuleExtra rule = new IppRuleExtra(alias);

        rule.setMainIpp(new ImmutablePair<String, String>(splitFirstWords[1],
                splitFirstWords[2]));

        final List<Pair<String, String>> listIppExtra = new ArrayList<>();
        final Set<String> extraIppNegate = new HashSet<>();
        final List<Pair<String, String>> listPpdExtra = new ArrayList<>();

        boolean isLineValid = true;

        for (final String attrChoice : ArrayUtils.removeAll(nextwords, 0)) {

            final String[] splitWords = StringUtils.split(attrChoice,
                    SP_RULE_ATTR_CHOICE_SEPARATOR);

            final String attr = StringUtils.stripStart(splitWords[0],
                    SP_EXTRA_PPD_OPTION_PFX);

            if (splitWords.length != 2) {
                LOGGER.warn(String.format("%s line %d: \"%s\" syntax invalid",
                        getConfigFile().getName(), lineNr, attrChoice));
                isLineValid = false;
                continue;
            }

            final String ippChoiceRaw = splitWords[1];

            if (splitWords[0].startsWith(SP_EXTRA_PPD_OPTION_PFX)) {
                listPpdExtra.add(
                        new ImmutablePair<String, String>(attr, ippChoiceRaw));
            } else {
                final boolean isChoiceNegate =
                        ippChoiceRaw.startsWith(SP_EXTRA_ATTR_CHOICE_NEGATE);

                final String ippChoice;

                if (isChoiceNegate) {
                    ippChoice = StringUtils.substring(ippChoiceRaw, 1);
                    extraIppNegate.add(attr);
                } else {
                    ippChoice = ippChoiceRaw;
                }

                listIppExtra.add(
                        new ImmutablePair<String, String>(attr, ippChoice));
            }
        }

        if (!isLineValid) {
            return;
        }

        rule.setExtraIpp(listIppExtra);
        rule.setExtraIppNegate(extraIppNegate);
        rule.setExtraPPD(listPpdExtra);

        this.rulesExtra.add(rule);
    }

    /**
     * Notifies an SPSubst option.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param firstword
     *            The first word.
     * @param nextwords
     *            The next words.
     */
    private void onSpSubst(final int lineNr, final String firstword,
            final String[] nextwords) {
        final String[] splitFirstWords =
                StringUtils.split(StringUtils.removeEnd(firstword, ":"),
                        SP_RULE_ATTR_CHOICE_SEPARATOR);

        if (splitFirstWords.length < 3
                || nextwords.length < SP_SUBST_MIN_ARGS) {
            LOGGER.warn(String.format("%s line %d: incomplete %s",
                    getConfigFile().getName(), lineNr, SP_SUBST_PFX));
            return;
        }

        final String alias = nextwords[0].trim();

        final IppRuleSubst rule = new IppRuleSubst(alias);

        rule.setMainIpp(new ImmutablePair<String, String>(splitFirstWords[1],
                splitFirstWords[2]));

        rule.setPpdValue(null);

        final List<Pair<String, String>> listIppExtra = new ArrayList<>();
        final Set<String> extraIppNegate = new HashSet<>();

        boolean isLineValid = true;

        for (final String attrChoice : ArrayUtils.removeAll(nextwords, 0)) {

            if (attrChoice.startsWith(SP_SUBST_PPD_VALUE_PFX)) {

                if (rule.getPpdValue() == null) {
                    rule.setPpdValue(StringUtils.stripStart(attrChoice,
                            SP_SUBST_PPD_VALUE_PFX));
                } else {
                    isLineValid = false;
                }
                continue;
            }

            final String[] splitWords = StringUtils.split(attrChoice,
                    SP_RULE_ATTR_CHOICE_SEPARATOR);

            if (splitWords.length != 2) {
                LOGGER.warn(String.format("%s line %d: \"%s\" syntax invalid",
                        getConfigFile().getName(), lineNr, attrChoice));
                isLineValid = false;
                continue;
            }

            final String ippAttr = splitWords[0];
            final String ippChoiceRaw = splitWords[1];
            final boolean isChoiceNegate =
                    ippChoiceRaw.startsWith(SP_SUBST_ATTR_CHOICE_NEGATE);

            final String ippChoice;

            if (isChoiceNegate) {
                ippChoice = StringUtils.substring(ippChoiceRaw, 1);
                extraIppNegate.add(ippAttr);
            } else {
                ippChoice = ippChoiceRaw;
            }

            listIppExtra
                    .add(new ImmutablePair<String, String>(ippAttr, ippChoice));
        }

        if (!isLineValid) {
            return;
        }

        rule.setExtraIpp(listIppExtra);
        rule.setExtraIppNegate(extraIppNegate);

        this.rulesSubst.add(rule);
    }

    /**
     * Notifies {@link #PPD_ATTR_LANDSCAPE_ORIENTATION} attribute.
     *
     * @param lineNr
     *            The 1-based line number.
     * @param value
     *            Attribute value.
     */
    private void onAttrLandscapeOrientation(final int lineNr,
            final String value) {

        if (value.equalsIgnoreCase(PPD_ATTR_LANDSCAPE_ORIENTATION_MINUS90)) {
            ppdLandscapeMinus90 = Boolean.TRUE;
        } else if (value
                .equalsIgnoreCase(PPD_ATTR_LANDSCAPE_ORIENTATION_PLUS90)) {
            ppdLandscapeMinus90 = Boolean.FALSE;
        } else {
            logSyntaxError(lineNr, value);
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

    /**
     * Logs a syntax error.
     *
     * @param lineNr
     *            The line number.
     * @param firstWord
     *            The first word on the line.
     */
    private void logSyntaxError(final int lineNr, final String firstWord) {
        LOGGER.warn(String.format("%s line %d: [%s] syntax error.",
                this.getConfigFile().getName(), lineNr, firstWord));
    }

    @Override
    protected void onConfigLine(final int lineNr, final String strLine) {

        final String[] words = StringUtils.split(strLine);

        if (words.length == 0) {
            return;
        }

        final String firstWord = words[0].trim();

        if (firstWord.equalsIgnoreCase(PPD_ATTR_LANDSCAPE_ORIENTATION)) {
            if (words.length == 2) {
                this.onAttrLandscapeOrientation(lineNr, words[1]);
            } else {
                logSyntaxError(lineNr, firstWord);
            }
            return;
        }

        if (firstWord.startsWith(SP_JOBTICKET_PFX)) {
            if (words.length > 1) {
                this.onSpJobTicket(lineNr, firstWord,
                        ArrayUtils.remove(words, 0));
            } else {
                logSyntaxError(lineNr, firstWord);
            }
            return;
        }

        if (firstWord.startsWith(SP_RULE_PFX)) {
            if (words.length > 1) {
                this.onSpRule(lineNr, firstWord, ArrayUtils.remove(words, 0));
            } else {
                logSyntaxError(lineNr, firstWord);
            }
            return;
        }

        if (firstWord.startsWith(SP_EXTRA_PFX)) {
            if (words.length > 1) {
                this.onSpExtra(lineNr, firstWord, ArrayUtils.remove(words, 0));
            } else {
                logSyntaxError(lineNr, firstWord);
            }
            return;
        }

        if (firstWord.startsWith(SP_SUBST_PFX)) {
            if (words.length > 1) {
                this.onSpSubst(lineNr, firstWord, ArrayUtils.remove(words, 0));
            } else {
                logSyntaxError(lineNr, firstWord);
            }
            return;
        }

        if (firstWord.equals(SP_CONSTRAINT)) {
            if (words.length == SP_CONSTRAINT_WORDS) {
                this.onSpConstraint(lineNr, ArrayUtils.remove(words, 0));
            } else {
                logSyntaxError(lineNr, firstWord);
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

        boolean printScalingExt = false;

        // The mapped PPD options
        for (final JsonProxyPrinterOpt opt : reader.ppdOptionMap.values()) {

            final String keywordIpp = opt.getKeyword();

            final ProxyPrinterOptGroupEnum optGroupEnum;

            if (keywordIpp.startsWith(
                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_PFX_FINISHINGS)) {
                optGroupEnum = PROXYPRINT_SERVICE.getUiOptGroup(keywordIpp);
            } else if (IppDictJobTemplateAttr.isCustomAttr(keywordIpp)
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

            if (keywordIpp.equals(IppDictJobTemplateAttr.ATTR_PRINT_SCALING)) {
                printScalingExt = true;
            }

            if (keywordIpp.equals(IppDictJobTemplateAttr.ATTR_SHEET_COLLATE)) {

                proxyPrinter.setSheetCollated(opt
                        .getChoice(IppKeyword.SHEET_COLLATE_COLLATED) != null);

                proxyPrinter.setSheetUncollated(opt.getChoice(
                        IppKeyword.SHEET_COLLATE_UNCOLLATED) != null);
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

        // SpJobTicket Set/Copy/Sheet/Media options.
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
        for (final JsonProxyPrinterOpt opt : reader.jobTicketOptMapSet
                .values()) {
            optGroupJobTicket.getOptions().add(opt);
        }
        for (final JsonProxyPrinterOpt opt : reader.jobTicketOptMapSheet
                .values()) {
            optGroupJobTicket.getOptions().add(opt);
        }

        // Custom cost rules.
        proxyPrinter.setCustomCostRulesSet(reader.jobTicketCostRulesSet);
        proxyPrinter.setCustomCostRulesCopy(reader.jobTicketCostRulesCopy);
        proxyPrinter.setCustomCostRulesMedia(reader.jobTicketCostRulesMedia);
        proxyPrinter.setCustomCostRulesSheet(reader.jobTicketCostRulesSheet);

        // Other rules.
        proxyPrinter.setCustomNumberUpRules(reader.numberUpRules);
        proxyPrinter.setCustomRulesConstraint(reader.rulesConstraint);
        proxyPrinter.setCustomRulesExtra(reader.rulesExtra);
        proxyPrinter.setCustomRulesSubst(reader.rulesSubst);

        // PPD attributes
        if (reader.ppdLandscapeMinus90 != null) {
            proxyPrinter.setPpdLandscapeMinus90(
                    reader.ppdLandscapeMinus90.booleanValue());
        }

        //
        proxyPrinter.setInjectPpdExt(true);
        proxyPrinter.setPrintScalingExt(printScalingExt);
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
