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

import java.awt.Color;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.savapage.core.SpException;
import org.savapage.core.dao.AccountVoucherDao;
import org.savapage.core.dao.AccountVoucherDao.DbVoucherType;
import org.savapage.core.dto.JrPageLayoutDto;
import org.savapage.core.dto.JrPageSizeDto;
import org.savapage.core.dto.JrVoucherPageLayoutDto;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.jpa.AccountVoucher;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.design.JRDesignBand;
import net.sf.jasperreports.engine.design.JRDesignRectangle;
import net.sf.jasperreports.engine.design.JRDesignSection;
import net.sf.jasperreports.engine.design.JRDesignStyle;
import net.sf.jasperreports.engine.design.JRDesignTextField;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.type.HorizontalAlignEnum;
import net.sf.jasperreports.engine.type.ModeEnum;
import net.sf.jasperreports.engine.type.PrintOrderEnum;
import net.sf.jasperreports.engine.type.VerticalAlignEnum;

/**
 *
 * @author Datraverse B.V.
 *
 */
public class JrVoucherPageDesign extends AbstractJrDesign {

    public static final String PARM_HEADER = "HEADER";
    public static final String PARM_FOOTER = "FOOTER";

    public static final String FIELD_CARD_NUMBER = "CARD_NUMBER";
    public static final String FIELD_CARD_VALUE = "CARD_VALUE";
    public static final String FIELD_CARD_EXPIRY = "CARD_EXPIRY";

    /**
     *
     * @author Datraverse B.V.
     *
     */
    private static class DataSource extends AbstractJrDataSource implements
            JRDataSource {

        private static final int CHUNK_SIZE = 100;

        private List<AccountVoucher> entryList = null;
        private Iterator<AccountVoucher> iterator;

        private AccountVoucher voucherWlk = null;

        private int counter = 1;
        private int chunkCounter = CHUNK_SIZE;

        private final AccountVoucherDao.Field sortField;
        private final Boolean sortAscending;
        private final AccountVoucherDao.ListFilter filter;

        private final DateFormat dfShortDateTime;

        /**
         *
         * @param req
         */
        public DataSource(final String batch, final Locale locale) {

            super(locale);

            this.sortField = AccountVoucherDao.Field.NUMBER;
            this.sortAscending = true;
            this.filter = new AccountVoucherDao.ListFilter();

            this.filter.setBatch(batch);
            this.filter.setUsed(Boolean.FALSE);
            this.filter.setExpired(Boolean.FALSE);
            this.filter.setDateNow(new Date()); // !!!
            this.filter.setVoucherType(DbVoucherType.CARD);

            this.counter = 0;
            this.chunkCounter = CHUNK_SIZE;

            dfShortDateTime =
                    DateFormat.getDateInstance(DateFormat.LONG, locale);
        }

        /**
         *
         * @param startPosition
         * @param maxResults
         */
        private void getNextChunk(Integer startPosition, Integer maxResults) {

            this.entryList =
                    ServiceContext
                            .getDaoContext()
                            .getAccountVoucherDao()
                            .getListChunk(this.filter, startPosition,
                                    maxResults, this.sortField,
                                    this.sortAscending);

            this.chunkCounter = 0;
            this.iterator = this.entryList.iterator();

        }

        @Override
        public Object getFieldValue(JRField jrField) throws JRException {

            switch (jrField.getName()) {
            case FIELD_CARD_NUMBER:
                return voucherWlk.getCardNumber();
            case FIELD_CARD_VALUE:
                try {
                    return BigDecimalUtil.localize(voucherWlk.getValueAmount(),
                            getUserBalanceDecimals(), this.getLocale(),
                            getCurrencySymbol(), false);
                } catch (ParseException e) {
                    throw new SpException(e);
                }
            case FIELD_CARD_EXPIRY:
                return localized("voucher-use-before",
                        dfShortDateTime.format(voucherWlk.getExpiryDate()));
            default:
                return null;
            }
        }

        @Override
        public boolean next() throws JRException {

            if (this.chunkCounter == CHUNK_SIZE) {
                getNextChunk(this.counter, CHUNK_SIZE);
            }

            if (!this.iterator.hasNext()) {
                return false;
            }

            this.voucherWlk = this.iterator.next();

            this.counter++;
            this.chunkCounter++;

            return true;
        }
    }

    /**
     *
     */
    private final JrVoucherPageLayoutDto layout;

    /**
     *
     * @param layout
     */
    public JrVoucherPageDesign(JrVoucherPageLayoutDto layout, Locale locale) {
        super(locale);
        this.layout = layout;
    }

    /**
     *
     * @param batch
     * @param locale
     * @return
     */
    public static JRDataSource createDataSource(final String batch,
            final Locale locale) {
        return new DataSource(batch, locale);
    }

    /**
     *
     * @param layout
     * @param defaultFontName
     * @return
     */
    private static JasperDesign createDesign(
            final JrVoucherPageLayoutDto layout,
            InternalFontFamilyEnum defaultFontName, Locale locale) {
        try {
            return new JrVoucherPageDesign(layout, locale)
                    .getDesign(defaultFontName);
        } catch (JRException e) {
            throw new SpException(e);
        }
    }

    /**
     *
     * @param defaultFontName
     * @return
     */
    public static JasperDesign createA4With3x5(
            final InternalFontFamilyEnum defaultFontName, final Locale locale) {

        JrVoucherPageLayoutDto layout = new JrVoucherPageLayoutDto();

        layout.setPageSize(JrPageSizeDto.A4_PORTRAIT);

        // 47 + x + 15 + x + 15 + x + 47 = 595 (A4 width = 595)
        // x = 157
        layout.setLeftMargin(47);

        layout.setColumnCount(3);
        layout.setColumnSpacing(15);

        layout.setColumnWidth(157);

        layout.setRightMargin(47);

        // 29 + (5 * 157) + 28 = 842 (A4 height = 842)
        layout.setTopMargin(29);
        layout.setBottomMargin(28);

        layout.setCardHeight(157);

        layout.setPrintOrder(PrintOrderEnum.HORIZONTAL);

        return createDesign(layout, defaultFontName, locale);
    }

    /**
     *
     * @param defaultFontName
     * @return
     */
    public static JasperDesign createA4With2x5(
            final InternalFontFamilyEnum defaultFontName, final Locale locale) {

        JrVoucherPageLayoutDto layout = new JrVoucherPageLayoutDto();

        layout.setPageSize(JrPageSizeDto.A4_PORTRAIT);

        // 47 + 243 + 15 + 243 + 47 = 595 (A4 width = 595)
        layout.setLeftMargin(47);

        layout.setColumnCount(2);
        layout.setColumnSpacing(15);

        layout.setColumnWidth(243);

        layout.setRightMargin(47);

        // 29 + (5 * 157) + 28 = 842 (A4 height = 842)
        layout.setTopMargin(29);
        layout.setBottomMargin(28);

        layout.setCardHeight(157);

        layout.setPrintOrder(PrintOrderEnum.HORIZONTAL);

        return createDesign(layout, defaultFontName, locale);
    }

    /**
     *
     * @param defaultFontName
     * @return
     */
    public static JasperDesign createLetterWith3x5(
            final InternalFontFamilyEnum defaultFontName, final Locale locale) {

        JrVoucherPageLayoutDto layout = new JrVoucherPageLayoutDto();

        layout.setPageSize(JrPageSizeDto.LETTER_PORTRAIT);

        // 47 + x + 16 + x + 16 + x + 47 = 612 (Letter width = 612)
        // x = 162

        layout.setLeftMargin(47);

        layout.setColumnCount(3);
        layout.setColumnSpacing(16);

        layout.setColumnWidth(162);

        layout.setRightMargin(47);

        // 29 + (5 * 147) + 28 = 792 (Letter height = 792)
        layout.setTopMargin(29);
        layout.setBottomMargin(28);

        layout.setCardHeight(147);

        layout.setPrintOrder(PrintOrderEnum.HORIZONTAL);

        return createDesign(layout, defaultFontName, locale);
    }

    /**
     *
     * @param defaultFontName
     * @return
     */
    public static JasperDesign createLetterWith2x5(
            final InternalFontFamilyEnum defaultFontName, final Locale locale) {

        JrVoucherPageLayoutDto layout = new JrVoucherPageLayoutDto();

        layout.setPageSize(JrPageSizeDto.LETTER_PORTRAIT);

        // 53 + 243 + 20 + 243 + 53 = 612 (Letter width = 612)
        layout.setLeftMargin(53);

        layout.setColumnCount(2);
        layout.setColumnSpacing(20);

        layout.setColumnWidth(243);

        layout.setRightMargin(53);

        // 29 + (5 * 147) + 28 = 792 (Letter height = 792)
        layout.setTopMargin(29);
        layout.setBottomMargin(28);

        layout.setCardHeight(147);

        layout.setPrintOrder(PrintOrderEnum.HORIZONTAL);

        return createDesign(layout, defaultFontName, locale);
    }

    /**
     *
     * @param band
     * @param expression
     * @param posY
     * @param fieldHeight
     * @return
     * @throws JRException
     */
    private JRDesignTextField addDesignTextField(JRDesignBand band,
            String expression, int posY, int fieldHeight) throws JRException {

        return addDesignTextField(band, expression, 0, posY, getLayout()
                .getColumnWidth().intValue(), fieldHeight,
                HorizontalAlignEnum.CENTER, VerticalAlignEnum.MIDDLE);
    }

    /**
     * @param defaultFontName
     * @return
     * @throws JRException
     */
    private JasperDesign getDesign(InternalFontFamilyEnum defaultFontName)
            throws JRException {

        // JasperDesign
        JasperDesign jasperDesign = new JasperDesign();
        jasperDesign.setName("Vouchers");

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
        JRDesignStyle baseStyle = new JRDesignStyle();
        baseStyle.setDefault(true);
        baseStyle.setName("Base");
        baseStyle.setFontName(defaultFontName.getJrName());
        jasperDesign.addStyle(baseStyle);

        // Parameters
        addParameters(jasperDesign, new String[] { PARM_HEADER, PARM_FOOTER },
                java.lang.String.class);

        // Fields
        addFields(jasperDesign, new String[] { FIELD_CARD_VALUE,
                FIELD_CARD_NUMBER, FIELD_CARD_EXPIRY }, java.lang.String.class);

        // Title (empty)
        JRDesignBand band = new JRDesignBand();
        jasperDesign.setTitle(band);

        // Page header (empty)
        band = new JRDesignBand();
        jasperDesign.setPageHeader(band);

        // Column header (empty)
        band = new JRDesignBand();
        jasperDesign.setColumnHeader(band);

        /*
         * Detail
         */

        // Dimensions

        final int cardHeight = layout.getCardHeight().intValue();

        final int fontSizeHeader = 14;
        final int fontSizeAmount = 16;
        final int fontSizeValidTill = 10;
        final int fontSizeFooter = 8;
        final int fontSizeCardNumber = 13;

        //
        band = new JRDesignBand();
        band.setHeight(cardHeight);

        // with border
        JRDesignRectangle rect = new JRDesignRectangle();
        rect.setForecolor(Color.GRAY);
        rect.setX(0);
        rect.setY(0);
        rect.setWidth(layout.getColumnWidth());
        rect.setHeight(layout.getCardHeight());
        rect.setMode(ModeEnum.TRANSPARENT);

        band.addElement(rect);

        //
        JRDesignTextField textField;
        int cardHeightRemainder = cardHeight;
        int fieldHeighWlk;

        // HEADER
        fieldHeighWlk = (int) Math.round(cardHeight * .20);

        textField =
                addDesignTextField(band, "$P{" + PARM_HEADER + "}", cardHeight
                        - cardHeightRemainder, fieldHeighWlk);
        textField.setFontSize(fontSizeHeader);
        textField.setBold(false);
        textField.setStretchWithOverflow(true);

        cardHeightRemainder -= fieldHeighWlk;

        // AMOUNT
        fieldHeighWlk = (int) Math.round(cardHeight * .20);

        textField =
                addDesignTextField(band, "$F{" + FIELD_CARD_VALUE + "}",
                        cardHeight - cardHeightRemainder, fieldHeighWlk);
        textField.setFontSize(fontSizeAmount);
        textField.setBold(false);

        cardHeightRemainder -= fieldHeighWlk;

        // CARD_NUMBER
        fieldHeighWlk = (int) Math.round(cardHeight * .20);

        textField =
                addDesignTextField(band, "$F{" + FIELD_CARD_NUMBER + "}",
                        cardHeight - cardHeightRemainder, fieldHeighWlk);
        textField.setFontSize(fontSizeCardNumber);
        textField.setBold(false);
        textField.setStretchWithOverflow(true);

        cardHeightRemainder -= fieldHeighWlk;

        // VALID_TILL
        fieldHeighWlk = (int) Math.round(cardHeight * .20);

        textField =
                addDesignTextField(band, "$F{" + FIELD_CARD_EXPIRY + "}",
                        cardHeight - cardHeightRemainder, fieldHeighWlk);
        textField.setFontSize(fontSizeValidTill);
        textField.setBold(false);
        textField.setStretchWithOverflow(true);

        cardHeightRemainder -= fieldHeighWlk;

        // FOOTER
        textField =
                addDesignTextField(band, "$P{" + PARM_FOOTER + "}", cardHeight
                        - cardHeightRemainder, cardHeightRemainder);
        textField.setFontSize(fontSizeFooter);
        textField.setBold(false);
        textField.setStretchWithOverflow(true);

        //
        ((JRDesignSection) jasperDesign.getDetailSection()).addBand(band);

        // Column footer (empty)
        band = new JRDesignBand();
        jasperDesign.setColumnFooter(band);

        // Page footer (empty)
        band = new JRDesignBand();
        jasperDesign.setPageFooter(band);

        // Summary (empty)
        band = new JRDesignBand();
        jasperDesign.setSummary(band);

        return jasperDesign;
    }

    @Override
    protected JrPageLayoutDto getLayout() {
        return this.layout;
    }
}
