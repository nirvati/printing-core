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
package org.savapage.core.inbox;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedHashMap;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.LetterheadNotFoundException;
import org.savapage.core.PostScriptDrmException;
import org.savapage.core.SpException;
import org.savapage.core.community.CommunityDictEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.doc.DocContent;
import org.savapage.core.img.ImageUrl;
import org.savapage.core.img.PdfToImgCommand;
import org.savapage.core.img.PopplerPdfToImgCommand;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;
import org.savapage.core.pdf.AbstractPdfCreator;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.impl.InboxServiceImpl;
import org.savapage.core.system.CommandExecutor;
import org.savapage.core.system.ICommandExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Datraverse B.V.
 */
public final class OutputProducer {

    /**
     * .
     */
    private static final InboxService INBOX_SERVICE = ServiceContext
            .getServiceFactory().getInboxService();

    /**
     *
     */
    private static final String USER_LETTERHEADS_DIR_NAME = "letterheads";

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(OutputProducer.class);

    private static final DocLogService DOCLOG_SERVICE = ServiceContext
            .getServiceFactory().getDocLogService();

    /**
     *
     */
    private final PdfToImgCommand pdfToImgCommand =
            new PopplerPdfToImgCommand();

    /**
     *
     */
    private OutputProducer() {

    }

    /**
     * The SingletonHolder is loaded on the first execution of
     * {@link OutputProducer#instance()} or the first access to
     * {@link SingletonHolder#INSTANCE}, not before.
     */
    private static class SingletonHolder {
        public static final OutputProducer INSTANCE = new OutputProducer();
    }

    /**
     * Gets the singleton instance.
     *
     * @return
     */
    public static OutputProducer instance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Creates a page image. This file needs to be released by the requester.
     *
     * @param user
     *            The userid.
     * @param jobName
     *            The basename of the job file. If {@code null}, then the
     *            {@code pageIn} parameter is the ordinal page number over all
     *            jobs.
     * @param pageIn
     *            The zero-based ordinal page number in the job (or over all
     *            jobs).
     * @param rotate
     *            The rotation to be applied for this page. If {@code null}, no
     *            rotation is applied.
     * @param thumbnail
     *            {@code true} if a thumbnail is requested, {@code false} if
     *            detailed image.
     * @param isLetterhead
     *            {@code true} if this is a letterhead image.
     * @param isLetterheadPublic
     *            {@code true} if this is a public letterhead image.
     * @param sessionId
     *            A unique session id used for the name of the image file. If
     *            <code>null</code> or empty, it is not used.
     * @return The created image file.
     */
    public File allocatePageImage(final String user, final String jobName,
            final String pageIn, final String rotate, final boolean thumbnail,
            final boolean isLetterhead, final boolean isLetterheadPublic,
            final String sessionId) {

        String job = jobName;
        String page = pageIn;

        String rotate2Apply = rotate;

        if (job == null) {

            Object[] ret = INBOX_SERVICE.findJob(user, Integer.parseInt(page));

            if (ret.length < 3) {
                throw new SpException("job not found");
            }

            job = ret[0].toString();
            page = ret[1].toString();
            rotate2Apply = ret[2].toString();
        }

        final String tmpdir = ConfigManager.getUserTempDir(user);

        String homedir = null;

        if (isLetterheadPublic) {

            homedir = ConfigManager.getLetterheadDir();

        } else {

            homedir = ConfigManager.getUserHomeDir(user);

            if (isLetterhead) {
                homedir += "/" + USER_LETTERHEADS_DIR_NAME;
            }
        }

        /*
         * Create a unique temp image filename (remember images are retrieved
         * concurrently by a browser).
         */
        long time = System.currentTimeMillis();

        final StringBuilder imgFileBuilder = new StringBuilder(128);

        imgFileBuilder.append(ConfigManager.getAppTmpDir()).append("/")
                .append(user).append("_").append(job).append("_").append(page)
                .append("_").append(time).append("_");

        if (thumbnail) {
            imgFileBuilder.append("0");
        } else {
            imgFileBuilder.append("1");
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            imgFileBuilder.append("_").append(sessionId);
        }
        imgFileBuilder.append(".").append(ImageUrl.FILENAME_EXT_IMAGE);

        /*
         * Build source file name.
         */
        final StringBuilder srcFileBuilder = new StringBuilder(128);

        if (InboxServiceImpl.isScanJobFilename(job)) {

            srcFileBuilder.append(homedir).append("/").append(job);

        } else {

            if (InboxServiceImpl.isPdfJobFilename(job)) {

                srcFileBuilder.append(homedir).append("/").append(job);

            } else if (InboxServiceImpl.isPsJobFilename(job)) {
                /*
                 * Reconstruct the corresponding pdf file name (this file SHOULD
                 * exist).
                 */
                srcFileBuilder
                        .append(tmpdir)
                        .append("/")
                        .append(job.replaceFirst("\\."
                                + DocContent.FILENAME_EXT_PS, "."
                                + DocContent.FILENAME_EXT_PDF));
            } else {
                throw new SpException("unknown job type");
            }
        }

        /*
         * Create image.
         */
        final String srcFile = srcFileBuilder.toString();
        final String imgFile = imgFileBuilder.toString();

        final String command =
                pdfToImgCommand.createCommand(Integer.parseInt(page),
                        thumbnail, rotate2Apply, srcFile, imgFile);

        LOGGER.trace(command);

        final ICommandExecutor exec = CommandExecutor.createSimple(command);

        try {
            if (exec.executeCommand() != 0) {
                LOGGER.error(command);
                LOGGER.error(exec.getStandardErrorFromCommand().toString());
                throw new SpException("image [" + imgFileBuilder
                        + "] could not be created.");
            }
        } catch (Exception e) {
            throw new SpException(e);
        }

        return new File(imgFile);
    }

    /**
     * Produces a warning message instead of the actual job page.
     *
     * @param user
     * @param job
     * @param page
     * @param sessionId
     *            A unique session id used for the name of the image file. If
     *            <code>null</code> or empty, it is not used.
     * @return
     */
    public File allocateWarningPageImage(final String user, final String job,
            final String page, final String msg, final String sessionId) {

        // final String tmpdir = ConfigManager.getUserTempDir(user);

        /*
         * Make filename unique.
         */
        long time = System.currentTimeMillis();

        String imgFile =
                ConfigManager.getAppTmpDir() + "/" + user + "_" + job + "_"
                        + page + "_" + time;

        if (sessionId != null && !sessionId.isEmpty()) {
            imgFile += "_" + sessionId;
        }
        imgFile += "." + ImageUrl.FILENAME_EXT_IMAGE;

        String label = CommunityDictEnum.SAVAPAGE.getWord() + "\\n\\n" + msg;

        String bg = "red";
        String fill = "white";
        // size is in A4 ratio 210x297
        // Alternatives ...
        // -size 410x594 -gravity center -pointsize 40
        // -size 1190x1683 -gravity center -pointsize 60
        // TODO: adapt the size
        String command =
                "convert -flatten -background "
                        + bg
                        + " -fill "
                        + fill
                        + " -size 410x594 -gravity center -pointsize 40 label:\""
                        + label + "\" \"" + imgFile + "\"";

        LOGGER.trace(command);

        ICommandExecutor exec = CommandExecutor.createSimple(command);
        try {
            if (exec.executeCommand() != 0) {
                LOGGER.error(command);
                LOGGER.error(exec.getStandardErrorFromCommand().toString());
                throw new SpException("image [" + imgFile
                        + "] could not be created.");
            }
        } catch (Exception e) {
            throw new SpException(e);
        }

        return new File(imgFile);
    }

    /**
     * Releases (deletes) the PDF file.
     *
     * @param pdf
     */
    public void releasePdf(final File pdf) {
        if (pdf.delete()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("deleted temp file [" + pdf.getAbsolutePath()
                        + "]");
            }
        } else {
            LOGGER.error("delete of temp file [" + pdf.getAbsolutePath()
                    + "] FAILED");
        }
    }

    /**
     * Creates a full path unique (meaningless) PDF file name.
     *
     * @param user
     * @param purpose
     *            A simple tag to insert into the filename (to add some
     *            meaningful).
     * @return
     */
    public static String createUniqueTempPdfName(final User user,
            final String purpose) {
        return createUniqueTempPdfName(user.getUserId(), purpose);
    }

    /**
     * Creates a full path unique (meaningless) PDF file name.
     *
     * @param user
     * @param purpose
     *            A simple tag to insert into the filename (to add some
     *            meaning).
     * @return
     */
    public static String createUniqueTempPdfName(final String userid,
            final String purpose) {
        final Date now = new Date();
        return ConfigManager.getAppTmpDir() + "/savapage-" + purpose + "-"
                + now.getTime() + "-" + userid + "."
                + DocContent.FILENAME_EXT_PDF;
    }

    /**
     * Creates a letterhead PDF from the current SafePages. The currently
     * selected letterhead is NOT applied to create the new letterhead.
     *
     * @param directory
     *            The directory to create letterhead PDF in.
     * @param user
     *            The user.
     * @return The letterhead file.
     * @throws PostScriptDrmException
     * @throws LetterheadNotFoundException
     */
    public File createLetterhead(final String directory, final User user)
            throws LetterheadNotFoundException, PostScriptDrmException {

        final Date now = new Date();

        final String pdfFile =
                directory + "/letterhead-" + now.getTime() + "."
                        + DocContent.FILENAME_EXT_PDF;

        return generatePdf(user, pdfFile,
                INBOX_SERVICE.getInboxInfo(user.getUserId()), false, false,
                false, false, null, null);
    }

    /**
     *
     * Generates PDF file from the edited jobs for a user.
     *
     * @param userObj
     *            The requesting user.
     * @param pdfFile
     *            The name of the PDF file to generate.
     * @param inboxInfo
     *            The {@link InboxInfoDto} (with the filtered jobs).
     * @param removeGraphics
     *            If <code>true</code> graphics are removed (minified to
     *            one-pixel).
     * @param applyPdfProps
     *            If <code>true</code> the stored PDF properties for 'user'
     *            should be applied.
     * @param applyLetterhead
     *            If <code>true</code> the selected letterhead should be
     *            applied.
     * @param forPrinting
     *            <code>true</code> if this is a PDF generated for printing.
     * @param uuidPageCount
     *            This object will be filled with the number of selected pages
     *            per input file UUID. A value of {@code null} is allowed. Note:
     *            {@link LinkedHashMap} is insertion ordered.
     * @param docLog
     *            The DocLog object to collect data on. A value of {@code null}
     *            is allowed: in that case no data is collected.
     * @return File object with generated PDF.
     * @throws PostScriptDrmException
     * @throws LetterheadNotFoundException
     */
    public File generatePdf(final User userObj, final String pdfFile,
            final InboxInfoDto inboxInfo, boolean removeGraphics,
            boolean applyPdfProps, boolean applyLetterhead,
            boolean forPrinting,
            final LinkedHashMap<String, Integer> uuidPageCount,
            final DocLog docLog) throws LetterheadNotFoundException,
            PostScriptDrmException {

        return AbstractPdfCreator.create().generate(userObj, pdfFile,
                inboxInfo, removeGraphics, applyPdfProps, applyLetterhead,
                forPrinting, uuidPageCount, docLog);
    }

    /**
     * Helper method to generate (intermediate) PDF file from the edited jobs
     * for a user for export purposes.
     * <p>
     * NOTE: the caller is responsible for releasing the generated PDF, see
     * {@link #releasePdf(File)}
     * </p>
     *
     * @see {@link #generatePdf(String, String)}
     *
     * @param propPdf
     *            PDF properties to apply.
     * @param user
     *            The requesting user.
     * @param pdfFile
     *            The name of the PDF file to generate.
     * @param documentPageRangeFilter
     *            The page range filter. For example: '1,2,5-6'. The page
     *            numbers in page range filter refer to one-based page numbers
     *            of the integrated {@link InboxInfoDto} document. When
     *            {@code null}, then the full page range is applied.
     * @param removeGraphics
     *            If <code>true</code> graphics are removed (minified to
     *            one-pixel).
     * @param docLog
     *            The document log to update.
     * @return File object with generated PDF.
     * @throws PostScriptDrmException
     * @throws LetterheadNotFoundException
     */
    public File generatePdfForExport(final User user, final String pdfFile,
            final String documentPageRangeFilter, boolean removeGraphics,
            final DocLog docLog) throws IOException,
            LetterheadNotFoundException, PostScriptDrmException {

        final LinkedHashMap<String, Integer> uuidPageCount =
                new LinkedHashMap<>();

        /*
         * Get the (filtered) jobs.
         */
        InboxInfoDto inboxInfo = INBOX_SERVICE.getInboxInfo(user.getUserId());

        if (StringUtils.isNotBlank(documentPageRangeFilter)) {
            inboxInfo =
                    INBOX_SERVICE.filterInboxInfoPages(inboxInfo,
                            documentPageRangeFilter);
        }

        final File file =
                generatePdf(user, pdfFile, inboxInfo, removeGraphics, true,
                        true, false, uuidPageCount, docLog);

        DOCLOG_SERVICE.collectData4DocOut(user, docLog, file, uuidPageCount);

        return file;
    }

}
