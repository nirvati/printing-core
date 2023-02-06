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
package org.savapage.core.pdf;

import java.awt.print.PageFormat;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.savapage.core.LetterheadNotFoundException;
import org.savapage.core.PostScriptDrmException;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.dao.enums.ACLOidEnum;
import org.savapage.core.dao.enums.ACLPermissionEnum;
import org.savapage.core.doc.PdfToPgpSignedPdf;
import org.savapage.core.dto.UserIdDto;
import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJob;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.inbox.LetterheadInfo;
import org.savapage.core.inbox.PdfOrientationInfo;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.ipp.rules.IppRuleNumberUp;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PdfOut;
import org.savapage.core.jpa.User;
import org.savapage.core.json.PdfProperties;
import org.savapage.core.json.PdfProperties.PdfAllow;
import org.savapage.core.json.PdfProperties.PdfPasswords;
import org.savapage.core.print.proxy.BasePrintSheetCalcParms;
import org.savapage.core.services.AccessControlService;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.PGPPublicKeyService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.services.impl.InboxServiceImpl;
import org.savapage.core.util.FileSystemHelper;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.savapage.lib.pgp.PGPSecretKeyInfo;
import org.savapage.lib.pgp.pdf.PdfPgpHelper;
import org.savapage.lib.pgp.pdf.PdfPgpHelperAnyone;
import org.savapage.lib.pgp.pdf.PdfPgpSigner;
import org.savapage.lib.pgp.pdf.PdfPgpVerifyUrl;

/**
 * Strategy for creating PDF document from inbox.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractPdfCreator {

    /** */
    private static final AccessControlService ACCESS_CONTROL_SERVICE =
            ServiceContext.getServiceFactory().getAccessControlService();

    /** */
    private static final InboxService INBOX_SERVICE =
            ServiceContext.getServiceFactory().getInboxService();
    /** */
    private static final PGPPublicKeyService PGP_PUBLICKEY_SERVICE =
            ServiceContext.getServiceFactory().getPGPPublicKeyService();
    /** */
    private static final UserService USER_SERVICE =
            ServiceContext.getServiceFactory().getUserService();

    /**
     * .
     */
    protected String user;
    protected String userhome;
    protected String pdfFile;

    /**
     * The global (application) temporary directory.
     */
    protected String appTmpDir;

    private boolean isForPrinting = false;

    private int printNup;

    /**
     * {@code true} when graphics are removed from PDF.
     */
    private boolean removeGraphics = false;

    /**
     * {@code true} when PDF EcoPrint shadow files are used.
     */
    private boolean useEcoPdfShadow = false;

    /**
     * {@code true} if Grayscale PDF is to be created.
     */
    private boolean isGrayscalePdf = false;

    /**
     * {@code true} if PDF is to be rasterized.
     */
    private boolean isRasterizedPdf = false;

    /**
     * {@code true} if PDF with page porder for 2-up duplex booklet is to be
     * created.
     */
    private boolean isBookletPageOrder = false;

    /**
     * {@code true} if PDF for print has to be repaired.
     */
    private boolean isRepairPdf = false;

    /**
     *
     */
    protected String myPdfFileLetterhead = null;

    /**
     *
     */
    protected LetterheadInfo.LetterheadJob myLetterheadJob = null;

    /**
     * The {@link PageFormat} of the first page of first job.
     */
    protected PageFormat firstPageFormat;

    /**
     * The {@link PdfOrientationInfo} of the first page of first job, used to
     * find the {@link IppRuleNumberUp}.
     */
    protected PdfOrientationInfo firstPageOrientationInfo;

    /** */
    private PdfPgpVerifyUrl verifyUrl;

    /**
     *
     * @return {@code true} if letterhead is to be applied.
     */
    protected boolean isApplyLetterhead() {
        return this.myPdfFileLetterhead != null;
    }

    /**
     *
     * @return {@code true} when PDF is created for proxy printing.
     */
    protected boolean isForPrinting() {
        return this.isForPrinting;
    }

    /**
     *
     * @return Get print number-up.
     */
    protected int getPrintNup() {
        return this.printNup;
    }

    /**
     * @return {@code true} if Grayscale PDF is to be created.
     */
    protected final boolean isGrayscalePdf() {
        return this.isGrayscalePdf;
    }

    /**
     * @return {@code true} if PDF has to be rasterized.
     */
    protected final boolean isRasterizedPdf() {
        return this.isRasterizedPdf;
    }

    /**
     * @return {@code true} if PDF with page porder for 2-up duplex booklet is
     *         to be created.
     */
    protected final boolean isBookletPageOrder() {
        return this.isBookletPageOrder;
    }

    /**
     * @return {@code true} if PDF (for print) has to be repaired.
     */
    protected final boolean isRepairPdf() {
        return this.isRepairPdf;
    }

    /**
     *
     * @return
     */
    public static AbstractPdfCreator create() {
        return new ITextPdfCreator();
    }

    public static int pageCountInPdfFile(final String filePathPdf) {
        return create().getNumberOfPagesInPdfFile(filePathPdf);
    }

    /**
     * @return The Creator string visible in the PDF properties of PDF Reader.
     */
    public static String getCreatorString() {
        return String.format("%s %s • %s • %s",
                CommunityDictEnum.BRAND_NAME.getWord(),
                ConfigManager.getAppVersion(),
                CommunityDictEnum.BRAND_SLOGAN.getWord(),
                CommunityDictEnum.BRAND_WEBSITE.getWord());
    }

    /**
     *
     * @param filePathPdf
     *            PDF file path.
     * @return {@link SpPdfPageProps}.
     * @throws PdfValidityException
     *             When invalid PDF document.
     * @throws PdfSecurityException
     *             When encrypted PDF document.
     * @throws PdfPasswordException
     *             When password protected PDF document.
     * @throws PdfUnsupportedException
     *             When unsupported PDF document.
     */
    public static SpPdfPageProps pageProps(final String filePathPdf)
            throws PdfValidityException, PdfSecurityException,
            PdfPasswordException, PdfUnsupportedException {
        return create().getPageProps(filePathPdf);
    }

    /**
     *
     * @param filePathPdf
     *            PDF file path.
     * @return Number of pages in PDF.
     */
    protected abstract int getNumberOfPagesInPdfFile(String filePathPdf);

    /**
     * Creates the {@link SpPdfPageProps} of an PDF document.
     *
     * @param filePathPdf
     *            The PDF document file path.
     * @return The {@link SpPdfPageProps}.
     * @throws PdfValidityException
     *             When invalid PDF document.
     * @throws PdfSecurityException
     *             When encrypted PDF document.
     * @throws PdfPasswordException
     *             When password protected PDF document.
     * @throws PdfUnsupportedException
     *             When unsupported PDF document.
     */
    protected abstract SpPdfPageProps getPageProps(String filePathPdf)
            throws PdfValidityException, PdfSecurityException,
            PdfPasswordException, PdfUnsupportedException;

    /**
     *
     */
    protected abstract void onInit();

    /**
     * .
     *
     * @throws Exception
     */
    protected abstract void onExit() throws Exception;

    /**
     *
     * @param jobPdfName
     *            PDF name.
     * @param userRotate
     *            The user rotate for the job.
     * @throws Exception
     *             When errors.
     */
    protected abstract void onInitJob(String jobPdfName, Integer userRotate)
            throws Exception;

    /**
     *
     * @param nPageFrom
     * @param nPageTo
     * @throws Exception
     */
    protected abstract void onProcessJobPages(int nPageFrom, int nPageTo,
            boolean removeGraphics) throws Exception;

    /**
     *
     * @param blankPagesToAppend
     *            The number of blank pages to append to the end of the output
     *            document.
     * @throws Exception
     */
    protected abstract void onExitJob(int blankPagesToAppend) throws Exception;

    /**
     *
     * @throws Exception
     */
    protected abstract void onExitJobs() throws Exception;

    /**
     *
     * @throws Exception
     */
    protected abstract void onInitStamp() throws Exception;

    /**
     * @param pageOverlay
     *            Base64 encoded SVG overlay (value) for one-based ordinal pages
     *            (key) of the job pages.
     * @throws Exception
     *             If error.
     */
    protected abstract void onExitStamp(Map<Integer, String> pageOverlay)
            throws Exception;

    /**
     *
     * @param pdfLetterhead
     * @throws Exception
     */
    protected abstract void onStampLetterhead(String pdfLetterhead)
            throws Exception;

    /**
     *
     * @throws Exception
     */
    protected abstract void onCompress() throws Exception;

    /**
     *
     */
    protected abstract void onProcessFinally();

    /**
     *
     * @param pdfFile
     *            The generated PDF file.
     * @throws Exception
     */
    protected abstract void onPdfGenerated(File pdfFile) throws Exception;

    /**
     *
     * @param now
     * @param propPdf
     */
    protected abstract void onStampMetaDataForExport(Calendar now,
            PdfProperties propPdf);

    /**
     *
     * @param now
     * @param propPdf
     */
    protected abstract void onStampMetaDataForPrinting(Calendar now,
            PdfProperties propPdf);

    /**
     * @param propPdfAllow
     *            PDF allowed properties.
     * @param ownerPass
     *            Owner password.
     * @param userPass
     *            User password.
     */
    protected abstract void onStampEncryptionForExport(
            PdfProperties.PdfAllow propPdfAllow, String ownerPass,
            String userPass);

    /**
     *
     * Generates PDF file from the edited jobs for a user.
     *
     * @param createReq
     *            The {@link PdfCreateRequest}.
     * @param uuidPageCount
     *            This object will be filled with the number of selected pages
     *            per input file UUID. A value of {@code null} is allowed.
     * @param docLog
     *            The DocLog object to collect data on. A value of {@code null}
     *            is allowed: in that case no data is collected.
     * @return {@link PdfCreateInfo}.
     *
     * @throws LetterheadNotFoundException
     *             When an attached letterhead cannot be found.
     * @throws PostScriptDrmException
     *             When the generated PDF is for export (i.e. not for printing)
     *             and one of the SafePages is DRM-restricted.
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    public PdfCreateInfo generate(final PdfCreateRequest createReq,
            final LinkedHashMap<String, Integer> uuidPageCount,
            final DocLog docLog) throws LetterheadNotFoundException,
            PostScriptDrmException, EcoPrintPdfTaskPendingException {
        //
        final boolean isUserInboxEditor =
                ACCESS_CONTROL_SERVICE.hasPermission(createReq.getUserObj(),
                        ACLOidEnum.U_INBOX, ACLPermissionEnum.EDITOR);
        //
        this.user = createReq.getUserObj().getUserId();
        this.userhome = ConfigManager.getUserHomeDir(this.user);
        this.appTmpDir = ConfigManager.getAppTmpDir();
        //
        final InboxInfoDto inboxInfo = createReq.getInboxInfo();

        this.useEcoPdfShadow = createReq.isEcoPdfShadow();

        this.pdfFile = createReq.getPdfFile();

        this.isForPrinting = createReq.isForPrinting();
        this.printNup = createReq.getPrintNup();

        this.isGrayscalePdf = createReq.isGrayscale();
        this.isRasterizedPdf = createReq.isRasterized();
        this.isBookletPageOrder = createReq.isBookletPageOrder();

        this.isRepairPdf = createReq.isForPrinting() && ConfigManager.instance()
                .isConfigValue(IConfigProp.Key.PROXY_PRINT_REPAIR_ENABLE);

        this.removeGraphics = createReq.isRemoveGraphics();

        this.firstPageFormat = null;
        this.firstPageOrientationInfo = null;

        /*
         * INVARIANT: if PDF is meant for export, DRM-restricted content is not
         * allowed.
         */
        if (!createReq.isForPrinting()) {

            for (final InboxInfoDto.InboxJob wlk : inboxInfo.getJobs()) {
                if (wlk.getDrm()) {
                    throw new PostScriptDrmException(
                            "SafePages contain DRM-restricted content: "
                                    + "PDF export is not permitted");
                }
            }
            this.verifyUrl = createReq.getVerifyUrl();
        }

        /*
         * INVARIANT: if letterhead is selected the PDF must be present.
         */
        this.myPdfFileLetterhead = null;

        if (createReq.isApplyLetterhead()) {

            final InboxInfoDto.InboxLetterhead lh = inboxInfo.getLetterhead();

            if (lh != null) {

                final User userWrk;
                final String location;

                if (lh.isPublic()) {
                    userWrk = null;
                    location = INBOX_SERVICE.getLetterheadLocation(null);
                } else {
                    userWrk = createReq.getUserObj();
                    location = INBOX_SERVICE.getLetterheadsDir(this.user);
                }

                this.myPdfFileLetterhead =
                        String.format("%s/%s", location, lh.getId());

                this.myLetterheadJob =
                        INBOX_SERVICE.getLetterhead(userWrk, lh.getId());

                if (this.myLetterheadJob == null) {
                    throw LetterheadNotFoundException.create(lh.isPublic(),
                            lh.getId());
                }
            }
        }

        /*
         * INVARIANT: if Eco Print shadow PDFs are used they must be present.
         */
        if (this.useEcoPdfShadow) {
            final int nTasksWaiting = INBOX_SERVICE
                    .lazyStartEcoPrintPdfTasks(this.userhome, inboxInfo);
            if (nTasksWaiting > 0) {
                throw new EcoPrintPdfTaskPendingException(String.format(
                        "%d EcoPrint conversion(s) waiting", nTasksWaiting));
            }
        }

        /* */
        onInit();

        // --------------------------------------------------------
        // Traverse the page ranges.
        // --------------------------------------------------------
        final List<InboxJobRange> pages = inboxInfo.getPages();

        final boolean doFillerPages =
                this.isForPrinting && createReq.isForPrintingFillerPages()
                        && INBOX_SERVICE.isInboxVanilla(inboxInfo);

        final boolean isPageOverlayAllowed =
                USER_SERVICE.hasSavaPageDrawPermission(
                        UserIdDto.create(createReq.getUserObj()));

        final int nJobRangeTot = pages.size();
        int nJobRangeWlk = 0;
        int totFillerPages = 0;

        final List<Integer> logicalJobPages;

        if (doFillerPages) {
            logicalJobPages = new ArrayList<>();
        } else {
            logicalJobPages = null;
        }

        final PdfProperties propPdf;
        final boolean isPgpSigned;

        boolean hasEncryption = false;
        PdfAllow pdfAllow = null;
        String ownerPass = null;
        String userPass = null;

        //
        final Map<Integer, String> pageOverlayMap = new HashMap<>();
        int nPageOverlayWlk = 0;

        try {
            propPdf = USER_SERVICE.getPdfProperties(createReq.getUserObj());

            isPgpSigned = !this.isForPrinting()
                    && BooleanUtils.isTrue(propPdf.isPgpSignature())
                    && this.verifyUrl != null;

            for (InboxJobRange page : pages) {

                nJobRangeWlk++;

                int totJobRangePages = 0;

                final InboxJob job = inboxInfo.getJobs().get(page.getJob());
                final String pdfFileWlk = job.getFile();

                final Map<Integer, InboxInfoDto.PageOverlay> pdfPageOverlayWlk;
                if (isUserInboxEditor) {
                    pdfPageOverlayWlk = job.getOverlay();
                } else {
                    pdfPageOverlayWlk = null;
                }

                final String filePath = String.format("%s%c%s", this.userhome,
                        File.separatorChar, pdfFileWlk);

                String jobPfdName = null;

                if (InboxServiceImpl.isPdfJobFilename(pdfFileWlk)) {
                    jobPfdName = filePath;
                } else {
                    throw new SpException("unknown input job type");
                }

                if (this.useEcoPdfShadow) {
                    jobPfdName =
                            INBOX_SERVICE.createEcoPdfShadowPath(jobPfdName);
                }

                // Init
                this.onInitJob(jobPfdName, Integer.valueOf(job.getRotate()));

                final List<RangeAtom> ranges =
                        INBOX_SERVICE.createSortedRangeArray(page.getRange());

                // Page ranges
                for (RangeAtom rangeAtom : ranges) {

                    final int nPageFrom = (rangeAtom.pageBegin == null ? 1
                            : rangeAtom.pageBegin);

                    if (rangeAtom.pageEnd == null) {
                        rangeAtom.pageEnd = inboxInfo.getJobs()
                                .get(page.getJob()).getPages();
                    }

                    final int nPageTo = rangeAtom.pageEnd;
                    final int nPagesinAtom = nPageTo - nPageFrom + 1;

                    if (isPageOverlayAllowed) {
                        // Traverse page-by-page to find page overlay.
                        for (int i = nPageFrom - 1; i < nPageTo; i++) {
                            nPageOverlayWlk++;
                            if (pdfPageOverlayWlk != null
                                    && !pdfPageOverlayWlk.isEmpty()) {
                                final InboxInfoDto.PageOverlay pageOverlay =
                                        pdfPageOverlayWlk
                                                .get(Integer.valueOf(i));
                                if (pageOverlay != null
                                        && pageOverlay.getSvg64() != null) {
                                    pageOverlayMap.put(
                                            Integer.valueOf(nPageOverlayWlk),
                                            pageOverlay.getSvg64());
                                }
                            }
                        }
                    }
                    //
                    this.onProcessJobPages(nPageFrom, nPageTo,
                            this.removeGraphics);

                    totJobRangePages += nPagesinAtom;
                }

                /*
                 * The number of blank filler pages to append to the end of this
                 * job part.
                 */
                final int fillerPagesToAppend;

                if (doFillerPages && nJobRangeTot > 1
                        && nJobRangeWlk < nJobRangeTot) {

                    final BasePrintSheetCalcParms calcParms =
                            new BasePrintSheetCalcParms();

                    calcParms.setNumberOfPages(totJobRangePages);
                    calcParms.setDuplex(createReq.isPrintDuplex());
                    calcParms.setNumberOfCopies(nJobRangeTot);
                    calcParms.setNup(createReq.getPrintNup());

                    fillerPagesToAppend = PdfPrintCollector
                            .calcBlankAppendPagesOfCopy(calcParms);

                } else {
                    fillerPagesToAppend = 0;
                }

                totFillerPages += fillerPagesToAppend;

                this.onExitJob(fillerPagesToAppend);

                /*
                 * Update grand totals.
                 */
                if (logicalJobPages != null) {
                    logicalJobPages.add(Integer.valueOf(totJobRangePages));
                }

                if (uuidPageCount != null) {
                    /*
                     * The base name of the file is the UUID as registered in
                     * the database (DocIn table).
                     */
                    final String uuid = FilenameUtils.getBaseName(pdfFileWlk);
                    Integer totUuidPages = uuidPageCount.get(uuid);
                    if (totUuidPages == null) {
                        totUuidPages = Integer.valueOf(0);
                    }
                    uuidPageCount.put(uuid, Integer.valueOf(
                            totUuidPages.intValue() + totJobRangePages));
                }
            } // end-for

            this.onExitJobs();

            this.onInitStamp();

            // --------------------------------------------------------
            // Prepare document logging.
            // --------------------------------------------------------
            final DocOut docOut;

            if (docLog == null) {
                docOut = null;
            } else {
                docOut = new DocOut();
                docLog.setDocOut(docOut);
                docOut.setDocLog(docLog);

                docOut.setEcoPrint(Boolean.valueOf(this.useEcoPdfShadow));
                docOut.setRemoveGraphics(Boolean.valueOf(this.removeGraphics));
            }

            // --------------------------------------------------------
            // Document Information
            // --------------------------------------------------------
            final Calendar now = new GregorianCalendar();

            if (docLog != null) {
                docLog.setTitle(propPdf.getDesc().getTitle());
            }

            if (createReq.isApplyPdfProps()) {
                this.onStampMetaDataForExport(now, propPdf);

                if (docOut != null) {

                    final PdfOut out = new PdfOut();

                    out.setAuthor(propPdf.getDesc().getAuthor());

                    if (propPdf.getApply().getKeywords()) {
                        out.setKeywords(propPdf.getDesc().getKeywords());
                    }
                    if (propPdf.getApply().getSubject()) {
                        out.setSubject(propPdf.getDesc().getSubject());
                    }

                    docOut.setPdfOut(out);
                    out.setDocOut(docOut);
                }

            } else if (createReq.isForPrinting()) {

                this.onStampMetaDataForPrinting(now, propPdf);

            }

            // --------------------------------------------------------
            // Letterhead (before encryption!)
            // --------------------------------------------------------
            boolean letterheadApplied = false;

            if (this.isApplyLetterhead()) {
                this.onStampLetterhead(this.myPdfFileLetterhead);
                letterheadApplied = true;
            }
            if (docOut != null) {
                docOut.setLetterhead(letterheadApplied);
            }

            // --------------------------------------------------------
            // Encryption
            // --------------------------------------------------------
            if (createReq.isApplyPdfProps()) {

                final boolean applyPasswords =
                        propPdf.getApply().getPasswords();
                final boolean applyEncryption =
                        propPdf.getApply().getEncryption();

                ownerPass = propPdf.getPw().getOwner();
                userPass = propPdf.getPw().getUser();
                String encryption = propPdf.getEncryption();

                if (ownerPass == null || !applyPasswords) {
                    ownerPass = "";
                }
                if (userPass == null || !applyPasswords) {
                    userPass = "";
                }
                if (encryption == null || !applyEncryption) {
                    encryption = "";
                }

                if (encryption.isEmpty()) {
                    pdfAllow = PdfAllow.createAllowAll();
                } else {
                    pdfAllow = propPdf.getAllow();
                }

                hasEncryption = !(ownerPass.isEmpty() && userPass.isEmpty()
                        && encryption.isEmpty());

                if (docLog != null) {
                    docLog.setDrmRestricted(hasEncryption);
                }

                /*
                 * PDF encryption must be part of last PDF (PGP sign) action.
                 */
                if (hasEncryption && !isPgpSigned) {
                    this.onStampEncryptionForExport(pdfAllow, ownerPass,
                            userPass);
                }

                if (docOut != null) {

                    final PdfOut out = docOut.getPdfOut();

                    out.setEncrypted(hasEncryption);

                    if (!ownerPass.isEmpty()) {
                        out.setPasswordOwner(
                                PdfProperties.PdfPasswords.encrypt(ownerPass));
                    }
                    if (!userPass.isEmpty()) {
                        out.setPasswordUser(
                                PdfProperties.PdfPasswords.encrypt(userPass));
                    }
                }

            }
            // --------------------------------------------------------
            // Compress
            // --------------------------------------------------------
            if (!createReq.isForPrinting()) {
                this.onCompress();
            }

            this.onExitStamp(pageOverlayMap);

            /*
             * End
             */
            this.onExit();

        } catch (Exception e) {
            throw new SpException(e.getMessage(), e);

        } finally {
            this.onProcessFinally();
        }

        final File generatedPdf = new File(this.pdfFile);

        try {
            this.onPdfGenerated(generatedPdf);
            if (isPgpSigned) {
                onPgpSign(generatedPdf, this.verifyUrl, this.user,
                        hasEncryption, pdfAllow, ownerPass, userPass);
            }
        } catch (Exception e) {
            throw new SpException(e.getMessage(), e);
        }

        final PdfCreateInfo createInfo = new PdfCreateInfo(generatedPdf);

        createInfo.setBlankFillerPages(totFillerPages);
        createInfo.setLogicalJobPages(logicalJobPages);
        createInfo.setFirstPageFormat(this.firstPageFormat);
        createInfo.setPdfOrientationInfo(this.firstPageOrientationInfo);
        createInfo.setPgpSigned(isPgpSigned);
        createInfo.setUuidPageCount(uuidPageCount);

        return createInfo;
    }

    /**
     *
     * @param generatedPdf
     *            The PDF.
     * @param verifyUrl
     *            The verification URL.
     * @param userid
     *            The User ID of the PDF author.
     * @param pdfEncryption
     *            If {@code true}, encryption is applied.
     * @param pdfAllow
     *            PDF allowed properties.
     * @param pdfOwnerPass
     *            PDF owner password.
     * @param pdfUserPass
     *            PDF user password.
     * @throws IOException
     *             When IO error.
     */
    private static void onPgpSign(final File generatedPdf,
            final PdfPgpVerifyUrl verifyUrl, final String userid,
            final boolean pdfEncryption, final PdfProperties.PdfAllow pdfAllow,
            final String pdfOwnerPass, final String pdfUserPass)
            throws IOException {

        final ConfigManager cm = ConfigManager.instance();

        final PGPSecretKeyInfo secKeyInfo = cm.getPGPSecretKeyInfo();
        final PGPPublicKeyInfo pubKeyInfoSigner = cm.getPGPPublicKeyInfo();

        try {
            boolean isForAnyone = false; // TODO

            final PdfPgpSigner signer;
            if (isForAnyone) {
                signer = PdfPgpHelperAnyone.instance();
            } else {
                signer = PdfPgpHelper.instance();
            }

            final PdfProperties encryptionProps;

            if (pdfEncryption) {
                final PdfPasswords pw = new PdfPasswords();
                pw.setOwner(pdfOwnerPass);
                pw.setUser(pdfUserPass);
                encryptionProps = new PdfProperties();
                encryptionProps.setPw(pw);
                encryptionProps.setAllow(pdfAllow);
                encryptionProps.setApply(null);
                encryptionProps.setDesc(null);
            } else {
                encryptionProps = null;
            }

            replaceWithConvertedPdf(generatedPdf,
                    new PdfToPgpSignedPdf(signer, secKeyInfo, pubKeyInfoSigner,
                            PGP_PUBLICKEY_SERVICE.readRingEntry(userid),
                            verifyUrl, encryptionProps).convert(generatedPdf));

        } catch (PGPBaseException e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Replaces original PDF file with converted version.
     *
     * @param pdfOrginal
     *            The original PDF file.
     * @param pdfConverted
     *            The converted PDF file.
     * @throws IOException
     *             When IO error.
     */
    protected static void replaceWithConvertedPdf(final File pdfOrginal,
            final File pdfConverted) throws IOException {
        FileSystemHelper.replaceWithNewVersion(pdfOrginal, pdfConverted);
    }

}
