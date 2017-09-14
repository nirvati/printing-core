/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2017 Datraverse B.V.
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
package org.savapage.core.services.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.inbox.OutputProducer;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfo;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfoSet;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.pdf.ITextPdfCreator;
import org.savapage.core.print.proxy.TicketJobSheetDto;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.MediaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Job Sheet PDF Creator.
 *
 * @author Rijk Ravestein
 *
 */
public final class TicketJobSheetPdfCreator {

    /**
     * The logger.
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(TicketJobSheetPdfCreator.class);

    /**
     * Unicode Character 'REVERSE SOLIDUS' (U+005C).
     */
    private static final char CHAR_REVERSE_SOLIDUS = 0x5C;

    /** */
    private static final Font FONT_CAT =
            new Font(Font.FontFamily.HELVETICA, 18, Font.BOLD, BaseColor.GRAY);

    /** */
    private static final Font FONT_NORMAL = new Font(Font.FontFamily.HELVETICA,
            12, Font.NORMAL, BaseColor.DARK_GRAY);

    /** */
    private static final AccountDao ACCOUNT_DAO =
            ServiceContext.getDaoContext().getAccountDao();

    /** */
    private final String userid;
    /** */
    private final OutboxJobDto job;
    /** */
    private final TicketJobSheetDto jobSheet;

    /** */
    private String currencySymbol;

    /**
     * Number of currency decimals to display.
     */
    private int currencyDecimals;

    /** */
    private Locale locale;

    /**
     *
     * @param user
     *            The unique user id.
     * @param jobSheetDto
     *            Job Sheet information.
     * @param jobDto
     *            The {@link OutboxJobDto} job ticket.
     */
    public TicketJobSheetPdfCreator(final String user,
            final OutboxJobDto jobDto, final TicketJobSheetDto jobSheetDto) {
        this.userid = user;
        this.job = jobDto;
        this.jobSheet = jobSheetDto;
    }

    /**
     * Creates a single page PDF Job Sheet file.
     *
     * @return The PDF file.
     */
    public File create() {

        this.currencyDecimals = ConfigManager.getUserBalanceDecimals();
        this.locale = ServiceContext.getLocale();
        this.currencySymbol = ServiceContext.getAppCurrencySymbol();

        //
        final MediaSizeName sizeName = MediaUtils
                .getMediaSizeFromInboxMedia(jobSheet.getMediaOption());

        final Document document =
                new Document(ITextPdfCreator.getPageSize(sizeName));

        final File filePdf = new File(OutputProducer
                .createUniqueTempPdfName(userid, "ticket-job-sheet"));

        OutputStream ostr = null;

        try {
            ostr = new FileOutputStream(filePdf);

            // final PdfWriter writer =
            PdfWriter.getInstance(document, ostr);

            document.open();

            final Paragraph secInfo = new Paragraph();

            secInfo.add(new Paragraph(String.format("%s %s\n",
                    NounEnum.TICKET.uiText(locale), job.getTicketNumber()),
                    FONT_CAT));

            onDocumentInfo(secInfo);
            onAccountTrxInfo(secInfo);

            document.add(secInfo);

        } catch (FileNotFoundException | DocumentException e) {
            LOGGER.error(e.getMessage(), e);
            throw new SpException(e.getMessage(), e);
        } finally {
            document.close();
            IOUtils.closeQuietly(ostr);
        }

        return filePdf;
    }

    /**
     * @param par
     *            The paragraph to append the text to.
     */
    private void onDocumentInfo(final Paragraph par) {

        final StringBuilder sb = new StringBuilder();

        sb.append("\n").append(NounEnum.USER.uiText(locale)).append(" : ")
                .append(userid);
        sb.append("\n").append(NounEnum.TITLE.uiText(locale)).append(" : ")
                .append(job.getJobName());

        sb.append("\n").append(NounEnum.TIME.uiText(locale)).append(" : ")
                .append(DateUtil.formattedDateTime(new Date()));

        final ProxyPrintCostDto costResult = job.getCostResult();
        final BigDecimal costTotal = costResult.getCostTotal();

        sb.append("\n").append(NounEnum.COST.uiText(locale)).append(" : ")
                .append(this.currencySymbol).append(" ")
                .append(localizedDecimal(costTotal));

        sb.append("\n").append(NounEnum.REMARK.uiText(locale)).append(" : ");
        if (StringUtils.isBlank(job.getComment())) {
            sb.append("-");
        } else {
            sb.append(job.getComment());
        }

        par.add(new Paragraph(sb.toString(), FONT_NORMAL));
    }

    /**
     * @param par
     *            The paragraph to append the text to.
     */
    private void onAccountTrxInfo(final Paragraph par) {

        final StringBuilder sb = new StringBuilder();

        sb.append("\n").append(PrintOutNounEnum.COPY.uiText(locale, true))
                .append(" : ").append(job.getCopies());

        final OutboxAccountTrxInfoSet trxInfoSet = job.getAccountTransactions();

        if (trxInfoSet == null) {
            return;
        }

        final int copies = trxInfoSet.getWeightTotal();
        int copiesDelegatorsImplicit = 0;

        for (final OutboxAccountTrxInfo trxInfo : trxInfoSet
                .getTransactions()) {

            final int weight = trxInfo.getWeight();

            final Account account =
                    ACCOUNT_DAO.findById(trxInfo.getAccountId());

            final AccountTypeEnum accountType =
                    AccountTypeEnum.valueOf(account.getAccountType());

            if (accountType != AccountTypeEnum.SHARED
                    && accountType != AccountTypeEnum.GROUP) {
                continue;
            }

            copiesDelegatorsImplicit += weight;

            final Account accountParent = account.getParent();

            sb.append("\n");
            if (accountParent != null) {
                sb.append(accountParent.getName()).append(' ');
                // Do not use regular '\' since this is a line break.
                // Use Unicode Character 'REVERSE SOLIDUS' (U+005C) instead.
                sb.append(CHAR_REVERSE_SOLIDUS);
                sb.append(' ');
            }
            sb.append(account.getName()).append(" : ").append(weight);
        }

        final int copiesDelegatorsIndividual =
                copies - copiesDelegatorsImplicit;

        if (copiesDelegatorsIndividual > 0) {
            sb.append("\n").append(NounEnum.USER.uiText(locale, true))
                    .append(" : ").append(copiesDelegatorsIndividual);
        }

        par.add(new Paragraph(sb.toString(), FONT_NORMAL));
    }

    /**
     * Gets the localized string for a BigDecimal.
     *
     * @param value
     *            The {@link BigDecimal}.
     * @return The localized string.
     */
    private String localizedDecimal(final BigDecimal value) {
        try {
            return BigDecimalUtil.localize(value, this.currencyDecimals,
                    this.locale, true);
        } catch (ParseException e) {
            throw new SpException(e);
        }
    }

}
