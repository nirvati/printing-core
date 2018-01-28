/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
 * Authors: Rijk Ravestein.
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
package org.savapage.core.ipp.rules;

import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;

/**
 * A rule to determine {@link IppDictJobTemplateAttr#CUPS_ATTR_NUMBER_UP_LAYOUT}
 * and {@link IppDictJobTemplateAttr#CUPS_ATTR_ORIENTATION_REQUESTED}.
 *
 * @author Rijk Ravestein
 *
 */
public final class IppRuleNumberUp {

    /**
     * The name of the rule.
     */
    private String name;

    // Independent variables.
    private boolean landscape;
    private int pdfRotation;
    private int pdfContentRotation;
    private int userRotate;
    private String numberUp;

    // Dependent variables.
    private String orientationRequested;
    private String numberUpLayout;
    private boolean landscapePrint;

    /**
     *
     * @param rule
     *            The name of the rule.
     */
    public IppRuleNumberUp(final String rule) {
        this.name = rule;
        this.pdfContentRotation = 0;
        this.userRotate = 0;
    }

    /**
     * Checks if a rule has same <i>independent</i> variables.
     *
     * @param rule
     *            The rule to check with.
     * @return {@code true} when rules have same input parameters.
     */
    public boolean isParameterMatch(final IppRuleNumberUp rule) {
        return this.landscape == rule.landscape
                && this.pdfRotation == rule.pdfRotation
                && this.pdfContentRotation == rule.pdfContentRotation
                && this.userRotate == rule.userRotate
                && this.numberUp.equals(rule.numberUp);
    }

    /**
     * Sets the <i>dependent</i> variables from source.
     *
     * @param source
     *            The source of the variables.
     */
    public void setDependentVars(final IppRuleNumberUp source) {
        this.numberUpLayout = source.numberUpLayout;
        this.orientationRequested = source.orientationRequested;
        this.landscapePrint = source.landscapePrint;
    }

    /**
     * Sets the <i>independent</i> variables from source.
     *
     * @param source
     *            The source of the variables.
     */
    public void setInDependentVars(final IppRuleNumberUp source) {
        this.landscape = source.landscape;
        this.pdfRotation = source.pdfRotation;
        this.pdfContentRotation = source.pdfContentRotation;
        this.userRotate = source.userRotate;
        this.numberUp = source.numberUp;
    }

    /**
     *
     * @return The identifying name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return {@code true} if orientation of input PDF is landscape.
     */
    public boolean isLandscape() {
        return landscape;
    }

    /**
     * @param landscape
     *            {@code true} if orientation of input PDF is landscape.
     */
    public void setLandscape(boolean landscape) {
        this.landscape = landscape;
    }

    public int getPdfRotation() {
        return pdfRotation;
    }

    public void setPdfRotation(int pdfRotation) {
        this.pdfRotation = pdfRotation;
    }

    public int getPdfContentRotation() {
        return pdfContentRotation;
    }

    public void setPdfContentRotation(int pdfContentRotation) {
        this.pdfContentRotation = pdfContentRotation;
    }

    public int getUserRotate() {
        return userRotate;
    }

    public void setUserRotate(int rotate) {
        this.userRotate = rotate;
    }

    public String getNumberUp() {
        return numberUp;
    }

    public void setNumberUp(String numberUp) {
        this.numberUp = numberUp;
    }

    public String getOrientationRequested() {
        return orientationRequested;
    }

    public void setOrientationRequested(String orientationRequested) {
        this.orientationRequested = orientationRequested;
    }

    public String getNumberUpLayout() {
        return numberUpLayout;
    }

    public void setNumberUpLayout(String numberUpLayout) {
        this.numberUpLayout = numberUpLayout;
    }

    /**
     * @return {@code true} if orientation of generated PDF for printing is
     *         landscape.
     */
    public boolean isLandscapePrint() {
        return landscapePrint;
    }

    /**
     * @param landscape
     *            {@code true} if orientation of generated PDF for printing is
     *            landscape.
     */
    public void setLandscapePrint(boolean landscape) {
        this.landscapePrint = landscape;
    }

    public void setName(String name) {
        this.name = name;
    }

}
