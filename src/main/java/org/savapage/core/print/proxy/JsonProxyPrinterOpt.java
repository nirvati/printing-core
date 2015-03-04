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
package org.savapage.core.print.proxy;

import java.util.ArrayList;

import org.savapage.core.jpa.PrinterAttr;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class JsonProxyPrinterOpt {

    private String keyword;

    /**
     * The default choice. Initially this choice is equal to
     * {@link #defchoiceIpp}, but may be overruled by a {@link PrinterAttr}
     * definition.
     */
    private String defchoice;

    /**
     * The original IPP default choice.
     */
    private String defchoiceIpp;

    private String text;
    private Integer ui;
    private Integer section;

    ArrayList<JsonProxyPrinterOptChoice> choices = new ArrayList<>();

    /**
     *
     * @return
     */
    public JsonProxyPrinterOpt copy() {

        final JsonProxyPrinterOpt copy = new JsonProxyPrinterOpt();

        copy.keyword = this.keyword;
        copy.defchoice = this.defchoice;
        copy.defchoiceIpp = this.defchoiceIpp;
        copy.text = this.text;
        copy.ui = this.ui;
        copy.section = this.section;

        for (final JsonProxyPrinterOptChoice choice : choices) {
            copy.choices.add(choice.copy());
        }

        return copy;
    }

    /**
     * Convenience method.
     *
     * @param choice
     * @param text
     */
    public void addChoice(final String choice, final String text) {

        final JsonProxyPrinterOptChoice ppdChoice =
                new JsonProxyPrinterOptChoice();

        ppdChoice.setChoice(choice);
        ppdChoice.setText(text);

        choices.add(ppdChoice);
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getDefchoice() {
        return defchoice;
    }

    public void setDefchoice(String defchoice) {
        this.defchoice = defchoice;
    }

    public String getDefchoiceIpp() {
        return defchoiceIpp;
    }

    public void setDefchoiceIpp(String defchoiceIpp) {
        this.defchoiceIpp = defchoiceIpp;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Integer getUi() {
        return ui;
    }

    public void setUi(Integer ui) {
        this.ui = ui;
    }

    public Integer getSection() {
        return section;
    }

    public void setSection(Integer section) {
        this.section = section;
    }

    public ArrayList<JsonProxyPrinterOptChoice> getChoices() {
        return choices;
    }

    public void setChoices(ArrayList<JsonProxyPrinterOptChoice> choices) {
        this.choices = choices;
    }
}
