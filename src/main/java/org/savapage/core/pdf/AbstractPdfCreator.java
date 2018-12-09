/*
 * This file is part of the SavaPage project <https://www.savapage.org>.
 * Copyright (c) 2011-2018 Datraverse B.V.
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
package org.savapage.core.pdf;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.savapage.core.LetterheadNotFoundException;
import org.savapage.core.PostScriptDrmException;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.doc.PdfToPgpSignedPdf;
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
import org.savapage.core.print.proxy.BasePrintSheetCalcParms;
import org.savapage.core.print.proxy.ProxyPrintSheetsCalcParms;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.PGPPublicKeyService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.services.impl.InboxServiceImpl;
import org.savapage.lib.pgp.PGPBaseException;
import org.savapage.lib.pgp.PGPPublicKeyInfo;
import org.savapage.lib.pgp.PGPSecretKeyInfo;
import org.savapage.lib.pgp.pdf.PdfPgpVerifyUrl;

/**
 * Strategy for creating PDF document from inbox.
 *
 * @author Rijk Ravestein
 *
 */
public abstract class AbstractPdfCreator {

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
     * For future use, for now: do NOT encrypt since
     * {@link PdfPrintCollector#collect(ProxyPrintSheetsCalcParms, boolean, File, File)}
     * will fail.
     */
    private final boolean encryptForPrinting = false;

    /**
     * {@code true} when graphics are removed from PDF.
     */
    private boolean removeGraphics = false;

    /**
     * {@code true} when PDF EcoPrint shadow files are used.
     */
    private boolean useEcoPdfShadow = false;

    /**
     * {@code true} when Grayscale PDF is to be created.
     */
    private boolean isGrayscalePdf = false;

    /**
     * {@code true} if PDF with page porder for 2-up duplex booklet is to be
     * created.
     */
    private boolean isBookletPageOrder = false;

    /**
     *
     */
    protected String myPdfFileLetterhead = null;

    /**
     *
     */
    protected LetterheadInfo.LetterheadJob myLetterheadJob = null;

    /**
     * The {@link PdfOrientationInfo} of the first page of first job, used to
     * find the {@link IppRuleNumberUp}.
     */
    protected PdfOrientationInfo firstPageOrientationInfo;

    /** */
    private PdfPgpVerifyUrl verifyUrl;

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
     *
     * @return {@code true} when Grayscale PDF is to be created.
     */
    protected final boolean isGrayscalePdf() {
        return this.isGrayscalePdf;
    }

    /**
     * @return {@code true} if PDF with page porder for 2-up duplex booklet is
     *         to be created.
     */
    protected final boolean isBookletPageOrder() {
        return this.isBookletPageOrder;
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

    public static SpPdfPageProps pageProps(final String filePathPdf)
            throws PdfSecurityException, PdfValidityException {
        return create().getPageProps(filePathPdf);
    }

    protected abstract int getNumberOfPagesInPdfFile(final String filePathPdf);

    /**
     * Creates the {@link SpPdfPageProps} of an PDF document.
     *
     * @param filePathPdf
     *            The PDF document file path.
     * @return The {@link SpPdfPageProps}.
     * @throws PdfSecurityException
     *             When encrypted or password protected PDF document.
     * @throws PdfValidityException
     *             When the document isn't a valid PDF document.
     */
    protected abstract SpPdfPageProps getPageProps(final String filePathPdf)
            throws PdfSecurityException, PdfValidityException;

    /**
     * Creates an ordinal list of {@link SpPdfPageProps} of an PDF document.
     * Each entry on the list represents a chunk of pages with the same size.
     *
     * @param filePathPdf
     *            The PDF document file path.
     * @return The {@link SpPdfPageProps} list.
     * @throws PdfSecurityException
     *             When encrypted or password protected PDF document.
     */
    public abstract List<SpPdfPageProps> getPageSizeChunks(
            final String filePathPdf) throws PdfSecurityException;

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
     * @param jobPfdName
     *            PDF name.
     * @param userRotate
     *            The user rotate for the job.
     * @throws Exception
     *             When errors.
     */
    protected abstract void onInitJob(String jobPfdName, Integer userRotate)
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
     *
     * @throws Exception
     */
    protected abstract void onExitStamp() throws Exception;

    /**
     *
     * @param pdfLetterhead
     * @throws Exception
     */
    protected abstract void onStampLetterhead(final String pdfLetterhead)
            throws Exception;

    /**
     *
     * @throws Exception
     */
    protected abstract void onCompress() throws Exception;

    /**
     *
     */
    protected abstract void onStampEncryptionForPrinting();

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
     *
     * @param propPdf
     * @param ownerPass
     * @param userPass
     * @param hasVisitorText
     */
    protected abstract void onStampEncryptionForExport(
            final PdfProperties propPdf, final String ownerPass,
            final String userPass, boolean hasVisitorText);

    /**
     *
     */
    protected abstract void onStampRotateForPrinting() throws Exception;

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
            final Map<String, Integer> uuidPageCount, final DocLog docLog)
            throws LetterheadNotFoundException, PostScriptDrmException,
            EcoPrintPdfTaskPendingException {
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
        this.isBookletPageOrder = createReq.isBookletPageOrder();

        this.removeGraphics = createReq.isRemoveGraphics();

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

        /*
         *
         */
        onInit();

        // --------------------------------------------------------
        // Traverse the page ranges.
        // --------------------------------------------------------
        final List<InboxJobRange> pages = inboxInfo.getPages();

        final boolean doFillerPages =
                this.isForPrinting && createReq.isForPrintingFillerPages()
                        && INBOX_SERVICE.isInboxVanilla(inboxInfo);

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

        try {
            propPdf = USER_SERVICE.getPdfProperties(createReq.getUserObj());

            for (InboxJobRange page : pages) {

                nJobRangeWlk++;

                int totJobRangePages = 0;

                final InboxJob job = inboxInfo.getJobs().get(page.getJob());
                final String pdfFile = job.getFile();

                final String filePath = String.format("%s%c%s", this.userhome,
                        File.separatorChar, pdfFile);

                String jobPfdName = null;

                if (InboxServiceImpl.isPdfJobFilename(pdfFile)) {
                    jobPfdName = filePath;
                } else {
                    throw new SpException("unknown input job type");
                }

                if (this.useEcoPdfShadow) {
                    jobPfdName =
                            INBOX_SERVICE.createEcoPdfShadowPath(jobPfdName);
                }

                // Init
                onInitJob(jobPfdName, Integer.valueOf(job.getRotate()));

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

                    onProcessJobPages(nPageFrom, nPageTo, this.removeGraphics);

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

                onExitJob(fillerPagesToAppend);

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
                    final String uuid = FilenameUtils.getBaseName(pdfFile);
                    Integer totUuidPages = uuidPageCount.get(uuid);
                    if (totUuidPages == null) {
                        totUuidPages = Integer.valueOf(0);
                    }
                    uuidPageCount.put(uuid, Integer.valueOf(
                            totUuidPages.intValue() + totJobRangePages));
                }
            }

            onExitJobs();

            onInitStamp();

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
                onStampMetaDataForExport(now, propPdf);

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

                onStampMetaDataForPrinting(now, propPdf);

            }

            // --------------------------------------------------------
            // Visitor text (init)
            // --------------------------------------------------------
            boolean hasVisitorText = false; // TODO

            // --------------------------------------------------------
            // Encryption
            // --------------------------------------------------------
            if (createReq.isApplyPdfProps()) {

                final boolean applyPasswords =
                        propPdf.getApply().getPasswords();
                final boolean applyEncryption =
                        propPdf.getApply().getEncryption();

                String ownerPass = propPdf.getPw().getOwner();
                String userPass = propPdf.getPw().getUser();
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

                // TODO: for later, if to be auto-generated
                // ownerPass = java.util.UUID.randomUUID().toString();

                boolean hasEncryption = !(ownerPass.isEmpty()
                        && userPass.isEmpty() && encryption.isEmpty());

                if (docLog != null) {
                    docLog.setDrmRestricted(hasEncryption);
                }

                if (hasEncryption) {
                    onStampEncryptionForExport(propPdf, ownerPass, userPass,
                            hasVisitorText);
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

            } else if (createReq.isForPrinting() && encryptForPrinting) {

                onStampEncryptionForPrinting();

                if (docLog != null) {
                    docLog.setDrmRestricted(true);
                }
            }

            // --------------------------------------------------------
            // Visitor text (apply)
            // --------------------------------------------------------
            if (hasVisitorText) {
                // imposeVisitorText(destination);
            }

            // --------------------------------------------------------
            // Letterhead
            // --------------------------------------------------------
            boolean letterheadApplied = false;

            if (myPdfFileLetterhead != null) {
                onStampLetterhead(myPdfFileLetterhead);
                letterheadApplied = true;
            }
            if (docOut != null) {
                docOut.setLetterhead(letterheadApplied);
            }

            // --------------------------------------------------------
            // Make sure everything is printed in portrait
            // --------------------------------------------------------
            if (createReq.isForPrinting()) {
                onStampRotateForPrinting();
            }

            // --------------------------------------------------------
            // Compress
            // --------------------------------------------------------
            if (!createReq.isForPrinting()) {
                onCompress();
            }

            onExitStamp();

            /*
             * End
             */
            onExit();

        } catch (Exception e) {
            throw new SpException(e.getMessage(), e);

        } finally {
            onProcessFinally();
        }

        final File generatedPdf = new File(pdfFile);

        final boolean isPgpSigned = !this.isForPrinting()
                && BooleanUtils.isTrue(propPdf.isPgpSignature())
                && this.verifyUrl != null;

        try {
            onPdfGenerated(generatedPdf);
            if (isPgpSigned) {
                onPgpSign(generatedPdf, this.verifyUrl, this.user);
            }
        } catch (Exception e) {
            throw new SpException(e.getMessage(), e);
        }

        final PdfCreateInfo createInfo = new PdfCreateInfo(generatedPdf);

        createInfo.setBlankFillerPages(totFillerPages);
        createInfo.setLogicalJobPages(logicalJobPages);
        createInfo.setPdfOrientationInfo(this.firstPageOrientationInfo);
        createInfo.setPgpSigned(isPgpSigned);

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
     * @throws IOException
     *             When IO error.
     */
    private static void onPgpSign(final File generatedPdf,
            final PdfPgpVerifyUrl verifyUrl, final String userid)
            throws IOException {

        final ConfigManager cm = ConfigManager.instance();

        final PGPSecretKeyInfo secKeyInfo = cm.getPGPSecretKeyInfo();
        final PGPPublicKeyInfo pubKeyInfoSigner = cm.getPGPPublicKeyInfo();

        try {
            replaceWithConvertedPdf(generatedPdf,
                    new PdfToPgpSignedPdf(secKeyInfo, pubKeyInfoSigner,
                            PGP_PUBLICKEY_SERVICE.readRingEntry(userid),
                            verifyUrl).convert(generatedPdf));
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

        final Path source = FileSystems.getDefault()
                .getPath(pdfConverted.getAbsolutePath());

        final Path target =
                FileSystems.getDefault().getPath(pdfOrginal.getAbsolutePath());

        Files.move(source, target,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

}
