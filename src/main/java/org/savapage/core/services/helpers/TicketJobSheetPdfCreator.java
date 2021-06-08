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
package org.savapage.core.services.helpers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.dao.AccountDao;
import org.savapage.core.dao.enums.ACLRoleEnum;
import org.savapage.core.dao.enums.ExternalSupplierEnum;
import org.savapage.core.i18n.AdjectiveEnum;
import org.savapage.core.i18n.JobTicketNounEnum;
import org.savapage.core.i18n.NounEnum;
import org.savapage.core.i18n.PrintOutAdjectiveEnum;
import org.savapage.core.i18n.PrintOutNounEnum;
import org.savapage.core.i18n.PrintOutVerbEnum;
import org.savapage.core.inbox.OutputProducer;
import org.savapage.core.ipp.attribute.IppDictJobTemplateAttr;
import org.savapage.core.ipp.helpers.IppOptionMap;
import org.savapage.core.jpa.Account;
import org.savapage.core.jpa.Account.AccountTypeEnum;
import org.savapage.core.jpa.DocInOut;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.PrintOut;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfo;
import org.savapage.core.outbox.OutboxInfoDto.OutboxAccountTrxInfoSet;
import org.savapage.core.outbox.OutboxInfoDto.OutboxJobDto;
import org.savapage.core.pdf.ITextPdfCreator;
import org.savapage.core.print.proxy.ProxyPrintInboxReq;
import org.savapage.core.print.proxy.TicketJobSheetDto;
import org.savapage.core.services.ProxyPrintService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.util.BigDecimalUtil;
import org.savapage.core.util.DateUtil;
import org.savapage.core.util.IOHelper;
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

    /** */
    private static final ProxyPrintService PROXY_PRINT_SERVICE =
            ServiceContext.getServiceFactory().getProxyPrintService();
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
    private final String useridDocLog;

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
    private final Locale locale;

    /**
     *
     * @param user
     *            The unique user id.
     * @param jobDto
     *            The {@link OutboxJobDto} job ticket.
     * @param jobSheetDto
     *            Job Sheet information.
     */
    public TicketJobSheetPdfCreator(final String user,
            final OutboxJobDto jobDto, final TicketJobSheetDto jobSheetDto) {
        this.userid = user;
        this.useridDocLog = user;
        this.job = jobDto;
        this.jobSheet = jobSheetDto;
        this.locale = ServiceContext.getLocale();
    }

    /**
     *
     * @param user
     *            The unique user id as {@link DocLog} owner.
     * @param req
     *            The {@link ProxyPrintInboxReq} request.
     * @param docLog
     *            {@link DocLog} parent of the {@link PrintOut}.
     * @param jobSheetDto
     *            Job Sheet information.
     */
    public TicketJobSheetPdfCreator(final String user,
            final ProxyPrintInboxReq req, final DocLog docLog,
            final TicketJobSheetDto jobSheetDto) {

        this.useridDocLog = user;
        this.locale = ServiceContext.getLocale();

        // Capture data as outbox job.

        final OutboxJobDto jobDto = new OutboxJobDto();
        this.job = jobDto;

        this.job.setCopies(req.getNumberOfCopies());
        this.job.getAccountTransactions();
        this.job.setJobName(req.getJobName());
        this.job.setPages(req.getNumberOfPages());
        this.job.setCostResult(req.getCostResult());
        this.job.setOptionValues(req.getOptionValues());
        this.job.setArchive(req.isArchive());

        final String comment;
        final String ticket;

        if (req.getSupplierInfo() != null && req.getSupplierInfo()
                .getSupplier() == ExternalSupplierEnum.MAIL_TICKET_OPER) {

            final ExternalSupplierData operData =
                    req.getSupplierInfo().getData();

            final StringBuilder ticketTxt = new StringBuilder();
            final Set<String> emails = new HashSet<>();

            // Collect ticket numbers and email addresses
            for (final DocInOut docInOut : docLog.getDocOut().getDocsInOut()) {
                final DocLog docLogIn = docInOut.getDocIn().getDocLog();
                if (ticketTxt.length() > 0) {
                    ticketTxt.append(", ");
                }
                ticketTxt.append(docLogIn.getExternalId());

                final MailPrintData extData = MailPrintData
                        .createFromData(docLogIn.getExternalData());
                if (extData != null && extData.getFromAddress() != null) {
                    emails.add(extData.getFromAddress());
                }
            }
            ticket = ticketTxt.toString();

            if (operData != null && operData instanceof MailTicketOperData) {
                final StringBuilder useridTxt = new StringBuilder();
                for (final String email : emails) {
                    if (useridTxt.length() > 0) {
                        useridTxt.append(", ");
                    }
                    useridTxt.append(email);
                }
                this.userid = useridTxt.toString();

                final MailTicketOperData mData = (MailTicketOperData) operData;
                comment = String.format(
                        "%s \"%s\"", ACLRoleEnum.MAIL_TICKET_OPERATOR
                                .uiText(this.locale).toLowerCase(),
                        mData.getOperator());
            } else {
                this.userid = user;
                comment = req.getComment();
            }

        } else {
            ticket = req.getJobTicketNumber();
            comment = req.getComment();
            this.userid = user;
        }

        this.job.setTicketNumber(ticket);
        this.job.setComment(comment);

        this.jobSheet = jobSheetDto;
    }

    /**
     * Creates a single page PDF Job Sheet file.
     *
     * @return The PDF file.
     */
    public File create() {

        this.currencyDecimals = ConfigManager.getUserBalanceDecimals();
        this.currencySymbol = ServiceContext.getAppCurrencySymbol();

        //
        final MediaSizeName sizeName = MediaUtils
                .getMediaSizeFromInboxMedia(this.jobSheet.getMediaOption());

        final Document document =
                new Document(ITextPdfCreator.getPageSize(sizeName));

        final File filePdf = new File(OutputProducer.createUniqueTempPdfName(
                this.useridDocLog, "ticket-job-sheet"));

        OutputStream ostr = null;

        try {
            ostr = new FileOutputStream(filePdf);

            PdfWriter.getInstance(document, ostr);

            document.open();

            final Paragraph secInfo = new Paragraph();

            secInfo.add(new Paragraph(
                    String.format("%s %s\n",
                            JobTicketNounEnum.TICKET.uiText(this.locale),
                            StringUtils.defaultIfEmpty(
                                    this.job.getTicketNumber(), "-")),
                    FONT_CAT));

            onDocumentInfo(secInfo);
            onAccountTrxInfo(secInfo);

            document.add(secInfo);

        } catch (FileNotFoundException | DocumentException e) {
            LOGGER.error(e.getMessage(), e);
            throw new SpException(e.getMessage(), e);
        } finally {
            document.close();
            IOHelper.closeQuietly(ostr);
        }

        return filePdf;
    }

    /**
     * @param par
     *            The paragraph to append the text to.
     */
    private void onDocumentInfo(final Paragraph par) {

        final StringBuilder sb = new StringBuilder();

        sb.append("\n").append(NounEnum.USER.uiText(this.locale)).append(" : ")
                .append(this.userid);
        sb.append("\n").append(NounEnum.TITLE.uiText(this.locale)).append(" : ")
                .append(job.getJobName());

        sb.append("\n").append(PrintOutNounEnum.PAGE.uiText(this.locale, true))
                .append(" : ").append(job.getPages());

        sb.append("\n").append(NounEnum.TIME.uiText(this.locale)).append(" : ")
                .append(DateUtil.formattedDateTime(new Date()));

        final ProxyPrintCostDto costResult = job.getCostResult();
        final BigDecimal costTotal = costResult.getCostTotal();

        sb.append("\n").append(NounEnum.COST.uiText(this.locale)).append(" : ")
                .append(this.currencySymbol).append(" ")
                .append(localizedDecimal(costTotal));

        sb.append("\n").append(NounEnum.REMARK.uiText(this.locale))
                .append(" : ");
        if (StringUtils.isBlank(job.getComment())) {
            sb.append("-");
        } else {
            sb.append(job.getComment());
        }

        final IppOptionMap ippMap = new IppOptionMap(job.getOptionValues());

        // Settings
        sb.append("\n\n");

        sb.append(getIppValueLocale(ippMap, IppDictJobTemplateAttr.ATTR_MEDIA));

        sb.append(", ");
        if (ippMap.isLandscapeJob()) {
            sb.append(PrintOutNounEnum.LANDSCAPE.uiText(this.locale));
        } else {
            sb.append(PrintOutNounEnum.PORTRAIT.uiText(this.locale));
        }

        sb.append(", ");
        if (ippMap.isDuplexJob()) {
            sb.append(PrintOutNounEnum.DUPLEX.uiText(this.locale));
        } else {
            sb.append(PrintOutNounEnum.SIMPLEX.uiText(this.locale));
        }

        sb.append(", ");
        if (job.isMonochromeJob()) {
            sb.append(PrintOutNounEnum.GRAYSCALE.uiText(this.locale));
        } else {
            sb.append(PrintOutNounEnum.COLOR.uiText(this.locale));
        }

        if (ippMap.getNumberUp() != null
                && ippMap.getNumberUp().intValue() > 1) {
            sb.append(", ");
            sb.append(PrintOutNounEnum.N_UP.uiText(this.locale,
                    ippMap.getNumberUp().toString()));
        }

        if (ippMap.hasPrintScaling()) {
            sb.append(", ");
            sb.append(AdjectiveEnum.SCALED.uiText(this.locale));
        }

        sb.append(", ");
        if (job.isCollate()) {
            sb.append(PrintOutVerbEnum.COLLATE.uiText(this.locale));
        } else {
            sb.append(PrintOutAdjectiveEnum.UNCOLLATED.uiText(this.locale));
        }

        if (BooleanUtils.isTrue(job.getArchive())) {
            sb.append(", ").append(NounEnum.ARCHIVE.uiText(this.locale));
        }

        // Finishings
        final List<String> ippKeywords = new ArrayList<>();

        if (ippMap.hasFinishingPunch()) {
            ippKeywords.add(
                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_PUNCH);
        }
        if (ippMap.hasFinishingStaple()) {
            ippKeywords.add(
                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_STAPLE);
        }
        if (ippMap.hasFinishingFold()) {
            ippKeywords.add(
                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_FOLD);
        }
        if (ippMap.hasFinishingBooklet()) {
            ippKeywords.add(
                    IppDictJobTemplateAttr.ORG_SAVAPAGE_ATTR_FINISHINGS_BOOKLET);
        }

        if (!ippKeywords.isEmpty()) {
            sb.append("\n");
            addIppAttrValues(sb, ippMap, ippKeywords);
        }

        par.add(new Paragraph(sb.toString(), FONT_NORMAL));
    }

    /**
     *
     * @param ippMap
     *            IPP attr/value map.
     * @param ippAttr
     *            IPP attribute.
     * @return The localized IPP attribute value.
     */
    private String getIppValueLocale(final IppOptionMap ippMap,
            final String ippAttr) {
        return PROXY_PRINT_SERVICE.localizePrinterOptValue(this.locale, ippAttr,
                ippMap.getOptionValue(ippAttr));
    }

    /**
     *
     * @param sb
     *            String to append on.
     * @param ippMap
     *            IPP attr/value map.
     * @param ippAttrKeywords
     *            List of IPP keywords.
     */
    private void addIppAttrValues(final StringBuilder sb,
            final IppOptionMap ippMap, final List<String> ippAttrKeywords) {

        if (ippAttrKeywords.isEmpty()) {
            sb.append("-");
        } else {
            for (int i = 0; i < ippAttrKeywords.size(); i++) {
                final String ippAttr = ippAttrKeywords.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(getIppValueLocale(ippMap, ippAttr));
            }
        }
    }

    /**
     * @param par
     *            The paragraph to append the text to.
     */
    private void onAccountTrxInfo(final Paragraph par) {

        final StringBuilder sb = new StringBuilder();

        sb.append("\n").append(PrintOutNounEnum.COPY.uiText(this.locale, true))
                .append(" : ").append(job.getCopies());

        final OutboxAccountTrxInfoSet trxInfoSet = job.getAccountTransactions();

        if (trxInfoSet == null) {
            return;
        }

        // Accumulated copies for assigned user accounts.
        final Map<String, Integer> userAccountAssigned = new HashMap<>();

        // Delegator copies implicit by SHARED or GROUP account.
        int copiesDelegatorsImplicit = 0;
        // Delegator copies assigned to USER account.
        int copiesDelegatorsAssigned = 0;

        for (final OutboxAccountTrxInfo trxInfo : trxInfoSet
                .getTransactions()) {

            final Account account =
                    ACCOUNT_DAO.findById(trxInfo.getAccountId());

            final int weightUnit;

            if (trxInfo.getWeightUnit() == null) {
                weightUnit = 1;
            } else {
                weightUnit = trxInfo.getWeightUnit().intValue();
            }

            final int copiesAccount = trxInfo.getWeight() / weightUnit;

            final AccountTypeEnum accountType =
                    AccountTypeEnum.valueOf(account.getAccountType());

            if (accountType != AccountTypeEnum.SHARED
                    && accountType != AccountTypeEnum.GROUP) {

                final String key = account.getNameLower();
                Integer count = userAccountAssigned.get(key);
                if (count == null) {
                    count = Integer.valueOf(0);
                }
                userAccountAssigned.put(key,
                        Integer.valueOf(count.intValue() + copiesAccount));

                copiesDelegatorsAssigned += copiesAccount;
                continue;
            }

            copiesDelegatorsImplicit += copiesAccount;

            final Account accountParent = account.getParent();

            sb.append("\n");
            if (accountParent != null) {
                sb.append(accountParent.getName()).append(' ');
                // Do not use regular '\' since this is a line break.
                // Use Unicode Character 'REVERSE SOLIDUS' (U+005C) instead.
                sb.append(CHAR_REVERSE_SOLIDUS);
                sb.append(' ');
            }
            sb.append(account.getName()).append(" : ").append(copiesAccount);
        }

        // Delegator copies explicit to USER account.
        final int copiesDelegatorsExplicit =
                job.getCopies() - copiesDelegatorsImplicit;

        if (copiesDelegatorsExplicit > 0) {
            sb.append("\n").append(NounEnum.USER.uiText(this.locale, true))
                    .append(" : ").append(copiesDelegatorsExplicit);

            /*
             * Give details of copies for individual users if ALL explicit
             * copies are assigned copies.
             */
            if (copiesDelegatorsExplicit == copiesDelegatorsAssigned) {
                sb.append("\n");
                int iWlk = 0;
                for (final Entry<String, Integer> entry : userAccountAssigned
                        .entrySet()) {
                    if (iWlk > 0) {
                        sb.append(", ");
                    }
                    sb.append(entry.getKey()).append(" (")
                            .append(entry.getValue()).append(")");
                    iWlk++;
                }
            }
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
