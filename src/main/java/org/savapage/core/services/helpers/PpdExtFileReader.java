/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2016 Datraverse B.V.
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
package org.savapage.core.services.helpers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.print.proxy.JsonProxyPrinter;
import org.savapage.core.print.proxy.JsonProxyPrinterOpt;
import org.savapage.core.print.proxy.JsonProxyPrinterOptChoice;
import org.savapage.core.print.proxy.JsonProxyPrinterOptGroup;
import org.savapage.core.print.proxy.ProxyPrinterOptGroupEnum;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.AbstractConfigFileReader;
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

    private static final ProxyPrintService PROXYPRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();

    private static final String PPD_CHOICE_DEFAULT_PFX = "*";

    private static final String PPD_OPTION_PFX_CHAR = "*";

    private static final String PPD_UI_CONSTRAINT =
            PPD_OPTION_PFX_CHAR + "UIConstraints:";

    private static final int PPD_OPTION_CONSTANT_WORDS = 1;
    private static final int PPD_OPTION_MAPPING_WORDS = 2;
    private static final int PPD_OPTION_VALUE_MAPPING_WORDS = 3;
    private static final int PPD_UI_CONSTRAINT_WORDS = 5;

    /**
     * PPD Option as key to JsonProxyPrinterOpt with IPP mapping.
     */
    private Map<String, JsonProxyPrinterOpt> ppdOptionMap;

    /**
     *
     */
    private PpdExtFileReader() {
    }

    /**
     * Lazy creates a mapped {@link JsonProxyPrinterOpt}.
     *
     * @param ppdOption
     *            The PPD option name.
     * @return The {@link JsonProxyPrinterOpt}.
     */
    private JsonProxyPrinterOpt
            lazyCreateOptionMapping(final String ppdOption) {

        JsonProxyPrinterOpt opt = this.ppdOptionMap.get(ppdOption);

        if (opt == null) {
            opt = new JsonProxyPrinterOpt();
            this.ppdOptionMap.put(ppdOption, opt);
        }
        return opt;
    }

    /**
     * Notifies start-of-file.
     */
    @Override
    protected void onInit() {
        this.ppdOptionMap = new HashMap<>();
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
                this.lazyCreateOptionMapping(ppdKeyword);
        opt.setKeyword(ippKeyword);
        opt.setKeywordPpd(
                StringUtils.stripStart(ppdKeyword, PPD_OPTION_PFX_CHAR));
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

        final JsonProxyPrinterOpt opt = this.lazyCreateOptionMapping(ppdOption);

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
    protected void onConfigLine(final String strLine) {

        final String[] words = StringUtils.split(strLine);

        if (words.length == 0) {
            return;
        }

        final String firstWord = words[0].trim();

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

        final PpdExtFileReader reader = new PpdExtFileReader();
        reader.read(filePpdExt);

        final IppDictJobTemplateAttr ippDict =
                IppDictJobTemplateAttr.instance();

        final Map<String, JsonProxyPrinterOpt> optionsLookup =
                proxyPrinter.getOptionsLookup();

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
                        "File %s: IPP attribute [%s] is not supported.",
                        filePpdExt, keywordIpp));
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
