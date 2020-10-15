/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2020 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * SPDX-FileCopyrightText: © 2020 Datraverse B.V. <info@datraverse.com>
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
package org.savapage.core.print.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class JsonProxyPrinterOptChoice {

    /**
     * The IPP choice.
     */
    @JsonProperty("choice")
    private String choice;

    /**
     * {@code true} when this is an extended option.
     */
    private boolean extended;

    /**
     * The PPD choice (can be {@code null} when neither present nor relevant).
     */
    @JsonIgnore
    private String choicePpd;

    /**
     * The UI text.
     */
    @JsonProperty("uiText")
    private String uiText;

    /**
     * The UI CSS class of the icon (can be {@code null}).
     */
    @JsonProperty("uiIconClass")
    private String uiIconClass;

    /**
     *
     * @return The IPP choice.
     */
    public String getChoice() {
        return choice;
    }

    /**
     * @param choice
     *            The IPP choice.
     */
    public void setChoice(final String choice) {
        this.choice = choice;
    }

    /**
     * @return {@code true} when this is an extended option.
     */
    public boolean isExtended() {
        return extended;
    }

    /**
     * @param extended
     *            {@code true} when this is an extended option
     */
    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    /**
     * @return The PPD choice (can be {@code null} when neither present nor
     *         relevant).
     */
    public String getChoicePpd() {
        return choicePpd;
    }

    /**
     * @param choicePpd
     *            The PPD choice (can be {@code null} when neither present nor
     *            relevant).
     */
    public void setChoicePpd(String choicePpd) {
        this.choicePpd = choicePpd;
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
    public void setUiText(final String text) {
        this.uiText = text;
    }

    /**
     * @return The UI CSS class of the icon (can be {@code null}).
     */
    public String getUiIconClass() {
        return uiIconClass;
    }

    /**
     * @param uiIconClass
     *            The UI CSS class of the icon (can be {@code null}).
     */
    public void setUiIconClass(String uiIconClass) {
        this.uiIconClass = uiIconClass;
    }

    /**
     *
     * @return A copy of this instance.
     */
    public JsonProxyPrinterOptChoice copy() {

        final JsonProxyPrinterOptChoice copy = new JsonProxyPrinterOptChoice();

        copy.choice = this.choice;
        copy.extended = this.extended;
        copy.choicePpd = this.choicePpd;
        copy.uiText = this.uiText;
        copy.uiIconClass = this.uiIconClass;

        return copy;
    }
}
