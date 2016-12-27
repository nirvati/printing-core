/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
     * {@code true} when this option is for JobTicket specification.
     */
    @JsonIgnore
    private boolean jobTicket;

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
        copy.copyFrom(this);
        return copy;
    }

    /**
     * Copies the state from another option.
     *
     * @param opt
     *            The option to copy from.
     */
    public void copyFrom(final JsonProxyPrinterOpt opt) {

        this.keyword = opt.keyword;
        this.keywordPpd = opt.keywordPpd;
        this.defchoice = opt.defchoice;
        this.defchoiceIpp = opt.defchoiceIpp;
        this.uiText = opt.uiText;
        this.jobTicket = opt.jobTicket;

        this.choices = new ArrayList<>();

        for (final JsonProxyPrinterOptChoice choice : opt.choices) {
            this.choices.add(choice.copy());
        }
    }

    /**
     * Checks if a choice is present.
     *
     * @param choice
     *            The choice to check.
     * @return {@code true} when choice is present.
     */
    @JsonIgnore
    public boolean hasChoice(final String choice) {
        for (final JsonProxyPrinterOptChoice choiceWlk : this.choices) {
            if (choiceWlk.getChoice().equals(choice)) {
                return true;
            }
        }
        return false;
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
     *
     * @return {@code true} when this option is for JobTicket specification.
     */
    public boolean isJobTicket() {
        return jobTicket;
    }

    /**
     *
     * @param jobTicket
     *            {@code true} when this option is for JobTicket specification.
     */
    public void setJobTicket(boolean jobTicket) {
        this.jobTicket = jobTicket;
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
