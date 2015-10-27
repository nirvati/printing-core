/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.pdf;

import java.io.File;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.savapage.core.LetterheadNotFoundException;
import org.savapage.core.PostScriptDrmException;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.doc.DocContent;
import org.savapage.core.imaging.EcoPrintPdfTask;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJob;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.inbox.LetterheadInfo;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.DocOut;
import org.savapage.core.jpa.PdfOut;
import org.savapage.core.jpa.User;
import org.savapage.core.json.PdfProperties;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.services.impl.InboxServiceImpl;

/**
 * Strategy for creating PDF document from inbox.
 *
 * @author Datraverse B.V.
 *
 */
public abstract class AbstractPdfCreator {

    /**
     * .
     */
    private static final InboxService INBOX_SERVICE = ServiceContext
            .getServiceFactory().getInboxService();
    /**
     * .
     */
    private static final UserService USER_SERVICE = ServiceContext
            .getServiceFactory().getUserService();

    /**
     * .
     */
    protected String user;
    protected String userhome;
    protected String tmpdir;
    protected String pdfFile;

    private boolean isForPrinting = false;

    /**
     * {@code true} when PDF is converted on the fly to EcoImages.
     */
    private boolean convertToEcoPdf = false;

    /**
     * {@code true} when PDF EcoPrint shadow files are used.
     */
    private boolean useEcoPdfShadow = false;

    /**
     * {@code true} when Grayscale PDF is to be created.
     */
    private boolean isGrayscalePdf = false;

    /**
     *
     */
    protected String myPdfFileLetterhead = null;

    /**
     *
     */
    protected LetterheadInfo.LetterheadJob myLetterheadJob = null;

    /**
     *
     * @return {@code true} when PDF is created for proxy printing.
     */
    protected boolean isForPrinting() {
        return this.isForPrinting;
    }

    /**
     *
     * @param convert
     *            {@code true} when PDF is converted on the fly to EcoImages.
     */
    protected final void setConvertToEcoPdf(final boolean convert) {
        this.convertToEcoPdf = convert;
    }

    /**
     *
     * @return {@code true} when PDF is converted on the fly to EcoImages.
     */
    protected final boolean isConvertToEcoPdf() {
        return this.convertToEcoPdf;
    }

    /**
     *
     * @return {@code true} when Grayscale PDF is to be created.
     */
    protected final boolean isGrayscalePdf() {
        return this.isGrayscalePdf;
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
     * @param rotation
     * @throws Exception
     */
    protected abstract void onInitJob(final String jobPfdName,
            final String rotation) throws Exception;

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
     * @throws Exception
     */
    protected abstract void onExitJob() throws Exception;

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
     * @return {@code true} if encryption is applied.
     */
    protected abstract boolean onStampEncryptionForPrinting();

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
     * @return File object with generated PDF.
     * @throws LetterheadNotFoundException
     *             When an attached letterhead cannot be found.
     * @throws PostScriptDrmException
     *             When the generated PDF is for export (i.e. not for printing)
     *             and one of the SafePages is DRM-restricted.
     * @throws EcoPrintPdfTaskPendingException
     *             When {@link EcoPrintPdfTask} objects needed for this PDF are
     *             pending.
     */
    public File generate(final PdfCreateRequest createReq,
            final Map<String, Integer> uuidPageCount, final DocLog docLog)
            throws LetterheadNotFoundException, PostScriptDrmException,
            EcoPrintPdfTaskPendingException {
        //
        this.user = createReq.getUserObj().getUserId();
        this.userhome = ConfigManager.getUserHomeDir(this.user);
        this.tmpdir = ConfigManager.getUserTempDir(this.user);

        //
        final InboxInfoDto inboxInfo = createReq.getInboxInfo();

        this.useEcoPdfShadow = createReq.isEcoPdfShadow();

        this.convertToEcoPdf =
                !createReq.isEcoPdfShadow() && createReq.isEcoPdf();

        this.pdfFile = createReq.getPdfFile();
        this.isForPrinting = createReq.isForPrinting();

        this.isGrayscalePdf = createReq.isGrayscale();

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
            final int nTasksWaiting =
                    INBOX_SERVICE.lazyStartEcoPrintPdfTasks(this.userhome,
                            inboxInfo);
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

        try {

            for (InboxJobRange page : pages) {

                final InboxJob job = inboxInfo.getJobs().get(page.getJob());

                final String file = job.getFile();
                /*
                 * Type of job?
                 */
                if (InboxServiceImpl.isScanJobFilename(file)) {
                    throw new SpException("Scan job type NOT supported yet.");
                }

                /*
                 * The base name of the file is the UUID as registered in the
                 * database (DocIn table).
                 */
                String uuid = null;
                Integer totUuidPages = null;

                if (uuidPageCount != null) {
                    uuid = FilenameUtils.getBaseName(file);
                    totUuidPages = uuidPageCount.get(uuid);
                    if (totUuidPages == null) {
                        totUuidPages = Integer.valueOf(0);
                    }
                }
                /*
                 *
                 */
                final String filePath =
                        String.format("%s/%s", this.userhome, file);

                String jobPfdName = null;

                if (InboxServiceImpl.isPdfJobFilename(file)) {

                    jobPfdName = filePath;

                } else if (InboxServiceImpl.isPsJobFilename(file)) {

                    final String regexReplace =
                            "\\." + DocContent.FILENAME_EXT_PS;
                    final String replacement =
                            "." + DocContent.FILENAME_EXT_PDF;

                    jobPfdName =
                            String.format(
                                    "%s/%s",
                                    this.tmpdir,
                                    FilenameUtils.getName(filePath)
                                            .replaceFirst(regexReplace,
                                                    replacement));

                } else {
                    throw new SpException("unknown input job type");
                }

                if (this.useEcoPdfShadow) {
                    jobPfdName =
                            INBOX_SERVICE.createEcoPdfShadowPath(jobPfdName);
                }

                onInitJob(jobPfdName, job.getRotate());

                final List<RangeAtom> ranges =
                        INBOX_SERVICE.createSortedRangeArray(page.getRange());

                for (RangeAtom rangeAtom : ranges) {

                    int nPageFrom =
                            (rangeAtom.pageBegin == null ? 1
                                    : rangeAtom.pageBegin);

                    if (rangeAtom.pageEnd == null) {
                        rangeAtom.pageEnd =
                                inboxInfo.getJobs().get(page.getJob())
                                        .getPages();
                    }

                    int nPageTo = rangeAtom.pageEnd;

                    onProcessJobPages(nPageFrom, nPageTo,
                            createReq.isRemoveGraphics());

                    if (uuidPageCount != null) {
                        totUuidPages += nPageTo - nPageFrom + 1;
                    }

                }

                onExitJob();

                if (uuidPageCount != null) {
                    uuidPageCount.put(uuid, totUuidPages);
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
            }

            // --------------------------------------------------------
            // Document Information
            // --------------------------------------------------------
            Calendar now = new GregorianCalendar();

            final PdfProperties propPdf =
                    USER_SERVICE.getPdfProperties(createReq.getUserObj());

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

                boolean hasEncryption =
                        !(ownerPass.isEmpty() && userPass.isEmpty() && encryption
                                .isEmpty());

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
                        out.setPasswordOwner(PdfProperties.PdfPasswords
                                .encrypt(ownerPass));
                    }
                    if (!userPass.isEmpty()) {
                        out.setPasswordUser(PdfProperties.PdfPasswords
                                .encrypt(userPass));
                    }
                }

            } else if (createReq.isForPrinting()) {

                final boolean isEncrypted = onStampEncryptionForPrinting();

                if (isEncrypted && docLog != null) {
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
            throw new SpException(e);

        } finally {
            onProcessFinally();
        }

        final File generatedPdf = new File(pdfFile);

        try {
            onPdfGenerated(generatedPdf);
        } catch (Exception e) {
            throw new SpException(e.getMessage(), e);
        }

        return generatedPdf;
    }
}
