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
package org.savapage.core.print.server;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.savapage.core.PostScriptDrmException;
import org.savapage.core.cometd.AdminPublisher;
import org.savapage.core.cometd.PubLevelEnum;
import org.savapage.core.cometd.PubTopicEnum;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.UserDao;
import org.savapage.core.dao.enums.DocLogProtocolEnum;
import org.savapage.core.doc.DocContent;
import org.savapage.core.doc.DocContentTypeEnum;
import org.savapage.core.doc.DocInputStream;
import org.savapage.core.doc.IFileConverter;
import org.savapage.core.doc.IStreamConverter;
import org.savapage.core.fonts.InternalFontFamilyEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.IppQueue;
import org.savapage.core.jpa.User;
import org.savapage.core.pdf.PdfSecurityException;
import org.savapage.core.pdf.SpPdfPageProps;
import org.savapage.core.services.DocLogService;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.UserService;
import org.savapage.core.services.helpers.DocContentPrintInInfo;
import org.savapage.core.users.UserAliasList;
import org.savapage.core.util.FileSystemHelper;
import org.savapage.core.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes a document print request. The document can be PDF, PostScripts,
 * {@link CupsCommandFile#FIRST_LINE_SIGNATURE}, or any other supported format.
 *
 * @author Datraverse B.V.
 *
 */
public class DocContentPrintProcessor {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory
            .getLogger(DocContentPrintProcessor.class);

    /**
     *
     */
    private static final DocLogService DOC_LOG_SERVICE = ServiceContext
            .getServiceFactory().getDocLogService();

    /**
    *
    */
    private static final InboxService INBOX_SERVICE = ServiceContext
            .getServiceFactory().getInboxService();
    /**
     *
     */
    private static final UserService USER_SERVICE = ServiceContext
            .getServiceFactory().getUserService();

    /**
     *
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     *
     */
    private java.util.UUID uuidJob = java.util.UUID.randomUUID();

    /**
     * The number of content bytes read from the input stream.
     */
    private long inputByteCount = 0;

    /**
     *
     */
    private byte[] readAheadInputBytes = null;

    /**
     *
     */
    private SpPdfPageProps pageProps = null;

    /**
     *
     */
    private boolean drmViolationDetected = false;

    /**
     *
     */
    private boolean drmRestricted = false;

    /**
     *
     */
    private User userDb = null;

    /**
     *
     */
    private String uidTrusted = null;

    /**
     *
     */
    private String mimetype;

    /**
     *
     */
    private String signatureString = null;

    /**
     *
     */
    private final IppQueue queue;

    /**
     *
     */
    private final String authWebAppUser;

    /**
     * The exception which occurred during processing the request.
     */
    private Exception deferredException = null;

    /**
     *
     */
    private String requestingUserId = null;

    /**
     *
     */
    private String jobName;

    /**
     *
     */
    private final String originatorIp;

    /**
     *
     */
    private String originatorEmail;

    /**
     *
     */
    @SuppressWarnings("unused")
    private DocContentPrintProcessor() {
        this.queue = null;
        this.authWebAppUser = null;
        this.jobName = null;
        this.originatorIp = null;
        this.originatorEmail = null;
    }

    /**
     * Creates a print server request.
     *
     * @param queue
     *            The queue to print to.
     * @param originatorIp
     *            The IP address of the requesting client.
     * @param jobName
     *            The name of the print job.
     * @param authWebAppUser
     *            The authenticated WebApp user: {@code null} if not present.
     */
    public DocContentPrintProcessor(final IppQueue queue,
            final String originatorIp, final String jobName,
            final String authWebAppUser) {
        this.jobName = jobName;
        this.queue = queue;
        this.originatorIp = originatorIp;
        this.authWebAppUser = authWebAppUser;
    }

    /**
     *
     * @return
     */
    public String getOriginatorIp() {
        return originatorIp;
    }

    /**
     *
     * @return
     */
    public String getOriginatorEmail() {
        return originatorEmail;
    }

    /**
     *
     * @param bytes
     *            The bytes read.
     */
    public final void setReadAheadInputBytes(final byte[] bytes) {
        readAheadInputBytes = bytes;
    }

    /**
     * Is the queue trusted?
     *
     * @return
     */
    public boolean isTrustedQueue() {
        return queue != null && queue.getTrusted();
    }

    /**
     * Is this an authorized print job? This takes the user and the queue into
     * account, but not the IP address of the requesting user.
     *
     * @return
     */
    public boolean isAuthorized() {
        return isTrustedUser() && (isTrustedQueue() || isAuthWebAppUser());
    }

    /**
     * Get the uid of the Person who is currently authenticated in User WebApp
     * at same IP-address as the job was issued from.
     *
     * @return {@code null} if no user is authenticated.
     */
    public String getAuthWebAppUser() {
        return authWebAppUser;
    }

    /**
     * Is the authenticated WebApp User present?
     *
     * @return
     */
    public boolean isAuthWebAppUser() {
        return StringUtils.isNotBlank(authWebAppUser);
    }

    /**
     * Processes the requesting user, i.e. checks whether he is trusted to print
     * a job. Trust can either be direct, by alias, or by authenticated WebApp
     * User. The user (alias) must be a Person.
     * <p>
     * <b>Note</b>: On a trusted Queue (and lazy print enabled) a user is lazy
     * inserted.
     * </p>
     *
     * @param requestingUserId
     *            The name (id) of the requesting user. Can be {@code null} is
     *            not available.
     * @return {@code true} if we have a trusted uid.
     */
    public boolean processRequestingUser(final String requestingUserId) {

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        this.requestingUserId = requestingUserId;

        String uid = null;

        if (requestingUserId == null) {

            this.userDb = null;
            this.uidTrusted = null;

        } else {
            /*
             * Get the alias (if present).
             */
            uid = UserAliasList.instance().getUserName(this.requestingUserId);

            if (!uid.equals(this.requestingUserId)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("using user [" + uid + "] for alias ["
                            + this.requestingUserId + "]");
                }
            }

            ConfigManager cm = ConfigManager.instance();
            /*
             * Read (alias) user from database.
             */
            this.userDb = userDao.findActiveUserByUserId(uid);

            /*
             * On a trusted queue (and lazy print enabled) we can lazy insert a
             * user...
             */
            if (this.userDb == null && this.isTrustedQueue()
                    && cm.isUserInsertLazyPrint()) {

                final String group =
                        cm.getConfigValue(Key.USER_SOURCE_GROUP).trim();

                this.userDb =
                        USER_SERVICE.lazyInsertExternalUser(cm.getUserSource(),
                                uid, group);
            }
        }

        /*
         * Do we have a (lazy inserted) database user?
         */
        if (this.userDb == null) {

            /*
             * The user is not found in the database (no lazy insert). Try the
             * authenticated WebApp user (if present)...
             */
            this.uidTrusted = this.getAuthWebAppUser();

            if (this.uidTrusted != null) {
                this.userDb = userDao.findActiveUserByUserId(this.uidTrusted);
            }

        } else {
            /*
             * The user is present in the database.
             */
            this.uidTrusted = uid;
        }

        /*
         * Check authorization.
         */
        boolean isAuthorized = false;
        String reason = null;

        if (this.userDb == null) {

            /*
             * No (trusted WepApp) database user.
             */
            reason = "is UNKNOWN";

        } else {

            if (this.userDb.getPerson()) {

                final Date dateNow = new Date();

                if (USER_SERVICE.isUserPrintInDisabled(this.userDb, dateNow)) {
                    reason = "is DISABLED for printing";
                } else {
                    isAuthorized = true;
                }

            } else {
                reason = "is NOT a Person";
            }

        }

        if (isAuthorized) {

            if (!this.uidTrusted.equals(uid)) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Requesting user [" + uid + "] is unknown:"
                            + " using WebApp user [" + uidTrusted + "]");
                }
            }

            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("job-name: [" + getJobName() + "]");
            }

        } else {

            /*
             * Log the reason why not authorized.
             */
            if (reason != null) {

                if (this.uidTrusted != null && !this.uidTrusted.equals(uid)) {

                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Requesting user [" + uid
                                + "] is unknown -> WebApp user ["
                                + this.uidTrusted + "] " + reason
                                + ": print denied");
                    }

                } else {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("Requesting user [" + uid + "] " + reason
                                + ": print denied");
                    }
                }
            }

            /*
             * Set uidTrusted as indicator for printing allowed.
             */
            this.uidTrusted = null;
        }

        return this.uidTrusted != null;
    }

    /**
     * Writes the content to an output stream.
     * <p>
     * Note: {@link #inputByteCount} is incremented while reading.
     * </p>
     *
     * @param istr
     *            The content input.
     * @param ostr
     *            The content ouput.
     * @throws IOException
     *             When reading or writing goes wrong.
     */
    private void saveBinary(final InputStream istr, final OutputStream ostr)
            throws IOException {

        final byte[] buffer = new byte[BUFFER_SIZE];

        int noOfBytesWlk = 0;

        /*
         * Read bytes from source file and write to destination file...
         */
        while ((noOfBytesWlk = istr.read(buffer)) != -1) {
            this.inputByteCount += noOfBytesWlk;
            ostr.write(buffer, 0, noOfBytesWlk);
        }
    }

    /**
     * Streams all PostScript input to the PostScript output stream. An
     * exception is throw when PostScript has copyright restrictions.
     *
     * @param istr
     *            The PostScript input stream.
     * @param ostr
     *            The PostScript ouput stream.
     * @throws IOException
     *             When a read/write error occurs.
     * @throws PostScriptDrmException
     *             When PostScript could not be re-distilled due to copyright
     *             restrictions. The obvious reason is when the PostScript file
     *             was created as a result of printing an encrypted PDF file. In
     *             this case the {@code ps2pdf} program fails and reports that
     *             <i>Redistilling encrypted PDF is not permitted</i>.
     */
    private void
            savePostScript(final InputStream istr, final OutputStream ostr)
                    throws IOException, PostScriptDrmException {

        final boolean respectDRM =
                !ConfigManager.instance().isConfigValue(
                        IConfigProp.Key.PRINT_IN_ALLOW_ENCRYPTED_PDF);

        BufferedReader reader = new BufferedReader(new InputStreamReader(istr));
        BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(ostr));

        switch (PostScriptFilter.process(reader, writer, respectDRM)) {
        case DRM_NEGLECTED:
            setDrmRestricted(true);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("DRM protected PostScript from user ["
                        + uidTrusted + "] accepted");
            }
            break;
        case DRM_NO:
            break;
        case DRM_YES:
            throw new PostScriptDrmException(
                    "PostScript not accepted due to copyright restrictions");
        default:
            break;
        }
    }

    /**
     * Checks or assigns the content type of the job depending on the delivery
     * protocol.
     * <p>
     * Optionally a read-ahead of the content input stream (to read the
     * signature) is performed.
     * </p>
     *
     * @param delivery
     *            The delivery protocol.
     * @param typeProvided
     *            The content type as claimed by the provider.
     * @param content
     *            The content input stream.
     * @return The assigned content type.
     * @throws IOException
     */
    private DocContentTypeEnum checkJobContent(
            final DocLogProtocolEnum delivery,
            final DocContentTypeEnum typeProvided, final InputStream content)
            throws IOException {

        DocContentTypeEnum contentType = DocContentTypeEnum.UNKNOWN;

        this.signatureString = null;

        if (delivery.equals(DocLogProtocolEnum.IPP)) {
            /*
             * What kind of file are we getting in?
             */
            byte[] signature = new byte[4];
            content.read(signature);

            setReadAheadInputBytes(signature);

            this.signatureString = new String(signature);

            if (this.signatureString.startsWith(DocContent.HEADER_PDF)) {
                contentType = DocContentTypeEnum.PDF;
            } else if (this.signatureString.startsWith(DocContent.HEADER_PS)) {
                contentType = DocContentTypeEnum.PS;
            } else if (this.signatureString
                    .startsWith(DocContent.HEADER_UNIRAST)) {
                contentType = DocContentTypeEnum.UNIRAST;
            } else if (CupsCommandFile.isSignatureStart(this.signatureString)) {
                contentType = DocContentTypeEnum.CUPS_COMMAND;
            }

        } else if (delivery.equals(DocLogProtocolEnum.IMAP)) {
            /*
             * Do not check: accept the provided type.
             */
            contentType = typeProvided;

        } else {
            /*
             * Do not check: accept the provided type.
             */
            contentType = typeProvided;
        }

        return contentType;
    }

    /**
     * Save UnsupportedPrintJobContent for debugging purposes?
     * <p>
     * TODO: make this a {@link IConfigProp.Key}.
     * </p>
     */
    private final static boolean SAVE_UNSUPPORTED_CONTENT = false;

    /**
     * Evaluates assigned the DocContentType and throws an exception when
     * content is NOT supported.
     * <p>
     * </p>
     *
     * @param delivery
     *            The delivery protocol.
     * @param assignedContentType
     * @param content
     * @throws IOException
     * @throws UnsupportedPrintJobContent
     */
    private void evaluateJobContent(DocLogProtocolEnum delivery,
            final DocContentTypeEnum assignedContentType,
            final InputStream content) throws IOException,
            UnsupportedPrintJobContent {

        UnsupportedPrintJobContent formatException = null;
        FileOutputStream fostr = null;
        try {
            if (assignedContentType == DocContentTypeEnum.UNKNOWN) {

                formatException =
                        new UnsupportedPrintJobContent(
                                "header ["
                                        + StringUtils
                                                .defaultString(this.signatureString)
                                        + "] unknown");

                if (SAVE_UNSUPPORTED_CONTENT) {
                    fostr =
                            new FileOutputStream(ConfigManager.getAppTmpDir()
                                    + "/" + delivery + "_"
                                    + System.currentTimeMillis() + ".unknown");
                    fostr.write(readAheadInputBytes);
                    saveBinary(content, fostr);
                }

            } else if (!DocContent.isSupported(assignedContentType)) {

                formatException =
                        new UnsupportedPrintJobContent("Content type ["
                                + assignedContentType + "] NOT supported.");

            }
        } finally {
            if (fostr != null) {
                fostr.close();
            }
        }

        if (formatException != null) {
            throw formatException;
        }

    }

    /**
     * Processes content to be printed as offered on the input stream, writes a
     * {@link DocLog}, and places the resulting PDF in the user's inbox.
     * <p>
     * When this is a {@link DocLogProtocolEnum#RAW} print it is assumed that
     * the input stream header is already validated.
     * </p>
     *
     * @param istrContent
     *            The input stream containing the content to be printed.
     * @param protocol
     *            The originating printing protocol.
     * @param originatorEmail
     *            MUST be present for {@link DocLogProtocolEnum#IMAP} and
     *            {@link DocLogProtocolEnum#GCP}. For all other protocols
     *            {@code null}.
     * @param contentTypeProvided
     *            The content type as claimed by the provider. This parameter is
     *            {@code null} and ignored when protocol is
     *            {@link DocLogProtocolEnum#IPP}.
     * @param preferredOutputFont
     *            The preferred font for the PDF output. This parameter is
     *            {@code null} when (user) preference is unknown or irrelevant.
     * @throws Exception
     */
    public void process(final InputStream istrContent,
            DocLogProtocolEnum protocol, String originatorEmail,
            DocContentTypeEnum contentTypeProvided,
            final InternalFontFamilyEnum preferredOutputFont) throws Exception {

        if (!isTrustedUser()) {
            return;
        }

        this.originatorEmail = originatorEmail;

        this.readAheadInputBytes = null;

        final DocContentTypeEnum inputType =
                checkJobContent(protocol, contentTypeProvided, istrContent);

        /*
         * Skip CUPS_COMMAND for now.
         */
        if (inputType == DocContentTypeEnum.CUPS_COMMAND) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(CupsCommandFile.FIRST_LINE_SIGNATURE + " ignored");
            }
            return;
        }

        /*
         *
         */
        final String homeDir = ConfigManager.getUserHomeDir(this.uidTrusted);
        final String tempDir = ConfigManager.getUserTempDir(this.uidTrusted);

        /*
         * Lazy create user home directory.
         */
        USER_SERVICE.lazyUserHomeDir(userDb);

        FileOutputStream fostrContent = null;

        this.inputByteCount = 0;

        final List<File> filesCreated = new ArrayList<>();
        final List<File> files2Delete = new ArrayList<>();

        try {
            /*
             * Keep the job content evaluation WITHIN the try block, so the
             * exception is handled appropriately.
             */
            evaluateJobContent(protocol, inputType, istrContent);

            setMimetype(DocContent.getMimeType(inputType));

            /*
             * The basename of the resulting file WITHOUT extension.
             */
            final String jobFileBase = this.uuidJob.toString() + ".";

            /*
             * The basename of the resulting PDF file.
             */
            final String jobFileBasePdf =
                    jobFileBase + DocContent.FILENAME_EXT_PDF;

            /*
             * File to receive the input stream content.
             */
            final File contentFile =
                    new File(tempDir + "/" + jobFileBase
                            + DocContent.getFileExtension(inputType));

            /*
             * Create the file.
             */
            fostrContent = new FileOutputStream(contentFile);

            /*
             * Administer the just created file as created or 2delete.
             */
            if (inputType == DocContentTypeEnum.PDF) {
                filesCreated.add(contentFile);
            } else {
                files2Delete.add(contentFile);
            }

            /*
             * Write the saved pre-processed input bytes to the output stream.
             */
            if (this.readAheadInputBytes != null) {
                this.inputByteCount += this.readAheadInputBytes.length;
                fostrContent.write(this.readAheadInputBytes);
            }

            /*
             * Be optimistic about PostScript content.
             */
            setDrmViolationDetected(false);
            setDrmRestricted(false);

            /*
             * Document content converters are needed for non-PDF content.
             */
            IStreamConverter streamConverter = null;
            IFileConverter fileConverter = null;

            /*
             * Directly write (rest of) offered content to PostScript or PDF, or
             * convert to PDF.
             */
            if (inputType == DocContentTypeEnum.PDF) {

                saveBinary(istrContent, fostrContent);

            } else if (inputType == DocContentTypeEnum.PS) {

                /*
                 * An exception is throw upon a DRM violation.
                 */
                savePostScript(istrContent, fostrContent);

                /*
                 * Always use a file converter,
                 */
                fileConverter = DocContent.createPdfFileConverter(inputType);

            } else {

                streamConverter =
                        DocContent.createPdfStreamConverter(inputType,
                                preferredOutputFont);

                if (streamConverter == null) {

                    fileConverter =
                            DocContent.createPdfFileConverter(inputType);

                    if (fileConverter != null) {
                        saveBinary(istrContent, fostrContent);
                    }
                }
            }

            /*
             * Path of the PDF file BEFORE it is moved to its final destination.
             * As a DEFAULT we use the path of the streamed content.
             */
            String tempPathPdf = contentFile.getAbsolutePath();

            /*
             * Convert to PDF with a stream converter?
             */
            if (streamConverter != null) {
                /*
                 * INVARIANT: no read-ahead on the input content stream.
                 */
                final DocInputStream istrDoc = new DocInputStream(istrContent);
                this.inputByteCount =
                        streamConverter.convert(inputType, istrDoc,
                                fostrContent);
            }

            /*
             * We're done with capturing the content input stream, so close the
             * file output stream.
             */
            fostrContent.close();
            fostrContent = null;

            /*
             * Convert to PDF with a FILE converter?
             */
            if (fileConverter != null) {
                this.inputByteCount = contentFile.length();
                final File pdfOutputFile =
                        fileConverter.convert(inputType, contentFile);
                filesCreated.add(pdfOutputFile);
                tempPathPdf = pdfOutputFile.getAbsolutePath();
            }

            /*
             * Calculate number of pages, etc...
             */
            this.setPageProps(SpPdfPageProps.create(tempPathPdf));

            /*
             * Logging in Database: this should be done BEFORE the file MOVE.
             */
            this.logPrintIn(protocol);

            /*
             * Move to user safepages home.
             */
            final Path pathTarget =
                    FileSystems.getDefault().getPath(homeDir, jobFileBasePdf);

            FileSystemHelper.doAtomicFileMove(//
                    FileSystems.getDefault().getPath(tempPathPdf), pathTarget);

            /*
             * Start task to create the shadow EcoPrint PDF file?
             */
            if (ConfigManager.isEcoPrintEnabled()
                    && this.getPageProps().getNumberOfPages() <= ConfigManager
                            .instance()
                            .getConfigInt(
                                    Key.ECO_PRINT_AUTO_THRESHOLD_SHADOW_PAGE_COUNT)) {
                INBOX_SERVICE.startEcoPrintPdfTask(homeDir,
                        pathTarget.toFile(), this.uuidJob);
            }

        } catch (Exception e) {

            if (e instanceof PostScriptDrmException) {

                setDrmRestricted(true);
                setDrmViolationDetected(true);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("DRM protected PostScript from user ["
                            + uidTrusted + "] REJECTED");
                }

                /*
                 * We also need to log the rejected print-in, since we want to
                 * notify the result in the User WebApp.
                 */
                this.logPrintIn(protocol);

            } else {
                /*
                 * Save the exception, so it can be thrown at the end of the
                 * parent operation.
                 */
                setDeferredException(e);
            }

            /*
             * Clean up any created files.
             */
            for (final File file : filesCreated) {
                if (file.exists()) {
                    file.delete();
                }
            }

        } finally {

            if (fostrContent != null) {
                fostrContent.close();
            }

            for (final File file : files2Delete) {
                if (file.exists()) {
                    file.delete();
                }
            }
        }

    }

    /**
     * Logs the PrintIn job.
     * <p>
     * <b>IMPORTANT</b>: This method has it <u>own transaction scope</u>.
     * </p>
     *
     * @param protocol
     *            The {@link DocLogProtocolEnum}.
     */
    private void logPrintIn(final DocLogProtocolEnum protocol) {

        final DocContentPrintInInfo printInInfo = new DocContentPrintInInfo();

        printInInfo.setDrmRestricted(this.isDrmRestricted());
        printInInfo.setJobBytes(this.getJobBytes());
        printInInfo.setJobName(this.getJobName());
        printInInfo.setMimetype(this.getMimetype());
        printInInfo.setOriginatorEmail(this.getOriginatorEmail());
        printInInfo.setOriginatorIp(this.getOriginatorIp());
        printInInfo.setPageProps(this.getPageProps());
        printInInfo.setUuidJob(this.getUuidJob());

        DOC_LOG_SERVICE.logPrintIn(this.getUserDb(), this.getQueue(), protocol,
                printInInfo);
    }

    /**
     *
     * @return
     */
    public java.util.UUID getUuidJob() {
        return uuidJob;
    }

    public void setUuidJob(java.util.UUID uuidJob) {
        this.uuidJob = uuidJob;
    }

    public long getJobBytes() {
        return inputByteCount;
    }

    public void setJobBytes(long jobBytes) {
        this.inputByteCount = jobBytes;
    }

    public SpPdfPageProps getPageProps() {
        return pageProps;
    }

    public void setPageProps(SpPdfPageProps pageProps) {
        this.pageProps = pageProps;
    }

    public String getMimetype() {
        return mimetype;
    }

    private void setMimetype(String mimetype) {
        this.mimetype = mimetype;
    }

    /**
     * The user object from the database representing the user who printed this
     * job.
     *
     * @return {@code null} when unknown.
     */
    public User getUserDb() {
        return userDb;
    }

    public boolean isDrmViolationDetected() {
        return drmViolationDetected;
    }

    public void setDrmViolationDetected(boolean drmViolationDetected) {
        this.drmViolationDetected = drmViolationDetected;
    }

    public boolean isDrmRestricted() {
        return drmRestricted;
    }

    public void setDrmRestricted(boolean restricted) {
        drmRestricted = restricted;
    }

    /**
     * Checks if we have a trusted userid who is allowed to print.
     *
     * @return {@code true} if we have a trusted user.
     */
    public boolean isTrustedUser() {
        return this.uidTrusted != null;
    }

    public Exception getDeferredException() {
        return deferredException;
    }

    public void setDeferredException(Exception deferredException) {
        this.deferredException = deferredException;
    }

    public boolean hasDeferredException() {
        return deferredException != null;
    }

    /**
     *
     * @return
     */
    public String getJobName() {
        return jobName;
    }

    /**
     *
     */
    public void setJobName(final String name) {
        this.jobName = name;
    }

    /**
     *
     * @return
     */
    public IppQueue getQueue() {
        return queue;
    }

    /**
     * Return a localized message string. IMPORTANT: The locale from the
     * application is used.
     *
     * @param key
     *            The key of the message.
     * @param args
     *            The placeholder arguments for the message template.
     *
     * @return The message text.
     */
    private String localize(final String key, final String... args) {
        return Messages.getMessage(getClass(), key, args);
    }

    /**
     * Evaluates (and adapts) the error state of the print job processed by this
     * request.
     * <p>
     * When a deferred exception is present or the user is not authorized to
     * print this job, messages are logged and send to the
     * {@link AdminPublisher}.
     * </p>
     * <p>
     * NOTE: Some "legal" deferred exceptions like
     * {@link PostScriptDrmException}, {@link PdfSecurityException} and
     * {@link UnsupportedPrintJobContent} are nullified after message handling,
     * i.e. {@link #getDeferredException()} will return {@code null} after this
     * method is performed.
     * </p>
     *
     * @param isAuthorized
     */
    public void evaluateErrorState(boolean isAuthorized) {

        final Exception exception = getDeferredException();

        if (exception == null && isAuthorized) {
            return;
        }

        final String urlQueue;

        if (this.queue == null) {
            urlQueue = "?";
        } else {
            urlQueue = "/" + this.queue.getUrlPath();
        }

        final String userid;

        if (getUserDb() == null) {
            userid = StringUtils.defaultString(getAuthWebAppUser());
        } else {
            userid = getUserDb().getUserId();
        }

        final String pubMessage;
        PubLevelEnum pubLevel = PubLevelEnum.WARN;

        if (exception == null) {

            pubMessage = localize("pub-user-print-in-not-authorized");

        } else {

            pubMessage = exception.getMessage();

            if (exception instanceof PostScriptDrmException) {

                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Distilling PostScript to PDF from user ["
                            + userid + "] FAILED: " + exception.getMessage());
                }

                setDeferredException(null);

            } else if (exception instanceof PdfSecurityException) {

                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Opening PDF from user [" + userid
                            + "] FAILED: " + exception.getMessage());
                }

                setDeferredException(null);

            } else if (exception instanceof UnsupportedPrintJobContent) {

                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Unsupported Print Job content from user ["
                            + userid + "] : " + exception.getMessage());
                }

                setDeferredException(null);

            } else {

                pubLevel = PubLevelEnum.ERROR;

                LOGGER.error(exception.getMessage(), exception);
            }
        }

        /*
         *
         */
        String deniedUserId = userid;

        if (StringUtils.isBlank(deniedUserId)) {
            deniedUserId = this.requestingUserId;
        }

        AdminPublisher.instance().publish(
                PubTopicEnum.USER,
                pubLevel,
                localize("pub-user-print-in-denied", deniedUserId, urlQueue,
                        originatorIp, pubMessage));
    }

}
