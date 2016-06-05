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
package org.savapage.core.print.proxy;

import java.util.ArrayList;

import org.savapage.core.jpa.PrinterAttr;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JsonProxyPrinterOpt {

    /**
     * The IPP attribute keyword.
     */
    @JsonProperty("keyword")
    private String keyword;

    /**
     * The PPD option keyword (can be {@code null} when neither present nor
     * relevant).
     */
    @JsonIgnore
    private String keywordPpd;

    /**
     * The default IPP choice. Initially this choice is equal to
     * {@link #defchoiceIpp}, but may be overruled by a {@link PrinterAttr}
     * definition.
     */
    @JsonProperty("defchoice")
    private String defchoice;

    /**
     * The original IPP default choice.
     */
    @JsonProperty("defchoiceIpp")
    private String defchoiceIpp;

    /**
     * The UI text.
     */
    @JsonProperty("uiText")
    private String uiText;

    /**
     * The choices of this option.
     */
    private ArrayList<JsonProxyPrinterOptChoice> choices = new ArrayList<>();

    /**
     * @return a copy of this object.
     */
    public JsonProxyPrinterOpt copy() {

        final JsonProxyPrinterOpt copy = new JsonProxyPrinterOpt();

        copy.keyword = this.keyword;
        copy.keywordPpd = this.keywordPpd;
        copy.defchoice = this.defchoice;
        copy.defchoiceIpp = this.defchoiceIpp;
        copy.uiText = this.uiText;

        for (final JsonProxyPrinterOptChoice choice : choices) {
            copy.choices.add(choice.copy());
        }

        return copy;
    }

    /**
     * Convenience method.
     *
     * @param choice
     *            The IPP choice value.
     * @param uiTtext
     *            The UI text.
     * @return The added {@link JsonProxyPrinterOptChoice}.
     */
    public JsonProxyPrinterOptChoice addChoice(final String choice,
            final String uiTtext) {

        final JsonProxyPrinterOptChoice optChoice =
                new JsonProxyPrinterOptChoice();

        optChoice.setChoice(choice);
        optChoice.setUiText(uiTtext);

        choices.add(optChoice);
        return optChoice;
    }

    /**
     *
     * @return The IPP attribute keyword.
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     *
     * @param keyword
     *            The IPP attribute keyword.
     */
    public void setKeyword(final String keyword) {
        this.keyword = keyword;
    }

    /**
     * @return The PPD option keyword (can be {@code null} when neither present
     *         nor relevant).
     */
    public String getKeywordPpd() {
        return keywordPpd;
    }

    /**
     * @param keywordPpd
     *            The PPD option keyword (can be {@code null} when neither
     *            present nor relevant).
     */
    public void setKeywordPpd(String keywordPpd) {
        this.keywordPpd = keywordPpd;
    }

    /**
     * @return The effective IPP default choice.
     */
    public String getDefchoice() {
        return defchoice;
    }

    /**
     * @param defchoice
     *            The effective IPP default choice.
     */
    public void setDefchoice(final String defchoice) {
        this.defchoice = defchoice;
    }

    /**
     * @return The original IPP default choice.
     */
    public String getDefchoiceIpp() {
        return defchoiceIpp;
    }

    /**
     * @param defchoiceIpp
     *            The original IPP default choice.
     */
    public void setDefchoiceIpp(final String defchoiceIpp) {
        this.defchoiceIpp = defchoiceIpp;
    }

    /**
     * @return The UI text.
     */
    public String getUiText() {
        return uiText;
    }

    /**
     * @param text
     *            The UI text.
     */
    public void setUiText(String text) {
        this.uiText = text;
    }

    /**
     * @return The choices of this option.
     */
    public ArrayList<JsonProxyPrinterOptChoice> getChoices() {
        return choices;
    }

    /**
     * @param choices
     *            The choices of this option.
     */
    public void setChoices(final ArrayList<JsonProxyPrinterOptChoice> choices) {
        this.choices = choices;
    }
}
