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
package org.savapage.core.ipp.helpers;

import org.savapage.core.inbox.PdfOrientationInfo;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.ipp.rules.IppRuleNumberUp;
import org.savapage.core.pdf.PdfPageRotateHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppNumberUpResolver implements IppNumberUpRuleFinder {

    private static final String PLUS__90 = "+";
    private static final String MINUS_90 = "-";

    /** Landscape. */
    private static final String _L_ = "L";
    /** Portrait. */
    private static final String _P_ = "P";

    private static final String N_1 = IppKeyword.NUMBER_UP_1;
    private static final String N_2 = IppKeyword.NUMBER_UP_2;
    private static final String N_4 = IppKeyword.NUMBER_UP_4;
    private static final String N_6 = IppKeyword.NUMBER_UP_6;
    private static final String N_9 = IppKeyword.NUMBER_UP_9;
    private static final String N_16 = IppKeyword.NUMBER_UP_16;

    @SuppressWarnings("unused")
    private static final String C___0 =
            IppKeyword.ORIENTATION_REQUESTED_0_DEGREES;
    private static final String C__90 =
            IppKeyword.ORIENTATION_REQUESTED_90_DEGREES;
    private static final String C_180 =
            IppKeyword.ORIENTATION_REQUESTED_180_DEGREES;
    private static final String C_270 =
            IppKeyword.ORIENTATION_REQUESTED_270_DEGREES;

    private static final String ____ = null;
    private static final String _____ = null;

    private static final String LRTB = IppKeyword.NUMBER_UP_LAYOUT_LRTB;

    //
    private static final int I_ORIENTATION_PRINTER = 0;
    private static final int I_ORIENTATION_VIEWED = 1;
    private static final int I_N_UP = 2;
    private static final int I_ORIENTATION_REQ = 3;
    private static final int I_N_UP_LAYOUT = 4;
    private static final int I_ORIENTATION_SHEET = 5;

    /**
     * Number-up rules for viewed pages.
     */
    private static final String[][] N_UP_RULES = { //

            // -------------------------------------------------
            // *LandscapeOrientation: Plus90 (Generic)
            // -------------------------------------------------
            { PLUS__90, _P_, N_1, _____, ____, _P_ }, // OK
            { PLUS__90, _P_, N_2, C_180, LRTB, _L_ }, //
            { PLUS__90, _P_, N_4, _____, LRTB, _P_ }, // OK
            { PLUS__90, _P_, N_6, C_180, LRTB, _L_ }, // OK

            { PLUS__90, _L_, N_1, C_270, ____, _L_ }, //
            { PLUS__90, _L_, N_2, C__90, LRTB, _P_ }, //
            { PLUS__90, _L_, N_4, C_270, LRTB, _L_ }, //
            { PLUS__90, _L_, N_6, C__90, LRTB, _P_ }, //

            // -------------------------------------------------
            // *LandscapeOrientation: Minus90 (Ricoh)
            // -------------------------------------------------
            { MINUS_90, _P_, N_1, _____, ____, _P_ }, // OK
            { MINUS_90, _P_, N_2, _____, LRTB, _L_ }, // OK
            { MINUS_90, _P_, N_4, _____, LRTB, _P_ }, // OK
            { MINUS_90, _P_, N_6, _____, LRTB, _L_ }, // OK

            { MINUS_90, _L_, N_1, _____, ____, _L_ }, //
            { MINUS_90, _L_, N_2, C_270, LRTB, _P_ }, //
            { MINUS_90, _L_, N_4, C_270, LRTB, _L_ }, //
            { MINUS_90, _L_, N_6, C_270, LRTB, _P_ }, //
    };

    /**
     * Number-up rules for viewed pages.
     */
    private static final String[][] N_UP_RULES_ROTATE180 = { //

            // -------------------------------------------------
            // *LandscapeOrientation: Plus90 (Generic)
            // -------------------------------------------------
            { PLUS__90, _P_, N_1, C_180, ____, _P_ }, // OK
            { PLUS__90, _P_, N_2, _____, LRTB, _L_ }, //
            { PLUS__90, _P_, N_4, C_180, LRTB, _P_ }, // OK
            { PLUS__90, _P_, N_6, _____, LRTB, _L_ }, // OK

            { PLUS__90, _L_, N_1, C__90, ____, _L_ }, //
            { PLUS__90, _L_, N_2, _____, LRTB, _P_ }, //
            { PLUS__90, _L_, N_4, C__90, LRTB, _L_ }, //
            { PLUS__90, _L_, N_6, C_270, LRTB, _P_ }, //

            // -------------------------------------------------
            // *LandscapeOrientation: Minus90 (Ricoh)
            // -------------------------------------------------
            { MINUS_90, _P_, N_1, C_180, ____, _P_ }, // OK
            { MINUS_90, _P_, N_2, C_180, LRTB, _L_ }, // OK
            { MINUS_90, _P_, N_4, C_180, LRTB, _P_ }, // OK
            { MINUS_90, _P_, N_6, C_180, LRTB, _L_ }, // OK

            { MINUS_90, _L_, N_1, C_180, ____, _L_ }, //
            { MINUS_90, _L_, N_2, C__90, LRTB, _P_ }, //
            { MINUS_90, _L_, N_4, C__90, LRTB, _L_ }, //
            { MINUS_90, _L_, N_6, C__90, LRTB, _P_ }, //
    };

    @Override
    public IppRuleNumberUp findNumberUpRule(final boolean landscapeMinus90,
            final IppRuleNumberUp template) {

        //
        final String orientationPrinter;

        if (landscapeMinus90) {
            orientationPrinter = MINUS_90;
        } else {
            orientationPrinter = PLUS__90;
        }

        //
        final PdfOrientationInfo info = new PdfOrientationInfo();

        info.setContentRotation(template.getPdfContentRotation());
        info.setLandscape(template.isLandscape());
        info.setRotate(template.getUserRotate());
        info.setRotation(template.getPdfRotation());

        final String orientationViewed;

        if (PdfPageRotateHelper.isSeenAsLandscape(info)) {
            orientationViewed = _L_;
        } else {
            orientationViewed = _P_;
        }

        //
        final String numberUp;

        if (template.getNumberUp().equals(N_9)
                || template.getNumberUp().equals(N_16)) {
            numberUp = N_4;
        } else {
            numberUp = template.getNumberUp();
        }

        //
        final String[][] numberUpRules;

        if (template.isFinishedPageRotate180()) {
            numberUpRules = N_UP_RULES_ROTATE180;
        } else {
            numberUpRules = N_UP_RULES;
        }

        //
        for (final String[] line : numberUpRules) {

            if (line[I_ORIENTATION_PRINTER].equals(orientationPrinter)
                    && line[I_ORIENTATION_VIEWED].equals(orientationViewed)
                    && line[I_N_UP].equals(numberUp)) {

                final IppRuleNumberUp rule = new IppRuleNumberUp("");

                rule.setInDependentVars(template);
                rule.setOrientationRequested(line[I_ORIENTATION_REQ]);
                rule.setNumberUpLayout(line[I_N_UP_LAYOUT]);
                rule.setLandscapePrint(line[I_ORIENTATION_SHEET].equals(_L_));

                return rule;
            }
        }

        return null;
    }

}
