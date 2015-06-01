/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2014 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.reports;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignExpression;
import net.sf.jasperreports.engine.design.JRDesignImage;
import net.sf.jasperreports.engine.design.JRDesignLine;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dto.JrPageLayoutDto;
import org.savapage.core.dto.JrPageSizeDto;
import org.savapage.core.dto.PosDepositReceiptDto;
import org.savapage.core.fonts.FontLocation;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.CurrencyUtil;
import org.savapage.core.util.LocaleHelper;

/**
 * Jasper Reports design for Point-of-Sale Receipt.
 *
 * @author Datraverse B.V.
 *
 */
public final class JrPosDepositReceipt extends AbstractJrDesign {

    private static final int PAGE_MARGIN_LEFT = 47;
    private static final int PAGE_MARGIN_RIGHT = PAGE_MARGIN_LEFT;
    private static final int PAGE_MARGIN_TOP = 28;
    private static final int PAGE_MARGIN_BOTTOM = PAGE_MARGIN_TOP;

    /**
     * {@link java.net.URL}
     */
    protected static final String PARM_REPORT_IMAGE = "SP_REPORT_IMAGE";

    /**
     * {@link String}.
     */
    protected static final String PARM_APP_VERSION = "SP_APP_VERSION";

    /**
     * {@link java.util.Date}.
     */
    protected static final String PARM_REPORT_DATE = "SP_REPORT_DATE";

    /**
     * {@link String}.
     */
    protected static final String PARM_REPORT_ACTOR = "SP_REPORT_ACTOR";

    /**
     * {@link String}.
     */
    private static final String PARM_RECEIPT_REF_NUMBER = "RCPT_REF_NUMBER";

    /**
     * {@link java.util.Date}.
     */
    private static final String PARM_RECEIPT_DATE = "RCPT_DATE";

    /**
     * {@link String}.
     */
    private static final String PARM_RECEIPT_USERNAME = "RCPT_USERNAME";

    /**
     * {@link String}.
     */
    private static final String PARM_RECEIPT_AMOUNT = "RCPT_AMOUNT";

    /**
     * {@link String}.
     */
    private static final String PARM_RECEIPT_PAYMENT_METHOD =
            "RCPT_PAYMENT_METHOD";

    /**
     * {@link String}.
     */
    private static final String PARM_RECEIPT_CASHIER = "RCPT_RECEIPT_CASHIER";

    /**
     * {@link String}.
     */
    private static final String PARM_RECEIPT_COMMENT = "RCPT_COMMENT";

    /**
     *
     */
    private final JrPageLayoutDto layout;

    /**
     *
     * @param layout
     */
    private JrPosDepositReceipt(JrPageLayoutDto layout, Locale locale) {
        super(locale);
        this.layout = layout;
    }

    /**
     * Gets the report parameters.
     *
     * @param receipt
     * @param locale
     * @return
     * @throws ParseException
     */
    public static Map<String, Object> getParameters(
            final PosDepositReceiptDto receipt, Locale locale) {

        final LocaleHelper helper = new LocaleHelper(locale);

        final Map<String, Object> parms = new HashMap<>();

        parms.put("REPORT_LOCALE", locale);

        parms.put("REPORT_RESOURCE_BUNDLE",
                ResourceBundle.getBundle(getResourceBundleBaseName(), locale));

        parms.put(PARM_APP_VERSION, ConfigManager.getAppNameVersion());

        parms.put(PARM_REPORT_ACTOR, ServiceContext.getActor());
        parms.put(PARM_REPORT_DATE,
                helper.getLongDate(ServiceContext.getTransactionDate()));
        parms.put(PARM_REPORT_IMAGE, getHeaderImage());

        //
        parms.put(PARM_RECEIPT_REF_NUMBER, receipt.getReceiptNumber());

        parms.put(PARM_RECEIPT_DATE, helper.getLongMediumDateTime(new Date(
                receipt.getTransactionDate().longValue())));

        parms.put(PARM_RECEIPT_USERNAME, receipt.getUserFullName());

        try {
            // final String currencyCode =
            parms.put(PARM_RECEIPT_AMOUNT, helper.getCurrencyDecimal(
                    BigDecimalUtil.valueOf(receipt.getPlainAmount()),
                    ConfigManager.getUserBalanceDecimals(), CurrencyUtil
                            .getCurrencySymbol(receipt.getAccountTrx()
                                    .getCurrencyCode(), "")));
        } catch (ParseException e) {
            throw new SpException(e);
        }

        parms.put(PARM_RECEIPT_CASHIER, receipt.getTransactedBy());
        parms.put(PARM_RECEIPT_COMMENT,
                StringUtils.defaultIfBlank(receipt.getComment(), ""));
        parms.put(PARM_RECEIPT_PAYMENT_METHOD,
                StringUtils.defaultIfBlank(receipt.getPaymentType(), ""));

        return parms;
    }

    /**
     * Creates the {@link JasperDesign}.
     *
     * @param pageSize
     * @param internalFront
     * @param locale
     * @return
     */
    public static JasperDesign create(JrPageSizeDto pageSize,
            InternalFontFamilyEnum internalFront, Locale locale) {

        JrPageLayoutDto layout = new JrPageLayoutDto();

        layout.setPageSize(pageSize);

        //
        layout.setLeftMargin(PAGE_MARGIN_LEFT);
        layout.setRightMargin(PAGE_MARGIN_RIGHT);

        layout.setColumnCount(1);
        layout.setColumnWidth(layout.getPageSize().getWidth()
                - layout.getLeftMargin() - layout.getRightMargin());
        layout.setColumnSpacing(0);

        //
        layout.setTopMargin(PAGE_MARGIN_TOP);
        layout.setBottomMargin(PAGE_MARGIN_BOTTOM);

        // layout.setPrintOrder(PrintOrderEnum.HORIZONTAL);

        try {
            return new JrPosDepositReceipt(layout, locale)
                    .getDesign(internalFront);
        } catch (JRException e) {
            throw new SpException(e);
        }

    }

    /**
     * @param defaultFontName
     * @return
     * @throws JRException
     */
    private JasperDesign getDesign(InternalFontFamilyEnum defaultFontName)
            throws JRException {

        // Dimensions
        final int PAGE_TITLE_HEIGHT = 90;
        final int PAGE_FOOTER_HEIGHT = 60;

        final int PAGE_DETAIL_HEIGHT =
                getLayout().getPageSize().getHeight()
                        - getLayout().getTopMargin()
                        - getLayout().getBottomMargin() - PAGE_TITLE_HEIGHT
                        - PAGE_FOOTER_HEIGHT;

        // JasperDesign
        JasperDesign jasperDesign = new JasperDesign();
        jasperDesign.setName(this.getClass().getSimpleName());

        jasperDesign.setPageWidth(layout.getPageSize().getWidth().intValue());
        jasperDesign.setPageHeight(layout.getPageSize().getHeight().intValue());

        jasperDesign.setColumnWidth(layout.getColumnWidth().intValue());
        jasperDesign.setColumnSpacing(layout.getColumnSpacing().intValue());
        jasperDesign.setColumnCount(layout.getColumnCount());
        jasperDesign.setLeftMargin(layout.getLeftMargin().intValue());
        jasperDesign.setRightMargin(layout.getRightMargin().intValue());
        jasperDesign.setTopMargin(layout.getTopMargin().intValue());
        jasperDesign.setBottomMargin(layout.getBottomMargin().intValue());
        jasperDesign.setPrintOrder(layout.getPrintOrder());

        // Default Style
        if (FontLocation.isFontPresent(defaultFontName)) {
            jasperDesign.addStyle(createDefaultBaseStyle(defaultFontName));
        }

        // Parameters
        addParameters(jasperDesign, new String[] { PARM_APP_VERSION,
                PARM_REPORT_ACTOR, PARM_REPORT_DATE, PARM_RECEIPT_DATE,
                PARM_RECEIPT_REF_NUMBER, PARM_RECEIPT_USERNAME,
                PARM_RECEIPT_AMOUNT, PARM_RECEIPT_CASHIER,
                PARM_RECEIPT_COMMENT, PARM_RECEIPT_PAYMENT_METHOD },
                java.lang.String.class);

        addParameters(jasperDesign, new String[] { PARM_REPORT_IMAGE },
                java.net.URL.class);

        // Fields (empty)

        final int FONT_SIZE_TITLE = 20;
        final int FONT_SIZE_HEADER = 12;

        // --------------------------------------------
        // Title
        // --------------------------------------------
        JRDesignBand band = new JRDesignBand();
        jasperDesign.setTitle(band);

        band.setHeight(PAGE_TITLE_HEIGHT);

        //
        JRDesignImage image = new JRDesignImage(jasperDesign);
        band.addElement(image);

        image.setExpression(new JRDesignExpression("$P{" + PARM_REPORT_IMAGE
                + "}"));

        image.setX(0);
        image.setY(0);

        image.setHeight(HEADER_IMAGE_HEIGHT);
        image.setWidth(HEADER_IMAGE_WIDTH);

        //
        JRDesignTextField textField;

        int posX = image.getY() + image.getWidth() + 20;

        int width = getLayout().getColumnWidth().intValue() - posX;

        textField =
                addDesignTextField(band, "$R{PosDepositReceipt.title}", posX,
                        15, width, 42, HorizontalAlignEnum.LEFT,
                        VerticalAlignEnum.MIDDLE);

        textField.setFontSize(FONT_SIZE_TITLE);

        //
        JRDesignLine line;

        line = new JRDesignLine();
        band.addElement(line);

        line.setX(0);
        line.setY(72);
        line.setWidth(getLayout().getColumnWidth().intValue());
        line.setHeight(1);

        // --------------------------------------------
        // Page Detail
        // --------------------------------------------
        band = new JRDesignBand();
        band.setHeight(PAGE_DETAIL_HEIGHT);
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(band);

        int wlkPosY = 0;

        // Receipt Header lines from configuration

        int fieldHeight = FONT_SIZE_HEADER + 2;
        int posYIncrement = fieldHeight + 4;

        for (final String headerLine : ConfigManager
                .getConfigMultiline(Key.FINANCIAL_POS_RECEIPT_HEADER)) {
            textField =
                    addDesignTextField(band, "\"" + headerLine + "\"", 0,
                            wlkPosY, getLayout().getColumnWidth(), fieldHeight,
                            HorizontalAlignEnum.LEFT, VerticalAlignEnum.MIDDLE);
            textField.setFontSize(FONT_SIZE_HEADER);
            wlkPosY += posYIncrement;
        }

        wlkPosY += posYIncrement;

        // Dimensions
        final int FONT_SIZE_DETAIL = 12;

        fieldHeight = FONT_SIZE_DETAIL + 4;
        posYIncrement = fieldHeight + 10;

        final int promptWidth =
                (int) Math.round(getLayout().getColumnWidth() * .35);

        final int fieldX = promptWidth + 5;

        final int fieldWidth = getLayout().getColumnWidth() - fieldX;

        // Fixed Header text
        textField =
                addDesignTextField(band, "$R{PosDepositReceipt.header}", 0,
                        wlkPosY, getLayout().getColumnWidth(), fieldHeight,
                        HorizontalAlignEnum.LEFT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        wlkPosY += 2 * posYIncrement;

        // Reference number

        textField =
                addDesignTextField(band,
                        "$R{PosDepositReceipt.prompt.refNumber} + \" :\"", 0,
                        wlkPosY, promptWidth, fieldHeight,
                        HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        textField =
                addDesignTextField(band, "$P{" + PARM_RECEIPT_REF_NUMBER + "}",
                        fieldX, wlkPosY, fieldWidth, fieldHeight,
                        HorizontalAlignEnum.LEFT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        wlkPosY += posYIncrement;

        // Date

        textField =
                addDesignTextField(band,
                        "$R{PosDepositReceipt.prompt.date} + \" :\"", 0,
                        wlkPosY, promptWidth, fieldHeight,
                        HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        textField =
                addDesignTextField(band, "$P{" + PARM_RECEIPT_DATE + "}",
                        fieldX, wlkPosY, fieldWidth, fieldHeight,
                        HorizontalAlignEnum.LEFT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        wlkPosY += posYIncrement;

        // Username

        textField =
                addDesignTextField(band,
                        "$R{PosDepositReceipt.prompt.userName} + \" :\"", 0,
                        wlkPosY, promptWidth, fieldHeight,
                        HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        textField =
                addDesignTextField(band, "$P{" + PARM_RECEIPT_USERNAME + "}",
                        fieldX, wlkPosY, fieldWidth, fieldHeight,
                        HorizontalAlignEnum.LEFT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        wlkPosY += posYIncrement;

        // Amount

        textField =
                addDesignTextField(band,
                        "$R{PosDepositReceipt.prompt.amount} + \" :\"", 0,
                        wlkPosY, promptWidth, fieldHeight,
                        HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        textField =
                addDesignTextField(band, "$P{" + PARM_RECEIPT_AMOUNT + "}",
                        fieldX, wlkPosY, fieldWidth, fieldHeight,
                        HorizontalAlignEnum.LEFT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        wlkPosY += posYIncrement;

        // Payment method

        textField =
                addDesignTextField(band,
                        "$R{PosDepositReceipt.prompt.paymentMethod} + \" :\"",
                        0, wlkPosY, promptWidth, fieldHeight,
                        HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        textField =
                addDesignTextField(band, "$P{" + PARM_RECEIPT_PAYMENT_METHOD
                        + "}", fieldX, wlkPosY, fieldWidth, fieldHeight,
                        HorizontalAlignEnum.LEFT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        wlkPosY += posYIncrement;

        // Cashier

        textField =
                addDesignTextField(band,
                        "$R{PosDepositReceipt.prompt.cashier} + \" :\"", 0,
                        wlkPosY, promptWidth, fieldHeight,
                        HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        textField =
                addDesignTextField(band, "$P{" + PARM_RECEIPT_CASHIER + "}",
                        fieldX, wlkPosY, fieldWidth, fieldHeight,
                        HorizontalAlignEnum.LEFT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        wlkPosY += posYIncrement;

        // Comment

        textField =
                addDesignTextField(band,
                        "$R{PosDepositReceipt.prompt.comment} + \" :\"", 0,
                        wlkPosY, promptWidth, fieldHeight,
                        HorizontalAlignEnum.RIGHT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);

        textField =
                addDesignTextField(band, "$P{" + PARM_RECEIPT_COMMENT + "}",
                        fieldX, wlkPosY, fieldWidth, fieldHeight,
                        HorizontalAlignEnum.LEFT, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(FONT_SIZE_DETAIL);
        textField.setStretchWithOverflow(true);

        wlkPosY += posYIncrement;

        // --------------------------------------------
        // Page Footer
        // --------------------------------------------
        band = new JRDesignBand();
        jasperDesign.setPageFooter(band);

        band.setHeight(PAGE_FOOTER_HEIGHT);

        textField =
                addDesignTextField(band, "$P{" + PARM_APP_VERSION + "}", 0, 30,
                        getLayout().getColumnWidth().intValue(), 20,
                        HorizontalAlignEnum.CENTER, VerticalAlignEnum.MIDDLE);
        textField.setFontSize(10);

        // --------------------------------------------
        return jasperDesign;
    }

    @Override
    protected JrPageLayoutDto getLayout() {
        return this.layout;
    }
}
