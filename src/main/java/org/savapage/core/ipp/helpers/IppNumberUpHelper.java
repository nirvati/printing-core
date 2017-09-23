package org.savapage.core.ipp.helpers;

import java.util.ArrayList;
import java.util.List;

import org.savapage.core.dto.IppNumberUpRule;
import org.savapage.core.inbox.PdfOrientationInfo;
import org.savapage.core.ipp.attribute.syntax.IppKeyword;
import org.savapage.core.pdf.PdfPageRotateHelper;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class IppNumberUpHelper {

    /**
     * Rules for a handling number-up printing.
     */
    private final List<IppNumberUpRule> numberUpRules;

    /** */
    private static final class SingletonPageRotationHelper {
        /** */
        public static final IppNumberUpHelper INSTANCE =
                new IppNumberUpHelper();
    }

    /** Landscape. */
    private static final String _L_ = "L";
    /** Portrait. */
    private static final String _P_ = "P";

    private static final String C___0 =
            IppKeyword.ORIENTATION_REQUESTED_0_DEGREES;
    private static final String C__90 =
            IppKeyword.ORIENTATION_REQUESTED_90_DEGREES;
    private static final String C_180 =
            IppKeyword.ORIENTATION_REQUESTED_180_DEGREES;
    private static final String C_270 =
            IppKeyword.ORIENTATION_REQUESTED_270_DEGREES;

    private static final String BTLR = IppKeyword.NUMBER_UP_LAYOUT_BTLR;
    private static final String BTRL = IppKeyword.NUMBER_UP_LAYOUT_BTRL;
    private static final String LRBT = IppKeyword.NUMBER_UP_LAYOUT_LRBT;
    private static final String LRTB = IppKeyword.NUMBER_UP_LAYOUT_LRTB;
    private static final String RLBT = IppKeyword.NUMBER_UP_LAYOUT_RLBT;
    private static final String RLTB = IppKeyword.NUMBER_UP_LAYOUT_RLTB;
    private static final String TBLR = IppKeyword.NUMBER_UP_LAYOUT_TBLR;
    private static final String TBRL = IppKeyword.NUMBER_UP_LAYOUT_TBRL;

    private static final String P___0 =
            PdfPageRotateHelper.PDF_ROTATION_0.toString();
    private static final String P__90 =
            PdfPageRotateHelper.PDF_ROTATION_90.toString();
    private static final String P_180 =
            PdfPageRotateHelper.PDF_ROTATION_180.toString();
    private static final String P_270 =
            PdfPageRotateHelper.PDF_ROTATION_270.toString();

    private static final String U__0 = P___0;
    private static final String U_90 = P__90;

    private static final String N_1 = IppKeyword.NUMBER_UP_1;
    private static final String N_2 = IppKeyword.NUMBER_UP_2;
    private static final String N_4 = IppKeyword.NUMBER_UP_4;
    private static final String N_6 = IppKeyword.NUMBER_UP_6;
    private static final String N_9 = IppKeyword.NUMBER_UP_9;
    private static final String N_16 = IppKeyword.NUMBER_UP_16;

    final static int IDX_ORIENTATION = 0;
    final static int IDX_PDF_ROTATION = 1;
    final static int IDX_USER_ROTATE = 2;
    final static int IDX_N_UP = 3;
    final static int IDX_CUPS_ORIENTATION = 4;
    final static int IDX_CUPS_N_UP_LAYOUT = 5;
    final static int IDX_CUPS_N_UP_ORIENTATION = 6;

    private static final String ____ = null;
    private static final String _____ = null;

    /**
     * Internal rules as tested with CUPS "Generic PostScript Printer".
     */
    private static final String[][] RULES = { //
            /*
             * Portrait.
             */
            { _P_, P___0, U__0, N_1, _____, ____, _P_ }, // OK
            { _P_, P___0, U__0, N_2, C_180, TBRL, _L_ }, // OK
            { _P_, P___0, U__0, N_4, _____, TBRL, _P_ }, // OK
            { _P_, P___0, U__0, N_6, C_180, TBRL, _L_ }, // OK

            { _P_, P___0, U_90, N_1, _____, ____ }, //
            { _P_, P___0, U_90, N_2, C_270, TBRL }, //
            { _P_, P___0, U_90, N_4, _____, TBRL }, //
            { _P_, P___0, U_90, N_6, C_270, LRTB }, //

            { _P_, P__90, U__0, N_1, C_180, ____, _P_ }, // OK, -Ricoh
            { _P_, P__90, U__0, N_2, C_270, TBRL, _P_ }, // OK, -Xerox
            { _P_, P__90, U__0, N_4, C_180, BTLR, _P_ }, // OK, -Ricoh
            { _P_, P__90, U__0, N_6, C_270, LRTB, _P_ }, // OK, -Xerox

            { _P_, P__90, U_90, N_1, _____, ____ }, //
            { _P_, P__90, U_90, N_2, C_270, TBRL }, //
            { _P_, P__90, U_90, N_4, _____, TBRL }, //
            { _P_, P__90, U_90, N_6, C_270, LRTB }, //

            { _P_, P_180, U__0, N_1, C_180, ____ }, //
            { _P_, P_180, U__0, N_2, C_270, TBRL }, //
            { _P_, P_180, U__0, N_4, C_180, BTLR }, //
            { _P_, P_180, U__0, N_6, C_270, LRTB }, //

            { _P_, P_180, U_90, N_1, _____, ____ }, //
            { _P_, P_180, U_90, N_2, C_180, TBRL }, //
            { _P_, P_180, U_90, N_4, _____, TBRL }, //
            { _P_, P_180, U_90, N_6, C_180, TBRL }, //

            { _P_, P_270, U__0, N_1, _____, ____ }, //
            { _P_, P_270, U__0, N_2, C_180, TBRL }, //
            { _P_, P_270, U__0, N_4, _____, TBRL }, //
            { _P_, P_270, U__0, N_6, C_180, TBRL }, //

            { _P_, P_270, U_90, N_1, _____, ____ }, //
            { _P_, P_270, U_90, N_2, C_180, TBRL }, //
            { _P_, P_270, U_90, N_4, _____, TBRL }, //
            { _P_, P_270, U_90, N_6, C_180, TBRL }, //

            /*
             * Landscape.
             */
            { _L_, P___0, U__0, N_1, C_270, ____, _P_ }, // OK
            { _L_, P___0, U__0, N_2, C_270, TBRL, _P_ }, // OK, -Xerox
            { _L_, P___0, U__0, N_4, C_270, LRTB, _P_ }, // OK
            { _L_, P___0, U__0, N_6, C_270, LRTB, _P_ }, // OK, -Xerox

            { _L_, P___0, U_90, N_1, C_270, ____ }, //
            { _L_, P___0, U_90, N_2, C_270, TBRL }, //
            { _L_, P___0, U_90, N_4, C_270, LRTB }, //
            { _L_, P___0, U_90, N_6, C_270, LRTB }, //

            { _L_, P__90, U__0, N_1, C_270, ____ }, //
            { _L_, P__90, U__0, N_2, C_270, TBRL }, //
            { _L_, P__90, U__0, N_4, C_270, LRTB }, //
            { _L_, P__90, U__0, N_6, C_270, LRTB }, //

            { _L_, P__90, U_90, N_1, _____, ____ }, //
            { _L_, P__90, U_90, N_2, C_180, TBRL }, //
            { _L_, P__90, U_90, N_4, _____, TBRL }, //
            { _L_, P__90, U_90, N_6, C_180, TBRL }, //

            { _L_, P_180, U__0, N_1, _____, ____ }, //
            { _L_, P_180, U__0, N_2, C_180, TBRL }, //
            { _L_, P_180, U__0, N_4, _____, TBRL }, //
            { _L_, P_180, U__0, N_6, C_180, TBRL }, //

            { _L_, P_180, U_90, N_1, _____, ____ }, //
            { _L_, P_180, U_90, N_2, C_180, TBRL }, //
            { _L_, P_180, U_90, N_4, _____, TBRL }, //
            { _L_, P_180, U_90, N_6, C_180, TBRL }, //

            // When driver printing a landscape oriented document.
            { _L_, P_270, U__0, N_1, _____, ____, _P_ }, // OK
            { _L_, P_270, U__0, N_2, _____, TBLR, _P_ }, // OK
            { _L_, P_270, U__0, N_4, C__90, LRTB, _L_ }, // OK
            { _L_, P_270, U__0, N_6, _____, BTLR, _P_ }, // OK

            { _L_, P_270, U_90, N_1, _____, ____, _L_ }, // OK
            { _L_, P_270, U_90, N_2, _____, TBRL, _P_ }, // OK
            { _L_, P_270, U_90, N_4, _____, TBRL, _L_ }, // OK
            { _L_, P_270, U_90, N_6, _____, TBRL, _P_ }, // OK
    };

    /** */
    private IppNumberUpHelper() {
        this.numberUpRules = createRuleList(RULES);
    }

    /**
     *
     * @param ruleArray
     * @return
     */
    private static List<IppNumberUpRule>
            createRuleList(final String[][] ruleArray) {

        final List<IppNumberUpRule> numberUpRules = new ArrayList<>();

        for (final String[] wlk : ruleArray) {
            final IppNumberUpRule rule = new IppNumberUpRule("internal");

            rule.setLandscape(wlk[IDX_ORIENTATION].equals(_L_));
            rule.setNumberUp(wlk[IDX_N_UP]);
            rule.setPdfRotation(Integer.parseInt(wlk[IDX_PDF_ROTATION]));
            rule.setUserRotate(Integer.parseInt(wlk[IDX_USER_ROTATE]));

            rule.setNumberUpLayout(wlk[IDX_CUPS_N_UP_LAYOUT]);
            rule.setOrientationRequested(wlk[IDX_CUPS_ORIENTATION]);

            rule.setLandscapePrint(wlk.length > IDX_CUPS_N_UP_ORIENTATION
                    && wlk[IDX_CUPS_N_UP_ORIENTATION].equals(_L_));

            numberUpRules.add(rule);
        }
        return numberUpRules;
    }

    /**
     *
     * @return The singleton instance.
     */
    public static IppNumberUpHelper instance() {
        return SingletonPageRotationHelper.INSTANCE;
    }

    /**
     *
     * @param template
     *            The template rule with <i>independent</i> variables.
     * @return The template rule object supplemented with <i>dependent</i>
     *         variables, or {@code null} when no rule found.
     */
    public IppNumberUpRule findCustomRuleTest(final IppNumberUpRule template) {
        final String[][] testRules = {};

        if (testRules.length == 0) {
            return null;
        }
        return findCustomRule(createRuleList(testRules), template);
    }

    /**
     * Finds a matching {@link IppNumberUpRule} for a template rule.
     *
     * @param template
     *            The template rule with <i>independent</i> variables.
     * @return The template rule object supplemented with <i>dependent</i>
     *         variables, or {@code null} when no rule found.
     */
    public IppNumberUpRule findCustomRule(final IppNumberUpRule template) {
        final IppNumberUpRule test = findCustomRuleTest(template);
        if (test != null) {
            return test;
        }
        return findCustomRule(this.numberUpRules, template);
    }

    /**
     * Finds a matching {@link IppNumberUpRule} for a template rule.
     *
     * @param numberUpRules
     *            The list of rules.
     * @param template
     *            The template rule with <i>independent</i> variables.
     * @return The template rule object supplemented with <i>dependent</i>
     *         variables, or {@code null} when no rule found.
     */
    private static IppNumberUpRule findCustomRule(
            final List<IppNumberUpRule> numberUpRules,
            final IppNumberUpRule template) {

        IppNumberUpRule rule = null;

        final String savedNup = template.getNumberUp();

        if (template.getNumberUp().equals(N_9)
                || template.getNumberUp().equals(N_16)) {
            template.setNumberUp(N_4);
        }

        for (final IppNumberUpRule wlk : numberUpRules) {
            if (template.isParameterMatch(wlk)) {
                template.setDependentVars(wlk);
                rule = template;
                break;
            }
        }

        template.setNumberUp(savedNup);
        return rule;
    }

    /**
     * @deprecated
     * @param pdfOrientation
     * @param numberUp
     * @param templateRule
     */
    @Deprecated
    public static void fillRuleTemplate(final PdfOrientationInfo pdfOrientation,
            final String numberUp, final IppNumberUpRule templateRule) {

        final String cupsOrientationRequested;
        final String cupsNupLayout;

        final PdfPageRotateHelper rotateHelper = PdfPageRotateHelper.instance();

        final Integer pdfRotationForPrint = rotateHelper
                .getPageRotationForPrinting(pdfOrientation.getLandscape(),
                        pdfOrientation.getRotation(),
                        pdfOrientation.getRotate());

        /*
         * A portrait PDF document without PDF rotation needed, does not need
         * correction.
         */
        if (!pdfOrientation.getLandscape() && pdfRotationForPrint
                .equals(PdfPageRotateHelper.PDF_ROTATION_0)) {
            templateRule.setOrientationRequested(null);
            templateRule.setNumberUpLayout(null);
            templateRule.setLandscapePrint(false);
            return;
        }

        final boolean landscapePrint;

        switch (numberUp) {
        case IppKeyword.NUMBER_UP_1:

            landscapePrint = true;
            cupsNupLayout = null;

            if (pdfRotationForPrint
                    .equals(PdfPageRotateHelper.PDF_ROTATION_90)) {

                /*
                 * Landscape -> Landscape reverse
                 */
                cupsOrientationRequested =
                        IppKeyword.ORIENTATION_REQUESTED_270_DEGREES;

            } else if (pdfRotationForPrint
                    .equals(PdfPageRotateHelper.PDF_ROTATION_180)) {

                if (!pdfOrientation.getLandscape() && pdfOrientation.getRotate()
                        .equals(PdfPageRotateHelper.PDF_ROTATION_90)) {
                    cupsOrientationRequested = null;
                } else {
                    cupsOrientationRequested =
                            IppKeyword.ORIENTATION_REQUESTED_180_DEGREES;
                }
            } else {
                cupsOrientationRequested = null;
            }
            break;

        /*
         * 4-up, 9-up and 16-up give result in logical landscape orientation.
         */
        case IppKeyword.NUMBER_UP_4:
        case IppKeyword.NUMBER_UP_9:
        case IppKeyword.NUMBER_UP_16:

            landscapePrint = true;

            if (pdfRotationForPrint
                    .equals(PdfPageRotateHelper.PDF_ROTATION_90)) {
                /*
                 * Landscape -> Landscape reverse
                 */
                cupsOrientationRequested =
                        IppKeyword.ORIENTATION_REQUESTED_270_DEGREES;

                cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_LRTB;

            } else if (pdfRotationForPrint
                    .equals(PdfPageRotateHelper.PDF_ROTATION_180)) {

                if (!pdfOrientation.getLandscape() && pdfOrientation.getRotate()
                        .equals(PdfPageRotateHelper.PDF_ROTATION_90)) {

                    cupsOrientationRequested = null;
                    cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_TBRL;

                } else {
                    /*
                     * Portrait -> Portrait reverse
                     */
                    cupsOrientationRequested =
                            IppKeyword.ORIENTATION_REQUESTED_180_DEGREES;
                    cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_BTLR;
                }
            } else {

                cupsOrientationRequested = null;

                if (pdfOrientation.getLandscape()
                        && pdfOrientation.getRotation()
                                .equals(PdfPageRotateHelper.PDF_ROTATION_270)) {

                    cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_LRTB;

                } else {
                    //
                    // LRTB (default)..... RLTB................TBRL
                    // (preferred)
                    // +----+----S ....... +----+----S ....... +----+----S
                    // |.*..|.*..| ....... |.*..|.*..| ....... |.*..|.*..|
                    // |.*1.|.*2.| ....... |.*2.|.*1.| ....... |.*3.|.*1.|
                    // |.*..|.*..|.........|.*..|.*..|.........|.*..|.*..|
                    // |----+----|........ |----+----|........ |----+----|
                    // |.*..|.*..|........ |.*..|.*..|........ |.*..|.*..|
                    // |.*3.|.*4.|........ |.*4.|.*3.|........ |.*4.|.*2.|
                    // |.*..|.*..|........ |.*..|.*..|........ |.*..|.*..|
                    // +----+----+........ +----+----+........ +----+----+
                    //
                    cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_TBRL;
                }
            }
            break;

        /*
         * 2-up and 6-up result in portrait orientation.
         */
        case IppKeyword.NUMBER_UP_2:

            landscapePrint = false;

            if (pdfRotationForPrint
                    .equals(PdfPageRotateHelper.PDF_ROTATION_90)) {
                /*
                 * Landscape -> Portrait
                 */
                cupsOrientationRequested =
                        IppKeyword.ORIENTATION_REQUESTED_270_DEGREES;
                cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_TBRL;

            } else if (pdfRotationForPrint
                    .equals(PdfPageRotateHelper.PDF_ROTATION_180)) {
                /*
                 * Portrait -> Landscape reverse
                 */
                cupsOrientationRequested =
                        IppKeyword.ORIENTATION_REQUESTED_270_DEGREES;
                cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_TBRL;

            } else {

                if (pdfOrientation.getLandscape()
                        && pdfOrientation.getRotation()
                                .equals(PdfPageRotateHelper.PDF_ROTATION_270)) {
                    cupsOrientationRequested = null;
                    cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_TBLR;
                } else {
                    cupsOrientationRequested =
                            IppKeyword.ORIENTATION_REQUESTED_180_DEGREES;
                    cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_TBRL;
                }
            }
            break;

        case IppKeyword.NUMBER_UP_6:

            landscapePrint = false;

            if (pdfRotationForPrint
                    .equals(PdfPageRotateHelper.PDF_ROTATION_90)) {
                /*
                 * Landscape -> Portrait
                 */
                cupsOrientationRequested =
                        IppKeyword.ORIENTATION_REQUESTED_270_DEGREES;
                cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_LRTB;

            } else if (pdfRotationForPrint
                    .equals(PdfPageRotateHelper.PDF_ROTATION_180)) {
                /*
                 * Portrait -> Landscape reverse
                 */
                cupsOrientationRequested =
                        IppKeyword.ORIENTATION_REQUESTED_270_DEGREES;
                cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_LRTB;

            } else {
                if (pdfOrientation.getLandscape()
                        && pdfOrientation.getRotation()
                                .equals(PdfPageRotateHelper.PDF_ROTATION_270)) {
                    cupsOrientationRequested = null;
                    cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_LRTB;
                } else {
                    cupsOrientationRequested =
                            IppKeyword.ORIENTATION_REQUESTED_180_DEGREES;
                    cupsNupLayout = IppKeyword.NUMBER_UP_LAYOUT_TBRL;
                }
            }
            break;

        default:
            cupsNupLayout = null;
            cupsOrientationRequested = null;
            landscapePrint = false;
            break;
        }

        templateRule.setOrientationRequested(cupsOrientationRequested);
        templateRule.setNumberUpLayout(cupsNupLayout);
        templateRule.setLandscapePrint(landscapePrint);
    }
}
