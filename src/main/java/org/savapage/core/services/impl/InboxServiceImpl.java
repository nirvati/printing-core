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
package org.savapage.core.services.impl;

import static java.nio.file.FileVisitResult.CONTINUE;

import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.print.attribute.standard.MediaSizeName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableLong;
import org.savapage.core.LetterheadNotFoundException;
import org.savapage.core.PostScriptDrmException;
import org.savapage.core.SpException;
import org.savapage.core.config.ConfigManager;
import org.savapage.core.config.IConfigProp.Key;
import org.savapage.core.dao.DocLogDao;
import org.savapage.core.dao.UserDao;
import org.savapage.core.doc.DocContent;
import org.savapage.core.imaging.EcoPrintPdfTaskInfo;
import org.savapage.core.imaging.EcoPrintPdfTaskPendingException;
import org.savapage.core.imaging.ImageUrl;
import org.savapage.core.inbox.InboxInfoDto;
import org.savapage.core.inbox.InboxInfoDto.InboxJob;
import org.savapage.core.inbox.InboxInfoDto.InboxJobRange;
import org.savapage.core.inbox.LetterheadInfo;
import org.savapage.core.inbox.OutputProducer;
import org.savapage.core.inbox.PageImages;
import org.savapage.core.inbox.RangeAtom;
import org.savapage.core.ipp.IppMediaSizeEnum;
import org.savapage.core.jpa.DocLog;
import org.savapage.core.jpa.User;
import org.savapage.core.pdf.AbstractPdfCreator;
import org.savapage.core.pdf.PdfPageRotateHelper;
import org.savapage.core.pdf.PdfSecurityException;
import org.savapage.core.pdf.PdfValidityException;
import org.savapage.core.pdf.SpPdfPageProps;
import org.savapage.core.print.proxy.ProxyPrintJobChunk;
import org.savapage.core.print.proxy.ProxyPrintJobChunkRange;
import org.savapage.core.services.EcoPrintPdfTaskService;
import org.savapage.core.services.InboxService;
import org.savapage.core.services.ServiceContext;
import org.savapage.core.services.helpers.InboxPageImageChunker;
import org.savapage.core.services.helpers.InboxPageImageInfo;
import org.savapage.core.services.helpers.InboxPageMover;
import org.savapage.core.util.FileSystemHelper;
import org.savapage.core.util.JsonHelper;
import org.savapage.core.util.MediaUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * TODO: Make this class a "real" {@link AbstractService}.
 *
 * @author Rijk Ravestein
 *
 */
public final class InboxServiceImpl implements InboxService {

    /**
     *
     */
    private static final Logger LOGGER =
            LoggerFactory.getLogger(InboxServiceImpl.class);

    /**
     * .
     */
    private static final EcoPrintPdfTaskService ECOPRINT_SERVICE =
            ServiceContext.getServiceFactory().getEcoPrintPdfTaskService();

    /**
     * File extension for EcoPrint shadow PDF file.
     */
    public static final String FILENAME_EXT_ECO = "eco";

    /**
     *
     */
    public static final String FILENAME_EXT_SCAN = DocContent.FILENAME_EXT_PNM;

    /**
     *
     */
    private static final String INBOX_DESCRIPT_FILE_NAME = "savapage.json";

    /**
     *
     */
    private static final String LETTERHEADS_DIR_NAME = "letterheads";

    /**
     *
     */
    private static final String LETTERHEADS_DESCRIPT_FILE_NAME =
            "letterheads.json";

    @Override
    public boolean doesHomeDirExist(final String userId) {
        return new File(ConfigManager.getUserHomeDir(userId)).exists();
    }

    /**
     * @param user
     *            The unique user id.
     * @return The full File path of the user's
     *         {@link #INBOX_DESCRIPT_FILE_NAME}.
     */
    private File getInboxInfoFile(final String user) {
        return new File(
                String.format("%s%c%s", ConfigManager.getUserHomeDir(user),
                        File.separatorChar, INBOX_DESCRIPT_FILE_NAME));
    }

    @Override
    public InboxInfoDto readInboxInfo(final String user) {

        final File file = getInboxInfoFile(user);
        final ObjectMapper mapper = new ObjectMapper();

        InboxInfoDto jobinfo = null;

        try {
            /*
             * First check if file exists, if not (first time use, or reset)
             * return an empty job info object.
             */
            if (file.exists()) {

                try {

                    jobinfo = mapper.readValue(file, InboxInfoDto.class);

                } catch (JsonMappingException e) {

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Error mapping from file ["
                                + file.getAbsolutePath() + "]: create new.");
                    }
                    /*
                     * There has been a change in layout of the JSON file, so
                     * create a new default and store it.
                     */
                    jobinfo = new InboxInfoDto();
                    storeInboxInfo(user, jobinfo);
                }
            }
            if (jobinfo == null) {
                jobinfo = new InboxInfoDto();
            }
        } catch (JsonParseException e) {
            throw new SpException("Error parsing file ["
                    + file.getAbsolutePath() + "] : " + e.getMessage());
        } catch (IOException e) {
            throw new SpException("Error reading file ["
                    + file.getAbsolutePath() + "]" + e.getMessage());
        }
        return jobinfo;
    }

    @Override
    public void storeInboxInfo(final String user, final InboxInfoDto jobinfo) {

        final boolean atomicMove = true; // Mantis #863

        final File fileTarget = getInboxInfoFile(user);
        final File fileSource;

        if (atomicMove) {
            fileSource = new File(String.format("%s%c%s_%s.%s",
                    ConfigManager.getAppTmpDir(), File.separatorChar, user,
                    INBOX_DESCRIPT_FILE_NAME, UUID.randomUUID().toString()));
        } else {
            fileSource = fileTarget;
        }

        try {

            JsonHelper.write(jobinfo, new FileWriter(fileSource));

        } catch (IOException e) {
            throw new SpException("Error writing file ["
                    + fileSource.getAbsolutePath() + "] : " + e.getMessage());
        }

        if (atomicMove) {
            try {
                FileSystemHelper.doAtomicFileMove(fileSource.toPath(),
                        fileTarget.toPath());
            } catch (IOException e) {
                throw new SpException(
                        "Error moving file [" + fileSource.getAbsolutePath()
                                + "] to [" + fileTarget.getAbsolutePath()
                                + "] : " + e.getMessage());
            } finally {
                fileSource.delete(); // just to be sure
            }
        }
    }

    @Override
    public int getNumberOfPagesInPdfFile(final String filePathPdf) {
        return AbstractPdfCreator.pageCountInPdfFile(filePathPdf);
    }

    /**
     * Creates the {@link SpPdfPageProps} of an PDF document.
     *
     * @param filePathPdf
     *            The PDF document file path.
     * @return The {@link SpPdfPageProps}.
     */
    private static SpPdfPageProps getPdfPageProps(final String filePathPdf) {
        try {
            return SpPdfPageProps.create(filePathPdf);

        } catch (PdfSecurityException | PdfValidityException e) {
            throw new SpException(e.getMessage());
        }
    }

    @Override
    public InboxInfoDto getInboxInfo(final String userName) {

        final DocLogDao doclogDao =
                ServiceContext.getDaoContext().getDocLogDao();

        final UserDao userDao = ServiceContext.getDaoContext().getUserDao();

        final String workdir = ConfigManager.getUserHomeDir(userName);

        final InboxInfoDto jobinfo = readInboxInfo(userName);

        int iJobOffset = jobinfo.jobCount();

        final FileFilter filefilter = new FileFilter() {
            @Override
            public boolean accept(final File file) {
                return isSupportedJobType(file);
            }
        };

        final File[] files = new File(workdir).listFiles(filefilter);

        if (null != files && files.length > 0) {
            // .....................................................
            // SORT ascending by creation time
            // .....................................................
            java.util.Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(final File o1, final File o2) {
                    final Long m1 = o1.lastModified();
                    final Long m2 = o2.lastModified();
                    return m1.compareTo(m2);
                }
            });

            User userObj = null;

            if (iJobOffset < files.length) {
                userObj = userDao.findActiveUserByUserId(userName);
            }

            for (int i = iJobOffset; i < files.length; i++) {

                final String filePath = files[i].getAbsolutePath();

                /*
                 *
                 */
                final DocLog docLog = doclogDao.findByUuid(userObj.getId(),
                        FilenameUtils.getBaseName(filePath));

                if (docLog == null) {
                    /*
                     * The file was not created by a printing (or another
                     * official front-end), but must have been manually copied
                     * to the user's SafePages.
                     */
                    LOGGER.error("file [" + files[i].getAbsolutePath()
                            + "] NOT found in DocLog.");

                    throw new SpException("File [" + files[i].getName()
                            + "] has NO log entry.");
                }

                /*
                 *
                 */
                final InboxInfoDto.InboxJob job = new InboxInfoDto.InboxJob();

                job.setFile(FilenameUtils.getName(filePath));
                job.setTitle(docLog.getTitle());
                job.setCreatedTime(docLog.getCreatedDate().getTime());

                job.setPages(docLog.getNumberOfPages());

                /*
                 * DRM restricted?
                 */
                job.setDrm(docLog.getDrmRestricted());

                /*
                 * Landscape, page rotation, content rotation, user rotate,
                 * media.
                 */
                final SpPdfPageProps pageProps = getPdfPageProps(filePath);

                final boolean isLandscape = pageProps.isLandscape();
                final int rotation = pageProps.getRotationFirstPage();
                final int contentRotation =
                        pageProps.getContentRotationFirstPage();

                final Integer rotate = PdfPageRotateHelper.PDF_ROTATION_0;

                job.setLandscape(Boolean.valueOf(isLandscape));
                job.setRotation(Integer.valueOf(rotation));
                job.setContentRotation(Integer.valueOf(contentRotation));
                job.setRotate(rotate.toString());
                job.setMedia(pageProps.getSize());
                job.setLandscapeView(Boolean.valueOf(
                        PdfPageRotateHelper.isSeenAsLandscape(contentRotation,
                                rotation, isLandscape, rotate.intValue())));
                /*
                 * Append
                 */
                jobinfo.getJobs().add(job);

                /*
                 *
                 */
                final InboxJobRange range = new InboxInfoDto.InboxJobRange();

                range.setJob(i);
                range.setRange(RangeAtom.FULL_PAGE_RANGE);

                jobinfo.getPages().add(range); // append
            }

            if (iJobOffset < files.length) {
                storeInboxInfo(userName, jobinfo);
            }
        }
        return jobinfo;
    }

    /**
     * Creates a {@link InboxPageImageInfo} from an inbox job.
     *
     * @param job
     *            The inbox job.
     * @param iPage
     *            The zero-based page number of the SafePage job.
     * @return The {@link InboxPageImageInfo}.
     */
    private static InboxPageImageInfo createInboxPageImageInfo(
            final InboxInfoDto.InboxJob job, final int iPage) {

        final InboxPageImageInfo dto = new InboxPageImageInfo();

        dto.setFile(job.getFile());
        dto.setLandscape(job.getLandscape().booleanValue());
        dto.setRotation(job.getRotation().intValue());

        dto.setPageInFile(iPage);

        final String rotate = job.getRotate();

        if (rotate == null) {
            dto.setRotate(PdfPageRotateHelper.PDF_ROTATION_0.intValue());
        } else {
            dto.setRotate(Integer.valueOf(rotate).intValue());
        }

        return dto;
    }

    @Override
    public InboxPageImageInfo getPageImageInfo(final String user,
            final String jobName, final int iPage) {

        final InboxInfoDto info = readInboxInfo(user);
        final List<InboxInfoDto.InboxJob> jobs = info.getJobs();

        for (final InboxInfoDto.InboxJob job : jobs) {
            if (job.getFile().equals(jobName)) {
                return createInboxPageImageInfo(job, iPage);
            }
        }
        return null;
    }

    @Override
    public InboxPageImageInfo getPageImageInfo(final String user,
            final int iPage) {

        final InboxInfoDto info = getInboxInfo(user);

        final List<InboxInfoDto.InboxJob> jobs = info.getJobs();

        int nPageWlk = 0;

        for (InboxJobRange page : info.getPages()) {

            List<RangeAtom> ranges = createSortedRangeArray(page.getRange());

            if (ranges.isEmpty()) {
                ranges = createSortedRangeArray("1-");
            }

            for (final RangeAtom atom : ranges) {

                final int nPageFrom;
                final int nPageTo;

                if (atom.pageBegin == null) {
                    nPageFrom = 1;
                } else {
                    nPageFrom = atom.pageBegin.intValue();
                }

                if (atom.pageEnd == null) {
                    nPageTo = jobs.get(page.getJob()).getPages();
                } else {
                    nPageTo = atom.pageEnd.intValue();
                }

                int nPagesInAtom = nPageTo - nPageFrom + 1;

                if (iPage < (nPageWlk + nPagesInAtom)) {

                    final Integer iPageInJob =
                            atom.pageBegin - 1 + (iPage - nPageWlk);

                    final InboxInfoDto.InboxJob job = jobs.get(page.getJob());

                    return createInboxPageImageInfo(job, iPageInJob.intValue());
                }
                nPageWlk += nPagesInAtom;
            }
        }
        return null;
    }

    @Override
    public int calcNumberOfPagesInJobs(final InboxInfoDto jobinfo) {
        return calcNumberOfPages(jobinfo.getJobs(), jobinfo.getPages());
    }

    /**
     * Calculates the number of selected pages in the document.
     *
     * @param selectedRanges
     *            The selected document ranges.
     * @param nTotPages
     *            The total number of pages in the document.
     * @return The number of pages.
     */
    public static int calcSelectedDocPages(final List<RangeAtom> selectedRanges,
            final int nTotPages) {

        int nPages = 0;

        for (RangeAtom atom : selectedRanges) {
            int nPageFrom = atom.pageBegin == null ? 1 : atom.pageBegin;
            int nPageTo = atom.pageEnd == null ? nTotPages : atom.pageEnd;
            nPages += nPageTo - nPageFrom + 1;
        }

        return nPages;
    }

    @Override
    public RangeAtom calcVanillaInboxJobRange(final InboxInfoDto inbox,
            final UUID uuidJob) {

        final String uuidSearch = uuidJob.toString();
        final ArrayList<InboxJob> jobs = inbox.getJobs();

        /*
         * Search the job with UUID.
         */
        InboxJob jobCandidate = null;
        int iJob;

        for (iJob = 0; iJob < jobs.size(); iJob++) {

            final InboxJob job = jobs.get(iJob);

            if (job.getFile().startsWith(uuidSearch)) {
                jobCandidate = job;
                break;
            }
        }

        if (jobCandidate == null) {
            // Job not found.
            return null;
        }

        /*
         * Collect all page ranges before the candidate job.
         */
        final ArrayList<InboxJobRange> rangesTillJob = new ArrayList<>();

        int jobRangesFound = 0;

        for (final InboxJobRange jobRange : inbox.getPages()) {

            if (jobRange.getJob().intValue() == iJob) {
                jobRangesFound++;
            } else {
                if (jobRangesFound == 0) {
                    rangesTillJob.add(jobRange);
                } else {
                    // Job is NOT vanilla.
                    break;
                }
            }
        }

        if (jobRangesFound > 1) {
            // Job is NOT vanilla.
            return null;
        }

        int nPagesStart = 1;

        if (rangesTillJob.size() > 0) {
            nPagesStart +=
                    this.calcNumberOfPages(inbox.getJobs(), rangesTillJob);
        }

        final RangeAtom rangeAtom = new RangeAtom();

        rangeAtom.pageBegin = nPagesStart;
        rangeAtom.pageEnd =
                nPagesStart + jobCandidate.getPages().intValue() - 1;

        return rangeAtom;
    }

    @Override
    public String toVanillaJobInboxRange(final InboxInfoDto jobInfo,
            final int iVanillaJobIndex,
            final List<RangeAtom> sortedRangeArrayJob) {

        /*
         * INVARIANT: jobInfo must be vanilla.
         */
        if (!this.isInboxVanilla(jobInfo)) {
            throw new IllegalStateException("Inbox is edited, job page "
                    + "Scope cannot be converted to inbox scope.");
        }

        final List<RangeAtom> rangeArrayInbox = new ArrayList<>();

        int iJob = 0;
        int nPagesWlk = 1;

        for (final InboxJob job : jobInfo.getJobs()) {

            final int nJobPages = job.getPages().intValue();

            if (iJob == iVanillaJobIndex) {

                for (final RangeAtom jobAtom : sortedRangeArrayJob) {

                    final RangeAtom inboxAtom = new RangeAtom();

                    if (jobAtom.pageBegin == null) {
                        inboxAtom.pageBegin = Integer.valueOf(nPagesWlk);
                    } else {
                        inboxAtom.pageBegin = Integer
                                .valueOf(nPagesWlk + jobAtom.pageBegin - 1);
                    }

                    if (jobAtom.pageEnd == null) {
                        inboxAtom.pageEnd =
                                Integer.valueOf(nPagesWlk + nJobPages - 1);
                    } else {
                        inboxAtom.pageEnd =
                                Integer.valueOf(inboxAtom.pageBegin.intValue()
                                        + jobAtom.calcPageTo(nJobPages)
                                        - jobAtom.calcPageFrom());
                    }

                    rangeArrayInbox.add(inboxAtom);
                }
                break;
            }

            nPagesWlk += nJobPages;
            iJob++;
        }

        return RangeAtom.asText(rangeArrayInbox);
    }

    @Override
    public int calcNumberOfPages(final List<InboxInfoDto.InboxJob> jobs,
            final List<InboxJobRange> pages) {

        int nPages = 0;

        for (InboxJobRange page : pages) {

            List<RangeAtom> ranges =
                    this.createSortedRangeArray(page.getRange());

            if (ranges.isEmpty()) {
                ranges = this.createSortedRangeArray("1-");
            }
            for (RangeAtom atom : ranges) {
                int nPageFrom = atom.pageBegin == null ? 1 : atom.pageBegin;
                int nPageTo = atom.pageEnd == null
                        ? jobs.get(page.getJob()).getPages() : atom.pageEnd;
                nPages += nPageTo - nPageFrom + 1;
            }
        }
        return nPages;
    }

    @Override
    public List<RangeAtom> createSortedRangeArray(final String rangesIn) {

        final List<RangeAtom> aRanges = new ArrayList<RangeAtom>();

        String ranges = rangesIn.trim();
        if (ranges.isEmpty()) {
            ranges = "1-";
        }

        Pattern p = Pattern.compile("^\\d+$");

        for (String value : ranges.split("\\,")) {

            String begin = null;
            String[] range = value.split("\\-", -1);
            switch (range.length) {
            case 1:
                begin = range[0].trim();
                if (!begin.isEmpty() && p.matcher(begin).matches()) {
                    RangeAtom atom = new RangeAtom();
                    atom.pageBegin = Integer.parseInt(begin);
                    atom.pageEnd = atom.pageBegin;
                    aRanges.add(atom);
                } else {
                    return null;
                }
                break;
            case 2:
                begin = range[0].trim();
                if (begin.isEmpty()) {
                    begin = "1";
                }

                String end = range[1].trim();
                if (end.isEmpty()) {
                    end = null;
                }

                if (!p.matcher(begin).matches()) {
                    return null;
                }

                if (end != null && !p.matcher(end).matches()) {
                    return null;
                }

                RangeAtom atom = new RangeAtom();
                atom.pageBegin = Integer.parseInt(begin);
                if (end != null) {
                    atom.pageEnd = Integer.parseInt(end);
                    if (atom.pageEnd < atom.pageBegin) {
                        // TODO: localize text
                        throw new SpException("range \"" + rangesIn
                                + "\" has invalid syntax");
                    }
                }
                aRanges.add(atom);
                break;
            default:
                return null;
            }
        }

        Collections.sort(aRanges, new Comparator<RangeAtom>() {
            @Override
            public int compare(final RangeAtom o1, final RangeAtom o2) {
                return o1.pageBegin.compareTo(o2.pageBegin);
            }
        });

        return aRanges;
    }

    /**
     * Checks if filename represents a scan job.
     *
     * @deprecated Scanning is not implemented (yet).
     * @param filename
     *            The name of the file.
     * @return true if filename represents a scan job.
     */
    @Deprecated
    public static boolean isScanJobFilename(final String filename) {
        return FilenameUtils.getExtension(filename)
                .equalsIgnoreCase(FILENAME_EXT_SCAN);
    }

    /**
     * Checks if filename represents a PostScript print job.
     *
     * @param filename
     *            The name of the file.
     * @return true if filename represents a print job.
     */
    public static boolean isPsJobFilename(final String filename) {
        return FilenameUtils.getExtension(filename)
                .equalsIgnoreCase(DocContent.FILENAME_EXT_PS);
    }

    /**
     * Checks if filename represents a PDF (print) job.
     *
     * @param filename
     *            The name of the file.
     * @return true if filename represents a print job.
     */
    public static boolean isPdfJobFilename(final String filename) {
        return FilenameUtils.getExtension(filename)
                .equalsIgnoreCase(DocContent.FILENAME_EXT_PDF);
    }

    @Override
    public boolean isSupportedJobType(final File file) {
        final String ext = FilenameUtils.getExtension(file.getName());
        return ext != null && (ext.equalsIgnoreCase(DocContent.FILENAME_EXT_PDF)
                || ext.equalsIgnoreCase(DocContent.FILENAME_EXT_PS)
                || ext.equalsIgnoreCase(FILENAME_EXT_SCAN));
    }

    @Override
    public PageImages getPageChunks(final String user,
            final Integer firstDetailPage, final String uniqueUrlValue,
            final boolean base64) {
        return InboxPageImageChunker.chunk(user, firstDetailPage,
                uniqueUrlValue, base64);
    }

    @Override
    public String getLetterheadsDir(final String user) {
        if (user == null) {
            return ConfigManager.getLetterheadDir();
        }
        return ConfigManager.getUserHomeDir(user) + "/" + LETTERHEADS_DIR_NAME;
    }

    @Override
    public String getLetterheadLocation(final User user) {
        if (user == null) {
            return getLetterheadsDir((String) null);
        }
        return getLetterheadsDir(user.getUserId());
    }

    @Override
    public Map<String, Object> getLetterheadList(final User userObj) {

        InboxInfoDto userJobInfo = null;
        String defaultLhFound = null;
        String defaultLhStored = null;

        if (userObj != null) {
            userJobInfo = readInboxInfo(userObj.getUserId());
            if (userJobInfo.getLetterhead() != null) {
                defaultLhStored = userJobInfo.getLetterhead().getId();
            }
        }

        LetterheadInfo letterheadInfo = getLetterheads(userObj);

        final List<Object> letterheads = new ArrayList<Object>();

        Iterator<LetterheadInfo.LetterheadJob> iter =
                letterheadInfo.getJobs().iterator();

        while (iter.hasNext()) {

            LetterheadInfo.LetterheadJob job = iter.next();

            Map<String, Object> props = new HashMap<String, Object>();
            props.put("id", job.getFile());
            props.put("name", job.getName());
            props.put("pub", job.isPublic());
            letterheads.add(props);

            /*
             * Check if the default matches a private letterhead.
             */
            if (defaultLhStored != null
                    && defaultLhStored.equals(job.getFile())) {
                defaultLhFound = job.getFile();
            }
        }

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("letterheads", letterheads);

        if (userObj != null) {
            /*
             * Recurse !!!
             */
            Map<String, Object> lhPublic = getLetterheadList(null);

            /*
             * Append public letterheads to private letterheads.
             */
            @SuppressWarnings("unchecked")
            List<Object> objects =
                    (ArrayList<Object>) lhPublic.get("letterheads");

            for (Object obj : objects) {

                letterheads.add(obj);

                /*
                 * Check if the default matches a public letterhead.
                 */
                if (defaultLhStored != null && defaultLhFound == null) {
                    @SuppressWarnings("unchecked")
                    String id =
                            ((Map<String, Object>) obj).get("id").toString();
                    if (defaultLhStored.equals(id)) {
                        defaultLhFound = id;
                    }
                }

            }

            /*
             * Default letterhead?
             */
            data.put("default", defaultLhFound);

            /*
             * Auto-correct dangling reference to default letterhead.
             */
            if (defaultLhStored != null && defaultLhFound == null) {
                userJobInfo.setLetterhead(null);
                storeInboxInfo(userObj.getUserId(), userJobInfo);
            }

        }

        return data;
    }

    @Override
    public File createLetterhead(final User user)
            throws PostScriptDrmException, LetterheadNotFoundException {

        File file;
        try {
            file = OutputProducer.instance().createLetterhead(
                    getLetterheadsDir(user.getUserId()), user);
        } catch (EcoPrintPdfTaskPendingException e) {
            throw new SpException(e.getMessage(), e);
        }

        attachLetterhead(user, file.getName(), false);

        return file;
    }

    @Override
    public void detachLetterhead(final String user) {

        final InboxInfoDto info = readInboxInfo(user);

        info.setLetterhead(null);

        storeInboxInfo(user, info);
    }

    @Override
    public void attachLetterhead(final User user, final String letterheadId,
            final boolean isPublic) throws LetterheadNotFoundException {

        final User userWrk;

        if (isPublic) {
            userWrk = null;
        } else {
            userWrk = user;
        }

        final LetterheadInfo.LetterheadJob ljob =
                getLetterhead(userWrk, letterheadId);

        if (ljob == null) {
            throw LetterheadNotFoundException.create(isPublic, letterheadId);
        }

        final InboxInfoDto info = readInboxInfo(user.getUserId());

        final InboxInfoDto.InboxLetterhead lh =
                new InboxInfoDto.InboxLetterhead();
        info.setLetterhead(lh);

        lh.setId(ljob.getFile());
        lh.setPub(isPublic);

        storeInboxInfo(user.getUserId(), info);
    }

    @Override
    public void setLetterhead(final User user, final String letterheadId,
            final String name, final boolean foreground, final boolean isPublic,
            final boolean isPublicNew)
            throws IOException, LetterheadNotFoundException {

        LetterheadInfo letterheadInfoPublic = null;
        LetterheadInfo letterheadInfoUser = null;
        LetterheadInfo letterheadInfo = null;

        if (isPublic != isPublicNew) {
            /*
             * We need both.
             */
            letterheadInfoPublic = getLetterheads(null);
            letterheadInfoUser = getLetterheads(user);

        } else if (isPublic) {
            letterheadInfo = getLetterheads(null);
        } else {
            letterheadInfo = getLetterheads(user);
        }

        if (letterheadInfo == null) {

            final String locationPublic = getLetterheadsDir(null);
            final String locationUser = getLetterheadLocation(user);

            if (isPublic) {

                /*
                 * Public -> Private.
                 */

                final LetterheadInfo.LetterheadJob job =
                        getLetterhead(letterheadId, letterheadInfoPublic);

                if (job == null) {
                    throw LetterheadNotFoundException.create(isPublic,
                            letterheadId);
                }

                FileUtils.moveFileToDirectory(
                        new File(locationPublic + "/" + letterheadId),
                        new File(locationUser), false);

                job.setPub(false);

                letterheadInfoUser.getJobs().add(job);
                letterheadInfoPublic.getJobs().remove(job);

            } else {

                /*
                 * Private -> Public.
                 */

                final LetterheadInfo.LetterheadJob job =
                        getLetterhead(letterheadId, letterheadInfoUser);

                if (job == null) {
                    throw LetterheadNotFoundException.create(isPublic,
                            letterheadId);
                }

                FileUtils.moveFileToDirectory(
                        new File(locationUser + "/" + letterheadId),
                        new File(locationPublic), false);

                job.setPub(true);

                letterheadInfoPublic.getJobs().add(job);
                letterheadInfoUser.getJobs().remove(job);
            }

            /*
             * Save both stores.
             */
            storeLetterheadInfo(locationUser, letterheadInfoUser);
            storeLetterheadInfo(locationPublic, letterheadInfoPublic);

        } else {

            LetterheadInfo.LetterheadJob job =
                    getLetterhead(letterheadId, letterheadInfo);

            if (job == null) {
                throw LetterheadNotFoundException.create(isPublic,
                        letterheadId);
            }

            job.setName(name);
            job.setForeground(foreground);

            String userTmp = null;

            if (!isPublic) {
                userTmp = user.getUserId();
            }
            storeLetterheadInfo(getLetterheadsDir(userTmp), letterheadInfo);

        }

    }

    @Override
    public void deleteLetterhead(final User userReq, final String letterheadId,
            final boolean isPublic) throws LetterheadNotFoundException {

        User userObj = userReq;

        if (isPublic) {
            userObj = null;
        }

        final LetterheadInfo letterheadInfo = getLetterheads(userObj);
        final Iterator<LetterheadInfo.LetterheadJob> iter =
                letterheadInfo.getJobs().iterator();

        boolean found = false;

        while (iter.hasNext()) {

            LetterheadInfo.LetterheadJob job = iter.next();

            if (job.getFile().equals(letterheadId)) {

                final String location = getLetterheadLocation(userObj);
                iter.remove();
                storeLetterheadInfo(location, letterheadInfo);

                final File file = new File(location + "/" + job.getFile());

                if (file.exists()) {
                    try {
                        FileUtils.forceDelete(file);
                    } catch (IOException e) {
                        throw new SpException("File [" + file.getAbsolutePath()
                                + "] could not be deleted", e);
                    }
                    found = true;
                }
                break;
            }
        }

        if (!found) {
            throw LetterheadNotFoundException.create(isPublic, letterheadId);
        }

        /*
         * If the deleted letterhead is selected by the requesting user, detach
         * it.
         */
        final String user = userReq.getUserId();

        final InboxInfoDto info = readInboxInfo(user);

        if (info.getLetterhead() != null && info.getLetterhead().getId() != null
                && info.getLetterhead().getId().equals(letterheadId)) {

            info.setLetterhead(null);
            storeInboxInfo(user, info);
        }
    }

    @Override
    public LetterheadInfo.LetterheadJob getLetterhead(final User user,
            final String letterheadId) {

        LetterheadInfo letterheadInfo = getLetterheads(user);
        return getLetterhead(letterheadId, letterheadInfo);
    }

    /**
     * Search a letterhead store for a letterhead with letterheadId.
     *
     * @param letterheadId
     *            The basename of the letterhead file.
     *
     * @param letterheadInfo
     *            The letterhead store to search in.
     *
     * @return The letterhead found, or <code>null</code> when not found.
     *
     */
    private LetterheadInfo.LetterheadJob getLetterhead(
            final String letterheadId, final LetterheadInfo letterheadInfo) {

        Iterator<LetterheadInfo.LetterheadJob> iter =
                letterheadInfo.getJobs().iterator();

        while (iter.hasNext()) {

            LetterheadInfo.LetterheadJob job = iter.next();

            if (job.getFile().equals(letterheadId)) {
                return job;
            }
        }
        return null;
    }

    @Override
    public Map<String, Object> getLetterheadDetails(final User userObj,
            final String letterheadId, final Boolean isPublic,
            boolean imgBase64) throws LetterheadNotFoundException {

        final String user = userObj.getUserId();

        final Map<String, Object> data = new HashMap<String, Object>();

        final List<Object> pages = new ArrayList<Object>();

        LetterheadInfo.LetterheadJob job;

        if (isPublic) {
            job = getLetterhead(null, letterheadId);
        } else {
            job = getLetterhead(userObj, letterheadId);
        }

        if (job == null) {
            throw LetterheadNotFoundException.create(isPublic, letterheadId);
        }

        ImageUrl imgUrl = new ImageUrl();

        imgUrl.setJob(job.getFile());
        imgUrl.setUser(user);
        imgUrl.setLetterhead(true);
        imgUrl.setLetterheadPublic(isPublic);
        imgUrl.setBase64(imgBase64);

        final int nPages = job.getPages();

        for (int i = 0; i < nPages; i++) {

            imgUrl.setPage(String.valueOf(i));

            Map<String, Object> props = new HashMap<String, Object>();

            props.put("url", imgUrl.composeImageUrl());
            props.put("pages", 1); // fixed

            pages.add(props);
        }

        data.put("pages", pages);
        data.put("pub", isPublic);
        data.put("foreground", (job == null) ? null : job.getForeground());

        return data;
    }

    @Override
    public LetterheadInfo getLetterheads(final User userObj) {

        final String user = userObj == null ? null : userObj.getUserId();

        final String workdir = getLetterheadsDir(user);

        File directory = new File(workdir);
        if (!directory.exists()) {
            try {
                FileUtils.forceMkdir(directory);
            } catch (IOException e) {
                throw new SpException(
                        "directory [" + workdir + "] could not be created.", e);
            }
        }

        LetterheadInfo jobs = readLetterheadInfo(workdir);

        int iJobOffset = jobs.getJobs().size();

        FileFilter filefilter = new FileFilter() {
            @Override
            public boolean accept(File file) {
                final String ext = FilenameUtils.getExtension(file.getName());
                return file.isFile() && ext != null
                        && (ext.equals(DocContent.FILENAME_EXT_PDF));
            }
        };

        File[] files = new File(workdir).listFiles(filefilter);

        if (null != files && files.length > 0) {

            // .....................................................
            // SORT ascending by creation time
            // .....................................................
            java.util.Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(final File o1, final File o2) {
                    final Long m1 = o1.lastModified();
                    final Long m2 = o2.lastModified();
                    return m1.compareTo(m2);
                }
            });

            for (int i = iJobOffset; i < files.length; i++) {

                final String filePath = files[i].getAbsolutePath();
                final String ext = FilenameUtils.getExtension(filePath);

                LetterheadInfo.LetterheadJob job =
                        new LetterheadInfo.LetterheadJob();
                job.setFile(FilenameUtils.getName(filePath));

                if (ext.equals(DocContent.FILENAME_EXT_PDF)) {
                    int nPages = getNumberOfPagesInPdfFile(filePath);
                    job.setPages(nPages);
                } else {
                    throw new SpException(
                            "[" + filePath + "] is not a PDF file");
                }

                job.setForeground(true);
                job.setName("untitled");
                job.setPub(userObj == null);

                /*
                 * Append
                 */
                jobs.getJobs().add(job);
            }

            if (iJobOffset < files.length) {
                storeLetterheadInfo(workdir, jobs);
            }

        }
        return jobs;
    }

    /**
     *
     * @return
     */
    private static String getLetterheadStoreFilePath(final String directory) {
        return directory + "/" + LETTERHEADS_DESCRIPT_FILE_NAME;
    }

    /**
     *
     * @param workdir
     * @return
     */
    private LetterheadInfo readLetterheadInfo(final String workdir) {

        final String filename = getLetterheadStoreFilePath(workdir);

        ObjectMapper mapper = new ObjectMapper();

        LetterheadInfo info = null;

        try {
            /*
             * First check if file exists, if not (first time use, or reset)
             * return an empty info object.
             */
            File file = new File(filename);
            if (file.exists()) {
                try {
                    info = mapper.readValue(file, LetterheadInfo.class);
                } catch (JsonMappingException e) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(
                                "Error mapping from file [" + filename + "]");
                    }
                }
            }

            if (info == null) {
                info = new LetterheadInfo();
            }

        } catch (JsonParseException e) {
            throw new SpException("Error parsing from file [" + filename + "]",
                    e);
        } catch (IOException e) {
            throw new SpException("Error reading file [" + filename + "]", e);
        }
        return info;
    }

    /**
     * Stores the letterhead store as json file in specified directory.
     *
     * @param directory
     *            The location of the letterhead store.
     * @param info
     *            The letterhead store.
     */
    private void storeLetterheadInfo(final String directory,
            final LetterheadInfo info) {
        final String filename =
                directory + "/" + LETTERHEADS_DESCRIPT_FILE_NAME;
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.writeValue(new File(filename), info);
        } catch (JsonGenerationException e) {
            throw new SpException("Error generating file [" + filename + "]",
                    e);
        } catch (JsonMappingException e) {
            throw new SpException("Error mapping to file [" + filename + "]",
                    e);
        } catch (IOException e) {
            throw new SpException("Error writing file [" + filename + "]", e);
        }
    }

    @Override
    public int deleteAllPages(final String user) {
        /*
         * TODO: there must be a more efficient way.
         */
        return deletePages(user, "1-"); // dirty trick
    }

    @Override
    public int deletePages(final String user, final String ranges) {
        final InboxInfoDto jobinfo = readInboxInfo(user);
        return InboxPageMover.deletePages(user, jobinfo, ranges);
    }

    @Override
    public int deleteJobPages(final String userId, final int iVanillaJobIndex,
            final String ranges) {

        final InboxInfoDto jobInfo = readInboxInfo(userId);

        final List<RangeAtom> sortedRangeArrayJob =
                this.createSortedRangeArray(ranges);

        return this.deletePages(userId, this.toVanillaJobInboxRange(jobInfo,
                iVanillaJobIndex, sortedRangeArrayJob));
    }

    @Override
    public int deleteJobs(final String userid,
            final List<ProxyPrintJobChunk> chunks) {

        final InboxInfoDto jobs = readInboxInfo(userid);

        final Set<Integer> removedJobs = new HashSet<>();

        for (final ProxyPrintJobChunk chunk : chunks) {

            for (final ProxyPrintJobChunkRange range : chunk.getRanges()) {

                final int iJob = range.getJob();
                /*
                 * Remove on first occurrence in collected jobs.
                 */
                if (removedJobs.add(Integer.valueOf(iJob))) {
                    this.removeJobPages(jobs, iJob);
                }
            }
        }

        if (!removedJobs.isEmpty()) {
            storeInboxInfo(userid, this.pruneJobs(
                    ConfigManager.getUserHomeDir(userid), userid, jobs));
        }

        return removedJobs.size();
    }

    @Override
    public int deleteJobs(final String userid, final long msecReferenceTime,
            final long msecExpiry) {

        final InboxInfoDto inboxInfo = readInboxInfo(userid);

        int nDeleted = 0;
        int iJob = 0;

        for (final InboxJob job : inboxInfo.getJobs()) {

            if (job.getCreatedTime().longValue()
                    + msecExpiry < msecReferenceTime) {
                this.removeJobPages(inboxInfo, iJob);
                nDeleted++;
            }
            iJob++;
        }

        if (nDeleted > 0) {
            storeInboxInfo(userid, this.pruneJobs(
                    ConfigManager.getUserHomeDir(userid), userid, inboxInfo));
        }

        return nDeleted;
    }

    /**
     * Removes job page ranges from the inbox {@link InboxInfoDto} object.
     *
     * @param jobs
     *            The {@link InboxInfoDto} object to remove from.
     * @param iJob
     *            The zero-based index of the job to remove.
     * @return The same {@link InboxInfoDto} object.
     */
    private InboxInfoDto removeJobPages(final InboxInfoDto jobs,
            final int iJob) {

        final ArrayList<InboxInfoDto.InboxJobRange> jobPagesNew =
                new ArrayList<>();

        for (final InboxJobRange range : jobs.getPages()) {
            if (range.getJob() != iJob) {
                jobPagesNew.add(range);
            }
        }
        jobs.setPages(jobPagesNew);

        return jobs;
    }

    @Override
    public void deleteJob(final String user, final int iJob) {
        storeInboxInfo(user, pruneJobs(ConfigManager.getUserHomeDir(user), user,
                this.removeJobPages(readInboxInfo(user), iJob)));
    }

    @Override
    public void editJob(final String user, final int iJob, final boolean rotate,
            final boolean undelete) {

        final InboxInfoDto jobs = readInboxInfo(user);

        //
        final Integer userRotate;

        if (rotate) {
            userRotate = PdfPageRotateHelper.PDF_ROTATION_90;
        } else {
            userRotate = PdfPageRotateHelper.PDF_ROTATION_0;
        }

        final InboxJob job = jobs.getJobs().get(iJob);
        job.setRotate(userRotate.toString());

        final int rotation;
        if (job.getRotation() == null) {
            rotation = 0;
        } else {
            rotation = job.getRotation().intValue();
        }

        final int contentRotation;
        if (job.getContentRotation() == null) {
            contentRotation = 0;
        } else {
            contentRotation = job.getContentRotation().intValue();
        }

        job.setLandscapeView(Boolean
                .valueOf(PdfPageRotateHelper.isSeenAsLandscape(contentRotation,
                        rotation, BooleanUtils.isTrue(job.getLandscape()),
                        userRotate.intValue())));
        //
        if (undelete) {

            final ArrayList<InboxInfoDto.InboxJobRange> jobPagesNew =
                    new ArrayList<>();

            boolean first = true;

            for (final InboxJobRange range : jobs.getPages()) {

                if (range.getJob() == iJob) {

                    if (first) {
                        range.setRange(RangeAtom.FULL_PAGE_RANGE);
                        first = false;
                        jobPagesNew.add(range);
                    }

                } else {
                    jobPagesNew.add(range);
                }
            }

            jobs.setPages(jobPagesNew);
        }

        storeInboxInfo(user, jobs);
    }

    @Override
    public int movePages(final String user, final String nRanges,
            final int nPage2Move2) {

        final InboxInfoDto jobinfo = readInboxInfo(user);

        return InboxPageMover.movePages(user, jobinfo, nRanges, nPage2Move2);
    }

    @Override
    public void pruneOrphanJobs(final String homedir, final User user) {

        final DocLogDao dao = ServiceContext.getDaoContext().getDocLogDao();
        final InboxInfoDto dto = this.readInboxInfo(user.getUserId());
        final Set<Integer> docLogAbsent = new HashSet<>();

        int iJob = 0;

        /*
         * Collect the zero-based index of the jobs that are NOT present in the
         * database.
         */
        for (final InboxJob job : dto.getJobs()) {

            final String uuid = FilenameUtils.getBaseName(job.getFile());

            final DocLog docLog = dao.findByUuid(user.getId(), uuid);

            if (docLog == null) {
                docLogAbsent.add(Integer.valueOf(iJob));
            }
            iJob++;
        }

        /*
         * An jobs absent?
         */
        if (!docLogAbsent.isEmpty()) {

            final Iterator<InboxJobRange> iter = dto.getPages().iterator();

            /*
             * Delete every InboxJobRange of an absent job.
             */
            while (iter.hasNext()) {

                final InboxJobRange range = iter.next();

                if (docLogAbsent.contains(range.getJob())) {
                    iter.remove();
                }
            }

            /*
             * Prune the jobs (this will actually delete the job files as well).
             */
            final InboxInfoDto prunedDto =
                    this.pruneJobs(homedir, user.getUserId(), dto);

            this.storeInboxInfo(user.getUserId(), prunedDto);
        }
    }

    @Override
    public InboxInfoDto pruneJobs(final String homedir, final String user,
            final InboxInfoDto jobs) {

        final InboxInfoDto pruned = new InboxInfoDto();

        // ----------------------------------------------
        boolean[] jobsPresent = new boolean[jobs.getJobs().size()];
        for (int i = 0; i < jobsPresent.length; i++) {
            jobsPresent[i] = false;
        }

        int nJobsPresent = 0;

        for (final InboxInfoDto.InboxJobRange page : jobs.getPages()) {
            jobsPresent[page.getJob()] = true;
            nJobsPresent++;
        }

        if (nJobsPresent == jobs.getJobs().size()) {
            return jobs;
        }

        // ----------------------------------------------
        final ArrayList<String> files2Unlink = new ArrayList<String>();

        pruned.setLetterhead(jobs.getLetterhead());
        pruned.setLastPreviewTime(jobs.getLastPreviewTime());

        final List<InboxInfoDto.InboxJob> prunedJobs = pruned.getJobs();
        final List<InboxInfoDto.InboxJobRange> prunedPages = pruned.getPages();

        // --------------------------------------------------------------------
        // Traverse the job presence, if present copy to pruned, if not delete.
        // --------------------------------------------------------------------
        final Map<Integer, Integer> iJobConvert =
                new HashMap<Integer, Integer>();

        for (int iNew = 0, i = 0; i < jobsPresent.length; i++) {

            if (jobsPresent[i]) {

                iJobConvert.put(i, iNew);
                prunedJobs.add(jobs.getJobs().get(i)); // copy
                iNew++;

            } else {

                final String filePath = String.format("%s%c%s", homedir,
                        File.separatorChar, jobs.getJobs().get(i).getFile());

                files2Unlink.add(filePath);
            }
        }
        // ---------------------------------------------------------
        // Re-create references in pages to job
        // ---------------------------------------------------------
        for (final InboxInfoDto.InboxJobRange page : jobs.getPages()) {
            // deep copy (just to be sure)
            InboxInfoDto.InboxJobRange prunedPage =
                    new InboxInfoDto.InboxJobRange();
            prunedPage.setJob(iJobConvert.get(page.getJob()));
            prunedPage.setRange(page.getRange());
            prunedPages.add(prunedPage);
        }

        // --------------------------------------------------------
        // Collect garbage
        // --------------------------------------------------------
        int nFilesDeleted = 0;

        for (final String filename : files2Unlink) {

            File file = new File(filename);

            if (file.exists()) {

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("PRUNE/DELETE [" + filename + "]");
                }

                try {
                    this.deleteJobFileAndRelated(file);
                } catch (IOException e) {
                    throw new SpException(
                            "File [" + filename + "] could not be deleted", e);
                }
                nFilesDeleted++;
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("[" + nFilesDeleted + "] job file(s) PRUNED/DELETED");
        }

        return pruned;
    }

    @Override
    public String createEcoPdfShadowPath(final String pdfPath) {
        return String.format("%s.%s", pdfPath, FILENAME_EXT_ECO);
    }

    /**
     * Deletes a job file and its related shadow files.
     *
     * @param file
     *            The main job file.
     * @throws IOException
     *             When IO error occurs.
     */
    private void deleteJobFileAndRelated(final File file) throws IOException {
        /*
         * Delete the main job file.
         */
        FileUtils.forceDelete(file);

        /*
         * Stop a running EcoPrint task.
         */
        final UUID uuid = getUuidFromInboxFile(file);

        if (ECOPRINT_SERVICE.cancelTask(uuid)) {

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                        "Stopped [" + uuid.toString() + "] EcoPrint task.");
            }

        } else {

            final File ecoFile =
                    new File(createEcoPdfShadowPath(file.getCanonicalPath()));

            if (ecoFile.exists()) {

                ecoFile.delete();

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Deleted [" + ecoFile.getCanonicalPath()
                            + "] file.");
                }
            }
        }
    }

    @Override
    public InboxInfoDto filterInboxInfoPages(final InboxInfoDto jobs,
            final String documentPageRangeFilter) {

        /*
         * Full range?
         */
        if (StringUtils.isBlank(documentPageRangeFilter)) {
            return jobs;
        }

        /*
         * Total number of pages in the document.
         */
        final int totDocPages = calcNumberOfPagesInJobs(jobs);

        /*
         * Read FIRST filter atom.
         */
        int nFilterPageFrom = 0;
        int nFilterPageTo = 0;

        final Iterator<RangeAtom> iterRangeAtomFilter =
                this.createSortedRangeArray(documentPageRangeFilter).iterator();

        RangeAtom rangeAtomFilterWlk = null;

        if (iterRangeAtomFilter.hasNext()) {
            rangeAtomFilterWlk = iterRangeAtomFilter.next();
            nFilterPageFrom = rangeAtomFilterWlk.calcPageFrom();
            nFilterPageTo = rangeAtomFilterWlk.calcPageTo(totDocPages);
        }

        /*
         * Full range?
         */
        if (nFilterPageFrom == 1 && nFilterPageTo == totDocPages) {
            return jobs;
        }

        /*
         * Read FIRST page atom
         */
        final Iterator<InboxInfoDto.InboxJobRange> iterJobPages =
                jobs.getPages().iterator();

        InboxInfoDto.InboxJobRange jobPagesWlk = null;
        InboxInfoDto.InboxJob jobWlk = null;

        RangeAtom jobRangeAtomWlk = null;

        int nDocPageFrom = 0;
        int nDocPageTo = 0;

        int nFilterJobPageFrom = 0;
        int nFilterJobPageTo = 0;

        Iterator<RangeAtom> jobIterRangeAtom = null;

        if (iterJobPages.hasNext()) {

            jobPagesWlk = iterJobPages.next();
            jobWlk = jobs.getJobs().get(jobPagesWlk.getJob());

            jobIterRangeAtom = this
                    .createSortedRangeArray(jobPagesWlk.getRange()).iterator();

            if (jobIterRangeAtom.hasNext()) {

                jobRangeAtomWlk = jobIterRangeAtom.next();

                jobRangeAtomWlk.pageBegin = jobRangeAtomWlk.calcPageFrom();
                jobRangeAtomWlk.pageEnd =
                        jobRangeAtomWlk.calcPageTo(jobWlk.getPages());

                nDocPageFrom = nDocPageTo + 1;
                nDocPageTo +=
                        jobRangeAtomWlk.pageEnd - jobRangeAtomWlk.pageBegin + 1;
            }

        }

        if (jobRangeAtomWlk != null) {
            /*
             * Mapping filter page numbers to job page numbers.
             */
            nFilterJobPageFrom = jobRangeAtomWlk.pageBegin
                    + (nFilterPageFrom - nDocPageFrom);

            nFilterJobPageTo =
                    jobRangeAtomWlk.pageEnd - (nDocPageTo - nFilterPageTo);
        }

        /*
         * Filtered ranges are accumulated on this array list.
         */
        final ArrayList<InboxJobRange> filteredJobRanges = new ArrayList<>();

        /*
         * Balanced line between filter and page atoms.
         */
        while (jobRangeAtomWlk != null && rangeAtomFilterWlk != null) {

            boolean bReadNextDocAtom = false;
            boolean bReadNextFilterAtom = false;

            RangeAtom filteredAtom = null;

            if (nFilterPageTo < nDocPageFrom) {
                /*
                 * Doc .........|-----------|
                 *
                 * Filter .|--|
                 */
                bReadNextFilterAtom = true;

            } else if (nFilterPageTo == nDocPageFrom
                    && nFilterPageFrom < nDocPageFrom) {
                /*
                 * Doc .........|-----------|
                 *
                 * Filter ...|--|
                 */
                filteredAtom = new RangeAtom();
                filteredAtom.pageBegin = jobRangeAtomWlk.pageBegin;
                filteredAtom.pageEnd = jobRangeAtomWlk.pageBegin;

                // split
                jobRangeAtomWlk.pageBegin++;
                nDocPageFrom++;

                //
                bReadNextFilterAtom = true;

            } else if (nFilterPageFrom < nDocPageFrom
                    && nDocPageFrom < nFilterPageTo
                    && nFilterPageTo < nDocPageTo) {

                /*
                 * Doc .........|-----------|
                 *
                 * Filter ..|---------|
                 */
                filteredAtom = new RangeAtom();
                filteredAtom.pageBegin = jobRangeAtomWlk.pageBegin;
                filteredAtom.pageEnd = nFilterJobPageTo;

                // split
                jobRangeAtomWlk.pageBegin = nFilterJobPageTo + 1;
                nDocPageFrom = nFilterPageTo + 1;

                //
                bReadNextFilterAtom = true;

            } else if (nFilterPageFrom < nDocPageFrom
                    && nDocPageTo < nFilterPageTo) {
                /*
                 * Doc .........|-----------|
                 *
                 * Filter ..|-------------------|
                 */
                filteredAtom = new RangeAtom();
                filteredAtom.pageBegin = jobRangeAtomWlk.pageBegin;
                filteredAtom.pageEnd = jobRangeAtomWlk.pageEnd;

                // split
                rangeAtomFilterWlk.pageBegin = nDocPageTo + 1;
                nFilterPageFrom = nDocPageTo + 1;

                //
                bReadNextDocAtom = true;

            } else if (nFilterPageFrom == nDocPageFrom
                    && nFilterPageTo < nDocPageTo) {
                /*
                 * Doc .........|-----------|
                 *
                 * Filter ......|-----|
                 */
                filteredAtom = new RangeAtom();
                filteredAtom.pageBegin = jobRangeAtomWlk.pageBegin;
                filteredAtom.pageEnd = nFilterJobPageTo;

                // split
                jobRangeAtomWlk.pageBegin = nFilterJobPageTo + 1;
                nDocPageFrom = nFilterPageTo + 1; // Mantis #440

                //
                bReadNextFilterAtom = true;

            } else if (nFilterPageFrom == nDocPageFrom
                    && nFilterPageTo == nDocPageTo) {
                /*
                 * Doc .........|-----------|
                 *
                 * Filter ......|-----------|
                 */
                filteredAtom = new RangeAtom();
                filteredAtom.pageBegin = jobRangeAtomWlk.pageBegin;
                filteredAtom.pageEnd = jobRangeAtomWlk.pageEnd;

                bReadNextFilterAtom = true;
                bReadNextDocAtom = true;

            } else if (nFilterPageFrom == nDocPageFrom
                    && nDocPageTo < nFilterPageTo) {
                /*
                 * Doc .........|-----------|
                 *
                 * Filter ......|------------------|
                 */
                filteredAtom = new RangeAtom();
                filteredAtom.pageBegin = jobRangeAtomWlk.pageBegin;
                filteredAtom.pageEnd = jobRangeAtomWlk.pageEnd;

                // split
                rangeAtomFilterWlk.pageBegin = nDocPageTo + 1;
                nFilterPageFrom = nDocPageTo + 1;

                //
                bReadNextDocAtom = true;

            } else if (nDocPageFrom < nFilterPageFrom
                    && nFilterPageTo < nDocPageTo) {
                /*
                 * Doc .........|-----------|
                 *
                 * Filter .........|-----|
                 */
                filteredAtom = new RangeAtom();
                filteredAtom.pageBegin = nFilterJobPageFrom;
                filteredAtom.pageEnd = nFilterJobPageTo;

                // split
                jobRangeAtomWlk.pageBegin = nFilterJobPageTo + 1;
                nDocPageFrom = nFilterPageTo + 1;

                //
                bReadNextFilterAtom = true;

            } else if (nDocPageFrom < nFilterPageFrom
                    && nDocPageTo == nFilterPageTo) {
                /*
                 * Doc .........|-----------|
                 *
                 * Filter ...........|------|
                 */
                filteredAtom = new RangeAtom();
                filteredAtom.pageBegin = nFilterJobPageFrom;
                filteredAtom.pageEnd = jobRangeAtomWlk.pageEnd;

                bReadNextFilterAtom = true;
                bReadNextDocAtom = true;

            } else if (nDocPageFrom < nFilterPageFrom
                    && nFilterPageFrom < nDocPageTo
                    && nDocPageTo < nFilterPageTo) {
                /*
                 * Doc .........|-----------|
                 *
                 * Filter ...........|---------|
                 */
                filteredAtom = new RangeAtom();
                filteredAtom.pageBegin = nFilterJobPageFrom;
                filteredAtom.pageEnd = jobRangeAtomWlk.pageEnd;

                // split
                rangeAtomFilterWlk.pageBegin = nDocPageTo + 1;
                nFilterPageFrom = nDocPageTo + 1;

                //
                bReadNextDocAtom = true;

            } else if (nDocPageTo == nFilterPageFrom) {
                /*
                 * Doc .........|-----------|
                 *
                 * Filter ..................|---|
                 */
                filteredAtom = new RangeAtom();
                filteredAtom.pageBegin = jobRangeAtomWlk.pageEnd;
                filteredAtom.pageEnd = jobRangeAtomWlk.pageEnd;

                // split
                rangeAtomFilterWlk.pageBegin = nDocPageTo + 1;
                nFilterPageFrom = nDocPageTo + 1;

                //
                bReadNextDocAtom = true;

            } else if (nDocPageTo < nFilterPageFrom) {
                /*
                 * Doc .........|-----------|
                 *
                 * Filter .....................|---|
                 */
                bReadNextDocAtom = true;
            }

            if (filteredAtom != null) {

                InboxJobRange filteredRange = new InboxJobRange();
                filteredJobRanges.add(filteredRange);

                filteredRange.setJob(jobPagesWlk.getJob());
                filteredRange.setRange(filteredAtom.asText());
            }

            /*
             * Read NEXT page atom
             */
            if (bReadNextDocAtom) {

                jobRangeAtomWlk = null;

                if (!jobIterRangeAtom.hasNext()) {

                    jobIterRangeAtom = null;

                    if (iterJobPages.hasNext()) {

                        jobPagesWlk = iterJobPages.next();
                        jobWlk = jobs.getJobs().get(jobPagesWlk.getJob());
                        jobIterRangeAtom = this
                                .createSortedRangeArray(jobPagesWlk.getRange())
                                .iterator();
                    }
                }

                if (jobIterRangeAtom != null && jobIterRangeAtom.hasNext()) {

                    jobRangeAtomWlk = jobIterRangeAtom.next();
                    jobRangeAtomWlk.pageBegin = jobRangeAtomWlk.calcPageFrom();
                    jobRangeAtomWlk.pageEnd =
                            jobRangeAtomWlk.calcPageTo(jobWlk.getPages());
                    nDocPageFrom = nDocPageTo + 1;
                    nDocPageTo += jobRangeAtomWlk.pageEnd
                            - jobRangeAtomWlk.pageBegin + 1;
                }

            }

            /*
             * Read NEXT filter atom
             */
            if (bReadNextFilterAtom) {
                rangeAtomFilterWlk = null;

                if (iterRangeAtomFilter.hasNext()) {

                    rangeAtomFilterWlk = iterRangeAtomFilter.next();
                    nFilterPageFrom = rangeAtomFilterWlk.calcPageFrom();
                    nFilterPageTo = rangeAtomFilterWlk.calcPageTo(totDocPages);
                }
            }

            /*
             * Mapping filter page numbers to job page numbers
             */
            if (jobRangeAtomWlk != null
                    && (bReadNextDocAtom || bReadNextFilterAtom)) {

                nFilterJobPageFrom = jobRangeAtomWlk.pageBegin
                        + (nFilterPageFrom - nDocPageFrom);

                nFilterJobPageTo =
                        jobRangeAtomWlk.pageEnd - (nDocPageTo - nFilterPageTo);
            }

        }

        /*
         * Compose new job info.
         */
        final InboxInfoDto jobInfoNew = new InboxInfoDto();

        jobInfoNew.setJobs(jobs.getJobs());
        jobInfoNew.setLastPreviewTime(jobs.getLastPreviewTime());
        jobInfoNew.setLetterhead(jobs.getLetterhead());

        jobInfoNew.setPages(filteredJobRanges);

        return InboxPageMover.optimizeJobs(jobInfoNew);
    }

    /**
     * Creates a string representation of an array of pages used for logging.
     *
     * @param pages
     * @return
     */
    public static String
            getPageRangeAsText(final List<InboxInfoDto.InboxJobRange> pages) {
        String txt = "";
        boolean first = true;
        for (InboxInfoDto.InboxJobRange page : pages) {
            if (first) {
                first = false;
            } else {
                txt += '|';
            }
            txt += page.getJob() + ":" + page.getRange();
        }
        return txt;
    }

    @Override
    public boolean isInboxVanilla(final InboxInfoDto jobInfo) {

        /*
         * Vanilla MUST have one (1) page range per job.
         */
        if (jobInfo.getJobs().size() != jobInfo.getPages().size()) {
            return false;
        }

        /*
         * For Vanilla each page range MUST include all pages and be in line
         * with job ordinal.
         */
        int iJobRange = 0;

        for (final InboxJobRange jobRange : jobInfo.getPages()) {

            if (jobRange.getJob().intValue() != iJobRange) {
                return false;
            }

            final RangeAtom atom = RangeAtom.fromText(jobRange.getRange());

            if (atom.pageBegin != null && atom.pageBegin.intValue() > 1) {
                return false;
            }

            if (atom.pageEnd != null && atom.pageEnd.intValue() != jobInfo
                    .getJobs().get(iJobRange).getPages().intValue()) {
                return false;
            }

            iJobRange++;
        }

        return true;
    }

    @Override
    public InboxInfoDto pruneForFastProxyPrint(final InboxInfoDto jobInfo,
            final Date expiryRef, final int expiryMins) {

        /*
         * Return when jobs are absent.
         */
        if (jobInfo.getJobs().isEmpty()) {
            return jobInfo;
        }

        /*
         * Time parameters.
         */
        final long expiryRefTime = expiryRef.getTime();
        final long expiryTimePeriod = expiryMins * 60 * 1000;

        /*
         * If the user previewed the inbox within the expiration window, the
         * complete (edited) job info can be fast proxy printed as it is.
         */
        final Long lastPreviewTime = jobInfo.getLastPreviewTime();

        /*
         * Beware of older versions without modified time.
         */
        if (lastPreviewTime != null) {

            final long deltaTimePeriod =
                    expiryRefTime - lastPreviewTime.longValue();

            if (deltaTimePeriod <= expiryTimePeriod) {
                return jobInfo;
            }
        }

        /*
         * Check if ALL jobs are vanilla.
         */
        final boolean isVanillaJobs = isInboxVanilla(jobInfo);

        /*
         * Initialize the pruned InboxInfoDto.
         */
        final InboxInfoDto fastInboxInfo = new InboxInfoDto();

        fastInboxInfo.setLetterhead(jobInfo.getLetterhead());
        fastInboxInfo.setLastPreviewTime(jobInfo.getLastPreviewTime());

        final ArrayList<InboxJob> fastJobs = new ArrayList<>();
        fastInboxInfo.setJobs(fastJobs);

        final ArrayList<InboxJobRange> fastPages = new ArrayList<>();
        fastInboxInfo.setPages(fastPages);

        /*
         * Add ALL jobs, but NOT the ranges (pages).
         */
        for (final InboxJob job : jobInfo.getJobs()) {
            fastJobs.add(job);
        }

        /*
         * Are we vanilla, or has user edited the inbox?
         */
        if (!isVanillaJobs) {
            /*
             * User edited the inbox: prune ALL jobs.
             */
            return fastInboxInfo;
        }

        /*
         * Traverse the vanilla jobs: add the full page range of jobs which are
         * not expired.
         */
        int iJobRange = 0;

        for (final InboxJob job : jobInfo.getJobs()) {

            final Long jobCreatedTime = job.getCreatedTime();

            /*
             * Do not handle jobs without creation time (older versions) so they
             * get pruned.
             */
            if (jobCreatedTime != null) {

                final long deltaTimePeriod =
                        expiryRefTime - jobCreatedTime.longValue();

                /*
                 * Add the full page range if job is not expired.
                 */
                if (deltaTimePeriod <= expiryTimePeriod) {

                    final InboxJobRange range = new InboxJobRange();

                    range.setJob(Integer.valueOf(iJobRange));
                    range.setRange(RangeAtom.FULL_PAGE_RANGE);

                    fastPages.add(range);
                }
            }
            iJobRange++;
        }

        /*
         * Return the original input jobInfo if end-result is identical (nothing
         * was pruned).
         */
        if (fastJobs.size() == fastPages.size()) {
            return jobInfo;
        }

        return fastInboxInfo;
    }

    @Override
    public InboxInfoDto pruneForFastProxyPrint(final String userId,
            final Date expiryRef, final int expiryMins) {

        final InboxInfoDto currentInfo = this.getInboxInfo(userId);

        final InboxInfoDto prunedInfoFast =
                this.pruneForFastProxyPrint(currentInfo, expiryRef, expiryMins);

        /*
         * If SAME object is returned, we know nothing was changed.
         */
        if (currentInfo == prunedInfoFast) {
            return currentInfo;
        }

        /*
         * Prune the jobs (this will actually delete the pruned job files in the
         * user home directory as well) and store them.
         */
        final InboxInfoDto prunedInfoPersist = this.pruneJobs(
                ConfigManager.getUserHomeDir(userId), userId, prunedInfoFast);

        this.storeInboxInfo(userId, prunedInfoPersist);

        return prunedInfoPersist;
    }

    /**
     * Updates the last preview time of the {@link InboxInfoDto} object with the
     * transaction time from the {@link ServiceContext}.
     *
     * @since 0.9.6
     * @param dto
     *            The {@link InboxInfoDto} to touch.
     */
    private void touchLastPreviewTime(final InboxInfoDto dto) {
        dto.setLastPreviewTime(
                Long.valueOf(ServiceContext.getTransactionDate().getTime()));
    }

    @Override
    public InboxInfoDto touchLastPreviewTime(final String userId) {

        final InboxInfoDto dto = this.readInboxInfo(userId);
        this.touchLastPreviewTime(dto);
        this.storeInboxInfo(userId, dto);

        return dto;
    }

    @Override
    public IppMediaSizeEnum getIppMediaSize(final String media) {

        final MediaSizeName sizeName;

        if (StringUtils.isNotBlank(media)) {
            sizeName =
                    MediaUtils.getMediaSizeFromInboxMedia(media.toLowerCase());
        } else {
            sizeName = null;
        }

        final IppMediaSizeEnum ippMediaSize;

        if (sizeName != null) {
            ippMediaSize = IppMediaSizeEnum.find(sizeName);
        } else {
            ippMediaSize = null;
        }

        return ippMediaSize;
    }

    @Override
    public IppMediaSizeEnum
            checkSingleInboxMedia(final InboxInfoDto inboxInfo) {

        String media = null;

        for (final InboxJob job : inboxInfo.getJobs()) {

            if (media == null) {
                media = job.getMedia();
            } else if (!media.equals(job.getMedia())) {
                media = null;
                break;
            }
        }

        final IppMediaSizeEnum ippMediaSize;

        if (media == null) {
            ippMediaSize = null;
        } else {
            ippMediaSize = this.getIppMediaSize(media);
        }

        return ippMediaSize;
    }

    @Override
    public ArrayList<InboxJobRange> replaceInboxInfoPages(
            final InboxInfoDto inboxInfo,
            final List<ProxyPrintJobChunkRange> chunkRanges) {

        final ArrayList<InboxJobRange> orgPages = inboxInfo.getPages();
        final ArrayList<InboxJobRange> chunkPages = new ArrayList<>();
        inboxInfo.setPages(chunkPages);

        for (final ProxyPrintJobChunkRange chunkRange : chunkRanges) {

            final InboxJobRange jobRange = new InboxJobRange();
            chunkPages.add(jobRange);

            jobRange.setJob(chunkRange.getJob());
            jobRange.setRange(chunkRange.asText());
        }
        return orgPages;
    }

    /**
     * Get the {@link UUID} from the inbox file, which is the file basename.
     *
     * @param file
     *            The inbox file.
     * @return The {@link UUID}.
     */
    private UUID getUuidFromInboxFile(final File file) {
        return UUID.fromString(FilenameUtils.getBaseName(file.getName()));
    }

    @Override
    public void startEcoPrintPdfTask(final String homedir, final File pdfIn,
            final UUID uuid) {

        final EcoPrintPdfTaskInfo info = new EcoPrintPdfTaskInfo(uuid);

        info.setPdfIn(pdfIn);

        final Path pathTargetEco = FileSystems.getDefault().getPath(homedir,
                String.format("%s.%s", pdfIn.getName(), FILENAME_EXT_ECO));

        info.setPdfOut(pathTargetEco.toFile());

        /*
         * Use the application's temp dir, since it is cleaned when the
         * application is started.
         */
        info.setPathTmpDir(
                FileSystems.getDefault().getPath(ConfigManager.getAppTmpDir()));

        info.setResolution(Integer.valueOf(ConfigManager.instance()
                .getConfigInt(Key.ECO_PRINT_RESOLUTION_DPI)));

        ECOPRINT_SERVICE.submitTask(info);
    }

    @Override
    public int lazyStartEcoPrintPdfTasks(final String homedir,
            final InboxInfoDto inboxInfo) {
        /*
         * Traverse the page ranges to collect the unique jobs.
         */
        final Set<Integer> jobsSet = new HashSet<>();

        for (InboxJobRange page : inboxInfo.getPages()) {
            jobsSet.add(page.getJob());
        }

        /*
         * Check presence of EcoPrint shadow.
         */
        int nTasksBusy = 0;
        int nTasksStarted = 0;

        final Iterator<Integer> iter = jobsSet.iterator();

        while (iter.hasNext()) {

            final InboxJob job =
                    inboxInfo.getJobs().get(iter.next().intValue());
            /*
             * The base name of the file is the UUID as registered in the
             * database (DocIn table).
             */
            final String uuid = FilenameUtils.getBaseName(job.getFile());

            final Path pdfPath = FileSystems.getDefault().getPath(homedir,
                    String.format("%s.%s", uuid, DocContent.FILENAME_EXT_PDF));

            final File fileEco =
                    new File(this.createEcoPdfShadowPath(pdfPath.toString()));

            /*
             * Is EcoPrint task in queue or busy executing?
             */
            if (ECOPRINT_SERVICE.hasTask(UUID.fromString(uuid))) {
                nTasksBusy++;
            } else if (!fileEco.exists()) {
                this.startEcoPrintPdfTask(homedir, pdfPath.toFile(),
                        UUID.fromString(uuid));
                nTasksStarted++;
            }
        }

        return nTasksBusy + nTasksStarted;
    }

    @Override
    public Long getLastPrintInTime(final String userId) throws IOException {

        final File userdir = new File(ConfigManager.getUserHomeDir(userId));

        if (!userdir.exists()) {
            return null;
        }

        final MutableLong lastTime = new MutableLong(-1L);

        final SimpleFileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(final Path file,
                    final BasicFileAttributes attrs) throws IOException {

                if (isPdfJobFilename(file.toString())) {
                    final long time = file.toFile().lastModified();
                    if (time > lastTime.longValue()) {
                        lastTime.setValue(time);
                    }
                }

                return CONTINUE;
            }
        };

        Files.walkFileTree(ConfigManager.getJobTicketsHome(), visitor);

        if (lastTime.longValue() > 0) {
            return lastTime.toLong();
        }

        return null;
    }

}
